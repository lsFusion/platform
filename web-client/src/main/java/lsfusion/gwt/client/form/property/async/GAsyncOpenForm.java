package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;
import lsfusion.gwt.client.form.view.ModalForm;
import lsfusion.gwt.client.view.MainFrame;

public class GAsyncOpenForm extends GAsyncExec {
    public String canonicalName;
    public String caption;
    public  boolean forbidDuplicate;
    public boolean modal;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncOpenForm() {
    }

    public GAsyncOpenForm(String canonicalName, String caption, boolean forbidDuplicate, boolean modal) {
        this.canonicalName = canonicalName;
        this.caption = caption;
        this.forbidDuplicate = forbidDuplicate;
        this.modal = modal;
    }

    @Override
    public void exec(GFormController formController, GPropertyDraw property, Event event, ExecuteEditContext editContext, String actionSID) {
        formController.asyncOpenForm(this, editContext, actionSID);
    }

    public boolean isModal() {
        //if current form is modal, new async form can't be non-modal
        return modal || MainFrame.getCurrentForm() instanceof ModalForm;
    }

    @Override
    public void exec(FormsController formsController) {
        formsController.asyncOpenForm(MainFrame.navigatorDispatchAsync.getNextRequestIndex(), this);
    }
}