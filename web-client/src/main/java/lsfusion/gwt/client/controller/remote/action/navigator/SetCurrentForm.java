package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.base.result.VoidResult;

public class SetCurrentForm extends NavigatorAction<VoidResult> {
    public String formID;

    public SetCurrentForm() {
    }
    public SetCurrentForm(String formID) {
        this.formID = formID;
    }
}