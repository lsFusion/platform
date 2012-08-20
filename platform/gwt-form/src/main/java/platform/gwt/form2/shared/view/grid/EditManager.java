package platform.gwt.form2.shared.view.grid;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.dom.client.Element;

public interface EditManager {
    public boolean isCurrentlyEditing();

    void executePropertyEditAction(GridEditableCell editCell, Cell.Context context, Element parent);

    void commitEditing(Object value);

    void cancelEditing();
}
