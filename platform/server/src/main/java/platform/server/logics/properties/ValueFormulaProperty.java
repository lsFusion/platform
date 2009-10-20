package platform.server.logics.properties;

import platform.server.data.classes.ConcreteValueClass;

import java.util.Collection;

// вообще Collection
abstract class ValueFormulaProperty<T extends FormulaPropertyInterface> extends FormulaProperty<T> {

    ConcreteValueClass value;

    protected ValueFormulaProperty(String sID, String caption, Collection<T> interfaces, ConcreteValueClass value) {
        super(sID, caption, interfaces);

        this.value = value;
    }
}
