package platform.server.form.entity;

import platform.server.form.instance.InstanceFactory;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PropertyObjectEntity<P extends PropertyInterface> extends PropertyImplement<PropertyObjectInterfaceEntity,P> implements OrderEntity<PropertyObjectInstance>, ServerIdentitySerializable {

    public Collection<ObjectEntity> getObjectInstances() {
        Collection<ObjectEntity> result = new ArrayList<ObjectEntity>();
        for(PropertyObjectInterfaceEntity object : mapping.values())
            if(object instanceof ObjectEntity)
                result.add((ObjectEntity) object);
        return result;
    }

    public void fillObjects(Set<ObjectEntity> objects) {
        objects.addAll(getObjectInstances());
    }

    private int ID = 0;

    public PropertyObjectEntity(int ID, LP<P> property, PropertyObjectInterfaceEntity... objects) {
        super(property.property);
        this.ID = ID;
        for(int i=0;i<property.listInterfaces.size();i++)
            mapping.put(property.listInterfaces.get(i),objects[i]);
    }

    public PropertyObjectEntity(int ID, Property<P> property, Map<P, PropertyObjectInterfaceEntity> mapping) {
        super(property, mapping);
        this.ID = ID;
    }

    public PropertyObjectInstance getInstance(InstanceFactory instanceFactory) {
        return instanceFactory.getInstance(this);
    }

    public int getID() {
        return ID;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeObject(outStream, property);

        outStream.writeInt(mapping.size());
        for (Map.Entry<P, PropertyObjectInterfaceEntity> entry : mapping.entrySet()) {
            pool.serializeObject(outStream, entry.getKey());
            pool.serializeObject(outStream, entry.getValue());
        }
    }

    public void customDeserialize(ServerSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        //todo: think about finals
//        property = (Property<P>) pool.deserializeObject(inStream);

//        mapping = new HashMap<P, PropertyObjectInterfaceEntity>();
        int size = inStream.readInt();
        for (int i = 0; i < size; ++i) {
            P inter = (P) pool.deserializeObject(inStream);
            PropertyObjectInterfaceEntity value = (PropertyObjectInterfaceEntity) pool.deserializeObject(inStream);

            mapping.put(inter, value);
        }
    }
}
