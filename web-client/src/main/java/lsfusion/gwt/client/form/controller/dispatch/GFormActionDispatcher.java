package lsfusion.gwt.client.form.controller.dispatch;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.action.*;
import lsfusion.gwt.client.base.log.GLog;
import lsfusion.gwt.client.base.view.DialogBoxHelper;
import lsfusion.gwt.client.classes.GObjectClass;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.classes.view.ClassChosenHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.form.view.ModalForm;
import lsfusion.gwt.client.navigator.window.GModalityType;
import lsfusion.gwt.client.view.MainFrame;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class GFormActionDispatcher extends GwtActionDispatcher {
    protected final GFormController form;

    public GFormActionDispatcher(GFormController form) {
        this.form = form;
    }

    @Override
    protected void continueServerInvocation(long requestIndex, Object[] actionResults, int continueIndex, AsyncCallback<ServerResponseResult> callback) {
        form.continueServerInvocation(requestIndex, actionResults, continueIndex, callback);
    }

    @Override
    protected void throwInServerInvocation(long requestIndex, Throwable t, int continueIndex, AsyncCallback<ServerResponseResult> callback) {
        form.throwInServerInvocation(requestIndex, t, continueIndex, callback);
    }

    @Override
    public void execute(final GFormAction action) {
        if (form.isModal() && action.modalityType == GModalityType.DOCKED_MODAL) {
            action.modalityType = GModalityType.MODAL;
        }

        if (action.modalityType.isModal()) {
            pauseDispatching();
        }
        form.openForm(action.form, action.modalityType, action.forbidDuplicate, getEditEvent(), () -> {
            if (action.modalityType.isModal()) {
                continueDispatching();
            }
        });
    }

    @Override
    public Object execute(GChooseClassAction action) {
        pauseDispatching();
        form.showClassDialog(action.baseClass, action.defaultClass, action.concreate, new ClassChosenHandler() {
            @Override
            public void onClassChosen(GObjectClass chosenClass) {
                continueDispatching(chosenClass == null ? null : chosenClass.ID);
            }
        });
        return null;
    }

    @Override
    public int execute(GConfirmAction action) {
        pauseDispatching();
        form.blockingConfirm(action.caption, action.message, action.cancel, action.timeout, action.initialValue, new DialogBoxHelper.CloseCallback() {
            @Override
            public void closed(DialogBoxHelper.OptionType chosenOption) {
                continueDispatching(chosenOption.asInteger());
            }
        });

        return 0;
    }

    @Override
    public void execute(GLogMessageAction action) {
        if (GLog.isLogPanelVisible || action.failed) {
            super.execute(action);
        } else {
            pauseDispatching();
            form.blockingMessage(action.failed, "lsFusion", action.message, new DialogBoxHelper.CloseCallback() {
                @Override
                public void closed(DialogBoxHelper.OptionType chosenOption) {
                    continueDispatching();
                }
            });
        }
    }

    @Override
    public void execute(GHideFormAction action) {
        form.hideForm(action.closeDelay);
    }

    @Override
    public void execute(GProcessFormChangesAction action) {
        form.applyRemoteChanges(action.formChanges);
    }

    @Override
    public void execute(GAsyncGetRemoteChangesAction action) {
        form.getRemoteChanges();
    }

    //todo: по идее, action должен заливать куда-то в сеть выбранный локально файл
    @Override
    public String execute(GLoadLinkAction action) {
        return null;
    }

    @Override
    public void execute(final GChangeColorThemeAction action) {
        MainFrame.changeColorTheme(action.colorTheme);
    }

    // editing (INPUT) functionality

    private GPropertyDraw editProperty;
    private GGroupObjectValue editColumnKey;

    protected Event editEvent;
    protected Element editElement;

    private Supplier<Object> editGetValue;
    private Consumer<Object> editSetValue;

    private RenderContext editRenderContext;
    private UpdateContext editUpdateContext;

    public void executePropertyActionSID(GPropertyDraw property, GGroupObjectValue columnKey, Element element, Event event, Supplier<Object> getValue, Consumer<Object> setValue, String actionSID, RenderContext renderContext, UpdateContext updateContext) {
        editProperty = property;
        editColumnKey = columnKey;

        editElement = element;
        editEvent = event;

        editGetValue = getValue;
        editSetValue = setValue;

        editRenderContext = renderContext;
        editUpdateContext = updateContext;

        form.executeEventAction(property, columnKey, actionSID);
    }

    protected Event getEditEvent() {
        return editEvent;
    }

    @Override
    public Object execute(GRequestUserInputAction action) {

        pauseDispatching();

        // we should not drop at least editSetValue since GUpdateEditValueAction might use it
        form.edit(editProperty, editElement, action.readType, editEvent, action.hasOldValue, action.oldValue, editGetValue, value -> {},
                value -> continueDispatching(new GUserInputResult(value)),
                () -> continueDispatching(GUserInputResult.canceled), editRenderContext, editUpdateContext);

        return null;
    }

    @Override
    public void execute(GUpdateEditValueAction action) {
        editSetValue.accept(action.value);
    }
}
