package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import lsfusion.gwt.client.base.view.grid.cell.AbstractCell;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.table.view.GPropertyTable;

import static com.google.gwt.dom.client.BrowserEvents.*;

public class GridEditableCell extends AbstractCell<Object> {

    private final GPropertyTable table;
    private final boolean cellTypeCanChange;

    boolean isEditing = false;

    public GridEditableCell(GPropertyTable table) {
        this(table, false);
    }

    public GridEditableCell(GPropertyTable table, boolean cellTypeCanChange) {
        super(CLICK, KEYDOWN, KEYPRESS, BLUR, CONTEXTMENU);
        this.table = table;
        this.cellTypeCanChange = cellTypeCanChange;
    }

    @Override
    public boolean isEditing(Context context, Element parent, Object value) {
        return isEditing;
    }

    public void setEditing(boolean editing) {
        isEditing = editing;
    }

    @Override
    public void onBrowserEvent(final Context context, final Element parent, final Object value, NativeEvent event) {
        // не используется, т.к. перехватывается GPropertyTable
    }

    @Override
    public String getCellType(Context context) {
        if (cellTypeCanChange) {
            GPropertyDraw property = table.getProperty(context);
            return property == null ? null : property.sID;
        }
        return null;
    }

    @Override
    public void renderDom(Context context, DivElement cellElement, Object value) {
        if (isEditing) {
            return;
        }

        GPropertyDraw property = table.getProperty(context);
        if (property != null) {
            GridCellRenderer cellRenderer = property.getGridCellRenderer();

            GFont tableFont = null;
            boolean isSingleCell = true;
            if (table instanceof GGridPropertyTable) {
                tableFont = ((GGridPropertyTable) table).font;
                isSingleCell = false;
            }
            cellRenderer.render(cellElement, tableFont, value, isSingleCell);
        }
    }

    @Override
    public void updateDom(Context context, DivElement cellElement, Object value) {
        if (isEditing) {
            return;
        }

        GPropertyDraw property = table.getProperty(context);
        if (property != null) {
            GridCellRenderer cellRenderer = property.getGridCellRenderer();

            GFont tableFont = null;
            boolean isSingleCell = true;
            if (table instanceof GGridPropertyTable) {
                tableFont = ((GGridPropertyTable) table).font;
                isSingleCell = false;
            }
            cellRenderer.renderDynamic(cellElement, tableFont, value, isSingleCell);
        }
    }
}