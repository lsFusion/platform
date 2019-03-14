package lsfusion.server.logics.property.init;

import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.Property;

public class FinalizeCalcAbstractTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Initializing abstract properties";
    }

    protected void runTask(Property property) {
        if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract()) {
            property.finalizeInit();
        }
    }
}