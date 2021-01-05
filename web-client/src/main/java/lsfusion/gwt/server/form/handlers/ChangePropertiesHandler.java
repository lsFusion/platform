package lsfusion.gwt.server.form.handlers;

import com.google.common.base.Throwables;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.controller.remote.action.form.ChangeProperties;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.remote.RemoteFormInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.BaseUtils.serializeObject;

public class ChangePropertiesHandler extends FormServerResponseActionHandler<ChangeProperties> {

    private static final GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ChangePropertiesHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangeProperties action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        return getServerResponseResult(action, new RemoteCall() {
            public ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException {
                GGroupObjectValue[] fullKeys = action.fullKeys;
                List<byte[]> convertedFullKeys = new ArrayList<>();
                for (GGroupObjectValue fullKey : fullKeys) {
                    convertedFullKeys.add(gwtConverter.convertOrCast(fullKey));
                }

                Serializable[] values = action.values;
                List<byte[]> pushChanges = null;

                if (values != null) {
                    pushChanges = new ArrayList<>();
                    for (Serializable serializable : values) {
                        Object value = gwtConverter.convertOrCast(serializable);
                        byte[] pushChange;
                        try {
                            pushChange = serializeObject(gwtConverter.convertOrCast(value));
                        } catch (IOException e) {
                            throw Throwables.propagate(e);
                        }
                        pushChanges.add(pushChange);
                    }
                }

                return remoteForm.changeProperties(
                        action.requestIndex,
                        action.lastReceivedRequestIndex,
                        action.propertyIds,
                        convertedFullKeys.toArray(new byte[convertedFullKeys.size()][]),
                        pushChanges != null ? pushChanges.toArray(new byte[pushChanges.size()][]) : new byte[][]{null},
                        action.addedObjectsIds
                );
            }
        });
    }
}
