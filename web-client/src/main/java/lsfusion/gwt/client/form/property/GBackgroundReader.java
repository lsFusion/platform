package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;

public class GBackgroundReader extends GExtraPropertyReader {

    public GBackgroundReader(){}

    public GBackgroundReader(int readerID, int groupObjectID) {
        super(readerID, groupObjectID, "BACKGROUND");
    }

    public void update(GTableController controller, NativeHashMap<GGroupObjectValue, Object> keys, boolean updateKeys) {
        controller.updateCellBackgroundValues(this, keys);
    }
}
