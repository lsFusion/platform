package platform.server.form.entity.filter;

import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.logics.property.PropertyInterface;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

public abstract class PropertyFilterEntity<P extends PropertyInterface> extends FilterEntity {

    public PropertyObjectEntity<P> property;

    public PropertyFilterEntity(PropertyObjectEntity<P> iProperty) {
        property = iProperty;
    }

    protected void fillObjects(Set<ObjectEntity> objects) {
        property.fillObjects(objects);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, property);
    }

    public void customDeserialize(ServerSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        property = (PropertyObjectEntity<P>) pool.deserializeObject(inStream);
    }
}
