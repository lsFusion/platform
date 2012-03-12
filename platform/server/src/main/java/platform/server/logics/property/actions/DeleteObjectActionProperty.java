package platform.server.logics.property.actions;

import platform.interop.KeyStrokes;
import platform.server.classes.BaseClass;
import platform.server.classes.ValueClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.OrderInstance;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeleteObjectActionProperty extends CustomActionProperty {

    public DeleteObjectActionProperty(BaseClass baseClass) {
        super("delete", ServerResourceBundle.getString("logics.property.actions.delete"), new ValueClass[]{baseClass});
    }

    public String getCode() {
        return "delete";
    }

    public void execute(ExecutionContext context) throws SQLException {
        // после удаления выбираем соседний объект
        DataObject nearObject = null;
        List<Map<ObjectInstance, DataObject>> keys = ((CustomObjectInstance) context.getSingleObjectInstance()).groupTo.keys.keyList();
        for (Map<ObjectInstance, DataObject> key : keys) {
            if (key.values().contains(context.getSingleKeyValue()) && nearObject == null) {
                int index = keys.indexOf(key);
                if (keys.size() == 1)
                    continue;
                index = index == keys.size() - 1 ? index - 1 : index + 1;
                nearObject = keys.get(index).get(context.getSingleObjectInstance());
            }
        }

        if (context.isInFormSession() && context.getSingleObjectInstance() != null) {
            context.getFormInstance().changeClass((CustomObjectInstance) context.getSingleObjectInstance(), context.getSingleKeyValue(), -1);
        } else {
            context.getSession().changeClass(context.getSingleKeyValue(), null, context.isGroupLast());
        }

        if (nearObject != null)
            ((CustomObjectInstance) context.getSingleObjectInstance()).groupTo.addSeek(context.getSingleObjectInstance(), nearObject, false);
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity<?> form) {
        super.proceedDefaultDraw(entity, form);
        entity.shouldBeLast = true;
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.editKey = KeyStrokes.getDeleteActionPropertyKeyStroke();
        propertyView.design.setIconPath("delete.png");
        propertyView.showEditKey = false;
    }
}
