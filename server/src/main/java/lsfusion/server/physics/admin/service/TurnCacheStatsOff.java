package lsfusion.server.physics.admin.service;

import lsfusion.server.base.caches.CacheStats;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.sql.SQLException;

public class TurnCacheStatsOff extends ScriptingAction {
    public TurnCacheStatsOff(ServiceLogicsModule LM) {
        super(LM);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        CacheStats.readCacheStats = false;    
    }
}