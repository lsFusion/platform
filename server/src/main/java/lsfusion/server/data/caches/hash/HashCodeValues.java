package lsfusion.server.data.caches.hash;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.translate.MapValuesTranslate;
import lsfusion.server.data.value.Value;

public class HashCodeValues extends HashValues {

    private HashCodeValues() {
    }
    public final static HashValues instance = new HashCodeValues();

    public int hash(Value expr) {
        return expr.hashCode();
    }

    public boolean isGlobal() {
        return true;
    }

    public HashValues filterValues(ImSet<Value> values) {
        return this;
    }

    public HashValues reverseTranslate(MapValuesTranslate translate, ImSet<Value> values) {
        if(translate.identityValues(values))
            return this;
        else
            return null;
    }
}
