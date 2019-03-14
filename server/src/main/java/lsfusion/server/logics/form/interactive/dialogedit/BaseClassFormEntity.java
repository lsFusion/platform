package lsfusion.server.logics.form.interactive.dialogedit;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.property.PropertyClassImplement;

public abstract class BaseClassFormEntity extends FormEntity {

    public final ObjectEntity object;

    protected BaseClassFormEntity(BaseLogicsModule LM, CustomClass cls, String canonicalName, LocalizedString caption) {
        super(canonicalName, caption, LM.getVersion());
        
        Version version = LM.getVersion();

        object = addSingleGroupObject(cls, version);

        ImList<PropertyClassImplement> idProps = LM.getRecognizeGroup().getProperties(cls, version);
        if(idProps.isEmpty()) {
            // we need at least one prop (otherwise there will be no grid in dialog)
            LCP objValueProp = LM.getObjValueProp(this, object);
            PropertyDrawEntity objectValue = addPropertyDraw(objValueProp, version, object);
            objectValue.setEditType(PropertyEditType.READONLY);
        }

        addPropertyDraw(object, version, LM.getBaseGroup());
    }

}