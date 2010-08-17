package platform.server.form.instance;

import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.type.Type;
import platform.server.form.entity.ObjectEntity;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.Property;
import platform.server.session.Changes;
import platform.server.session.ChangesSession;
import platform.server.session.Modifier;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

// на самом деле нужен collection но при extend'е нужна конкретная реализация
public abstract class ObjectInstance extends CellInstance<ObjectEntity> implements PropertyObjectInterfaceInstance {

    // 0 !!! - изменился объект, 1 !!! - класс объекта, 3 !!! - класса, 4 - классовый вид
    public final static int UPDATED_OBJECT = (1);
    public final static int UPDATED_CLASS = (1 << 1);
    public final static int UPDATED_GRIDCLASS = (1 << 3);

    protected int updated = UPDATED_CLASS | UPDATED_GRIDCLASS;

    public GroupObjectInstance groupTo;

    public String getCaption() {
        return entity.caption;
    }

    public boolean isResetOnApply() {
        return entity.resetOnApply;
    }

    public ObjectInstance(ObjectEntity entity) {
        super(entity);
        this.entity = entity;
    }

    public String toString() {
        return getCaption();
    }

    protected abstract Expr getExpr();
    public Expr getExpr(Set<GroupObjectInstance> classGroup, Map<ObjectInstance, ? extends Expr> classSource) {
        return (classGroup!=null && classGroup.contains(groupTo)?classSource.get(this):getExpr());
    }

    public abstract ValueClass getBaseClass();
    public abstract void setDefaultValue(ChangesSession session) throws SQLException;

    public abstract ObjectValue getObjectValue();
    public DataObject getDataObject() {
        return (DataObject)getObjectValue();
    }

    public boolean isNull() {
        return getObjectValue() instanceof NullValue;
    }

    public abstract ValueClass getGridClass();

    public abstract void changeValue(ChangesSession session, Object changeValue) throws SQLException;
    public abstract void changeValue(ChangesSession session, ObjectValue changeValue) throws SQLException;

    public abstract boolean classChanged(Collection<CustomClass> changedClasses);
    public abstract boolean classUpdated();

    public abstract Type getType();

    public boolean objectUpdated(GroupObjectInstance classGroup) { return groupTo!=classGroup && (updated & UPDATED_OBJECT)!=0; }
    public boolean dataUpdated(Collection<Property> changedProps) { return false; }
    public void fillProperties(Set<Property> properties) { }
    public Expr getExpr(Set<GroupObjectInstance> classGroup, Map<ObjectInstance, ? extends Expr> classSource, Modifier<? extends Changes> modifier) {
        return getExpr(classGroup, classSource);
    }

    public GroupObjectInstance getApplyObject() {
        return groupTo;
    }
}
