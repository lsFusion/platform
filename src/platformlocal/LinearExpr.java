package platformlocal;

import java.util.*;

// среднее что-то между CaseExpr и FormulaExpr - для того чтобы не плодить экспоненциальные case'ы
// придется делать AndExpr
class LinearExpr extends AndExpr {

    class Operand {
        SourceExpr expr;
        int coeff;

        Operand(SourceExpr iExpr, int iCoeff) {
            expr = iExpr;
            coeff = iCoeff;
        }

        String addToString(boolean first, String string) {
            if(coeff>=0) return (first?"":"+") + (coeff==1?"":coeff+"*") + string;
            if(coeff==-1) return "-"+string;
            return coeff+"*"+string; // <0
        }
    }

    // !!!! он меняется при add'е, но конструктора пока нету так что все равно
    Collection<Operand> operands = new ArrayList<Operand>();
    void add(SourceExpr expr,Integer coeff) {
        for(Operand operand : operands)
            if(operand.expr.hashCode()==expr.hashCode() && operand.expr.equals(expr)) {
                operand.coeff += coeff;
//                if(operand.coeff==0)
//                    throw new RuntimeException("!!!!! Сейчас она при убирании операндов немного по другому работать будет ???? В этом тоже глюк может быть");
                return;
            }
        operands.add(new Operand(expr, coeff));
    }

    LinearExpr() {
    }
    LinearExpr(SourceExpr expr,SourceExpr opExpr,boolean sum) {
        add(expr,1);
        add(opExpr,(sum?1:-1));
    }
    LinearExpr(SourceExpr expr,Integer coeff) {
        add(expr,coeff);
    }

    // при translate'ы вырезает LinearExpr'ы

    Type getType() {
        return operands.iterator().next().expr.getType();
    }

    // возвращает Where на notNull
    Where getWhere() {
        Where result = Where.FALSE;
        for(Operand operand : operands)
            result = result.or(operand.expr.getWhere());
        return result;
    }

    // получает список ExprCase'ов
    ExprCaseList getCases() {
        return new ExprCaseList(this);
    }

    public String getSource(Map<QueryData, String> queryData, SQLSyntax syntax) {

        if(operands.size()==1) {
            Operand operand = operands.iterator().next();
            return (operand.coeff==-1?"-":operand.coeff) + operand.expr.getSource(queryData, syntax);                    
        }

        String source = "";
        String notNull = "";
        for(Operand operand : operands) {
            notNull = (notNull.length()==0?"":notNull+" OR ")+operand.expr.getWhere().getSource(queryData, syntax);
            source = source + operand.addToString(source.length()==0,
                    syntax.isNULL(operand.expr.getSource(queryData, syntax),operand.expr.getType().getEmptyString(),true));
        }
        return "(CASE WHEN " + notNull + " THEN " + source + " ELSE NULL END)";
    }

    public String toString() {
        String result = "";
        for(Operand operand : operands)
            result = result + operand.addToString(result.length()==0,operand.expr.toString());
        return "L(" + result + ")";
    }

    public <J extends Join> void fillJoins(List<J> joins, Set<ValueExpr> values) {
        for(Operand operand : operands)
            operand.expr.fillJoins(joins, values);
    }

    public void fillJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
        // просто гоним по операндам
        for(Operand operand : operands)
            operand.expr.fillJoinWheres(joins, andWhere);
    }

    // для кэша
    boolean equals(SourceExpr expr, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {
        if(!(expr instanceof LinearExpr)) return false;

        LinearExpr linearExpr = (LinearExpr) expr;
        if(operands.size()!=linearExpr.operands.size()) return false;

        for(Operand exprOperand : linearExpr.operands) {
            boolean found = false;
            for(Operand operand : operands)
                if(operand.coeff==exprOperand.coeff && operand.expr.hash() == exprOperand.expr.hash() &&
                        operand.expr.equals(exprOperand.expr, mapExprs, mapWheres)) {
                    found = true;
                    break;
                }
            if(!found) return false;
        }

        return true;
    }

    int getHash() {
        int result = 0;
        for(Operand operand : operands)
            result += operand.coeff*31 + operand.expr.hash();
        return result;
    }


    public boolean equals(Object obj) {
        if(!(obj instanceof LinearExpr)) return false;

        LinearExpr linearExpr = (LinearExpr) obj;
        if(operands.size()!=linearExpr.operands.size()) return false;

        for(Operand exprOperand : linearExpr.operands) {
            boolean found = false;
            for(Operand operand : operands)
                if(operand.coeff==exprOperand.coeff && operand.expr.hashCode() == exprOperand.expr.hashCode() &&
                        operand.expr.equals(exprOperand.expr)) {
                    found = true;
                    break;
                }
            if(!found) return false;
        }

        return true;
    }

    SourceExpr translateCase(Map<Operand,? extends SourceExpr> mapOperands) {
        LinearExpr transLinear = new LinearExpr();

        for(Map.Entry<Operand,? extends SourceExpr> mapOperand : mapOperands.entrySet()) {
            SourceExpr transExpr = mapOperand.getValue();
            if(transExpr instanceof LinearExpr) {
                for(Operand transOperand : ((LinearExpr)transExpr).operands)
                    transLinear.add(transOperand.expr,mapOperand.getKey().coeff*transOperand.coeff);
            } else // LinearExpr, его нужно влить сюда
                if(!(transExpr instanceof NullExpr))
                    transLinear.add(transExpr,mapOperand.getKey().coeff);
        }

        if(transLinear.operands.size()==0)
            return new NullExpr(getType());

        if(transLinear.operands.size()==1) {
            Operand operand = transLinear.operands.iterator().next();
            if(operand.coeff==1) return operand.expr;
        }

        return transLinear;
    }

    // транслирует выражение/ также дополнительно вытаскивает ExprCase'ы
    SourceExpr translate(Translator translator) {

        Map<Operand,SourceExpr> mapOperands = new HashMap<Operand, SourceExpr>();
        for(Operand operand : operands)
            mapOperands.put(operand,operand.expr);

        MapCaseList<Operand> caseList = CaseExpr.translateCase(mapOperands, translator, false, false);
        if(caseList==null)
            return this;

        ExprCaseList result = new ExprCaseList();
        for(MapCase<Operand> mapCase : caseList)  // здесь напрямую потому как MapCaseList уже все проверил
            result.add(new ExprCase(mapCase.where,translateCase(mapCase.data))); // кстати могут быть и одинаковые case'ы
        return result.getExpr();
    }

    public int hashCode() {
        return getHash();
    }

    DataWhereSet getFollows() {
        if(operands.size()==1)
            return ((AndExpr)operands.iterator().next().expr).getFollows();
        else
            return new DataWhereSet();
    }

    protected void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
    }
}