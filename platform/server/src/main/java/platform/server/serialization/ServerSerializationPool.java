package platform.server.serialization;

import platform.interop.serialization.CustomSerializable;
import platform.interop.serialization.SerializationPool;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.*;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.property.ObjectValueProperty;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.SelectionProperty;
import platform.server.logics.property.actions.AddObjectActionProperty;
import platform.server.logics.property.actions.ImportFromExcelActionProperty;

public class ServerSerializationPool extends SerializationPool<Object> {
    public ServerSerializationPool() {
        this(null);
    }

    public ServerSerializationPool(Object context) {
        super(context);
        //порядок добавления должен соответствовать порядку в ClientSerializationPool

        addMapping2(FormEntity.class);
        addMapping2(GroupObjectEntity.class);
        addMapping2(PropertyDrawEntity.class);
        addMapping2(Property.class);
        addMapping2(PropertyFilterEntity.class);
        addMapping2(OrFilterEntity.class);
        addMapping2(NotFilterEntity.class);
        addMapping2(CompareFilterEntity.class);
        addMapping2(RegularFilterEntity.class);
        addMapping2(RegularFilterGroupEntity.class);

        addMapping2(PropertyInterface.class);

        addMapping2(DataObject.class);
        addMapping2(CurrentComputerEntity.class);
        addMapping2(ObjectEntity.class);
        addMapping2(PropertyObjectEntity.class);

        addMapping2(AddObjectActionProperty.class);
        addMapping2(ImportFromExcelActionProperty.class);
        addMapping2(ObjectValueProperty.class);
        addMapping2(SelectionProperty.class);
    }

    // IDEA даёт ошибку при добавлении генериков,
    // хотя компилируется нормально, вставим такую затычку, чтобы не надо было везде кастить...
    protected void addMapping2(Class<? extends ServerCustomSerializable> clazz) {
        super.addMapping(clazz);
    }
}
