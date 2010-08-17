package platform.server.form.view;

import platform.base.DefaultIDGenerator;
import platform.base.IDGenerator;
import platform.base.OrderedMap;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.server.form.entity.CellEntity;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.logics.linear.LP;
import platform.server.logics.property.Property;
import platform.server.logics.property.group.AbstractGroup;
import platform.server.form.entity.ObjectEntity;

import javax.swing.*;
import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FormView implements ClientSerialize {

    // нужен для того, чтобы генерировать уникальный идентификаторы объектам рисования, для передачи их клиенту
    protected IDGenerator idGenerator = new DefaultIDGenerator();

    public Collection<ContainerView> containers = new ArrayList<ContainerView>();

    public ContainerView addContainer() {
        return addContainer(null);
    }

    public ContainerView addContainer(String title) {

        ContainerView container = new ContainerView(idGenerator.idShift());
        container.title = title;

        containers.add(container);
        return container;
    }

    // список групп
    public List<GroupObjectView> groupObjects = new ArrayList<GroupObjectView>();

    // список свойств
    public List<PropertyDrawView> properties = new ArrayList<PropertyDrawView>();

    // список фильтров
    public List<RegularFilterGroupView> regularFilters = new ArrayList<RegularFilterGroupView>();

    public OrderedMap<CellView,Boolean> defaultOrders = new OrderedMap<CellView, Boolean>();

    public FunctionView printFunction = new FunctionView(idGenerator.idShift());
    public FunctionView xlsFunction = new FunctionView(idGenerator.idShift());
    public FunctionView nullFunction = new FunctionView(idGenerator.idShift());
    public FunctionView refreshFunction = new FunctionView(idGenerator.idShift());
    public FunctionView applyFunction = new FunctionView(idGenerator.idShift());
    public FunctionView cancelFunction = new FunctionView(idGenerator.idShift());
    public FunctionView okFunction = new FunctionView(idGenerator.idShift());
    public FunctionView closeFunction = new FunctionView(idGenerator.idShift());

    public List<CellView> order = new ArrayList<CellView>();

    public boolean readOnly = false;

    public KeyStroke keyStroke = null;

    public String caption = "";

    public FormView() {
    }

    static <T extends ClientSerialize> void serializeList(DataOutputStream outStream, Collection<T> list) throws IOException {
        outStream.writeInt(list.size());
        for(T element : list)
            element.serialize(outStream);
    }

    public void serialize(DataOutputStream outStream) throws IOException {

        outStream.writeBoolean(readOnly);

        List<ContainerView> orderedContainers = new ArrayList<ContainerView>();
        for(ContainerView container : containers)
            container.fillOrderList(orderedContainers);
        serializeList(outStream,orderedContainers);

        serializeList(outStream,groupObjects);
        serializeList(outStream, properties);
        serializeList(outStream,regularFilters);

        outStream.writeInt(defaultOrders.size());
        for(Map.Entry<CellView,Boolean> order : defaultOrders.entrySet()) {
            if (order.getKey() instanceof ObjectIDCellView)
                outStream.writeByte(0);
            else if (order.getKey() instanceof ClassCellView)
                outStream.writeByte(1);
            else
                outStream.writeByte(2);
            outStream.writeInt(order.getKey().ID);
            outStream.writeBoolean(order.getValue());
        }

        printFunction.serialize(outStream);
        xlsFunction.serialize(outStream);
        nullFunction.serialize(outStream);
        refreshFunction.serialize(outStream);
        applyFunction.serialize(outStream);
        cancelFunction.serialize(outStream);
        okFunction.serialize(outStream);
        closeFunction.serialize(outStream);

        outStream.writeInt(order.size());
        for(CellView orderCell : order) {
            outStream.writeInt(orderCell.getID());
            if (orderCell instanceof PropertyDrawView)
                outStream.writeBoolean(true);
            else {
                outStream.writeBoolean(false);
                outStream.writeBoolean(orderCell instanceof ClassCellView);
            }
        }

        new ObjectOutputStream(outStream).writeObject(keyStroke);

        outStream.writeUTF(caption);
    }

    public void addIntersection(ComponentView comp1, ComponentView comp2, DoNotIntersectSimplexConstraint cons) {

        if (comp1.container != comp2.container)
            throw new RuntimeException("Запрещено создавать пересечения для объектов в разных контейнерах");
        comp1.constraints.intersects.put(comp2, cons);
    }

    public GroupObjectView getGroupObject(GroupObjectEntity entity) {
        for (GroupObjectView groupObject : groupObjects)
            if (entity.equals(groupObject.entity))
                return groupObject;
        return null;
    }

    public List<PropertyDrawView> getProperties() {
        return properties;
    }

    public List<CellView> getCells() {

        List<CellView> result = new ArrayList<CellView>(getProperties());

        for (GroupObjectView groupObject : groupObjects)
            for (ObjectView object : groupObject) {
                result.add(object.objectIDCell);
                result.add(object.classCell);
            }

        return result;
    }


    public List<PropertyDrawView> getProperties(AbstractGroup group) {

        List<PropertyDrawView> result = new ArrayList<PropertyDrawView>();

        for (PropertyDrawView property : properties) {
            if (group.hasChild(property.entity.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<CellView> getCells(AbstractGroup group) {

        List<CellView> result = new ArrayList<CellView>(getProperties(group));

        for (GroupObjectView groupObject : groupObjects)
            for (ObjectView object : groupObject)
                if (group.hasChild(object.entity.baseClass.getParent())) {
                    result.add(object.objectIDCell);
                    result.add(object.classCell);
                }

        return result;
    }

     public List<CellView> getCells(ObjectEntity objectEntity) {
        List<CellView> result = new ArrayList<CellView>(getProperties(objectEntity.groupTo));
        for (GroupObjectView groupObject : groupObjects)
            for (ObjectView object : groupObject)
                if (object.entity.equals(objectEntity)) {
                    result.add(object.objectIDCell);
                    result.add(object.classCell);
                }
        return result;
    }

    public List<PropertyDrawView> getProperties(AbstractGroup group, GroupObjectEntity groupObject) {

        List<PropertyDrawView> result = new ArrayList<PropertyDrawView>();

        for (PropertyDrawView property : properties) {
            if (groupObject.equals(property.entity.toDraw) && group.hasChild(property.entity.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawView> getProperties(Property prop, GroupObjectEntity groupObject) {

        List<PropertyDrawView> result = new ArrayList<PropertyDrawView>();

        for (PropertyDrawView property : properties) {
            if (groupObject.equals(property.entity.toDraw) && prop.equals(property.entity.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawView> getProperties(Property prop) {

        List<PropertyDrawView> result = new ArrayList<PropertyDrawView>();

        for (PropertyDrawView property : properties) {
            if (prop.equals(property.entity.propertyObject.property)) {
                result.add(property);
            }
        }

        return result;
    }

    public List<PropertyDrawView> getProperties(GroupObjectEntity groupObject) {

        List<PropertyDrawView> result = new ArrayList<PropertyDrawView>();

        for (PropertyDrawView property : properties) {
            if (groupObject.equals(property.entity.toDraw)) {
                result.add(property);
            }
        }

        return result;
    }

    public void setFont(Font font, boolean cells) {

        for (CellView property : cells ? getCells() : getProperties()) {
            setFont(property, font);
        }
    }

    public void setFont(AbstractGroup group, Font font) {
        setFont(group, font, false);
    }

    public void setFont(AbstractGroup group, Font font, boolean cells) {

        for (CellView property : cells ? getCells(group) : getProperties(group)) {
            setFont(property, font);
        }
    }

    public void setFont(AbstractGroup group, Font font, GroupObjectEntity groupObject) {
        
        for (PropertyDrawView property : getProperties(group, groupObject)) {
            setFont(property, font);
        }
    }

    public void setFont(Font font, GroupObjectEntity groupObject) {

        for (PropertyDrawView property : getProperties(groupObject)) {
            setFont(property, font);
        }
    }

    public void setFont(LP property, Font font, GroupObjectEntity groupObject) {
        setFont(property.property, font, groupObject);
    }

    public void setFont(Property property, Font font, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(property, groupObject)) {
            setFont(propertyView, font);
        }
    }

    public void setFont(LP property, Font font) {
        setFont(property.property, font);
    }

    public void setFont(Property property, Font font) {

        for (PropertyDrawView propertyView : getProperties(property)) {
            setFont(propertyView, font);
        }
    }

    public void setFont(CellView property, Font font) {
        property.design.font = font;
    }

    public void setBackground(AbstractGroup group, Color background, GroupObjectEntity groupObject) {

        for (PropertyDrawView property : getProperties(group, groupObject)) {
            setBackground(property, background);
        }
    }

    public void setBackground(LP prop, Color background) {
        setBackground(prop.property, background);
    }

    public void setBackground(Property prop, Color background) {

        for (PropertyDrawView property : getProperties(prop)) {
            setBackground(property, background);
        }
    }

    public void setBackground(PropertyDrawView property, Color background) {
        property.design.background = background;
    }

    public void setFocusable(AbstractGroup group, boolean focusable, GroupObjectEntity groupObject) {

        for (PropertyDrawView property : getProperties(group, groupObject)) {
            setFocusable(property, focusable);
        }
    }

    public void setFocusable(LP property, boolean focusable) {
        setFocusable(property.property, focusable);
    }

    public void setFocusable(LP property, boolean focusable, GroupObjectEntity groupObject) {
        setFocusable(property.property, focusable, groupObject);
    }

    public void setFocusable(Property property, boolean focusable) {

        for (PropertyDrawView propertyView : getProperties(property)) {
            setFocusable(propertyView, focusable);
        }
    }

    public void setFocusable(Property property, boolean focusable, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(property, groupObject)) {
            setFocusable(propertyView, focusable);
        }
    }

    public void setFocusable(boolean focusable, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(groupObject)) {
            setFocusable(propertyView, focusable);
        }
    }

    public void setFocusable(ObjectEntity objectEntity, boolean focusable, boolean cells) {
        for (CellView property : cells ? getCells(objectEntity) : getProperties(objectEntity.groupTo)) {
            setFocusable(property, focusable);
        }
    }

    public void setFocusable(CellView property, boolean focusable) {
        property.focusable = focusable;
    }

    public void setReadOnly(AbstractGroup group, boolean readOnly, GroupObjectEntity groupObject) {

        for (PropertyDrawView property : getProperties(group, groupObject)) {
            setReadOnly(property, readOnly);
        }
    }

    public void setReadOnly(LP property, boolean readOnly) {
        setReadOnly(property.property, readOnly);
    }

    public void setReadOnly(LP property, boolean readOnly, GroupObjectEntity groupObject) {
        setReadOnly(property.property, readOnly, groupObject);
    }

    public void setReadOnly(Property property, boolean readOnly) {

        for (PropertyDrawView propertyView : getProperties(property)) {
            setReadOnly(propertyView, readOnly);
        }
    }

    public void setReadOnly(Property property, boolean readOnly, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(property, groupObject)) {
            setReadOnly(propertyView, readOnly);
        }
    }

    public void setReadOnly(boolean readOnly, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(groupObject)) {
            setReadOnly(propertyView, readOnly);
        }
    }

    public void setReadOnly(ObjectEntity objectEntity, boolean readOnly, boolean cells) {
        for (CellView property : cells ? getCells(objectEntity) : getProperties(objectEntity.groupTo)) {
            setReadOnly(property, readOnly);
        }
    }

    public void setReadOnly(CellView property, boolean readOnly) {
        property.readOnly = readOnly;
    }

    public void setKeyStroke(KeyStroke keyStroke) {
        this.keyStroke = keyStroke;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setEnabled(AbstractGroup group, boolean readOnly, GroupObjectEntity groupObject) {
        setFocusable(group, readOnly, groupObject);
        setReadOnly(group, !readOnly, groupObject);
    }

    public void setEnabled(LP property, boolean readOnly) {
        setFocusable(property, readOnly);
        setReadOnly(property, !readOnly);
    }

    public void setEnabled(LP property, boolean readOnly, GroupObjectEntity groupObject) {
        setFocusable(property, readOnly, groupObject);
        setReadOnly(property, !readOnly, groupObject);
    }

    public void setEnabled(Property property, boolean readOnly) {
        setFocusable(property, readOnly);
        setReadOnly(property, !readOnly);
    }

    public void setEnabled(Property property, boolean readOnly, GroupObjectEntity groupObject) {
        setFocusable(property, readOnly, groupObject);
        setReadOnly(property, !readOnly, groupObject);
    }

    public void setEnabled(boolean readOnly, GroupObjectEntity groupObject) {
        setFocusable(readOnly, groupObject);
        setReadOnly(!readOnly, groupObject);
    }

    public void setEnabled(ObjectEntity objectEntity, boolean readOnly, boolean cells) {
        setFocusable(objectEntity, readOnly, cells);
        setReadOnly(objectEntity, !readOnly, cells);
    }

    public void setEnabled(CellView property, boolean readOnly) {
        setFocusable(property, readOnly);
        setReadOnly(property, !readOnly);
    }

    public void setEditKey(LP property, KeyStroke keyStroke, GroupObjectEntity groupObject) {
        setEditKey(property.property, keyStroke, groupObject);
    }

    public void setEditKey(LP property, KeyStroke keyStroke) {
        setEditKey(property.property, keyStroke);
    }

    public void setEditKey(Property property, KeyStroke keyStroke, GroupObjectEntity groupObject) {

        for (PropertyDrawView propertyView : getProperties(property, groupObject)) {
            setEditKey(propertyView, keyStroke);
        }
    }

    public void setEditKey(Property property, KeyStroke keyStroke) {

        for (PropertyDrawView propertyView : getProperties(property)) {
            setEditKey(propertyView, keyStroke);
        }
    }

    public void setEditKey(CellView property, KeyStroke keyStroke) {
        property.editKey = keyStroke;
    }

    public void setPanelLabelAbove(AbstractGroup group, boolean panelLabelAbove, GroupObjectEntity groupObject) {

        for (PropertyDrawView property : getProperties(group, groupObject)) {
            setPanelLabelAbove(property, panelLabelAbove);
        }
    }

    public void setPanelLabelAbove(AbstractGroup group, boolean panelLabelAbove) {

        for (PropertyDrawView property : getProperties(group)) {
            setPanelLabelAbove(property, panelLabelAbove);
        }
    }

    public void setPanelLabelAbove(CellView property, boolean panelLabelAbove) {
        property.panelLabelAbove = panelLabelAbove;
    }
}
