package platform.client.descriptor.filter;

import platform.base.context.ContextObject;
import platform.client.descriptor.GroupObjectDescriptor;
import platform.client.descriptor.ObjectDescriptor;
import platform.client.descriptor.nodes.NodeCreator;
import platform.client.descriptor.nodes.filters.FilterNode;
import platform.client.serialization.ClientCustomSerializable;

import java.util.List;
import java.util.Map;

public abstract class FilterDescriptor extends ContextObject implements ClientCustomSerializable, NodeCreator {

    public abstract GroupObjectDescriptor getGroupObject(List<GroupObjectDescriptor> groupList);

    public static GroupObjectDescriptor getDownGroup(GroupObjectDescriptor group1, GroupObjectDescriptor group2, List<GroupObjectDescriptor> groupList) {
        if (groupList.indexOf(group1) > groupList.indexOf(group2)) {
            return group1;
        } else {
            return group2;
        }
    }

    public abstract FilterNode createNode(Object group);

    public abstract String getCodeConstructor(Map<ObjectDescriptor, String> objectNames);

    public static String[] derivedNames = new String[]{"Сравнение", "Определено", "Класс", "Отрицание", "Или"};
    public static Class[] derivedClasses = new Class[]{CompareFilterDescriptor.class, NotNullFilterDescriptor.class, IsClassFilterDescriptor.class, NotFilterDescriptor.class, OrFilterDescriptor.class};
}
