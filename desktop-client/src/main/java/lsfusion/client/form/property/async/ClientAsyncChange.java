package lsfusion.client.form.property.async;

import lsfusion.client.classes.ClientType;
import lsfusion.client.classes.ClientTypeSerializer;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.controller.remote.serialization.ClientSerializationPool;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientAsyncChange extends ClientAsyncInputExec {
    public ClientType changeType;

    @SuppressWarnings("UnusedDeclaration")
    public ClientAsyncChange() {
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream) throws IOException {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        this.changeType = ClientTypeSerializer.deserializeClientType(inStream);
    }

    @Override
    public boolean exec(ClientFormController form, EditPropertyDispatcher dispatcher, ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) throws IOException {
        return dispatcher.asyncChange(property, columnKey, actionSID, this);
    }
}
