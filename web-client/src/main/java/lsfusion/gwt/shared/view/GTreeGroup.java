package lsfusion.gwt.shared.view;

import java.util.ArrayList;
import java.util.List;

public class GTreeGroup extends GComponent {
    public List<GGroupObject> groups = new ArrayList<>();

    public GToolbar toolbar;
    public GFilter filter;
    
    public boolean expandOnClick;

    public int calculateSize() {
        int size = 0;
        for (GGroupObject groupObject : groups) {
            size += groupObject.isRecursive ? 20 * 4 : 20;
        }
        return size;
    }
}
