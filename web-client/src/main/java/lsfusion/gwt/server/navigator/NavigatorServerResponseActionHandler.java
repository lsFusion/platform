package lsfusion.gwt.server.navigator;

import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorRequestAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.action.ServerResponse;

public abstract class NavigatorServerResponseActionHandler<A extends NavigatorRequestAction> extends NavigatorActionHandler<A, ServerResponseResult> {

    protected NavigatorServerResponseActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    protected ServerResponseResult getServerResponseResult(A action, ServerResponse serverResponse) {
        return FormServerResponseActionHandler.getServerResponseResult(null, new FormSessionObject(null, null, action.sessionID), serverResponse, servlet);
    }
}
