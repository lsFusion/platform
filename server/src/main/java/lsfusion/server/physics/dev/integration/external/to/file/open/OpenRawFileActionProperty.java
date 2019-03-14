package lsfusion.server.physics.dev.integration.external.to.file.open;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.OpenFileClientAction;
import lsfusion.server.logics.classes.StaticFormatFileClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.data.DataObject;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.language.ScriptingActionProperty;

import java.sql.SQLException;
import java.util.Iterator;

public class OpenRawFileActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface sourceInterface;
    private final ClassPropertyInterface nameInterface;

    public OpenRawFileActionProperty(BaseLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        sourceInterface = i.next();
        nameInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            ObjectValue sourceObject = context.getKeyValue(sourceInterface);
            RawFileData source = (RawFileData) sourceObject.getValue();
            String name = (String) context.getKeyValue(nameInterface).getValue();

            if (sourceObject instanceof DataObject && source != null) {
                String extension = BaseUtils.firstWord(((StaticFormatFileClass) ((DataObject) sourceObject).objectClass).getOpenExtension(source), ",");
                context.delayUserInteraction(new OpenFileClientAction(source, name, extension));
            }


        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}