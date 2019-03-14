package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.server.physics.admin.profiler.ProfiledObject;

public interface PropertyReaderInstance extends ProfiledObject {

    CalcPropertyObjectInstance getPropertyObjectInstance();

    byte getTypeID();

    int getID(); // ID в рамках Type

}