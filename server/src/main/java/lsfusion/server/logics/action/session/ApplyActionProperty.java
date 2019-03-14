package lsfusion.server.logics.action.session;

import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.physics.exec.DBManager;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.action.flow.KeepContextActionProperty;
import lsfusion.server.physics.dev.debug.ActionDelegationType;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.Connection;
import java.sql.SQLException;

public class ApplyActionProperty extends KeepContextActionProperty {
    private final ActionPropertyMapImplement<?, PropertyInterface> action;
    private final CalcProperty canceled;
    private final CalcProperty applyMessage;
    private final FunctionSet<SessionDataProperty> keepSessionProperties;
    private final boolean serializable;

    public <I extends PropertyInterface> ApplyActionProperty(BaseLogicsModule LM, ActionPropertyMapImplement<?, I> action,
                                                             LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                             FunctionSet<SessionDataProperty> keepSessionProperties, boolean serializable) {
        super(caption, innerInterfaces.size());
        this.keepSessionProperties = keepSessionProperties;
        this.serializable = serializable;

        this.action = action.map(getMapInterfaces(innerInterfaces).reverse());
        this.canceled = LM.getCanceled().property;
        this.applyMessage = LM.getApplyMessage().property;
        
        finalizeInit();
    }
    
    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return super.aspectChangeExtProps().replaceValues(true);
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        return super.aspectUsedExtProps().replaceValues(true);
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        
        MList<ActionPropertyMapImplement<?, PropertyInterface>> actions = ListFact.mList();
        if(action != null)
            actions.add(action);

        ImList<CalcPropertyInterfaceImplement<PropertyInterface>> listWheres =
                ((ImList<ActionPropertyMapImplement<?, PropertyInterface>>)actions).mapListValues(
                        new GetValue<CalcPropertyInterfaceImplement<PropertyInterface>, ActionPropertyMapImplement<?, PropertyInterface>>() {
                            public CalcPropertyInterfaceImplement<PropertyInterface> getMapValue(ActionPropertyMapImplement<?, PropertyInterface> value) {
                                return value.mapCalcWhereProperty();
                            }});
        return DerivedProperty.createUnion(interfaces, listWheres);
        
    }

    @Override
    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        
        try {
            if (serializable)
                DBManager.pushTIL(Connection.TRANSACTION_REPEATABLE_READ);

            Result<String> rApplyMessage = new Result<>(); 
            if (!context.apply(action == null ? SetFact.<ActionPropertyValueImplement>EMPTYORDER() : SetFact.<ActionPropertyValueImplement>singletonOrder(action.getValueImplement(context.getKeys(), context.getObjectInstances(), context.getFormAspectInstance())), keepSessionProperties, rApplyMessage)) // no need to change canceled property if apply succeeds, because it is not nested and is dropped automatically
                canceled.change(context, true);
            applyMessage.change(context, rApplyMessage.result);
        } finally {
            if (serializable)
                DBManager.popTIL();
        }
        return FlowResult.FINISH;
    }

    public ImSet<ActionProperty> getDependActions() {
        ImSet<ActionProperty> result = SetFact.EMPTY();
        if (action != null) {
            result = result.merge(action.property);
        }        
        return result;
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.IN_DELEGATE; // need this because events are called (and we need step out)
    }

    @Override
    public boolean endsWithApplyAndNoChangesAfterBreaksBefore() {
        return true;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if (type == ChangeFlowType.APPLY)
            return true;
        if (type == ChangeFlowType.READONLYCHANGE)
            return true;
        return super.hasFlow(type);
    }
}