package lsfusion.server.data.query.exec.materialize;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.base.col.interfaces.mutable.add.MAddExclMap;
import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.query.exec.DynamicExecEnvOuter;
import lsfusion.server.data.query.exec.DynamicExecEnvSnapshot;
import lsfusion.server.data.query.exec.DynamicExecuteEnvironment;
import lsfusion.server.data.query.exec.TypeExecuteEnvironment;
import lsfusion.server.data.sql.SQLCommand;
import lsfusion.server.data.sql.SQLQuery;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.connection.ExConnection;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.stat.Cost;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.parse.ParseInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.log.ServerLoggers;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.function.Function;

public class AdjustMaterializedExecuteEnvironment extends DynamicExecuteEnvironment<ImMap<SQLQuery, MaterializedQuery>, AdjustMaterializedExecuteEnvironment.Snapshot> {

    private Step current;

    private static class SubQueryContext {
        private final SQLQuery query;

        public SubQueryContext(SQLQuery query) {
            this.query = query;
        }

        public String toString() {
            return query.hashCode() +  " " + query.toString();
        }

        public void toStrings(MList<String> mList) {
            mList.add(toString());
        }
    }

    private static class SubQueryUpContext extends SubQueryContext {
        private final Step step;
        private final AdjustMaterializedExecuteEnvironment env;

        public SubQueryUpContext(Step step, SQLQuery query, AdjustMaterializedExecuteEnvironment env) {
            super(query);
            this.step = step;
            this.env = env;
        }

        @Override
        public void toStrings(MList<String> mList) {
            env.toStrings(mList);
            mList.add(step + " " + toString());
        }
    }

    private final SubQueryContext context;

    private AdjustMaterializedExecuteEnvironment(SubQueryContext context) {
        this.context = context;
    }

    public AdjustMaterializedExecuteEnvironment(SQLQuery query) {
        this(new SubQueryContext(query));
    }

    public AdjustMaterializedExecuteEnvironment(SubQueryUpContext upContext) {
        this((SubQueryContext)upContext);
    }

    public TypeExecuteEnvironment getType() {
        return TypeExecuteEnvironment.MATERIALIZE;
    }

    private boolean assertNoRecheckBefore() {
        Step previous = current.previous;
        while (previous != null) {
            ServerLoggers.assertLog(!previous.recheck, "NO RECHECK"); // из-за race condition'ов в createNavigator -> applyFormDefinedUserPolicy
            previous = previous.previous;
        }
        return true;
    }

    private ImSet<SQLQuery> outerQueries; // для assertion'а

    private boolean assertSameMaterialized(SQLCommand command, Step previousStep, ImSet<SQLQuery> materialized) {
        ImSet<SQLQuery> queries;
        if(previousStep == null)
            queries = SetFact.EMPTY();
        else
            queries = previousStep.getMaterializedQueries();
        ServerLoggers.assertLog(BaseUtils.hashEquals(SetFact.addExclSet(outerQueries, queries), materialized), "SHOULD ALWAYS BE EQUAL");
        return true;
    }

    @Override
    public synchronized Snapshot getSnapshot(SQLCommand command, int transactTimeout, DynamicExecEnvOuter<ImMap<SQLQuery, MaterializedQuery>, Snapshot> outerEnv) {
        Step previousStep;
        Snapshot snapshot = outerEnv.getSnapshot();
        if(snapshot == null) // "верхний"
            previousStep = null;
        else
            previousStep = snapshot.step;

        ImMap<SQLQuery, MaterializedQuery> materializedQueries = outerEnv.getOuter();
        if(materializedQueries == null)
            materializedQueries = MapFact.EMPTY();

        if(current == null) { // first start
            current = new Step(null, SetFact.EMPTYORDER(), false);
            outerQueries = materializedQueries.keys();
            setDefaultTimeout(current, command, materializedQueries);
        }

        ServerLoggers.assertLog(current.recheck, "RECHECK");
        assertNoRecheckBefore(); // assert что предыдущие не recheck

        Step nextStep = current;
        if(previousStep != null && nextStep.getIndex() < previousStep.getIndex()) // если current "вернулся назад", просто идем вперед, назад никогда смысла идти нет
            nextStep = getExecuteNextStep(previousStep, command, snapshot);
        return new Snapshot(nextStep, previousStep, command, transactTimeout, materializedQueries);
    }

    public synchronized void succeeded(SQLCommand command, Snapshot snapshot, long runtime, DynamicExecEnvOuter<ImMap<SQLQuery, MaterializedQuery>, Snapshot> outerEnv) {
        if(snapshot.noHandled || snapshot.isTransactTimeout)
            return;

        Step step = snapshot.step;

        long stepTimeout = step.getTimeout();
        if(stepTimeout != 0 && runtime > stepTimeout && stepTimeout < snapshot.secondsFromTransactStart * 1000) { // если на самом деле больше timeout, то по сути можно считать failed
            innerFailed(command, snapshot, " [SUCCEEDED - RUN : " + runtime + ", STEP : " + stepTimeout + ", TRANSACT : " + snapshot.secondsFromTransactStart * 1000 + "]");
            return;
        }

        // пытаемся вернуться назад
        long totalTime = runtime;
        int coeff = Settings.get().getLastStepCoeff();
        Step runBack = step;
        assertNoRecheckBefore();
        MOrderExclSet<Step> mBackSteps = SetFact.mOrderExclSet();
        while (true) {
            totalTime += runBack.getMaterializeTime(snapshot.getMaterializedQueries()); // достаточно inner

            runBack = runBack.previous;
            if(runBack == null)
                break;

            long adjtime = totalTime; // millis -> seconds
            if(adjtime > runBack.getTimeout() * coeff) { // если время выполнения превысило время материализации, пробуем с увеличенным timeout'ом
                runBack.setTimeout((int) adjtime);
                mBackSteps.exclAdd(runBack);
                runBack.recheck = true;
                current = runBack;
            }
        }

        boolean failedBefore = outerEnv.getSnapshot() != null; // не очень красиво конечно
        ImOrderSet<Step> backSteps = mBackSteps.immutableOrder();
        if(failedBefore || !backSteps.isEmpty())
            log("SUCCEEDED" + (failedBefore ? " (AFTER FAILURE)" : "" ) + " TIME (" + (runtime) + " OF " + snapshot.setTimeout + ")" + (snapshot.usedPessQuery()?" USED PESSIMISTIC QUERY":"") + (backSteps.isEmpty() ? "" : " - BACK"), step, backSteps);
    }

    public void innerFailed(SQLCommand command, Snapshot snapshot, String outerMessage) {
        // если current до, ничего не трогаем и так идет recheck
        // если current после, тоже ничего не трогаем, current и так уже подвинут
        if(BaseUtils.hashEquals(current, snapshot.step)) { // подвигаем current до следующего recheck'а
            current.recheck = false; // при succeded'е он и так сбросится, так что на всякий случай
            Step nextStep = getExecuteNextStep(current, command, snapshot);
            log("FAILED TIMEOUT (" + snapshot.setTimeout + ")" + outerMessage + " - NEXT", current, SetFact.singletonOrder(nextStep));
            current = nextStep;
        }
    }
    public synchronized void failed(SQLCommand command, Snapshot snapshot) {
        assert !snapshot.noHandled;
        if(snapshot.isTransactTimeout)
            return;
        innerFailed(command, snapshot, "");
    }

    private void toStrings(MList<String> mList) {
        context.toStrings(mList);
    }

    private void log(String message, Step baseStep, ImOrderSet<Step> changeTo) {
        // step + его timeout
        MList<String> mList = ListFact.mList();
        toStrings(mList);
        mList.add(message + (changeTo.isEmpty() ? "" : " FROM") + " : " + baseStep);
        for(Step change : changeTo)
            mList.add(" TO : " + change);
        ServerLoggers.adjustLog(mList.immutableList(), true);
    }
    
    private static void checkLastStep(Step step, SQLCommand command, ImMap<SQLQuery, MaterializedQuery> materializedQueries) {
        if(Settings.get().isNoDisablingNestedLoop()) {
            Step nextStep = getCachedNextStep(step, command, step.subQueries.getSet().addExcl(materializedQueries.keys())); // по идее коррелировано с assertSameMaterialized
            if (nextStep.isLastStep())
                step.setTimeout(ALOT);
        }
    }

    public static final long ALOT = 100*60*60*60*1000L;
    
    private static void setDefaultTimeout(Step step, SQLCommand command, ImMap<SQLQuery, MaterializedQuery> materializedQueries) {
        step.setTimeout(BaseUtils.max(step.getTimeout(), getDefaultTimeout(command, materializedQueries)));
        checkLastStep(step, command, materializedQueries);
    }
    
    private static Step getCachedNextStep(Step current, SQLCommand command, ImSet<SQLQuery> materializedQueries) {
        Step next = current.next;
        if (next == null) {
            next = createNextStep(command, current, materializedQueries);
            assert next.recheck;
            current.next = next;
        }
        return next;
    }
    
    private static Step getExecuteNextStep(Step current, SQLCommand command, Snapshot snapshot) {
        assert snapshot.step == current;
        ImMap<SQLQuery, MaterializedQuery> materializedQueries = snapshot.getMaterializedQueries();
        Settings settings = Settings.get();
        int coeff = settings.getLastStepCoeff();
        while (true) {
            Step next;
            if(current.isLastStep()) { // идем назад, но этому шагу и себе увеличиваем timeout
                ServerLoggers.assertLog(!settings.isNoDisablingNestedLoop(), "SHOULD NOT BE");
                
                next = current.previous;
                next.setTimeout(next.getTimeout() * coeff);
                next.recheck = true;
                current.setTimeout(current.getTimeout() * coeff);
                current.recheck = true;
            } else
                next = getCachedNextStep(current, command, materializedQueries.keys());
            current = next;
            if (current.recheck) {
                setDefaultTimeout(current, command, materializedQueries); // adjust'м timeout в любом случае
                return current;
            }
        }
    }

    private static class Node {
        private SQLQuery query;
        private Set<Node> parents = new HashSet<>();

        public Node(SQLQuery query, SQLCommand command) {
            this.query = query;

            size = command.getCost(MapFact.EMPTY()).rows.getWeight();
            hasTooLongKeys = query != null && SQLQuery.hasTooLongKeys(query.keyReaders);
            hasNotMaterializables = query != null && query.getEnv().hasNotMaterializable();
            hasPessQuery = query != null && query.pessQuery != null;
        }

        public boolean isRoot() {
            return query == null;
        }

        private int getParentDegree() { // количество путей до node'а
            int result = isRoot() ? 1 : 0;
            for(Node parent : parents)
                result += parent.getParentDegree();
            return result;
        }
        private int getChildrenDegree() {
            int result = 1;
            for(Node child : children)
                result += child.getChildrenDegree();
            return result;
        }
        private final int size;
        private final boolean hasTooLongKeys;
        private final boolean hasNotMaterializables;
        private final boolean hasPessQuery;
        private Set<Node> children = new HashSet<>();

        private Integer priority = null;
    }

    private static void recCreateNode(SQLCommand<?> command, Node parent, ImSet<SQLQuery> materializedQueries, Map<SQLCommand, Node> nodes) {
        for(SQLQuery subQuery : command.subQueries.values()) {
            if(!materializedQueries.contains(subQuery)) {
                Node subNode = nodes.get(subQuery);
                if(subNode == null) {
                    subNode = new Node(subQuery, subQuery);
                    nodes.put(subQuery, subNode);
                }

                recCreateNode(subQuery, subNode, materializedQueries, nodes);

                if(parent != null) {
                    subNode.parents.add(parent);
                    parent.children.add(subNode);
                }
            }
        }
    }

    private static Step createNextStep(SQLCommand command, Step step, ImSet<SQLQuery> outerQueries) {
        // есть n вершин (с учетом materialized), берем корень n-й степени
        // наша цель найти поддеревья с максимально близкой степенью к этой величине, материализовать ее
        // ищем вершину с максимально подходящей степенью, включаем в результат, берем следующую и т.д. пока не останется пустое дерево

        Settings settings = Settings.get();

        final Map<SQLCommand, Node> nodes = new HashMap<>();
        ServerLoggers.assertLog(outerQueries.containsAll(step.getMaterializedQueries()), "SHOULD CONTAIN ALL"); // может включать еще "верхние"
        final Node topNode = new Node(null, command);
        recCreateNode(command, topNode, outerQueries, nodes); // не важно inner или нет
        if(!nodes.isEmpty()) { // оптимизация
            nodes.put(command, topNode);

            int split = settings.getSubQueriesSplit();
            final int threshold = new Stat(settings.getSubQueriesRowsThreshold()).getWeight();
            final int max = new Stat(settings.getSubQueriesRowsMax()).getWeight();
            final int rowCountCoeff = settings.getSubQueriesRowCountCoeff();
            final int parentCoeff = settings.getSubQueriesParentCoeff();
            final int pessQueryCoeff = settings.getSubQueriesPessQueryCoeff();

            final int target = (int) Math.round(((double)topNode.getChildrenDegree()) / split);

            Function<Node, Integer> priorityCalc = o -> {
                int pdeg = o.getParentDegree();
                int cdeg = o.getChildrenDegree();
                if(o == topNode) {
                    assert pdeg == 1;
                    if(cdeg > target) // если больше target по сути запретим выбирать
                        return Integer.MAX_VALUE / 2;
                } else {
                    if (pdeg == 0 || (o.size >= max && o.size > topNode.size) || o.hasTooLongKeys || o.hasNotMaterializables) // если удаленная вершина или больше порога не выбираем вообще
                        return Integer.MAX_VALUE;
                }

                int result = BaseUtils.max(o.size, threshold) * rowCountCoeff + Math.abs(pdeg * cdeg - target) - pdeg * parentCoeff;
                if(o.hasPessQuery)
                    result = result * pessQueryCoeff;
                return result;
            };
            Comparator<Node> comparator = Comparator.comparingInt(o -> o.priority);

            PriorityQueue<Node> priority = new PriorityQueue<>(nodes.size(), comparator);
            addNodes(nodes.values(), priorityCalc, priority);

            MOrderSet<SQLQuery> mNextQueries = SetFact.mOrderSet();
            while(true) {
                Node bestNode = priority.poll();

                if(bestNode.isRoot()) {
                    assert bestNode == topNode;
                    break;
                }
                mNextQueries.add(bestNode.query);

                // удаляем parent'ы и children'ы из priority
                Set<Node> removedNodes = new HashSet<>();
                priority.remove(bestNode);
                recRemoveChildren(bestNode, priority, removedNodes);
                recRemoveParent(bestNode, priority, removedNodes);

                // удаляем вершину
                for(Node node : bestNode.parents)
                    node.children.remove(bestNode);
                for(Node node : bestNode.children)
                    node.parents.remove(bestNode);

                // закидываем заново элементы (новые приоритеты пересчитаются)
                addNodes(removedNodes, priorityCalc, priority);
            }
            final ImOrderSet<SQLQuery> nextQueries = mNextQueries.immutableOrder();

            if(!nextQueries.isEmpty())
                return new Step(nextQueries, step);
        }

        return new Step(true, step); // включение disableNestedLoop
    }

    private static void addNodes(Collection<Node> nodes, Function<Node, Integer> priorityCalc, PriorityQueue<Node> queue) {
        for(Node node : nodes) {
            node.priority = priorityCalc.apply(node);
            queue.add(node);
        }
    }
    private static boolean removeNode(Node node, Set<Node> removedNodes, PriorityQueue<Node> queue) {
        if(removedNodes.add(node)) {
            queue.remove(node);
            node.priority = null;
            return true;
        }
        return false;
    }

    private static void recRemoveChildren(Node node, PriorityQueue<Node> queue, Set<Node> removedNodes) {
        for(Node child : node.children) {
            if(removeNode(child, removedNodes, queue))
                recRemoveChildren(child, queue, removedNodes);
        }
    }

    private static void recRemoveParent(Node node, PriorityQueue<Node> priority, Set<Node> removedNodes) {
        for(Node parent : node.parents) {
            if(removeNode(parent, removedNodes, priority)) {
                recRemoveParent(parent, priority, removedNodes);
            }
        }
    }

    private static long getDefaultTimeout(SQLCommand command, ImMap<SQLQuery, MaterializedQuery> queries) {
        Cost baseCost = command.getCost(queries.mapValues(value -> new Stat(value.count)));
        return baseCost.getDefaultTimeout();
    }

    private static void drop(MaterializedQuery query, SQLSession session, OperationOwner owner) throws SQLException {
        session.lockedReturnTemporaryTable(query.tableName, query.owner, owner, query.count);
    }

    private class SubQueryEnv {
        private final MAddExclMap<SQLQuery, DynamicExecuteEnvironment> map = MapFact.mAddExclMap();

        public synchronized DynamicExecuteEnvironment get(SQLQuery subQuery, Step step) {
            DynamicExecuteEnvironment subEnv = map.get(subQuery);
            if(subEnv == null) {
                subEnv = new AdjustMaterializedExecuteEnvironment(new SubQueryUpContext(step, subQuery, AdjustMaterializedExecuteEnvironment.this));
                map.exclAdd(subQuery, subEnv);
            }
            return subEnv;
        }
    }
    private final SubQueryEnv subQueryEnv = new SubQueryEnv();

    // Mutable, inner, not Thread safe
    private static class Step {
        private Step next; // более пессимистичный
        private final Step previous; // более оптимистичный

        private final ImOrderSet<SQLQuery> subQueries; // одновременно является кэшем, чтобы несколько раз не считать
        private final boolean disableNestedLoop;

        public boolean isLastStep() {
            return disableNestedLoop;
        }

        private long timeout; // estimated execution time (in millis), after which it is considered to be something wrong with the query
        public boolean recheck; // mutable часть - есть вероятность (а точнее не было случая что он не выполнился) что он выполнится за timeout

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        private Step(Step previous, ImOrderSet<SQLQuery> subQueries, boolean disableNestedLoop) {
            recheck = true;
            this.previous = previous;
            this.subQueries = subQueries;
            this.disableNestedLoop = disableNestedLoop;
        }

        public Step(ImOrderSet<SQLQuery> subQueries, Step previous) {
            this(previous, subQueries, false);
        }

        public Step(boolean disableNestedLoop, Step previous) {
            this(previous, SetFact.EMPTYORDER(), disableNestedLoop);
            assert disableNestedLoop;
            assert !previous.disableNestedLoop;
            timeout = 0;
        }

        public int getIndex() {
            return previous == null ? 0 : (previous.getIndex() + (disableNestedLoop ? 0 : 1));
        }

        @Override
        public String toString() {
            return "step : " + getIndex() + " timeout : " + timeout + " " + (disableNestedLoop ? "NONESTED" : "");
        }

        public long getMaterializeTime(ImMap<SQLQuery, MaterializedQuery> materializedQueries) {
            long result = 0;
            for(SQLQuery subQuery : subQueries)
                result += materializedQueries.get(subQuery).timeExec;
            return result;
        }

        public boolean fillSubQueries(MOrderExclSet<SQLQuery> mSubQueries, Step previousStep) {
            if(equals(previousStep)) // нашли
                return true;
            if((previous != null && previous.fillSubQueries(mSubQueries, previousStep)) || (previous == null && previousStep == null)) {
                mSubQueries.exclAddAll(subQueries);
                return true;
            }
            return false;
        }

        private ImSet<SQLQuery> getMaterializedQueries() {
            MOrderExclSet<SQLQuery> mSubQueries = SetFact.mOrderExclSet();
            fillSubQueries(mSubQueries, null);
            return mSubQueries.immutableOrder().getSet();
        }

    }

    public class Snapshot implements DynamicExecEnvSnapshot<ImMap<SQLQuery, MaterializedQuery>, Snapshot> {
        // immutable часть
        public final Step step;
        public final ImOrderSet<SQLQuery> queries;

        // состояние сессии (точнее потока + сессии), есть assertion что не изменяются вплоть до окончания выполнения
        public final ImMap<SQLQuery, MaterializedQuery> materializedOuterQueries; // param
        public final int transactTimeout; // param

        public boolean noHandled; // ThreadLocal
        public boolean inTransaction; // LockWrite
        public int secondsFromTransactStart; // LockWrite

        // рассчитанное локальное состояние
        public ImMap<SQLQuery, MaterializedQuery> materializedQueries;
        public boolean isTransactTimeout = false;
        public boolean needConnectionLock;
        public boolean disableNestedLoop;
        public long setTimeout; // in millis
        public boolean needSavePoint;
        public boolean useSavePoint;
        
        public boolean usedPessQuery() {
            for(SQLQuery query : queries)
                if(query.pessQuery != null)
                    return true;
            return false;
        }

        @Override
        public boolean isUseSavePoint() {
            return useSavePoint;
        }

        public Snapshot getSnapshot() {
            return this;
        }

        public Snapshot(Step step, Step previousStep, SQLCommand command, int transactTimeout, ImMap<SQLQuery, MaterializedQuery> materializedQueries) {
            this.step = step;

            MOrderExclSet<SQLQuery> mSubQueries = SetFact.mOrderExclSet();
            boolean found = step.fillSubQueries(mSubQueries, previousStep); // бежим от nextStep назад пока не встретим previous, определяем queries которые надо материализовать
//            ServerLoggers.assertLog(found, "SHOULD HIT PREVIOUS");
            this.queries = mSubQueries.immutableOrder();

            assertSameMaterialized(command, previousStep, materializedQueries.keys()); // assert'им что previousStep все queries + outer совпадают с materialized
            this.materializedOuterQueries = materializedQueries;

            ServerLoggers.assertLog(!queries.getSet().intersect(materializedOuterQueries.keys()), "SHOULD NOT INTERSECT"); // если быть точным queries должны быть строго выше materialized

            this.transactTimeout = transactTimeout;
        }

        // forAnalyze
        private boolean forAnalyze;
        public Snapshot(Step step, ImOrderSet<SQLQuery> queries, ImMap<SQLQuery, MaterializedQuery> materializedOuterQueries, int transactTimeout, ImMap<SQLQuery, MaterializedQuery> materializedQueries) {
            this.step = step;
            this.queries = queries;
            this.materializedOuterQueries = materializedOuterQueries;
            this.transactTimeout = transactTimeout;
            this.materializedQueries = materializedQueries;
            this.setTimeout = 0;
            this.forAnalyze = true;
        }

        public Snapshot forAnalyze() {
            assert materializedQueries != null;
            assert !forAnalyze;

            return new Snapshot(step, queries, materializedOuterQueries, transactTimeout, materializedQueries);
        }

        public void beforeOuter(SQLCommand command, SQLSession session, ImMap<String, ParseInterface> paramObjects, OperationOwner owner, PureTimeInterface pureTime) throws SQLException, SQLHandledException {
            MExclMap<SQLQuery, MaterializedQuery> mMaterializedQueries = MapFact.mExclMapMax(queries.size());
            try {
                for (int i = 0, size = queries.size(); i < size; i++) {
                    SQLQuery query = queries.get(i);
                    ImMap<SQLQuery, MaterializedQuery> copyMaterializedQueries = MapFact.addExcl(mMaterializedQueries.immutableCopy(), materializedOuterQueries);
                    MaterializedQuery materialized = query.materialize(session, subQueryEnv.get(query, step), owner, copyMaterializedQueries, paramObjects, transactTimeout);
                    mMaterializedQueries.exclAdd(query, materialized);
                    pureTime.add(materialized.timeExec);
                }
            } finally {
                materializedQueries = mMaterializedQueries.immutable();
            }
        }

        public ImMap<SQLQuery, MaterializedQuery> getMaterializedQueries() {
            return materializedQueries.addExcl(materializedOuterQueries);
        }

        public ImMap<SQLQuery, MaterializedQuery> getOuter() {
            return getMaterializedQueries();
        }

        public void afterOuter(SQLSession session, OperationOwner owner) throws SQLException {
            for(MaterializedQuery matQuery : materializedQueries.valueIt())
                drop(matQuery, session, owner);
        }

        // assert что session.locked
        private void prepareEnv(SQLSession session) { // "смешивает" универсальное состояние (при отсуствии ограничений) и "местное", DynamicExecuteEnvironment.checkSnapshot выполняет обратную функцию
            noHandled = forAnalyze || session.isNoHandled();
            if(noHandled)
                return;

            inTransaction = session.isInTransaction();
            if(inTransaction)
                secondsFromTransactStart = session.getSecondsFromTransactStart();            
            setTimeout = step.getTimeout();

            useSavePoint = false;
            needSavePoint = false;
            if(inTransaction && hasRepeatCommand()) {
                if(session.syntax.hasTransactionSavepointProblem()) {
                    Integer count;
                    int savePointCountForExceptions = Settings.get().getSavePointCountForExceptions();
                    if (savePointCountForExceptions >= 0 && (count = session.getTransactTimeouts()) != null && count >= savePointCountForExceptions) {
                        useSavePoint = session.registerUseSavePoint();
                        needSavePoint = true;
                    }
                }

                if(!useSavePoint) // if not using savepoints, increasing timeout to the time from transaction start
                    setTimeout = BaseUtils.max(setTimeout, secondsFromTransactStart * 1000);
            }

            if(session.syntax.supportsDisableNestedLoop()) {
                disableNestedLoop = step.disableNestedLoop;
//                sessionVolatileStats = session.isVolatileStats();
//                if (sessionVolatileStats) { // проверяем локальный volatileStats
//                    disableNestedLoop = true;
//                    setTimeout = 0; // выключаем timeout
//                }
            }

            // уменьшаем timeout до локального максимума
            if(inTransaction && !session.isNoTransactTimeout() && transactTimeout > 0 && (setTimeout >= transactTimeout || setTimeout == 0)) {
                setTimeout = transactTimeout;
                isTransactTimeout = true;
            }

            needConnectionLock = disableNestedLoop || (setTimeout > 0 && session.syntax.hasJDBCTimeoutMultiThreadProblem()); // проверка на timeout из-за бага в драйвере postgresql
        }

        // после readLock сессии, но до получения connection'а
        public void beforeConnection(SQLSession session, OperationOwner owner) throws SQLException {
            prepareEnv(session);

            if(needSavePoint)
                session.registerNeedSavePoint();

            if(needConnectionLock)
                session.lockNeedPrivate();
        }

        public void afterConnection(SQLSession session, OperationOwner owner) throws SQLException {
            if(needConnectionLock)
                session.lockTryCommon(owner);
            
            if(needSavePoint)
                session.unregisterNeedSavePoint();
        }

        public boolean isTransactTimeout() {
            return isTransactTimeout;
        }

        public boolean needConnectionLock() {
            return needConnectionLock;
        }

        public void beforeStatement(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
            if(disableNestedLoop) {
                assert needConnectionLock; // чтобы запрещать connection должен быть заблокирован
                sqlSession.setEnableNestLoop(connection, owner, false);
            }
        }

        public void afterStatement(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException {
            if(disableNestedLoop) {
                assert needConnectionLock;
                sqlSession.setEnableNestLoop(connection, owner, true);
            }
        }

        public void beforeExec(Statement statement, SQLSession session) throws SQLException {
            if(setTimeout > 0 && setTimeout < ALOT)
                session.syntax.setQueryTimeout(statement, setTimeout);
        }

        public boolean hasRepeatCommand() {
            return setTimeout > 0;
        }
    }

}
