package platform.server.logics.property;

import platform.base.BaseUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * User: DAle
 * Date: 16.11.2010
 * Time: 14:36:18
 */

public class PropertyClassImplement<P extends PropertyInterface> extends PropertyImplement<ValueClassWrapper, P> {

    public PropertyClassImplement(Property<P> property, List<ValueClassWrapper> classes, List<P> interfaces) {
        super(property, BaseUtils.toMap(interfaces, classes));
    }

    public PropertyClassImplement(Property<P> property, ValueClassWrapper vClass, P iFace) {
        super(property, Collections.singletonMap(iFace, vClass));
    }
}
