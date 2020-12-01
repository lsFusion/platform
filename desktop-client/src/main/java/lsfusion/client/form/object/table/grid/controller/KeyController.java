package lsfusion.client.form.object.table.grid.controller;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.form.property.ClientPropertyDraw;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class KeyController {
    private final GridTable table;
    public boolean isRecording = false;
    private Map<ClientGroupObjectValue, Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Object>> values = new LinkedHashMap<>();

    public KeyController(GridTable table) {
        this.table = table;
    }

    public Set<ClientGroupObjectValue> getKeys() {
        return values.keySet();
    }

    public Map<ClientGroupObjectValue, Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Object>> getValues() {
        return values;
    }

    public void startRecording(int firstIndex) {
        if (firstIndex == -1 || table.getRowCount() <= 0)
            return;
        if (!isRecording) {
            values.clear();
            values.put(table.getRowKeys().get(firstIndex), getRowData(table, firstIndex));
        }
        isRecording = true;
    }

    public void stopRecording() {
        isRecording = false;
    }

    public void completeRecording(int lastIndex) {
        if (isRecording && lastIndex != -1 && !values.isEmpty())
            record(table.getRowKeys().indexOf(BaseUtils.lastSetElement(values.keySet())) < lastIndex, lastIndex);
    }

    public static Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Object> getRowData(GridTable table, int rowIndex) {
        Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Object> map = new HashMap<>();
        for (Pair<ClientPropertyDraw, ClientGroupObjectValue> propertyColumn : table.getVisibleProperties(false)) {
            map.put(propertyColumn, table.getValueAt(rowIndex, table.getModel().getPropertyIndex(propertyColumn.first, propertyColumn.second)));
        }
        return map;
    }

    public void record(boolean down, int index) {
        if (!isRecording) return;

        int previousRow = table.getSelectedRow();
        if (index != -1 && previousRow != -1) {
            int start = values.isEmpty() ? previousRow : table.getRowKeys().indexOf(BaseUtils.lastSetElement(values.keySet()));
            start = down ? start + 1 : start - 1;

            Map<ClientGroupObjectValue, Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Object>> segment = new LinkedHashMap<>();
            for (int i = start; (down && i <= index) || (!down && i >= index); i = (down ? i + 1 : i - 1)) {
                segment.put(table.getRowKeys().get(i), getRowData(table, i));
            }

            values = mergeSelectionMaps(values, segment);
        }
    }

    public void clear() {
        values.clear();
    }

    public static Map<ClientGroupObjectValue, Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Object>> mergeSelectionMaps(Map<ClientGroupObjectValue, Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Object>> primary, Map<ClientGroupObjectValue, Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Object>> secondary) {
        boolean intersect = false;
        for (ClientGroupObjectValue value : secondary.keySet()) {
            if (primary.keySet().contains(value)) {
                intersect = true;
                break;
            }
        }

        if (intersect) {
            if (primary.keySet().containsAll(secondary.keySet())) {
                primary.remove(BaseUtils.lastSetElement(primary.keySet()));
                secondary.remove(BaseUtils.lastSetElement(secondary.keySet()));
                for (ClientGroupObjectValue key : secondary.keySet()) {
                    primary.remove(key);
                }
            } else {
                for (ClientGroupObjectValue key : primary.keySet()) {
                    secondary.remove(key);
                }
                Map<ClientGroupObjectValue, Map<Pair<ClientPropertyDraw, ClientGroupObjectValue>, Object>> tmp = new LinkedHashMap<>();
                tmp.put(primary.keySet().iterator().next(), primary.values().iterator().next());
                tmp.putAll(secondary);
                primary = tmp;
            }
        } else {
            primary.putAll(secondary);
        }
        return primary;
    }
}
