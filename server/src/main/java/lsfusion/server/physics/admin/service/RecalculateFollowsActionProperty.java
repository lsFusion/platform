package lsfusion.server.physics.admin.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.session.SessionCreator;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

import static lsfusion.server.base.context.ThreadLocalContext.localize;

public class RecalculateFollowsActionProperty extends ScriptingAction {
    public RecalculateFollowsActionProperty(ServiceLogicsModule LM) {
        super(LM);
    }
    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ServiceDBActionProperty.runData(context, new RunServiceData() {
            public void run(SessionCreator session, boolean isolatedTransaction) throws SQLException, SQLHandledException {
                String result = context.getBL().recalculateFollows(session, isolatedTransaction, context.stack);
                if(result != null)
                    context.delayUserInterfaction(new MessageClientAction(result, localize("{logics.recalculation.follows}")));
            }
        });

        context.delayUserInterfaction(new MessageClientAction(localize(LocalizedString.createFormatted("{logics.recalculation.completed}", localize("{logics.recalculation.follows}"))), localize("{logics.recalculation.follows}")));
    }
}