package platform.server.view.navigator;

import platform.server.classes.ValueClass;
import platform.server.view.form.PropertyObjectInterface;

import java.util.Set;

public class ObjectNavigator extends CellViewNavigator implements PropertyInterfaceNavigator {

    public GroupObjectNavigator groupTo;

    public String caption;

    public boolean addOnTransaction = false;
    public boolean resetOnApply = false;

    public final ValueClass baseClass;

    public ObjectNavigator(int ID, ValueClass baseClass, String caption) {
        super(ID);
        this.caption = caption;
        this.baseClass = baseClass;
    }

    public PropertyObjectInterface doMapping(Mapper mapper) {
        return mapper.mapObject(this);
    }

    public void fillObjects(Set<ObjectNavigator> objects) {
        objects.add(this);
    }    
}
