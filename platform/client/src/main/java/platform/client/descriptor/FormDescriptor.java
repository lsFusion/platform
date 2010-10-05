package platform.client.descriptor;

import platform.client.descriptor.filter.FilterDescriptor;
import platform.client.logics.ClientForm;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class FormDescriptor extends IdentityDescriptor implements ClientIdentitySerializable {

    ClientForm client;

    public List<GroupObjectDescriptor> groups;
    public List<PropertyDrawDescriptor> propertyDraws;
    public Set<FilterDescriptor> fixedFilters;

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.serializeCollection(outStream, groups);
        pool.serializeCollection(outStream, propertyDraws);
        pool.serializeCollection(outStream, fixedFilters);
    }

    public void customDeserialize(ClientSerializationPool pool, int ID, DataInputStream inStream) throws IOException {
        this.ID = ID;

        groups = pool.deserializeList(inStream);
        propertyDraws = pool.deserializeList(inStream);
        fixedFilters = pool.deserializeSet(inStream);

        client = pool.context;
    }

    @Override
    public String toString() {
        return client.caption;
    }
}
