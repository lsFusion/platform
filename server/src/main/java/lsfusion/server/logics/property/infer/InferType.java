package lsfusion.server.logics.property.infer;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.property.*;

public class InferType implements AlgType {
    public InferType() {
    }

    public static InferInfoType PREVBASE = new InferInfoType();
    public static InferType PREVSAME = new InferType();
    public static InferType RESOLVE = new InferType(); // PREVSAME

    public <P extends PropertyInterface> ClassWhere<Object> getClassValueWhere(CalcProperty<P> property) {
        return getClassValueWhere(property, null);
    }

    public <P extends PropertyInterface> ClassWhere<Object> getClassValueWhere(CalcProperty<P> property, ExClassSet valueClasses) {
        assert this != RESOLVE;
        return property.inferClassValueWhere(this, valueClasses);
    }

    public <P extends PropertyInterface> ImMap<P, ValueClass> getInterfaceClasses(CalcProperty<P> property, ExClassSet valueClasses) {
        return property.inferGetInterfaceClasses(this, valueClasses);
    }

    public <P extends PropertyInterface> ValueClass getValueClass(CalcProperty<P> property) {
        return property.inferGetValueClass(this);
    }

    public <P extends PropertyInterface> boolean isInInterface(CalcProperty<P> property, ImMap<P, ? extends AndClassSet> interfaceClasses, boolean isAny) {
        assert this != RESOLVE;
        return property.inferIsInInterface(interfaceClasses, isAny, this);
    }

    public <T extends PropertyInterface, P extends PropertyInterface> void checkExclusiveness(CalcProperty<T> property, String caption, CalcProperty<P> intersect, String intersectCaption, ImRevMap<P, T> map) {
        assert this != RESOLVE;        
        property.inferCheckExclusiveness(caption, intersect, intersectCaption, map, this);
    }

    public <T extends PropertyInterface, P extends PropertyInterface> void checkContainsAll(CalcProperty<T> property, CalcProperty<P> intersect, String caption, ImRevMap<P, T> map, CalcPropertyInterfaceImplement<T> value) {
        assert this != RESOLVE;
        property.inferCheckContainsAll(intersect, caption, map, this, value);
    }

    public <T extends PropertyInterface, P extends PropertyInterface> void checkAllImplementations(CalcProperty<T> property, ImList<CalcProperty<P>> intersects, ImList<ImRevMap<P, T>> maps) {
        assert this != RESOLVE;        
        property.inferCheckAllImplementations(intersects, maps, this);
    }

    public AlgInfoType getAlgInfo() {
        assert this != RESOLVE;
        return PREVBASE;
    }
}
