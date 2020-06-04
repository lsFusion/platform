/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import lsfusion.gwt.client.base.view.grid.cell.Cell.Context;
import lsfusion.gwt.client.base.view.grid.cell.HasCell;

import java.util.Collection;
import java.util.List;

/**
 * Builder used to construct a CellTable.
 *
 * @param <T> the row data type
 */
public interface CellTableBuilder<T> {

    void update(TableSectionElement tbodyElement, List<T> values, int minRenderedRow, int renderedRowCount, boolean columnsChanged);

    /**
     * Return the column containing an element.
     *
     * @param context  the context for the element
     * @param rowValue the value for the row corresponding to the element
     * @param elem     the elm that the column contains
     * @return the immediate column containing the element
     */
    HasCell<T, ?> getColumn(Context context, T rowValue, Element elem);

    /**
     * Return all the columns that this table builder has rendered.
     */
    Collection<HasCell<T, ?>> getColumns();

    /**
     * Get the index of the primary row from the associated
     * {@link TableRowElement} (an TR element).
     *
     * @param row the row element
     * @return the row value index
     */
    int getRowValueIndex(TableRowElement row);

    /**
     * Return if an element contains a cell. This may be faster to execute than
     * {@link #getColumn(Context, Object, Element)}.
     *
     * @param elem the element of interest
     */
    boolean isColumn(Element elem);
}
