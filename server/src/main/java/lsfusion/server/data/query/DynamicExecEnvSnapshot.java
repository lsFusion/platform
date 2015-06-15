package lsfusion.server.data.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.*;
import lsfusion.server.data.type.ParseInterface;

import java.sql.SQLException;
import java.sql.Statement;

// ThreadSafe
public interface DynamicExecEnvSnapshot<OE, S extends DynamicExecEnvSnapshot<OE, S>> extends DynamicExecEnvOuter<OE, S> {

    void beforeOuter(SQLCommand command, SQLSession session, ImMap<String, ParseInterface> paramObjects, OperationOwner owner, PureTimeInterface runTime) throws SQLException, SQLHandledException;

    void afterOuter(SQLSession session, OperationOwner owner) throws SQLException;

    // после readLock сессии, но до получения connection'а
    void beforeConnection(SQLSession session, OperationOwner owner) throws SQLException;

    void afterConnection(SQLSession session, OperationOwner owner) throws SQLException;

    void beforeStatement(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException;

    void afterStatement(SQLSession sqlSession, ExConnection connection, String command, OperationOwner owner) throws SQLException;

    void beforeExec(Statement statement, SQLSession session) throws SQLException;

    boolean isTransactTimeout();

    boolean needConnectionLock();

    ImMap<SQLQuery, MaterializedQuery> getMaterializedQueries();
}
