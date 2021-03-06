package lsfusion.server.data.sql.exception;

import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.physics.admin.Settings;

import java.sql.SQLException;

// cached plan must change resultType i.e
public class SQLRetryException extends SQLHandledException {
    
    private final String reason;

    public SQLRetryException(String reason) {
        this.reason = reason;
    }

    @Override
    public boolean willDefinitelyBeHandled() {
        return false;
    }

    @Override
    public boolean repeatApply(SQLSession sql, OperationOwner owner, int attempts) {
        if(attempts > Settings.get().getTooMuchRetryAttempts())
            return false;

        return true;
    }

    @Override
    public String getMessage() {
        return "RETRY : " + reason;
    }

    @Override
    public String getDescription(boolean wholeTransaction) {
        return "re";
    }
}
