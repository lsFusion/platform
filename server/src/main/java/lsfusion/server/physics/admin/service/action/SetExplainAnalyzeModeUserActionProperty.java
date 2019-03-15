package lsfusion.server.physics.admin.service.action;

import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SetExplainAnalyzeModeUserActionProperty extends ScriptingAction {

    public SetExplainAnalyzeModeUserActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        List<Object> params = new ArrayList<>();
        for (ClassPropertyInterface classPropertyInterface : context.getKeys().keys()) {
            params.add(context.getKeyObject(classPropertyInterface));
        }
        
        SQLSession.setExplainAnalyzeMode((Long) params.get(1), (Boolean) params.get(0));
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}