package lsfusion.client.form.object.table.grid.controller;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.form.property.ClientPropertyDraw;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

import static lsfusion.base.BaseUtils.nullEquals;

public class GridSelectionController {
    private GridTable table;
    private Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Map<ClientGroupObjectValue, Object>> selectedCells = new HashMap<>();
    private List<ClientGroupObjectValue> gridKeys = new ArrayList<>();
    private Map<ClientGroupObjectValue, Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Object>> temporaryValues = new LinkedHashMap<>();
    private boolean temporarySelectionAddition;
    private boolean directionDown;

    private Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Map<ClientGroupObjectValue, Object>> mergedSelection = new HashMap<>();

    private Pair<ClientPropertyDraw, ClientGroupObjectValue> firstColumn;
    private Pair<ClientPropertyDraw, ClientGroupObjectValue> lastColumn;

    public GridSelectionController(GridTable table) {
        this.table = table;
        resetSelection();
    }

    private void commitSelection() {
        selectedCells = mergedSelection;
    }

    private List<ClientGroupObjectValue> getRowKeys() {
        return table.getRowKeys();
    }

    private List<Pair<ClientPropertyDraw, ClientGroupObjectValue>> getProperties() {
        return table.getVisibleProperties(true);
    }

    private int indexOf(Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn) {
        if (propertyColumn != null) {
            return table.getModel().getPropertyIndex(propertyColumn.first, propertyColumn.second);
        } else {
            return -1;
        }
    }

    public void addProperty(ClientPropertyDraw newProperty) {
        Pair<ClientPropertyDraw, ClientGroupObjectValue> property = new Pair<>(newProperty, ClientGroupObjectValue.EMPTY);
        if (!selectedCells.containsKey(property)) {
            selectedCells.put(property, new HashMap<>());
        }
    }

    public void removeProperty(ClientPropertyDraw property, List<ClientGroupObjectValue> columnKeys) {
        for (ClientGroupObjectValue columnKey : columnKeys) {
            Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn = new Pair<>(property, columnKey);
            if (nullEquals(firstColumn, lastColumn) && nullEquals(firstColumn, propertyColumn)) {
                firstColumn = null;
                lastColumn = null;
                return;
            }

            List<Pair<ClientPropertyDraw, ClientGroupObjectValue>> visibleProperties = getProperties();
            int removeIndex = visibleProperties.indexOf(propertyColumn);
            if (propertyColumn.equals(firstColumn)) {
                int newIndex = indexOf(firstColumn) > indexOf(lastColumn) ? removeIndex - 1 : removeIndex + 1;
                newIndex = Math.max(Math.min(newIndex, visibleProperties.size() - 1), 0);
                firstColumn = visibleProperties.get(newIndex);
            } else if (propertyColumn.equals(lastColumn)) {
                int newIndex = indexOf(lastColumn) > indexOf(firstColumn) ? removeIndex - 1 : removeIndex + 1;
                newIndex = Math.max(Math.min(newIndex, visibleProperties.size() - 1), 0);
                lastColumn = visibleProperties.get(newIndex);
            }
        }
    }

    private void mergeSelections() {
        int firstColumnIndex = indexOf(firstColumn);
        int lastColumnIndex = indexOf(lastColumn);
        if (temporaryValues.isEmpty() ||  firstColumnIndex == -1 || lastColumnIndex == -1) {
            mergedSelection = selectedCells;
            return;
        }

        Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Map<ClientGroupObjectValue, Object>> newMap = new HashMap<>(selectedCells);
        for (int column = Math.min(firstColumnIndex, lastColumnIndex); column <= Math.max(firstColumnIndex, lastColumnIndex); column++) {
            Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn = table.getColumnProperty(column);
            Map<ClientGroupObjectValue, Object> valueMap = selectedCells.get(propertyColumn) != null ?
                    new HashMap<>(selectedCells.get(propertyColumn)) :
                    new HashMap<>();
            for (ClientGroupObjectValue key : temporaryValues.keySet()) {
                if (temporaryValues.containsKey(key) && temporaryValues.get(key).containsKey(propertyColumn)) {
                    if (temporarySelectionAddition) {
                        valueMap.put(key, temporaryValues.get(key).get(propertyColumn));
                    } else {
                        valueMap.remove(key);
                    }
                }
                newMap.put(propertyColumn, valueMap);
            }
        }
        mergedSelection = newMap;
    }

    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        lastColumn = table.getColumnProperty(columnIndex);
        directionDown = rowIndex > table.getSelectedRow();
        Pair<ClientPropertyDraw, ClientGroupObjectValue> newProperty = table.getColumnProperty(columnIndex);

        if (toggle) {
            if (extend) {
                //temporarySelectionAddition равен значению выделенности первой ячейки, т.к. уже сработал toggle && !extend
                modifyTemporaryValues(rowIndex);
            } else {
                resetTemporarySelection();
                if (!removeFromSelection(newProperty, rowIndex)) {
                    addToSelection(newProperty, rowIndex);
                }
                Pair<ClientPropertyDraw, ClientGroupObjectValue> selProp = table.getColumnProperty(columnIndex);
                temporarySelectionAddition = selectedCells.get(selProp) != null && selectedCells.get(selProp).containsKey(getRowKeys().get(rowIndex));
                addToTemporaryValues(rowIndex);
            }
        } else {
            if (extend) {
                //если без shift, resetSelection() уже сработал в !toggle && !extend
                modifyTemporaryValues(rowIndex);
            } else {
                firstColumn = table.getColumnProperty(columnIndex);
                temporarySelectionAddition = true;
                resetSelection();
                if (table.getTableModel().getRowCount() != 0) {
                    addToSelection(newProperty, rowIndex);
                    addToTemporaryValues(rowIndex);
                }
            }
        }
    }

    private void addToSelection(Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn, int rowIndex) {
        if (selectedCells.get(propertyColumn) != null) {
            selectedCells.get(propertyColumn).put(getRowKeys().get(rowIndex), table.getValueAt(rowIndex, indexOf(propertyColumn)));
        } else {
            resetSelection();
        }
    }

    private void modifyTemporaryValues(int currentRow) {
        int previousRow = table.getSelectedRow();
        currentRow = Math.min(currentRow, table.getRowKeys().size() - 1);  // IndexOutOfBoundsException fix
        if (currentRow != -1 && previousRow != -1) {
            int start = temporaryValues.isEmpty() ? previousRow : table.getRowKeys().indexOf(BaseUtils.lastSetElement(temporaryValues.keySet()));
            start = directionDown ? start + 1 : start - 1;

            Map<ClientGroupObjectValue, Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Object>> segment = new LinkedHashMap<>();
            for (int i = start; (directionDown && i <= currentRow) || (!directionDown && i >= currentRow); i = (directionDown ? i + 1 : i - 1)) {
                segment.put(table.getRowKeys().get(i), KeyController.getRowData(table, i));
            }

            temporaryValues = KeyController.mergeSelectionMaps(temporaryValues, segment);
            mergeSelections();
        }
    }

    private boolean removeFromSelection(Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn, int rowIndex) {
        if (selectedCells.get(propertyColumn) != null && selectedCells.get(propertyColumn).containsKey(getRowKeys().get(rowIndex))) {
            selectedCells.get(propertyColumn).remove(getRowKeys().get(rowIndex));
            return true;
        }
        return false;
    }

    private void resetTemporarySelection() {
        temporaryValues.clear();
        mergeSelections();
    }

    private void addToTemporaryValues(int rowIndex) {
        Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Object> valueMap = new HashMap<>();
        for (Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn : getProperties()) {
            valueMap.put(propertyColumn, table.getValueAt(rowIndex, indexOf(propertyColumn)));
        }
        temporaryValues.put(getRowKeys().get(rowIndex), valueMap);
    }

    public void resetSelection() {
        resetTemporarySelection();
        selectedCells.clear();
        for (Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn : getProperties()) {
            selectedCells.put(propertyColumn, new HashMap<>());
        }

        gridKeys = new ArrayList<>();
        keysChanged(true);
    }

    public boolean isCellSelected(Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn, ClientGroupObjectValue key) {
        return mergedSelection.get(propertyColumn) != null && mergedSelection.get(propertyColumn).containsKey(key);
    }

    public void mousePressed(int firstColumnSelectionIndex) {
        firstColumn = table.getColumnProperty(firstColumnSelectionIndex);
    }

    public void mouseReleased() {
        commitSelection();
    }

    public void recordingStarted(int column) {
        firstColumn = table.getColumnProperty(column);
    }

    public void submitShiftSelection(Map<ClientGroupObjectValue, Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Object>> recording) {
        temporaryValues = recording;
        mergeSelections();
    }

    public void recordingStopped() {
        commitSelection();
    }

    public void keysChanged(boolean movedDown) {
        if (table.getRowCount() == 0 || gridKeys.containsAll(getRowKeys()))
            return;

        int start;
        int end;
        if (gridKeys.isEmpty()) {
            start = 0;
            end = getRowKeys().size() - 1;
        } else {
            if (movedDown) {
                start = getRowKeys().indexOf(gridKeys.get(gridKeys.size() - 1)) + 1;
                end = getRowKeys().size() - 1;
            } else {
                start = 0;
                end = getRowKeys().indexOf(gridKeys.get(0)) - 1;
            }
        }
        if (start == -1 || end == -1)
            return;
        List<ClientGroupObjectValue> differenceMap = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            differenceMap.add(getRowKeys().get(i));
        }

        if (!gridKeys.containsAll(differenceMap))
            if (movedDown) {
                gridKeys.addAll(differenceMap);
            } else {
                differenceMap.addAll(gridKeys);
                gridKeys = differenceMap;
            }
    }

    private Object modifyIfString(Object value, boolean multiline) {
        if (value != null && value instanceof String) {
            if (multiline) {
                if (((String) value).contains("\n") || ((String) value).contains("\t")) {
                    value = "\"" + ((String) value).replace("\"", "\"\"") + "\"";
                }
            // убрал замену ради единообразия - в панельных свойствах и вэбе переносы не вырезаем
            // теоретически негативно повлияет только на вставку скопированного значения в гриде
            // следует удалить, если не возникнет необходимость вернуть
//            } else {
//                value = ((String) value).replaceAll("\n", " ");
//                value = ((String) value).replaceAll("\t", " ");
            }
        }
        return value;
    }

    public Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Pair<List<ClientGroupObjectValue>, List<Object>>> getSelectedCells() {
        Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Pair<List<ClientGroupObjectValue>, List<Object>>> cellsMap
                = new HashMap<>();
        for (Map.Entry<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Map<ClientGroupObjectValue, Object>> e : selectedCells.entrySet()) {
            Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn = e.getKey();
            List<ClientGroupObjectValue> keysValues = new ArrayList<>();
            List<Object> oldValues = new ArrayList<>();
            for (Map.Entry<ClientGroupObjectValue, Object> columnValue : e.getValue().entrySet()) {
                keysValues.add(columnValue.getKey());
                oldValues.add(columnValue.getValue());
            }
            if (!keysValues.isEmpty()) {
                cellsMap.put(propertyColumn, new Pair<>(keysValues, oldValues));
            }
        }
        return cellsMap;
    }

    public boolean hasSingleSelection() {
        int selectedCellsCount = 0;
        for (Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn : selectedCells.keySet()) {
            selectedCellsCount += selectedCells.get(propertyColumn).size();
        }
        //сейчас возвращает true, даже если селекшн совсем пустой. сделано для более красивой работы CTRL+C/CTRL+V -
        //там при пустом выделении используется в качестве выделенной фокусная ячейка
        return selectedCellsCount <= 1;
    }

    public String getSelectedTableString() throws ParseException {
        commitSelection();

        if (selectedCells.isEmpty())
            return null;

        int firstPropertyIndex = -1, lastPropertyIndex = -1;
        List<Pair<ClientPropertyDraw, ClientGroupObjectValue>> visibleProperties = getProperties();

        for (int i = 0; i < visibleProperties.size(); i++) {
            Map<ClientGroupObjectValue, Object> visibleSelectedProperty = selectedCells.get(visibleProperties.get(i));
            if (visibleSelectedProperty != null && !visibleSelectedProperty.isEmpty()) {
                if (firstPropertyIndex == -1)
                    firstPropertyIndex = i;
                lastPropertyIndex = i;
            }
        }

        //если выделена одна ячейка (или ни одной) и нажали CTRL+C, копируем текущее значение
        if (hasSingleSelection() && firstPropertyIndex != -1) {
            Pair<ClientPropertyDraw, ClientGroupObjectValue> firstProperty = visibleProperties.get(firstPropertyIndex);
            Object value = modifyIfString(table.getSelectedValue(firstProperty.first, firstProperty.second), false);
            Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn = visibleProperties.get(firstPropertyIndex);
            return value == null ? "" : propertyColumn.first.formatString(value);
        }

        if (firstPropertyIndex == -1 || lastPropertyIndex == -1)
            return null;

        String str = "";

        for (ClientGroupObjectValue key : gridKeys) {
            boolean addString = false;
            String rowString = "";
            for (int i = firstPropertyIndex; i <= lastPropertyIndex; i++) {
                Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn = visibleProperties.get(i);
                Object value = null;
                if (selectedCells.get(propertyColumn).containsKey(key)) {
                    addString = true;
                    value = modifyIfString(selectedCells.get(propertyColumn).get(key), true);
                }
                rowString += (value == null ? "" : propertyColumn.first.formatString(value));
                if (i < lastPropertyIndex) {
                    rowString += "\t";
                }
            }
            if(addString) str +=rowString +"\n";
        }

        return str;
    }

    public int getQuantity() {
        int quantity = 0;
        for (Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn : mergedSelection.keySet()) {
            quantity += mergedSelection.get(propertyColumn).size();
        }
        return quantity;
    }

    public int getNumbersQuantity() {
        int quantity = 0;
        for (Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn : mergedSelection.keySet()) {
            for (ClientGroupObjectValue key : mergedSelection.get(propertyColumn).keySet()) {
                if (mergedSelection.get(propertyColumn).get(key) instanceof Number) {
                    quantity++;
                }
            }
        }
        return quantity;
    }

    public BigDecimal getSum() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn : mergedSelection.keySet()) {
            for (ClientGroupObjectValue key : mergedSelection.get(propertyColumn).keySet()) {
                Object value = mergedSelection.get(propertyColumn).get(key);
                if(value != null) {
                    if (value instanceof BigDecimal)
                        sum = sum.add((BigDecimal) value);
                    else if (value instanceof Number) {
                        sum = sum.add(BigDecimal.valueOf(((Number) value).doubleValue()));
                    }
                }
            }
        }
        return sum;
    }
}
