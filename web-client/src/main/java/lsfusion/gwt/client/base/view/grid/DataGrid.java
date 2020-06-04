/*
/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package lsfusion.gwt.client.base.view.grid;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.*;
import com.google.gwt.dom.client.Style.TableLayout;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.base.view.grid.cell.Cell.Context;
import lsfusion.gwt.client.base.view.grid.cell.CellPreviewEvent;
import lsfusion.gwt.client.base.view.grid.cell.HasCell;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.*;

import static com.google.gwt.dom.client.TableCellElement.TAG_TD;
import static com.google.gwt.dom.client.TableCellElement.TAG_TH;
import static java.lang.Math.min;
import static lsfusion.gwt.client.base.view.ColorUtils.getDisplayColor;
import static lsfusion.gwt.client.base.view.ColorUtils.mixColors;
import static lsfusion.gwt.client.view.StyleDefaults.getFocusedCellBorderColor;
import static lsfusion.gwt.client.view.StyleDefaults.getSelectedRowBackgroundColor;

/**
 * Abstract base class for tabular views that supports paging and columns.
 * <p/>
 * <p>
 * <h3>Columns</h3> The {@link Column} class defines the {@link Cell} used to
 * render a column. Implement {@link Column#getValue(Object)} to retrieve the
 * field value from the row object that will be rendered in the {@link Cell}.
 * </p>
 * <p/>
 * <p>
 * <h3>Headers and Footers</h3> A {@link Header} can be placed at the top
 * (header) or bottom (footer) of the {@link DataGrid}. You can specify
 * a header as text using {@link #addColumn(Column, String)}, or you can create
 * a custom {@link Header} that can change with the value of the cells, such as
 * a column total. The {@link Header} will be rendered every time the row data
 * changes or the table is redrawn. If you pass the same header instance (==)
 * into adjacent columns, the header will span the columns.
 * </p>
 *
 * @param <T> the data type of each row
 */
// we need resizesimplepanel for "scroller" padding in headers (we don't know if there gonna be vertival scroller)
public class DataGrid<T> extends ResizableSimplePanel implements HasData<T>, Focusable, KeyboardRowChangedEvent.HasKeyboardRowChangedHandlers, ColorThemeChangeListener {

    private static GridStyle DEFAULT_STYLE;

    protected static GridStyle getDefaultStyle() {
        if (DEFAULT_STYLE == null) {
            DEFAULT_STYLE = new GridTableStyle();
        }
        return DEFAULT_STYLE;
    }

    public static int nativeScrollbarWidth = AbstractNativeScrollbar.getNativeScrollbarWidth();
    public static int nativeScrollbarHeight = AbstractNativeScrollbar.getNativeScrollbarHeight();

    /**
     * A ClientBundle that provides images for this widget.
     */
    public interface Resources extends ClientBundle {
        /**
         * The loading indicator used while the table is waiting for data.
         */
        @Source("cellTableLoading.gif")
        @ImageResource.ImageOptions(flipRtl = true)
        ImageResource dataGridLoading();

        /**
         * Icon used when a column is sorted in ascending order.
         */
        @Source("sortAscending.png")
        @ImageResource.ImageOptions(flipRtl = true)
        ImageResource sortAscending();

        /**
         * Icon used when a column is sorted in descending order.
         */
        @Source("sortDescending.png")
        @ImageResource.ImageOptions(flipRtl = true)
        ImageResource sortDescending();
    }

    /**
     * The current state of the presenter reflected in the view.
     */
    private State<T> state;

    /**
     * The pending state of the presenter to be pushed to the view.
     */
    private State<T> pendingState;

    /**
     * A boolean indicating that we are in the process of resolving state.
     */
    protected boolean isResolvingState;

    /**
     * A boolean indicating that the widget is refreshing, so all events should be ignored.
     */
    private boolean isRefreshing;

    /**
     * The command used to resolve the pending state.
     */
    boolean isFocused;

    private boolean focusable = true;

    private final List<Column<T, ?>> columns = new ArrayList<>();
    private final Map<Column<T, ?>, String> columnWidths = new HashMap<>();

    private boolean cellWasEditing;
    protected boolean cellIsEditing;

    private HandlerRegistration keyboardSelectionReg;
    /**
     * Indicates, that keyboard styles will be removed, when table loses focus
     */
    private boolean removeKeyboardStylesOnBlur = false;

    protected final GridStyle style;

    private String selectedRowStyle;
    private String selectedRowCellStyle;
    private String focusedCellStyle;
    private String focusedCellLastInRowStyle;
    private String topOfFocusedCellStyle;
    private String rightOfFocusedCellStyle;

    private HeaderBuilder<T> footerBuilder;
    private int nonNullFootersCount = 0;
    private final List<Header<?>> footers = new ArrayList<>();

    /**
     * Indicates that at least one column handles selection.
     */
    private HeaderBuilder<T> headerBuilder;
    private int nonNullHeadersCount = 0;
    private final List<Header<?>> headers = new ArrayList<>();

    /**
     * Indicates that either the headers or footers are dirty, and both should be
     * refreshed the next time the table is redrawn.
     */
    private boolean columnsChanged;
    private boolean headersChanged;

    private CellTableBuilder<T> tableBuilder;

    private final TableWidget tableData;
    protected TableScrollPanel tableDataScroller; // vertical scroller

    private FooterWidget tableFooter;
    private TableScrollPanel tableFooterScroller;

    private HeaderWidget tableHeader;
    private TableScrollPanel tableHeaderScroller;

    private final FlexTable emptyTableWidgetContainer;

    private int renderedRowCount = 0;

    //focused cell indices local to table (aka real indices in rendered portion of the data)
    int oldLocalSelectedRow = -1;
    int oldLocalSelectedCol = -1;

    private int rowHeight = StyleDefaults.VALUE_HEIGHT;

    private int pageIncrement = 30;

    private final boolean noHeaders;
    private final boolean noFooters;
    private final boolean noScrollers;

    // there are two ways to remove outer grid borders :
    // 1) set styles for outer cells and set border-left/top : none there
    // 2) set margins (not paddings since they do not support negative values) to -1
    // first solution is cleaner (since border can be wider than 1px + margin can be used for other purposes, for example scroller padding), however the problem is that in that case the same should be implemented for focus which is pretty tricky
    // the second solution is easier to implement
    // the same problem is in gpivot, and there it is solved the same way (however since there is no focus cell there in history there should be a solution with styles, conditional css)
    public static void removeOuterGridBorders(Widget parent) {
        parent.getElement().getStyle().setMarginLeft(-1, Unit.PX);
        parent.getElement().getStyle().setMarginRight(-1, Unit.PX);
    }

    public DataGrid(GridStyle style, boolean noHeaders, boolean noFooters, boolean noScrollers) {
        this.noHeaders = noHeaders;
        this.noFooters = noFooters;
        this.noScrollers = noScrollers;

        this.state = new State<>();

        this.style = style;

        tableData = new TableWidget();

        Widget tableDataContainer;
        if(!noScrollers) {
            tableDataScroller = new TableScrollPanel(tableData, Style.Overflow.AUTO);
            tableDataContainer = tableDataScroller;
        } else {
            tableDataContainer = tableData;
        }

        if(noHeaders && noFooters) {
            setFillWidget(tableDataContainer);
        } else {
            FlexPanel headerPanel = new FlexPanel(true); // will use flex for headers / footers

            if(!noHeaders) {
                Widget tableHeaderContainer;
                tableHeader = new HeaderWidget();
                if(!noScrollers) {
                    tableHeaderScroller = new TableScrollPanel(tableHeader, Style.Overflow.HIDDEN);
                    tableHeaderContainer = tableHeaderScroller;
                } else
                    tableHeaderContainer = tableHeader;

                headerPanel.add(tableHeaderContainer, GFlexAlignment.STRETCH);

                headerBuilder = new DefaultHeaderBuilder<>(this, false);
            } else
                tableHeaderScroller = null;

            headerPanel.addFillFlex(tableDataContainer, 1);

            if (!noFooters) { // the same as for headers
                Widget tableFooterContainer;
                tableFooter = new FooterWidget();
                if(!noScrollers) {
                    tableFooterScroller = new TableScrollPanel(tableFooter, Style.Overflow.HIDDEN);
                    tableFooterContainer = tableFooterScroller;
                } else
                    tableFooterContainer = tableFooter;

                headerPanel.add(tableFooterContainer, GFlexAlignment.STRETCH);

                footerBuilder = new DefaultHeaderBuilder<>(this, true);
            } else
                tableFooterScroller = null;

            if(!noScrollers) {
                // Synchronize the scroll positions of the three tables.
                // it's not possible to avoid this by splitting to separate horizontal and vertical scroller, since we don't want vertical scroller to be scrolled by horizontal scroller
                tableDataScroller.addScrollHandler(event -> {
                    int scrollLeft = tableDataScroller.getHorizontalScrollPosition();
                    if (tableHeaderScroller != null) {
                        tableHeaderScroller.setHorizontalScrollPosition(scrollLeft);
                    }
                    if (tableFooterScroller != null) {
                        tableFooterScroller.setHorizontalScrollPosition(scrollLeft);
                    }
                });
            } else {
                removeOuterGridBorders(headerPanel);
            }

            setFillWidget(headerPanel);
        }

        // вообще в оригинальном DataGrid в этот контейнер предполагается класть виджеты, информирующие о том, что таблица пуста.
        // но мы пока ограничимся просто наличием самого этого контейнера.
        // нам нужно, чтобы в пустой таблице у него была какая-нибудь минимальная высота (а значит и ненулевая ширина)
        // и чтобы при необходимости он вылезал за viewport скролл-панели, тем самым вызывая горизонтальный скроллбар
        emptyTableWidgetContainer = new FlexTable();
        emptyTableWidgetContainer.setHeight("1px");

        setTableFocusable(true);

        initSinkEvents();
        initStyles(style);

        setKeyboardSelectionHandler(new DataGridKeyboardSelectionHandler<>(this));

        MainFrame.addColorThemeChangeListener(this);
    }

    public void initStyles(GridStyle style) {
        selectedRowStyle = style.dataGridKeyboardSelectedRow();
        selectedRowCellStyle = style.dataGridKeyboardSelectedRowCell();
        focusedCellStyle = style.dataGridFocusedCell();
        focusedCellLastInRowStyle = style.dataGridFocusedCellLastInRow();
        topOfFocusedCellStyle = style.dataGridTopOfFocusedCell();
        rightOfFocusedCellStyle = style.dataGridRightOfFocusedCell();

        addStyleName(style.dataGridWidget());
    }

    public void initSinkEvents() {
        Set<String> eventTypes = new HashSet<>();
        eventTypes.add(BrowserEvents.FOCUS);
        eventTypes.add(BrowserEvents.BLUR);
        eventTypes.add(BrowserEvents.KEYDOWN); // Used for keyboard navigation.
        eventTypes.add(BrowserEvents.KEYUP); // Used by subclasses for selection.
        eventTypes.add(BrowserEvents.CLICK); // Used by subclasses for selection.
        eventTypes.add(BrowserEvents.MOUSEDOWN); // No longer used, but here for legacy support.
        CellBasedWidgetImpl.get().sinkEvents(this, eventTypes);
    }

    public GridStyle getStyle() {
        return style;
    }

    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
        ensurePendingState();
    }

    public int getRowHeight() {
        return rowHeight;
    }

    public int getPageIncrement() {
        return pageIncrement;
    }

    public void setPageIncrement(int pageIncrement) {
        this.pageIncrement = Math.max(1, pageIncrement);
    }

    @Override
    public void setAccessKey(char key) {
    }

    @Override
    public void setTabIndex(int index) {
    }

    @Override
    public int getTabIndex() {
        return 0;
    }

    @Override
    public void setFocus(boolean focused) {
        Element focusHolderElement = getFocusHolderElement();

        if (focused) {
            focusHolderElement.focus();
        } else {
            focusHolderElement.blur();
        }
    }

    @Override
    public HandlerRegistration addCellPreviewHandler(CellPreviewEvent.Handler<T> handler) {
        return addHandler(handler, CellPreviewEvent.getType());
    }

    @Override
    public HandlerRegistration addKeyboardRowChangedHandler(KeyboardRowChangedEvent.Handler handler) {
        return addHandler(handler, KeyboardRowChangedEvent.getType());
    }

    /**
     * Get the overall data size.
     *
     * @return the data size
     */
    @Override
    public int getRowCount() {
        return getCurrentState().getRowCount();
    }

    @Override
    public T getRowValue(int row) {
        checkRowBounds(row);
        return getCurrentState().getRowValue(row);
    }

    public final void setRowValue(int row, T rowValue) {
        setRowData(row, Arrays.asList(rowValue));
    }

    /**
     * <p>
     * Set the complete list of values to display on one page.
     * </p>
     *
     * @param values
     */
    public final void setRowData(List<? extends T> values) {
        setRowData(0, values, true);
    }

    @Override
    public void setRowData(int start, List<? extends T> values) {
        setRowData(start, values, false);
    }

    private void setRowData(int start, List<? extends T> values, boolean all) {
        assert start >= 0;

        int end = start + values.size();

        if (!all && end == start) {
            //0-range update
            return;
        }

        State<T> pending = ensurePendingState();

        // Insert the new values into the data array.
        int currentCnt = pending.rowData.size();
        for (int i = start; i < end; i++) {
            T value = values.get(i - start);
            if (i < currentCnt) {
                pending.rowData.set(i, value);
            } else {
                pending.rowData.add(value);
            }
        }

        if (all) {
            assert start == 0;

            int removeCnt = currentCnt - values.size();
            for (int i = 1; i <= removeCnt; ++i) {
                pending.rowData.remove(currentCnt - i);
            }
            redraw();
        } else {
            // Redraw the range that has been replaced.
            pending.redrawRows(start, end);
        }
    }

    /**
     * Handle browser events. Subclasses should override
     * {@link #onBrowserEvent2(com.google.gwt.user.client.Event)} if they want to extend browser event
     * handling.
     *
     * @see #onBrowserEvent2(com.google.gwt.user.client.Event)
     */
    @Override
    public final void onBrowserEvent(Event event) {
        CellBasedWidgetImpl.get().onBrowserEvent(this, event);

        // Ignore spurious events (such as onblur) while we refresh the table.
        if (isRefreshing) {
            return;
        }

        // Verify that the target is still a child of this widget. IE fires focus
        // events even after the element has been removed from the DOM.
        EventTarget eventTarget = event.getEventTarget();
        if (!Element.is(eventTarget)) {
            return;
        }
        Element target = Element.as(eventTarget);
        if (!getElement().isOrHasChild(Element.as(eventTarget))) {
            return;
        }
        super.onBrowserEvent(event);

        String eventType = event.getType();
        if (BrowserEvents.FOCUS.equals(eventType)) {
            // Remember the focus state.
            isFocused = true;
            onFocus();
        } else if (BrowserEvents.BLUR.equals(eventType)) {
            // Remember the blur state.
            isFocused = false;
            onBlur();
        } else if (BrowserEvents.KEYDOWN.equals(eventType)) {
            // A key event indicates that we already have focus.
            isFocused = true;
        } else if (BrowserEvents.MOUSEDOWN.equals(eventType)
                && CellBasedWidgetImpl.get().isFocusable(Element.as(target))) {
            // If a natively focusable element was just clicked, then we must have
            // focus.
            isFocused = true;
        }

        // Let subclasses handle the event now.
        onBrowserEvent2(event);
    }

    @SuppressWarnings("deprecation")
    protected void onBrowserEvent2(Event event) {
        // Get the event target.
        EventTarget eventTarget = event.getEventTarget();
        if (!Element.is(eventTarget)) {
            return;
        }
        final Element target = event.getEventTarget().cast();

        // Find the cell where the event occurred.
        TableSectionElement tbody = getTableBodyElement();
        TableSectionElement tfoot = getTableFootElement();
        TableSectionElement thead = getTableHeadElement();

        Element cellParent = null;
        Element headerParent = null;
        Element footerParent = null;

        TableSectionElement targetTableSection = null;
        TableCellElement targetTableCell = null;
        String eventType = event.getType();

        if (target == getFocusHolderElement()) {
            // forward events bubbled on table container to current cell,
            // but don't forward blur or focus events on the container itself
            if (!BrowserEvents.BLUR.equals(eventType) &&
                !BrowserEvents.BLUR.equals(eventType) &&
                !BrowserEvents.CLICK.equals(eventType) &&
                !BrowserEvents.DBLCLICK.equals(eventType)) {
                targetTableSection = tbody;

                targetTableCell = getKeyboardSelectedTableCellElement();
                if (targetTableCell != null) {
                    cellParent = targetTableCell.getFirstChildElement();
                }
            }
        } else {
            Element maybeTableCell = null;
            Element cur = target;
            while (cur != null && targetTableSection == null) {
                /*
                 * Found the table section. Return the most recent cell element that we
                 * discovered.
                 */
                if (cur == tbody || cur == tfoot || cur == thead) {
                    targetTableSection = cur.cast(); // We found the table section.
                    if (maybeTableCell != null) {
                        targetTableCell = maybeTableCell.cast();
                        break;
                    }
                }

                // Look for a table cell.
                String tagName = cur.getTagName();
                if (TAG_TD.equalsIgnoreCase(tagName) || TAG_TH.equalsIgnoreCase(tagName)) {
                  /*
                   * Found a table cell, but we can't return yet because it may be part
                   * of a sub table within the a CellTable cell.
                   */
                    maybeTableCell = cur;
                }

                // Look for the most immediate cell parent if not already found.
                if (cellParent == null && tableBuilder.isColumn(cur)) {
                    cellParent = cur;
                }

                /*
                 * Look for the most immediate header parent if not already found. Its
                 * possible that the footer or header will mistakenly identify a header
                 * from the other section, so we remember both. When we eventually reach
                 * the target table section element, we'll know for sure if its a header
                 * of footer.
                 */
                if (headerParent == null && !noHeaders && headerBuilder.isHeader(cur)) {
                    headerParent = cur;
                }
                if (footerParent == null && !noFooters && footerBuilder.isHeader(cur)) {
                    footerParent = cur;
                }

                // Iterate.
                cur = cur.getParentElement();
            }
        }

        if (targetTableCell == null) {
            return;
        }

        /*
         * Forward the event to the associated header, footer, or column.
         */
        TableRowElement targetTableRow = targetTableCell.getParentElement().cast();

        int col = targetTableCell.getCellIndex();
        if (targetTableSection == thead || targetTableSection == tfoot) {
            boolean isHeader = (targetTableSection == thead);
            headerParent = isHeader ? headerParent : footerParent;

            // Fire the event to the header.
            if (headerParent != null) {
                Header<?> header = isHeader ? headerBuilder.getHeader(headerParent) : footerBuilder.getHeader(footerParent);

                if (header != null) {
                    if (consumesEventType(header.getConsumedEvents(), eventType)) {
                        header.onBrowserEvent(headerParent, event);
                    }
                }
            }
        } else if (targetTableSection == tbody) {

            int row = tableBuilder.getRowValueIndex(targetTableRow);

          /*
           * Fire a preview event. The preview event is fired even if the TD does
           * not contain a cell so the selection handler and keyboard handler have a
           * chance to act.
           */
            if (isRowWithinBounds(row)) {
                T value = getRowValue(row);

                Context context = new Context(row, col, value);
                CellPreviewEvent<T> previewEvent = CellPreviewEvent.fire(this, event, this, context, value, cellIsEditing);

                // Pass the event to the cell.
                if (cellParent != null && !previewEvent.isCanceled()) {
                    HasCell<T, ?> column;
                    column = tableBuilder.getColumn(context, value, cellParent);
                    if (column != null) {
                        fireEventToCell(event, eventType, cellParent, value, context, column);
                    }
                }
            }
        }
    }

    /**
     * Set the handler that handles keyboard selection/navigation.
     */
    public void setKeyboardSelectionHandler(CellPreviewEvent.Handler<T> keyboardSelectionReg) {
        // Remove the old manager.
        if (this.keyboardSelectionReg != null) {
            this.keyboardSelectionReg.removeHandler();
            this.keyboardSelectionReg = null;
        }

        // Add the new manager.
        if (keyboardSelectionReg != null) {
            this.keyboardSelectionReg = addCellPreviewHandler(keyboardSelectionReg);
        }
    }

    /**
     * Check if a cell consumes the specified event type.
     *
     * @param cell      the cell
     * @param eventType the event type to check
     * @return true if consumed, false if not
     */
    protected boolean cellConsumesEventType(Cell<?> cell, String eventType) {
        return consumesEventType(cell.getConsumedEvents(), eventType);
    }

    protected boolean consumesEventType(Set<String> consumedEvents, String eventType) {
        return consumedEvents != null && consumedEvents.contains(eventType);
    }

    /**
     * Check that the row is within the correct bounds.
     *
     * @param row row index to check
     * @throws IndexOutOfBoundsException
     */
    protected void checkRowBounds(int row) {
        if (!isRowWithinBounds(row)) {
            throw new IndexOutOfBoundsException("Row index: " + row + ", Row size: " + getRowCount());
        }
    }

    /**
     * Checks that the row is within bounds of the view.
     *
     * @param row row index to check
     * @return true if within bounds, false if not
     */
    protected boolean isRowWithinBounds(int row) {
        return row >= 0 && row < getRowCount();
    }

    /**
     * Checks that the row is currently rendered in the table
     *
     * @param row row index to check
     * @return true if row is rendered, false if not
     */
    protected boolean isRowRendered(int row) {
        return row >= 0 && row < renderedRowCount;
    }

    @Override
    protected void onUnload() {
        isFocused = false;
        super.onUnload();
    }

    /**
     * Make an element focusable or not.
     *
     * @param elem      the element
     * @param focusable true to make focusable, false to make unfocusable
     */
    protected static void setFocusable(Element elem, boolean focusable) {
        if (focusable) {
            elem.setTabIndex(0);
        } else {
            // Chrome: Elements remain focusable after removing the tabIndex, so set it to -1 first.
            elem.setTabIndex(-1);
            elem.removeAttribute("tabIndex");
            elem.removeAttribute("accessKey");
        }
    }

    /**
     * Adds a column to the end of the table.
     *
     * @param col the column to be added
     */
    public void addColumn(Column<T, ?> col) {
        insertColumn(getColumnCount(), col);
    }

    /**
     * Adds a column to the end of the table with an associated header.
     *
     * @param col    the column to be added
     * @param header the associated {@link Header}
     */
    public void addColumn(Column<T, ?> col, Header<?> header) {
        insertColumn(getColumnCount(), col, header);
    }

    /**
     * Inserts a column into the table at the specified index.
     *
     * @param beforeIndex the index to insert the column
     * @param col         the column to be added
     */
    public void insertColumn(int beforeIndex, Column<T, ?> col) {
        insertColumn(beforeIndex, col, (Header<?>) null, (Header<?>) null);
    }

    /**
     * Inserts a column into the table at the specified index with an associated
     * header.
     *
     * @param beforeIndex the index to insert the column
     * @param col         the column to be added
     * @param header      the associated {@link Header}
     */
    public void insertColumn(int beforeIndex, Column<T, ?> col, Header<?> header) {
        insertColumn(beforeIndex, col, header, null);
    }

    /**
     * Inserts a column into the table at the specified index with an associated
     * header and footer.
     *
     * @param beforeIndex the index to insert the column
     * @param col         the column to be added
     * @param header      the associated {@link Header}
     * @param footer      the associated footer (as a {@link Header} object)
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public void insertColumn(int beforeIndex, Column<T, ?> col, Header<?> header, Header<?> footer) {
        if (noHeaders && header != null) {
            throw new UnsupportedOperationException("the table isn't allowed to have header");
        }
        if (noFooters && footer != null) {
            throw new UnsupportedOperationException("the table isn't allowed to have footer");
        }

        // Allow insert at the end.
        if (beforeIndex != getColumnCount()) {
            checkColumnBounds(beforeIndex);
        }

        headers.add(beforeIndex, header);
        footers.add(beforeIndex, footer);
        columns.add(beforeIndex, col);

        // Increment the keyboard selected column.
        int selectedColumn = getKeyboardSelectedColumn();
        if (beforeIndex <= selectedColumn) {
            ensurePendingState().keyboardSelectedColumn = min(selectedColumn + 1, columns.size() - 1);
        }

        // Sink events used by the new column.
        Set<String> consumedEvents = new HashSet<>();
        {
            Set<String> cellEvents = col.getCell().getConsumedEvents();
            if (cellEvents != null) {
                consumedEvents.addAll(cellEvents);
            }
        }
        if (header != null) {
            Set<String> headerEvents = header.getConsumedEvents();
            if (headerEvents != null) {
                consumedEvents.addAll(headerEvents);
            }
        }
        if (footer != null) {
            Set<String> footerEvents = footer.getConsumedEvents();
            if (footerEvents != null) {
                consumedEvents.addAll(footerEvents);
            }
        }
        CellBasedWidgetImpl.get().sinkEvents(this, consumedEvents);

        refreshColumnsAndRedraw();
    }

    public void moveColumn(int oldIndex, int newIndex) {
        checkColumnBounds(oldIndex);
        checkColumnBounds(newIndex);
        if (oldIndex == newIndex) {
            return;
        }

        int selectedColumn = getKeyboardSelectedColumn();
        if (oldIndex == selectedColumn) {
            ensurePendingState().keyboardSelectedColumn = newIndex;
        } else if (oldIndex < selectedColumn && selectedColumn > 0) {
            ensurePendingState().keyboardSelectedColumn--;
        }

        Column<T, ?> column = columns.remove(oldIndex);
        Header<?> header = headers.remove(oldIndex);
        Header<?> footer = footers.remove(oldIndex);

        columns.add(newIndex, column);
        headers.add(newIndex, header);
        footers.add(newIndex, footer);

        // Redraw the table asynchronously.
        refreshColumnsAndRedraw();
    }

    /**
     * Remove a column.
     *
     * @param col the column to remove
     */
    public void removeColumn(Column<T, ?> col) {
        int index = columns.indexOf(col);
        if (index < 0) {
            throw new IllegalArgumentException("The specified column is not part of this table.");
        }
        removeColumn(index);
    }

    /**
     * Remove a column.
     *
     * @param index the column index
     */
    public void removeColumn(int index) {
        if (index < 0 || index >= columns.size()) {
            throw new IndexOutOfBoundsException("The specified column index is out of bounds.");
        }
        columns.remove(index);
        headers.remove(index);
        footers.remove(index);

        int selectedColumn = getKeyboardSelectedColumn();
        // Decrement the keyboard selected column.
        if (index <= selectedColumn && selectedColumn > 0) {
            ensurePendingState().keyboardSelectedColumn = selectedColumn - 1;
        }

        // Redraw the table asynchronously.
        refreshColumnsAndRedraw();

        // We don't unsink events because other handlers or user code may have sunk them intentionally.
    }

    /**
     * Get the column at the specified index.
     *
     * @param col the index of the column to retrieve
     * @return the {@link Column} at the index
     */
    public Column<T, ?> getColumn(int col) {
        checkColumnBounds(col);
        return columns.get(col);
    }

    /**
     * Get the number of columns in the table.
     *
     * @return the column count
     */
    public int getColumnCount() {
        return columns.size();
    }

    /**
     * Get the index of the specified column.
     *
     * @param column the column to search for
     * @return the index of the column, or -1 if not found
     */
    public int getColumnIndex(Column<T, ?> column) {
        return columns.indexOf(column);
    }

    /**
     * Get the width of a {@link Column}.
     *
     * @param column the column
     * @return the width of the column, or null if not set
     * @see #setColumnWidth(Column, double, Unit)
     */
    public String getColumnWidth(Column<T, ?> column) {
        return columnWidths.get(column);
    }

    public CellTableBuilder<T> getTableBuilder() {
        return tableBuilder;
    }

    /**
     * Specify the {@link CellTableBuilder} that will be used to render the row
     * values into the table.
     */
    public void setTableBuilder(CellTableBuilder<T> tableBuilder) {
        assert tableBuilder != null : "tableBuilder cannot be null";
        this.tableBuilder = tableBuilder;
        redraw();
    }

    public void refreshColumnsAndRedraw() {
        columnsChanged = true;
        redraw();
    }

    /**
     * Get the {@link Header} from the footer section that was added with a
     * {@link Column}.
     */
    public Header<?> getFooter(int index) {
        return footers.get(index);
    }

    /**
     * Get the {@link HeaderBuilder} used to generate the footer section.
     */
    public HeaderBuilder<T> getFooterBuilder() {
        return footerBuilder;
    }

    /**
     * Get the {@link Header} from the header section that was added with a
     * {@link Column}.
     */
    public Header<?> getHeader(int index) {
        return headers.get(index);
    }
    public int getHeaderIndex(Header<?> header) {
        return headers.indexOf(header);
    }

    /**
     * Get the index of the column that is currently selected via the keyboard.
     *
     * @return the currently selected column, or -1 if none selected
     */
    public int getKeyboardSelectedColumn() {
        return getCurrentState().keyboardSelectedColumn;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p>
     * The row element may not be the same as the TR element at the specified
     * index if some row values are rendered with additional rows.
     * </p>
     *
     * @param row the row index, relative to the page start
     * @return the row element, or null if it doesn't exists
     * @throws IndexOutOfBoundsException if the row index is outside of the
     *                                   current page
     */
    protected TableRowElement getChildElement(int row) {
        return getRowElementNoFlush(row);
    }

    public boolean isRemoveKeyboardStylesOnBlur() {
        return removeKeyboardStylesOnBlur;
    }

    public void setRemoveKeyboardStylesOnBlur(boolean removeKeyboardStylesOnBlur) {
        this.removeKeyboardStylesOnBlur = removeKeyboardStylesOnBlur;
    }

    /**
     * Set the width of a {@link Column}. The width will persist with the column
     * and takes precedence of any width set via
     * {@link #setColumnWidth(int, double, Unit)}.
     *
     * @param column the column
     * @param width  the width of the column
     */
    public void setColumnWidth(Column<T, ?> column, double width, Unit unit) {
        setColumnWidth(column, width + unit.getType());
    }

    /**
     * Set the width of a {@link Column}. The width will persist with the column
     * and takes precedence of any width set via
     * {@link #setColumnWidth(int, String)}.
     *
     * @param column the column
     * @param width  the width of the column
     */
    public void setColumnWidth(Column<T, ?> column, String width) {
        columnWidths.put(column, width);
        updateColumnWidthImpl(column, width);
    }

    /**
     * Set the {@link HeaderBuilder} used to build the footer section of the
     * table.
     */
    public void setFooterBuilder(HeaderBuilder<T> builder) {
        assert builder != null : "builder cannot be null";
        this.footerBuilder = builder;
        refreshColumnsAndRedraw();
    }

    /**
     * Set the {@link HeaderBuilder} used to build the header section of the
     * table.
     */
    public void setHeaderBuilder(HeaderBuilder<T> builder) {
        assert builder != null : "builder cannot be null";
        this.headerBuilder = builder;
        refreshColumnsAndRedraw();
    }

    /**
     * Set the keyboard selected column index.
     * <p/>
     * <p>
     * If keyboard selection is disabled, this method does nothing.
     * </p>
     * <p/>
     * <p>
     * If the keyboard selected column is greater than the number of columns in
     * the keyboard selected row, the last column in the row is selected, but the
     * column index is remembered.
     * </p>
     *
     * @param column the column index, greater than or equal to zero
     */
    public final void setKeyboardSelectedColumn(int column) {
        setKeyboardSelectedColumn(column, true);
    }

    /**
     * Set the keyboard selected column index and optionally focus on the new
     * cell.
     *
     * @param column     the column index, greater than or equal to zero
     * @param stealFocus true to focus on the new column
     * @see #setKeyboardSelectedColumn(int)
     */
    public void setKeyboardSelectedColumn(int column, boolean stealFocus) {
        assert column >= 0 : "Column must be zero or greater";

        if (getKeyboardSelectedColumn() == column) {
            return;
        }

        if (column < 0) {
            column = 0;
        } else {
            int columnCount = getColumnCount();
            if (column >= columnCount) {
                column = columnCount - 1;
            }
        }

        ensurePendingState().setKeyboardSelectedColumn(column);

        // Reselect the row to move the selected column.
        setKeyboardSelectedRow(getKeyboardSelectedRow(), stealFocus);
    }

    /**
     * Set the keyboard selected row. The row index is the index relative to the
     * current page start index.
     *
     * <p>
     * If keyboard selection is disabled, this method does nothing.
     * </p>
     *
     * <p>
     * If the keyboard selected row is outside of the range of the current page
     * (that is, less than 0 or greater than or equal to the page size), the page
     * or range will be adjusted depending on the keyboard paging policy. If the
     * keyboard paging policy is limited to the current range, the row index will
     * be clipped to the current page.
     * </p>
     *
     * @param row the row index relative to the page start
     */
    public final void setKeyboardSelectedRow(int row) {
        setKeyboardSelectedRow(row, true);
    }

    /**
     * Set the row index of the keyboard selected element.
     *
     * @param row        the row index
     * @param stealFocus true to steal focus
     */
    public void setKeyboardSelectedRow(int row, boolean stealFocus) {
        if (stealFocus) {
            ensurePendingState().keyboardStealFocus = stealFocus;
        }

        int rowCount = getRowCount();
        if (rowCount == 0 || getKeyboardSelectedRow() == row) {
            return;
        }

        if (row < 0) {
            row = 0;
        } else if (row >= rowCount) {
            row = rowCount - 1;
        }

        ensurePendingState().setKeyboardSelectedRow(row);

        KeyboardRowChangedEvent.fire(this);
    }

    protected void setDesiredVerticalScrollPosition(int newVerticalScrollPosition) {
        ensurePendingState().desiredVerticalScrollPosition = newVerticalScrollPosition;
    }

    /**
     * Set the minimum width of the tables in this widget. If the widget become
     * narrower than the minimum width, a horizontal scrollbar will appear so the
     * user can scroll horizontally.
     * <p/>
     * <p>
     * Note that this method is not supported in IE6 and earlier versions of IE.
     * </p>
     *
     * @param value the width
     * @param unit  the unit of the width
     * @see #setTableWidth(double, Unit)
     */
    private double minWidthValue = 0.0;
    private Unit minWidthUnit = null;
    public void setMinimumTableWidth(double value, Unit unit) {
        minWidthValue = value;
        minWidthUnit = unit;

        setBlockMinWidth(value, tableData);
        updateHeaderFooterTableMinimumWidth();
    }

    public void setBlockMinWidth(double value, Widget tableData) {
        tableData.getElement().getStyle().setProperty("minWidth", value, minWidthUnit);
    }

    private void updateHeaderTablePadding(TableWrapperWidget headerWidget, TableScrollPanel tableHeaderScroller) {
        updateTablePadding(hasPadding, headerWidget.tableElement);
//        updateTableMargin(hasPadding, tableHeaderScroller.getElement());

        updateHeaderTableMinimumWidth(headerWidget); // padding changed, we need to reupdate minimumWidth
    }

    /* see DataGrid.css, dataGridTableWrapperWidget description */
    public static void updateTablePadding(boolean hasPadding, Element tableElement) {
        if(hasPadding)
            tableElement.getStyle().setPaddingRight(nativeScrollbarWidth, Unit.PX);
        else
            tableElement.getStyle().clearPaddingRight();
    }
    public static void updateTableMargin(boolean hasPadding, Element tableElement) {
        if(hasPadding)
            tableElement.getStyle().setMarginRight(nativeScrollbarWidth, Unit.PX);
        else
            tableElement.getStyle().clearMarginRight();
    }

    private void updateHeaderTableMinimumWidth(TableWrapperWidget headerWidget) {
        if(minWidthUnit != null)
            setBlockMinWidth(minWidthValue + (hasPadding ? nativeScrollbarWidth : 0), headerWidget);
    }

    private void updateHeaderFooterTablePadding() {
        if(!noHeaders)
            updateHeaderTablePadding(tableHeader, tableHeaderScroller);
        if(!noFooters)
            updateHeaderTablePadding(tableHeader, tableFooterScroller);
    }
    private void updateHeaderFooterTableMinimumWidth() {
        if(!noHeaders)
            updateHeaderTableMinimumWidth(tableHeader);
        if(!noFooters)
            updateHeaderTableMinimumWidth(tableFooter);
    }

    /**
     * Set the width of the tables in this widget. By default, the width is not
     * set and the tables take the available width.
     * <p/>
     * <p>
     * The table width is not the same as the width of this widget. If the tables
     * are narrower than this widget, there will be a gap between the table and
     * the edge of the widget. If the tables are wider than this widget, a
     * horizontal scrollbar will appear so the user can scroll horizontally.
     * </p>
     * <p/>
     * <p>
     * If your table has many columns and you want to ensure that the columns are
     * not truncated, you probably want to use
     * {@link #setMinimumTableWidth(double, Unit)} instead. That will ensure that
     * the table is wide enough, but it will still allow the table to expand to
     * 100% if the user has a wide screen.
     * </p>
     * <p/>
     * <p>
     * Note that setting the width in percentages will not work on older versions
     * of IE because it does not account for scrollbars when calculating the
     * width.
     * </p>
     *
     * @param value the width
     * @param unit  the unit of the width
     * @see #setMinimumTableWidth(double, Unit)
     */
    public void setTableWidth(double value, Unit unit) {
        /*
         * The min-width style attribute doesn't apply to tables, so we set the
         * min-width of the element that contains the table instead. For
         * consistency, we set the width of the container as well.
         */
        assert noHeaders && noScrollers && noFooters;
        tableData.getElement().getStyle().setWidth(value, unit);
    }

    /**
     * Get a row element given the index of the row value.
     *
     * @param row the absolute row value index
     * @return the row element, or null if not found
     */
    private TableRowElement getRowElementNoFlush(int row) {
        if (!isRowRendered(row)) {
            return null;
        }

        return getTableBodyElement().getRows().getItem(row);
    }

    private Element getKeyboardSelectedElement() {
        return getCellParentElement(getKeyboardSelectedTableCellElement());
    }

    protected final TableElement getTableElement() {
        return tableData.tableElement;
    }

    protected final TableSectionElement getTableBodyElement() {
        return tableData.getSection();
    }

    protected final TableSectionElement getTableFootElement() {
        if(!noFooters)
            return tableFooter.getSection();
        return null;
    }

    protected final TableSectionElement getTableHeadElement() {
        if(!noHeaders)
            return tableHeader.getSection();
        return null;
    }

    protected void onFocus() {
        updateSelectedRowStyles();
    }

    protected void onBlur() {
        updateSelectedRowStyles();
    }

    protected Element getFocusHolderElement() {
        if(!noScrollers)
            return tableDataScroller.getElement();
        else
            return tableData.getElement();
    }

    /**
     * Get the keyboard selected element from the selected table cell.
     *
     * @return the keyboard selected element, or null if there is none
     */
    protected Element getCellParentElement(TableCellElement td) {
        if (td == null) {
            return null;
        }

        /*
         * The TD itself is a cell parent, which means its internal structure
         * (including the tabIndex that we set) could be modified by its Cell. We
         * return the TD to be safe.
         */
        if (tableBuilder.isColumn(td)) {
            return td;
        }

        /*
         * The default table builder adds a focusable div to the table cell because
         * TDs aren't focusable in all browsers. If the user defines a custom table
         * builder with a different structure, we must assume the keyboard selected
         * element is the TD itself.
         */
        Element firstChild = td.getFirstChildElement();
        if (firstChild != null && td.getChildCount() == 1 && "div".equalsIgnoreCase(firstChild.getTagName())) {
            return firstChild;
        }

        return td;
    }

    /**
     * Check that the specified column is within bounds.
     *
     * @param col the column index
     * @throws IndexOutOfBoundsException if the column is out of bounds
     */
    private void checkColumnBounds(int col) {
        if (col < 0 || col >= getColumnCount()) {
            throw new IndexOutOfBoundsException("Column index is out of bounds: " + col);
        }
    }

    /**
     * Fire an event to the Cell within the specified {@link TableCellElement}.
     */
    private <C> void fireEventToCell(Event event, String eventType, Element cellParent, final T rowValue, Context context, HasCell<T, C> column) {
        // Check if the cell consumes the event.
        if (preFireEventToCell(event, eventType, cellParent, rowValue, context, column)) {
            fireEventToCellImpl(event, eventType, cellParent, rowValue, context, column);
            postFireEventToCell(event, eventType, cellParent, rowValue, context, column);
        }
    }

    protected <C> boolean preFireEventToCell(Event event, String eventType, Element cellParent, final T rowValue, Context context, HasCell<T, C> column) {
        Cell<C> cell = column.getCell();
        cellWasEditing = cell.isEditing(context, cellParent, column.getValue(rowValue));
        return cellConsumesEventType(cell, eventType);
    }

    protected <C> void fireEventToCellImpl(Event event, String eventType, Element cellParent, final T rowValue, Context context, HasCell<T, C> column) {
        // Fire the event to the cell.
        column.getCell().onBrowserEvent(context, cellParent, column.getValue(rowValue), event);
    }

    protected <C> void postFireEventToCell(Event event, String eventType, Element cellParent, final T rowValue, Context context, HasCell<T, C> column) {
        // Reset focus if needed.
        cellIsEditing = column.getCell().isEditing(context, cellParent, column.getValue(rowValue));
        if (cellWasEditing && !cellIsEditing) {
            setFocus(true);
        }
    }

    /**
     * Get the {@link TableCellElement} that is currently keyboard selected.
     *
     * @return the table cell element, or null if not selected
     */
    private TableCellElement getKeyboardSelectedTableCellElement() {
        int colIndex = getKeyboardSelectedColumn();
        if (colIndex < 0) {
            return null;
        }

        // Do not use getRowElement() because that will flush the presenter.
        int rowIndex = getKeyboardSelectedRow();
        TableRowElement tr = getRowElementNoFlush(rowIndex);
        if (tr != null) {
            int cellCount = tr.getCells().getLength();
            if (cellCount > 0) {
                int column = min(colIndex, cellCount - 1);
                return tr.getCells().getItem(column);
            }
        }
        return null;
    }

    /**
     * Apply a style to a row and all cells in the row.
     *
     * @param tr        the row element
     * @param rowStyle  the style to apply to the row
     * @param cellStyle the style to apply to the cells
     * @param add       true to add the style, false to remove
     */
    private void setRowStyleName(TableRowElement tr, String rowStyle, String cellStyle, boolean add) {
        setStyleName(tr, rowStyle, add);
        NodeList<TableCellElement> cells = tr.getCells();
        for (int i = 0; i < cells.getLength(); i++) {
            setStyleName(cells.getItem(i), cellStyle, add);
        }
    }

    /**
     * Update the width of all instances of the specified column. A column
     * instance may appear multiple times in the table.
     *
     * @param column the column to update
     * @param width  the width of the column, or null to clear the width
     */
    private void updateColumnWidthImpl(Column<T, ?> column, String width) {
        int columnCount = getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            if (columns.get(i) == column) {
                setColumnWidthImpl(i, width);
            }
        }
    }

    private void setColumnWidthImpl(int column, String width) {
        if (width == null) {
            tableData.ensureTableColElement(column).getStyle().clearWidth();
            if(!noHeaders)
                tableHeader.ensureTableColElement(column).getStyle().clearWidth();
            if(!noFooters)
                tableFooter.ensureTableColElement(column).getStyle().clearWidth();
        } else {
            tableData.ensureTableColElement(column).getStyle().setProperty("width", width);
            if(!noHeaders)
                tableHeader.ensureTableColElement(column).getStyle().setProperty("width", width);
            if(!noFooters)
                tableFooter.ensureTableColElement(column).getStyle().setProperty("width", width);
        }
    }

    /**
     * Get the index of the row that is currently selected via the keyboard,
     * relative to the page start index.
     *
     * <p>
     * This is not same as the selected row in the {@link com.google.gwt.view.client.SelectionModel}. The
     * keyboard selected row refers to the row that the user navigated to via the
     * keyboard or mouse.
     * </p>
     *
     * @return the currently selected row, or -1 if none selected
     */
    public int getKeyboardSelectedRow() {
        return getCurrentState().getKeyboardSelectedRow();
    }

    /**
     * Get the value that the user selected.
     *
     * @return the value, or null if a value was not selected
     */
    public T getKeyboardSelectedRowValue() {
        int selectedRow = getKeyboardSelectedRow();
        return isRowWithinBounds(selectedRow) ? getRowValue(selectedRow) : null;
    }

    /**
     * Check whether or not the data set is empty. That is, the row count is exactly 0.
     *
     * @return true if data set is empty
     */
    public boolean isEmpty() {
        return getRowCount() == 0;
    }

    /**
     * Redraw the list with the current data.
     */
    public void redraw() {
        ensurePendingState().redrawAllRows();
        ensurePendingState().redrawAllColumns();
    }

    public void redrawColumns(Set<? extends Column> updatedColumns) {
        redrawColumns(updatedColumns, true);
    }

    public void redrawColumns(Set<? extends Column> updatedColumns, boolean redrawAllRows) {
        ensurePendingState().redrawColumns(updatedColumns);
        if (redrawAllRows) {
            pendingState.redrawAllRows();
        }
    }

    /**
     * Mark the headers as dirty and redraw the table.
     */
    public void refreshHeaders() {
        if (pendingState != null) {
            //already pending some redraw, so render headers there...
            headersChanged = true;
        } else {
            //just update headers
            updateHeadersImpl(false);
        }
    }

    /**
     * Get the current state of the presenter.
     *
     * @return the pending state if one exists, otherwise the state
     */
    private State<T> getCurrentState() {
        return pendingState == null ? state : pendingState;
    }

    /**
     * Ensure that a pending {@link DefaultState} exists and return it.
     *
     * @return the pending state
     */
    private State<T> ensurePendingState() {
        if (isResolvingState) {
            throw new IllegalStateException("It's not allowed to change current state, when resolving pending state");
        }

        // Create the pending state if needed.
        if (pendingState == null) {
            pendingState = new State<>(state);
            PendingStateCommand.schedule(this);
        }

        return pendingState;
    }

    protected void resolvePendingStateBeforeUpdate() {
        if (isResolvingState || pendingState == null) {
            return;
        }

        isResolvingState = true;
    }

    protected void resolvePendingStateUpdate() {
        if (!isResolvingState) {
            return;
        }
        if (columnsChanged) {
            refreshColumnWidths();
        }

        if (columnsChanged || headersChanged) {
            updateHeadersImpl(columnsChanged);
        }

        boolean stealFocus = pendingState.keyboardStealFocus;

        pendingState.pendingWasFocused = isFocused = isFocused || stealFocus;

        isRefreshing = true;

        updateTableData(pendingState);

        isRefreshing = false;
    }

    protected void preResolvePendingStateAfterUpdate() {
        int rowToShow = pendingState.keyboardSelectedRowSet ? pendingState.keyboardSelectedRow : -1;
        int colToShow = pendingState.keyboardSelectedColumnSet ? pendingState.keyboardSelectedColumn : -1;

        //force browser-flush
        preAfterUpdateTableData(pendingState, rowToShow, colToShow);
    }

    protected void resolvePendingStateAfterUpdate() {
        if (!isResolvingState) {
            return;
        }

        boolean stealFocus = pendingState.keyboardStealFocus;

        isRefreshing = true;

        afterUpdateTableData(pendingState);

        isRefreshing = false;

        headersChanged = false;
        columnsChanged = false;

        if (stealFocus && !cellIsEditing) {
            setFocus(true);
        }

        updateSelectedRowStyles();

        state = pendingState;
        pendingState = null;
        isResolvingState = false;
    }

    private void preAfterUpdateTableData(State<T> pendingState, int rowToShow, int colToShow) {
        if(!noScrollers) {
            preAfterUpdateScrollHorizontal(pendingState, colToShow);
            preAfterUpdateScrollVertical(pendingState, rowToShow);
        }
    }

    private void preAfterUpdateScrollHorizontal(State<T> pendingState, int colToShow) {
        int colCount = getColumnCount();

        int scrollWidth = tableDataScroller.getClientWidth();
        boolean hasVerticalScroll = scrollWidth != tableDataScroller.getOffsetWidth();
        int currentScrollLeft = tableDataScroller.getHorizontalScrollPosition();

        //scroll column to visible if needed
        int scrollLeft = currentScrollLeft;
        if (colToShow >=0 && colToShow < colCount && renderedRowCount > 0) {
            TableRowElement tr = tableData.tableElement.getRows().getItem(0);
            TableCellElement td = tr.getCells().getItem(colToShow);

            int columnLeft = td.getOffsetLeft();
            int columnRight = columnLeft + td.getOffsetWidth();
            if (columnRight >= scrollLeft + scrollWidth) {
                scrollLeft = columnRight - scrollWidth;
            }

            if (columnLeft < scrollLeft) {
                scrollLeft = columnLeft;
            }
        }

        pendingState.pendingScrollLeft = scrollLeft;
        pendingState.pendingCurrentScrollLeft = currentScrollLeft;
        pendingState.pendingHasVerticalScroll = hasVerticalScroll;
    }

    private void preAfterUpdateScrollVertical(State<T> pendingState, int rowToShow) {
        int rowCount = pendingState.rowData.size();

        int tableHeight = 0;
        if (rowCount > 0) {
            TableRowElement lastRowElement = getChildElement(rowCount - 1);
            tableHeight = lastRowElement.getOffsetTop() + lastRowElement.getClientHeight();
        }

        int scrollHeight = tableDataScroller.getClientHeight();
        int currentScrollTop = tableDataScroller.getVerticalScrollPosition();
        int scrollTop = pendingState.desiredVerticalScrollPosition;

        if (tableHeight <= scrollHeight) {
            scrollTop = 0;
        } else {
            if (scrollTop < 0) {
                scrollTop = currentScrollTop;
            }
            if (scrollTop > tableHeight - scrollHeight) {
                scrollTop = tableHeight - scrollHeight;
            }
        }

        //scroll row to visible if needed
        if (rowToShow >= 0 && rowToShow < rowCount) {
            TableRowElement rowElement = getChildElement(rowToShow);
            int rowTop = rowElement.getOffsetTop();
            int rowBottom = rowTop + rowElement.getClientHeight();
            if (rowBottom >= scrollTop + scrollHeight) {
                scrollTop = rowBottom - scrollHeight;
            }

            if (rowTop < scrollTop) {
                scrollTop = rowTop;
            }
        }

        pendingState.pendingScrollTop = scrollTop;
        pendingState.pendingCurrentScrollTop = currentScrollTop;
    }

    boolean hasPadding = false;
    private void afterUpdateTableData(State<T> pendingState) {
        if(!noScrollers) {
            afterUpdateScrollHorizontal(pendingState);
            afterUpdateScrollVertical(pendingState);
        }
    }

    private void afterUpdateScrollVertical(State<T> pendingState) {
        if (pendingState.pendingScrollTop != pendingState.pendingCurrentScrollTop) {
            tableDataScroller.setVerticalScrollPosition(pendingState.pendingScrollTop);
        }
    }

    private void afterUpdateScrollHorizontal(State<T> pendingState) {
        if(!noScrollers) {
            boolean newPadding = pendingState.pendingHasVerticalScroll;
            if(hasPadding != newPadding) {
                hasPadding = newPadding;
                updateHeaderFooterTablePadding();
            }
        }

        if (pendingState.pendingScrollLeft != pendingState.pendingCurrentScrollLeft) {
            tableDataScroller.setHorizontalScrollPosition(pendingState.pendingScrollLeft);
        }
    }

    private void updateTableData(State<T> pendingState) {
        int pendingNewRowCnt = pendingState.getRowCount();
        if (pendingState.needRedraw() || columnsChanged || renderedRowCount != pendingNewRowCnt) {
            tableBuilder.update(tableData.getSection(), pendingState.rowData, 0, pendingNewRowCnt, columnsChanged);

            renderedRowCount = pendingNewRowCnt;
            if(!noScrollers) {
                if (renderedRowCount == 0) {
                    tableDataScroller.setWidget(emptyTableWidgetContainer);
                } else {
                    tableDataScroller.setWidget(tableData);
                }
            }
        }
    }

    private void updateSelectedRowStyles() {
        int newLocalSelectedRow = getKeyboardSelectedRow();
        int newLocalSelectedCol = getKeyboardSelectedColumn();

        NodeList<TableRowElement> rows = tableData.tableElement.getRows();
        NodeList<TableRowElement> headerRows = noHeaders ? null : tableHeader.tableElement.getTHead().getRows();
        int tableRowCount = rows.getLength();

        if (oldLocalSelectedRow >= 0 && oldLocalSelectedRow <= tableRowCount) {
            // if old row index is out of bounds only by 1, than we still need to clean top cell, which is in bounds
            TableRowElement oldTR = rows.getItem(oldLocalSelectedRow);
            boolean rowChanged = oldLocalSelectedRow != newLocalSelectedRow;
            if (oldTR != null && rowChanged) {
                updateSelectedRowCellsBackground(oldTR, oldLocalSelectedRow, false);
            }
            if (rowChanged || oldLocalSelectedCol != newLocalSelectedCol) {
                if (oldLocalSelectedCol >= 0 && oldLocalSelectedCol < getColumnCount()) {
                    setFocusedCellStyles(oldLocalSelectedRow, oldLocalSelectedCol, rows, headerRows, false, !rowChanged);
                }
            }
        }

        if (newLocalSelectedRow >= 0 && newLocalSelectedRow < tableRowCount) {
            TableRowElement newTR = rows.getItem(newLocalSelectedRow);

            boolean setSelected = focusable && (isFocused || !isRemoveKeyboardStylesOnBlur());
            updateSelectedRowCellsBackground(newTR, newLocalSelectedRow , setSelected);

            setFocusedCellStyles(newLocalSelectedRow, newLocalSelectedCol, rows, headerRows, focusable && isFocused, setSelected);

            oldLocalSelectedRow = newLocalSelectedRow;
            oldLocalSelectedCol = newLocalSelectedCol;
        }
    }
    
    private void updateSelectedRowCellsBackground(TableRowElement tr, int localSelectedRow, boolean selected) {
        NodeList<TableCellElement> cells = tr.getCells();
        for (int i = 0; i < cells.getLength(); i++) {
            String background = null;
            if (tableBuilder instanceof GPropertyTableBuilder<?> && isRowWithinBounds(localSelectedRow)) {
                background = ((GPropertyTableBuilder<T>) tableBuilder).getBackground(getRowValue(localSelectedRow), localSelectedRow, i);
            }
            Style tdStyle = cells.getItem(i).getStyle();
            if (selected) {
                if (background != null) {
                    tdStyle.setBackgroundColor(mixColors(background, getSelectedRowBackgroundColor()));
                } else {
                    tdStyle.setBackgroundColor(getSelectedRowBackgroundColor());
                }
            } else if (background != null) {
                tdStyle.setBackgroundColor(getDisplayColor(background));
            } else {
                tdStyle.clearBackgroundColor();
            }
        }
    }

    private void setFocusedCellStyles(int row, int column, NodeList<TableRowElement> rows, NodeList<TableRowElement> headerRows, boolean focused, boolean inSelectedRow) {
        int rowCount = rows.getLength();
        int columnCount = getColumnCount();

        TableRowElement tr = rows.getItem(row);

        if (tr != null) {
            NodeList<TableCellElement> cells = tr.getCells();
            TableCellElement td = cells.getItem(column);
            if (td != null) {
                String background = null;
                if (tableBuilder instanceof GPropertyTableBuilder<?> && isRowWithinBounds(row)) {
                    background = ((GPropertyTableBuilder<T>) tableBuilder).getBackground(getRowValue(row), row, column);
                }

                Style tdStyle = td.getStyle();
                if (focused) {
                    if (background == null) {
                        tdStyle.setBackgroundColor(StyleDefaults.getFocusedCellBackgroundColor());
                    } else {
                        tdStyle.setBackgroundColor(getDisplayColor(background));
                    }
                } else if (!inSelectedRow && background != null) { // is painted as selected row
                    tdStyle.setBackgroundColor(getDisplayColor(background));
                }

                if (drawFocusedCellBorder()) {
                    setFocusedCellBorder(td, focused);
                    setFocusedCellLastInRowBorder(td, focused && column == columnCount - 1);
                }
            }

            if (drawFocusedCellBorder() && column < columnCount - 1) {
                TableCellElement rightTD = cells.getItem(column + 1);
                if (rightTD != null) {
                    setRightOfFocusedCellBorder(rightTD, focused);
                }
            }
        }

        if (drawFocusedCellBorder()) {
            if (row == 0 && headerRows != null) {
                TableRowElement headerTR = headerRows.getItem(0);
                if (headerTR != null) {
                    TableCellElement headerCell = headerTR.getCells().getItem(column);
                    if (headerCell != null) {
                        setTopOfFocusedCellBorder(headerCell, focused);
                    }
                }
            } else if (row > 0 && row <= rowCount) {
                TableCellElement topTD = rows.getItem(row - 1).getCells().getItem(column);
                if (topTD != null) {
                    setTopOfFocusedCellBorder(topTD, focused);
                }
            }
        }
    }
    
    private void setFocusedCellBorder(TableCellElement td, boolean focused) {
        if (focused) {
            td.getStyle().setProperty("borderBottom", "1px solid " + getFocusedCellBorderColor());
            td.getStyle().setProperty("borderLeft", "1px solid " + getFocusedCellBorderColor());
        } else {
            td.getStyle().clearProperty("borderBottom");
            td.getStyle().clearProperty("borderLeft");
        }
    }
    
    private void setFocusedCellLastInRowBorder(TableCellElement td, boolean focused) {
        if (focused) {
            td.getStyle().setProperty("borderRight", "1px solid " + getFocusedCellBorderColor());
        } else {
            td.getStyle().clearProperty("borderRight");
        }
    }
    
    private void setRightOfFocusedCellBorder(TableCellElement td, boolean focused) {
        if (focused) {
            td.getStyle().setProperty("borderLeft", "1px solid " + getFocusedCellBorderColor());
        } else {
            td.getStyle().clearProperty("borderLeft");
        }
    }

    private void setTopOfFocusedCellBorder(TableCellElement td, boolean focused) {
        if (focused) {
            td.getStyle().setProperty("borderBottom", "1px solid " + getFocusedCellBorderColor());
        } else {
            td.getStyle().clearProperty("borderBottom");
        }
    }
    
    protected boolean drawFocusedCellBorder() {
        return true;
    }

    private void updateHeadersImpl(boolean columnsChanged) {
        if (!noHeaders)
            headerBuilder.update(columnsChanged);
        if (!noFooters)
            footerBuilder.update(columnsChanged);
    }

    private void refreshColumnWidths() {
        int columnCount = getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            setColumnWidthImpl(i, getColumnWidth(columns.get(i)));
        }

        // Hide unused col elements in the colgroup.
        if(!noHeaders)
            tableHeader.hideUnusedColumns(columnCount);
        tableData.hideUnusedColumns(columnCount);
        if(!noFooters)
            tableFooter.hideUnusedColumns(columnCount);
    }

    public void setTableFocusable(boolean focusable) {
        this.focusable = focusable;
        setFocusable(getFocusHolderElement(), focusable);
    }

    @Override
    public void colorThemeChanged() {
        refreshColumnsAndRedraw();
    }

    /**
     * Represents the pending state of the presenter.
     *
     * @param <T> the data type of the presenter
     */
    private static class State<T> {
        protected final List<T> rowData;
        protected int keyboardSelectedRow = 0;
        protected int keyboardSelectedColumn = 0;

        private boolean keyboardStealFocus = false;

        private int desiredVerticalScrollPosition = -1;

        private boolean keyboardSelectedRowSet = false;
        private boolean keyboardSelectedColumnSet = false;

        /**
         * The list of ranges that has to be redrawn.
         */
        private List<Range> rangesToRedraw = null;
        private Set<Column> columnsToRedraw = null;
        private boolean redrawAllColumns = false;

        //values, used for delayed updating
        int pendingScrollTop;
        int pendingScrollLeft;
        int pendingCurrentScrollTop;
        int pendingCurrentScrollLeft;
        boolean pendingWasFocused;
        boolean pendingHasVerticalScroll;

        public State() {
            this(new ArrayList<T>());
        }

        public State(ArrayList<T> rowData) {
            this.rowData = rowData;
        }

        public State(State<T> state) {
            this(new ArrayList<>(state.rowData));
            this.keyboardSelectedRow = state.getKeyboardSelectedRow();
            this.keyboardSelectedColumn = state.getKeyboardSelectedColumn();
        }

        public int getKeyboardSelectedRow() {
            return keyboardSelectedRow;
        }

        public void setKeyboardSelectedRow(int keyboardSelectedRow) {
            this.keyboardSelectedRow = keyboardSelectedRow;
            this.keyboardSelectedRowSet = true;
        }

        public int getKeyboardSelectedColumn() {
            return keyboardSelectedColumn;
        }

        public void setKeyboardSelectedColumn(int keyboardSelectedColumn) {
            this.keyboardSelectedColumn = keyboardSelectedColumn;
            this.keyboardSelectedColumnSet = true;
        }

        public int getRowCount() {
            return rowData.size();
        }

        public T getRowValue(int index) {
            return rowData.get(index);
        }

        private List<Range> createRangesToRedraw() {
            if (rangesToRedraw == null) {
                rangesToRedraw = new ArrayList<>();
            }
            return rangesToRedraw;
        }

        private Set<Column> createColumnsToRedraw() {
            if (columnsToRedraw == null) {
                columnsToRedraw = new HashSet<>();
            }
            return columnsToRedraw;
        }

        /**
         * Update the range data to be redraw.
         *
         * @param start the start index
         * @param end   the end index (excluded)
         */
        public void redrawRows(int start, int end) {
            // merge ranges on insertion

            if (createRangesToRedraw().size() == 0) {
                rangesToRedraw.add(new Range(start, end));
                return;
            }

            int rangeCnt = rangesToRedraw.size();

            int startIndex;
            for (startIndex = 0; startIndex < rangeCnt; ++startIndex) {
                Range prevRange = rangesToRedraw.get(startIndex);
                if (prevRange.start > start) {
                    break;
                }
            }

            if (startIndex > 0) {
                //range previous range

                //prevRange.start < start because of cycle break condition
                Range prevRange = rangesToRedraw.get(startIndex - 1);
                if (prevRange.end >= end) {
                    //fully included to the bigger range
                    return;
                }

                if (prevRange.end >= start) {
                    //merge prevRange with this one
                    rangeCnt--;
                    startIndex--;
                    rangesToRedraw.remove(startIndex);
                    start = prevRange.start;
                }
            }

            //merge following ranges
            if (startIndex < rangeCnt) {
                Range nextRange = rangesToRedraw.get(startIndex);
                while (nextRange.start <= end) {
                    rangeCnt--;
                    rangesToRedraw.remove(startIndex);
                    if (nextRange.end > end) {
                        //extend current range and break if merged range is farther
                        end = nextRange.end;
                        break;
                    }

                    if (startIndex == rangeCnt) {
                        break;
                    }

                    nextRange = rangesToRedraw.get(startIndex);
                }
            }

            rangesToRedraw.add(startIndex, new Range(start, end));
        }

        public void redrawAllRows() {
            createRangesToRedraw().clear();
            redrawRows(0, rowData.size());
        }

        public boolean needRedraw() {
            return rangesToRedraw != null && (redrawAllColumns || columnsToRedraw != null);
        }

        public void redrawColumns(Set<? extends Column> updatedColumns) {
            if (!redrawAllColumns) {
                createColumnsToRedraw().addAll(updatedColumns);
            }
        }

        public void redrawAllColumns() {
            redrawAllColumns = true;
            columnsToRedraw = null;
        }

        public int[] getColumnsToRedraw(DataGrid thisGrid) {
            if (redrawAllColumns) {
                return null;
            }

            int[] columnsToRedrawIndices = null;
            if (columnsToRedraw != null) {
                columnsToRedrawIndices = new int[columnsToRedraw.size()];
                int i = 0;
                for (Column column : columnsToRedraw) {
                    columnsToRedrawIndices[i++] = thisGrid.getColumnIndex(column);
                }
            }
            return columnsToRedrawIndices;
        }
    }

    private static PendingStateCommand pendingStateCommandStatic;

    private static class PendingStateCommand implements Scheduler.ScheduledCommand {
        private final ArrayList<DataGrid> grids = new ArrayList<>();

        private PendingStateCommand(DataGrid grid) {
            grids.add(grid);
        }

        @Override
        public void execute() {
            for (DataGrid grid : grids) {
                grid.resolvePendingStateBeforeUpdate();
            }
            for (DataGrid grid : grids) {
                grid.resolvePendingStateUpdate();
            }
            for (DataGrid grid : grids) {
                grid.preResolvePendingStateAfterUpdate();
            }
            for (DataGrid grid : grids) {
                grid.resolvePendingStateAfterUpdate();
            }
            pendingStateCommandStatic = null;
        }

        public static void schedule(DataGrid grid) {
            if (pendingStateCommandStatic == null) {
                pendingStateCommandStatic = new PendingStateCommand(grid);
                Scheduler.get().scheduleFinally(pendingStateCommandStatic);
            } else {
                pendingStateCommandStatic.add(grid);
            }
        }

        private void add(DataGrid grid) {
            grids.add(grid);
        }
    }

    private abstract static class TableWrapperWidget extends Widget {
        protected final TableElement tableElement;
        protected final TableColElement colgroupElement;
        protected final TableSectionElement sectionElement;

        public TableWrapperWidget() {
            tableElement = Document.get().createTableElement();

            tableElement.addClassName("dataGridTableWrapperWidget");

            colgroupElement = tableElement.appendChild(Document.get().createColGroupElement());

            sectionElement = createSectionElement();
        }

        protected abstract TableSectionElement createSectionElement();

        /**
         * Get the {@link TableColElement} at the specified index, creating it if
         * necessary.
         *
         * @param index the column index
         * @return the {@link TableColElement}
         */
        public TableColElement ensureTableColElement(int index) {
            // Ensure that we have enough columns.
            for (int i = colgroupElement.getChildCount(); i <= index; i++) {
                colgroupElement.appendChild(Document.get().createColElement());
            }
            return colgroupElement.getChild(index).cast();
        }

        /**
         * Hide columns that aren't used in the table.
         *
         * @param start the first unused column index
         */
        void hideUnusedColumns(int start) {
            // Remove all col elements that appear after the last column.
            int colCount = colgroupElement.getChildCount();
            for (int i = start; i < colCount; i++) {
                colgroupElement.removeChild(ensureTableColElement(start));
            }
        }

        public TableSectionElement getSection() {
            return sectionElement;
        }
    }

    private class HeaderWidget extends TableWrapperWidget {
        public HeaderWidget() {
            this(style.dataGridHeader());
        }

        protected HeaderWidget(String headerStyle) {
            tableElement.addClassName(headerStyle);
            setElement(tableElement);
        }

        @Override
        protected TableSectionElement createSectionElement() {
            return tableElement.createTHead();
        }
    }

    private class FooterWidget extends HeaderWidget {
        private FooterWidget() {
            super(style.dataGridFooter());
        }

        @Override
        protected TableSectionElement createSectionElement() {
            return tableElement.createTFoot();
        }
    }

    private class TableWidget extends TableWrapperWidget {
//        private final DivElement containerElement;

        public TableWidget() {
//            containerElement = Document.get().createDivElement();
//            containerElement.getStyle().setOutlineStyle(OutlineStyle.NONE);
//            containerElement.appendChild(tableElement);
//
////            setFocusable(containerElement, true);
//
////            GwtClientUtils.setupFillParent(tableElement);
//
//            setElement(containerElement);

            setElement(tableElement);
        }

        @Override
        protected TableSectionElement createSectionElement() {
            if (tableElement.getTBodies().getLength() > 0) {
                return tableElement.getTBodies().getItem(0);
            } else {
                return tableElement.appendChild(Document.get().createTBodyElement());
            }
        }
    }

    @Override
    public void onResize() {
        if(!isResolvingState) // hack, because in preAfterUpdateTableData there is browser event flush, which causes scheduleDeferred flush => HeaderPanel.forceLayout => onResize and IllegalStateException, everything is really twisted here, so will just suppress ensurePendingState
            ensurePendingState();
        super.onResize();
    }

    /**
     * Default implementation of a keyboard navigation handler for tables that
     * supports navigation between cells.
     *
     * @param <T> the data type of each row
     */
    public static class DataGridKeyboardSelectionHandler<T> implements CellPreviewEvent.Handler<T> {
        /**
         * The number of rows to jump when PAGE_UP or PAGE_DOWN
         */
        private final DataGrid<T> display;

        /**
         * Construct a new keyboard selection handler for the specified table.
         *
         * @param display the display being handled
         */
        public DataGridKeyboardSelectionHandler(DataGrid<T> display) {
            this.display = display;
        }

        public DataGrid<T> getDisplay() {
            return display;
        }

        @Override
        public void onCellPreview(CellPreviewEvent<T> event) {
            NativeEvent nativeEvent = event.getNativeEvent();
            String eventType = event.getNativeEvent().getType();
            if (BrowserEvents.KEYDOWN.equals(eventType) && !event.isCellEditing()) {
                if (handleKeyEvent(event)) {
                    handledEvent(event);
                }
            } else if (BrowserEvents.CLICK.equals(eventType) ||
                    BrowserEvents.FOCUS.equals(eventType) ||
                    (BrowserEvents.MOUSEDOWN.equals(eventType) && nativeEvent.getButton() == Event.BUTTON_RIGHT)) {

                boolean stealFocus = false;
                if (BrowserEvents.CLICK.equals(eventType)) {
                    // If a natively focusable element was just clicked, then do not steal focus.
                    Element target = Element.as(event.getNativeEvent().getEventTarget());
                    stealFocus = !CellBasedWidgetImpl.get().isFocusable(target);
                }

                int col = event.getColumn();
                int row = event.getIndex();
                if ((display.getKeyboardSelectedColumn() != col) || (display.getKeyboardSelectedRow() != row)) {

                    display.setKeyboardSelectedRow(row, true);

                    // Update the column index.
                    display.setKeyboardSelectedColumn(col, true);
                    
                    handledEvent(event);
                } else if (stealFocus) {
                    display.setFocus(stealFocus);
                }

                // Do not cancel the event as the click may have occurred on a Cell.
            }
        }

        protected void handledEvent(CellPreviewEvent<?> event) {
            event.setCanceled(true);
            event.getNativeEvent().preventDefault();
        }

        public boolean handleKeyEvent(CellPreviewEvent<T> event) {
            int keyCode = event.getNativeEvent().getKeyCode();
            switch (keyCode) {
                case KeyCodes.KEY_RIGHT:
                    return nextColumn(true);
                case KeyCodes.KEY_LEFT:
                    return nextColumn(false);
                case KeyCodes.KEY_DOWN:
                    return nextRow(true);
                case KeyCodes.KEY_UP:
                    return nextRow(false);
                case KeyCodes.KEY_PAGEDOWN:
                    display.setKeyboardSelectedRow(display.getKeyboardSelectedRow() + display.pageIncrement);
                    return true;
                case KeyCodes.KEY_PAGEUP:
                    display.setKeyboardSelectedRow(display.getKeyboardSelectedRow() - display.pageIncrement);
                    return true;
                case KeyCodes.KEY_HOME:
                    display.setKeyboardSelectedRow(0);
                    return true;
                case KeyCodes.KEY_END:
                    display.setKeyboardSelectedRow(display.getRowCount() - 1);
                    return true;
            }
            return false;
        }

        protected boolean nextRow(boolean down) {
            int rowIndex = getDisplay().getKeyboardSelectedRow();
            display.setKeyboardSelectedRow(down ? rowIndex + 1 : rowIndex - 1);
            return true;
        }

        protected boolean nextColumn(boolean forward) {
            if (display.renderedRowCount > 0) {
                int rowCount = display.getRowCount();
                int columnCount = display.getColumnCount();

                int rowIndex = display.getKeyboardSelectedRow();
                int columnIndex = display.getKeyboardSelectedColumn();

                if (forward) {
                    if (columnIndex == columnCount - 1) {
                        if (rowIndex != rowCount - 1) {
                            columnIndex = 0;
                            rowIndex++;
                        }
                    } else {
                        columnIndex++;
                    }
                } else {
                    if (columnIndex == 0) {
                        if (rowIndex != 0) {
                            columnIndex = columnCount - 1;
                            rowIndex--;
                        }
                    } else {
                        columnIndex--;
                    }
                }

                display.setKeyboardSelectedRow(rowIndex);
                display.setKeyboardSelectedColumn(columnIndex);
            }
            //allways handle KEY_LEFT/RIGHT
            return true;
        }
    }
}
