package lsfusion.server.logics.navigator;

import lsfusion.server.logics.form.struct.property.async.AsyncExec;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class NavigatorFolder extends NavigatorElement {
    public NavigatorFolder(String canonicalName, LocalizedString caption) {
        super(canonicalName, caption);

        setImage("open.png", DefaultIcon.OPEN);
    }

    @Override
    public boolean isLeafElement() {
        return false;
    }

    @Override
    public byte getTypeID() {
        return 1;
    }

    @Override
    public AsyncExec getAsyncExec() {
        return null;
    }
}
