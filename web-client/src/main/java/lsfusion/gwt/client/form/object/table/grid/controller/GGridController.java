package lsfusion.gwt.client.form.object.table.grid.controller;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GFormChanges;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.classes.data.GIntegralType;
import lsfusion.gwt.client.form.GUpdateMode;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GAbstractTableController;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGridUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.GGroupObjectUserPreferences;
import lsfusion.gwt.client.form.object.table.grid.user.design.view.GUserPreferencesDialog;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GCalculateSumButton;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GCountQuantityButton;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.grid.view.*;
import lsfusion.gwt.client.form.object.table.view.GridPanel;
import lsfusion.gwt.client.form.property.*;
import lsfusion.gwt.client.form.view.Column;

import java.util.*;

import static lsfusion.gwt.client.base.GwtClientUtils.setupFillParent;

public class GGridController extends GAbstractTableController {
    private final ClientMessages messages = ClientMessages.Instance.get();

    public GGroupObject groupObject;

    private Widget gridView;
    private ResizableSimplePanel gridContainerView;
    public Widget recordView;

    private GTableView table;

    private static boolean isList(GGroupObject groupObject) {
        return groupObject != null && groupObject.viewType.isList();
    }
    public boolean isList() {
        return groupObject != null && groupObject.viewType.isList();
    }

    public GPivotOptions getPivotOptions() {
        return groupObject != null ? groupObject.pivotOptions : null;
    }

    public GGridController(GFormController iformController, GGroupObject igroupObject, GGridUserPreferences[] userPreferences) {
        super(iformController, igroupObject == null ? null : igroupObject.toolbar, isList(igroupObject));
        groupObject = igroupObject;

        if (isList()) {
            boolean autoSize = groupObject.grid.autoSize;

            ResizableSimplePanel gridContainerView = new ResizableSimplePanel();
            gridContainerView.setStyleName("gridResizePanel");
            if(autoSize) { // убираем default'ый minHeight
                gridContainerView.getElement().getStyle().setProperty("minHeight", "0px");
                gridContainerView.getElement().getStyle().setProperty("minWidth", "0px");
            }
            this.gridContainerView = gridContainerView;

            Widget gridView = gridContainerView;

            // proceeding recordView
            GComponent record = groupObject.grid.record;
            if(record != null) {
                Widget recordView = getFormLayout().getComponentView(record);
                this.recordView = recordView;

                // we need to add recordview somewhere, to attach it (events, listeners, etc.)
                ResizableComplexPanel virtualGridView = new ResizableComplexPanel();
                virtualGridView.add(gridView);
                setupFillParent(gridView.getElement());

                // need to wrap recordView to setVisible false recordView's parent and not recordView itself (since it will be moved and shown by table view implementation)
                ResizableSimplePanel virtualRecordView = new ResizableSimplePanel();
                virtualRecordView.add(recordView);
                virtualRecordView.setVisible(false);
                virtualGridView.add(virtualRecordView);

                gridView = virtualGridView;
            }

            this.gridView = new GridPanel(gridView, gridContainerView);

            this.userPreferences = userPreferences;

            getFormLayout().addBaseComponent(groupObject.grid, this.gridView, getDefaultFocusReceiver());

            configureToolbar();

            setUpdateMode(false);
            switch (groupObject.listViewType) { // we don't have to do changeListViewType, since it's a first start and it should be set on server
                case PIVOT:
                    setPivotTableView();
                    ((GPivot)table).initDefaultSettings(this);
                    if(!groupObject.asyncInit)
                        ((GPivot)table).setDefaultChangesApplied();
                    break;
                case MAP:
                    setMapTableView();
                    break;
                case GRID:
                default:
                    setGridTableView();
            }
            table.setSetRequestIndex(-1);
            updateSettingsButton();
        }
    }
    
    private GGridUserPreferences[] userPreferences;
    private void setGridTableView() {
        changeTableView(new GGridTable(formController, this, userPreferences, groupObject.grid.autoSize));
        gridTableButton.showBackground(true);
        pivotTableButton.showBackground(false);
        if(mapTableButton != null)
            mapTableButton.showBackground(false);
    }
    private void setPivotTableView() {
        changeTableView(new GPivot(formController, this, getSelectedProperty()));
        pivotTableButton.showBackground(true);
        gridTableButton.showBackground(false);
        if(mapTableButton != null)
            mapTableButton.showBackground(false);
    }
    private void setMapTableView() {
        changeTableView(new GMap(formController, this));
        mapTableButton.showBackground(true);
        gridTableButton.showBackground(false);
        pivotTableButton.showBackground(false);
    }
    private void changeMode(Runnable updateView,  int pageSize, GListViewType viewType) {
        updateView.run();
        table.setSetRequestIndex(formController.changeListViewType(groupObject, pageSize, viewType));
        updateSettingsButton();
    }

    private boolean manual;
    private void setUpdateMode(boolean manual) {
        this.manual = manual;
        if(manual) {
            forceUpdateTableButton.setVisible(true);
            forceUpdateTableButton.setEnabled(false);
        } else
            forceUpdateTableButton.setVisible(false);
        manualUpdateTableButton.showBackground(manual);
    }

    private void changeTableView(GTableView table) {
        assert isList();

        gridContainerView.setFillWidget(table.getThisWidget());
        
        this.table = table;
        updateSettingsButton();
    }

    private GToolbarButton gridTableButton;
    private GToolbarButton pivotTableButton;
    private GToolbarButton settingsButton;
    private GCountQuantityButton quantityButton;
    private GCalculateSumButton sumButton;
    private GToolbarButton manualUpdateTableButton;
    private GToolbarButton forceUpdateTableButton;

    private GToolbarButton mapTableButton;

    private void configureToolbar() {
        assert isList();

        gridTableButton = new GToolbarButton("grid.png", messages.formGridTableView()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    changeMode(() -> setGridTableView(), -2, GListViewType.GRID);
                };
            }
        };
        addToToolbar(gridTableButton);

        pivotTableButton = new GToolbarButton("pivot.png", messages.formGridPivotView()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    changeMode(() -> setPivotTableView(), -1, GListViewType.PIVOT); // we need to make a call to get columns to init default config
                };
            }
        };
        addToToolbar(pivotTableButton);

        if(groupObject.isMap) {
            mapTableButton = new GToolbarButton("map.png", messages.formGridMapView()) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> {
                        changeMode(() -> setMapTableView(), ((GMap)table).getPageSize(), GListViewType.MAP);
                    };
                }
            };
            addToToolbar(mapTableButton);
        }

        addToolbarSeparator();

        if(showFilter() || groupObject.toolbar.showGridSettings) {

            if (showFilter()) {
                addFilterButton();
            }

            if (groupObject.toolbar.showGridSettings) {
                settingsButton = new GToolbarButton("userPreferences.png", messages.formGridPreferences()) {
                    @Override
                    public ClickHandler getClickHandler() {
                        return event -> {
                            changeSettings();
                        };
                    }
                };
                addToToolbar(settingsButton);
            }

            addToolbarSeparator();
        }

        if(groupObject.toolbar.showCountQuantity || groupObject.toolbar.showCalculateSum) {

            if (groupObject.toolbar.showCountQuantity) {
                quantityButton = new GCountQuantityButton() {
                    @Override
                    public ClickHandler getClickHandler() {
                        return event -> formController.countRecords(groupObject);
                    }
                };
                addToToolbar(quantityButton);
            }

            if (groupObject.toolbar.showCalculateSum) {
                sumButton = new GCalculateSumButton() {
                    @Override
                    public ClickHandler getClickHandler() {
                        return event -> {
                            GPropertyDraw property = getSelectedProperty();
                            if (property != null) {
                                if (property.baseType instanceof GIntegralType) {
                                    formController.calculateSum(groupObject, property, table.getCurrentColumnKey());
                                } else {
                                    showSum(null, property);
                                }
                            }
                        };
                    }
                };
                addToToolbar(sumButton);
            }

            addToolbarSeparator();
        }

        if(groupObject.toolbar.showPrintGroupXls) {
            addToToolbar(new GToolbarButton("excelbw.png", messages.formGridExport()) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> table.runGroupReport();
                }
            });

            addToolbarSeparator();
        }


        manualUpdateTableButton = new GToolbarButton("update.png", messages.formGridManualUpdate()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    setUpdateMode(!manual);
                    formController.changeMode(groupObject, false, null, null, 0, null, null, false, manual ? GUpdateMode.MANUAL : GUpdateMode.AUTO, null);
                };
            }
        };
        addToToolbar(manualUpdateTableButton);

        forceUpdateTableButton = new GToolbarButton(messages.formGridUpdate(), "ok.png", messages.formGridUpdate(), false) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    formController.changeMode(groupObject, false, null, null, 0, null, null, false, GUpdateMode.FORCE, null);
                };
            }
        };
        forceUpdateTableButton.addStyleName("actionPanelRendererValue");

        addToToolbar(forceUpdateTableButton);
    }

    public void showRecordQuantity(int quantity) {
        assert isList();
        quantityButton.showPopup(quantity);
    }

    public void showSum(Number sum, GPropertyDraw property) {
        assert isList();
        sumButton.showPopup(sum, property);
    }

    public void processFormChanges(long requestIndex, GFormChanges fc, NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> currentGridObjects) {
        for (GPropertyDraw property : fc.dropProperties) {
            if (property.groupObject == groupObject) {
                removeProperty(property);
            }
        }

        if (isList()) {
            ArrayList<GGroupObjectValue> keys = fc.gridObjects.get(groupObject);
            if (keys != null)
                table.setKeys(keys);

            GGroupObjectValue currentKey = fc.objects.get(groupObject);
            if (currentKey != null)
                table.setCurrentKey(currentKey);
        }

        // first proceed property with its values, then extra values
        fc.properties.foreachEntry((key, value) -> {
            if (key instanceof GPropertyDraw) {
                GPropertyDraw property = (GPropertyDraw) key;
                if (property.groupObject == groupObject) // filling keys
                    updateProperty(property, getColumnKeys(property, currentGridObjects), fc.updateProperties.contains(property), value);
            }
        });

        fc.properties.foreachEntry((key, value) -> {
            if (!(key instanceof GPropertyDraw)) {
                GPropertyReader propertyReader = key;
                if (formController.getGroupObject(propertyReader.getGroupObjectID()) == groupObject) {
                    propertyReader.update(this, value, false);
                }
            }
        });

        Boolean updateState = null;
        if(isList())
            updateState = fc.updateStateObjects.get(groupObject);

        update(requestIndex, updateState);
    }

    public ArrayList<GGroupObjectValue> getColumnKeys(GPropertyDraw property, NativeSIDMap<GGroupObject, ArrayList<GGroupObjectValue>> currentGridObjects) {
        ArrayList<GGroupObjectValue> columnKeys = GGroupObjectValue.SINGLE_EMPTY_KEY_LIST;
        if (property.columnGroupObjects != null) {
            LinkedHashMap<GGroupObject, ArrayList<GGroupObjectValue>> groupColumnKeys = new LinkedHashMap<>();
            for (GGroupObject columnGroupObject : property.columnGroupObjects) {
                ArrayList<GGroupObjectValue> columnGroupKeys = currentGridObjects.get(columnGroupObject);
                if (columnGroupKeys != null) {
                    groupColumnKeys.put(columnGroupObject, columnGroupKeys);
                }
            }

            columnKeys = GGroupObject.mergeGroupValues(groupColumnKeys);
        }
        return columnKeys;
    }

    @Override
    public void updateCellBackgroundValues(GBackgroundReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            table.updateCellBackgroundValues(property, values);
        } else {
            panel.updateCellBackgroundValues(property, values);
        }
    }

    @Override
    public void updateCellForegroundValues(GForegroundReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            table.updateCellForegroundValues(property, values);
        } else {
            panel.updateCellForegroundValues(property, values);
        }
    }

    @Override
    public void updateImageValues(GImageReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            table.updateImageValues(property, values);
        } else {
            panel.updateCellImages(property, values);
        }
    }

    @Override
    public void updatePropertyCaptions(GCaptionReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            table.updatePropertyCaptions(property, values);
        } else {
            panel.updatePropertyCaptions(property, values);
        }
    }

    @Override
    public void updateShowIfValues(GShowIfReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            table.updateShowIfValues(property, values);
        } else {
            panel.updateShowIfValues(property, values);
        }
    }

    @Override
    public void updateReadOnlyValues(GReadOnlyReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        if (property.grid) {
            table.updateReadOnlyValues(property, values);
        } else {
            panel.updateReadOnlyValues(property, values);
        }
    }

    @Override
    public void updateLastValues(GLastReader reader, NativeHashMap<GGroupObjectValue, Object> values) {
        GPropertyDraw property = formController.getProperty(reader.readerID);
        assert property.grid;
        if(property.grid)
            table.updateLastValues(property, reader.index, values);
    }

    @Override
    public void updateRowBackgroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        if (isList()) {
            table.updateRowBackgroundValues(values);
        } else {
            if (values != null && !values.isEmpty()) {
                panel.updateRowBackgroundValue(values.firstValue());
            }
        }
    }

    @Override
    public void updateRowForegroundValues(NativeHashMap<GGroupObjectValue, Object> values) {
        if (isList()) {
            table.updateRowForegroundValues(values);
        } else {
            if (values != null && !values.isEmpty()) {
                panel.updateRowForegroundValue(values.firstValue());
            }
        }
    }

    public GGroupObjectValue getCurrentKey() {
        GGroupObjectValue result = null;
        if (isList()) {
            result = table.getCurrentKey();
        }
        return result == null ? GGroupObjectValue.EMPTY : result;
    }

    @Override
    public GGroupObject getSelectedGroupObject() {
        return groupObject;
    }

    @Override
    public List<GPropertyDraw> getGroupObjectProperties() {
        ArrayList<GPropertyDraw> properties = new ArrayList<>();
        for (GPropertyDraw property : formController.getPropertyDraws()) {
            if (groupObject.equals(property.groupObject)) {
                properties.add(property);
            }
        }
        return properties;
    }

    @Override
    public GPropertyDraw getSelectedProperty() {
        return table != null ? table.getCurrentProperty() : null;
    }
    @Override
    public GGroupObjectValue getSelectedColumnKey() {
        return table.getCurrentColumnKey();
    }

    @Override
    public Object getSelectedValue(GPropertyDraw property, GGroupObjectValue columnKey) {
        return table.getSelectedValue(property, columnKey);
    }

    @Override
    public List<Pair<Column, String>> getSelectedColumns() {
        return table.getSelectedColumns();
    }

    private void removeProperty(GPropertyDraw property) {
        if (property.grid) {
            table.removeProperty(property);
        } else {
            panel.removeProperty(property);
        }
    }
    
    private void updateProperty(GPropertyDraw property, ArrayList<GGroupObjectValue> columnKeys, boolean updateKeys, NativeHashMap<GGroupObjectValue, Object> values) {
        if (property.grid) {
            table.updateProperty(property, columnKeys, updateKeys, values);
        } else {
            panel.updateProperty(property, columnKeys, updateKeys, values);
        }
    }

    private void update(long requestIndex, Boolean updateState) {
        if (isList()) {
            if(updateState != null)
                forceUpdateTableButton.setEnabled(updateState);
            table.update(updateState);

            boolean isVisible = !(table.isNoColumns() && requestIndex >= table.getSetRequestIndex());
            gridView.setVisible(isVisible);

            if (toolbarView != null)
                toolbarView.setVisible(isVisible);

            formController.setFiltersVisible(groupObject, isVisible);
        }

        panel.update();
        panel.setVisible(true);
    }

    @Override
    public boolean changeOrders(GGroupObject groupObject, LinkedHashMap<GPropertyDraw, Boolean> orders, boolean alreadySet) {
        assert this.groupObject.equals(groupObject);
        if(isList()) {
            return changeOrders(orders, alreadySet);
        }
        return false; // doesn't matter
    }
    public boolean changeOrders(LinkedHashMap<GPropertyDraw, Boolean> orders, boolean alreadySet) {
        assert isList();
        return table.changePropertyOrders(orders, alreadySet);
    }

    public LinkedHashMap<GPropertyDraw, Boolean> getUserOrders() {
        boolean hasUserPreferences = isList() && table.hasUserPreferences();
        if (hasUserPreferences) return table.getUserOrders(getGroupObjectProperties());
        return null;
    }

    public LinkedHashMap<GPropertyDraw, Boolean> getDefaultOrders() {
        return formController.getDefaultOrders(groupObject);
    }

    public List<List<GPropertyDraw>> getPivotColumns() {
        return formController.getPivotColumns(groupObject);
    }

    public List<List<GPropertyDraw>> getPivotRows() {
        return formController.getPivotRows(groupObject);
    }

    public List<GPropertyDraw> getPivotMeasures() {
        return formController.getPivotMeasures(groupObject);
    }

    public GGroupObjectUserPreferences getUserGridPreferences() {
        return table.getCurrentUserGridPreferences();
    }

    public GGroupObjectUserPreferences getGeneralGridPreferences() {
        return table.getGeneralGridPreferences();
    }
    
    public boolean isPropertyInGrid(GPropertyDraw property) {
        return isList() && table.containsProperty(property);
    }

    public int getSelectedRow() {
        return table.getSelectedRow();
    }

    public boolean isPropertyInPanel(GPropertyDraw property) {
        return panel.containsProperty(property);
    }

    public boolean isPropertyShown(GPropertyDraw property) {
        if(property.grid)
            return table.containsProperty(property);
        else
            return panel.containsProperty(property);
    }

    public void modifyGroupObject(GGroupObjectValue key, boolean add, int position) {
        assert isList();

        table.modifyGroupObject(key, add, position);
    }

    public boolean focusFirstWidget() {
        if (table != null && GwtClientUtils.isShowing(table.getThisWidget())) {
            table.focus();
            return true;
        }

        return panel.focusFirstWidget();
    }

    @Override
    public GComponent getGridComponent() {
        return isList() ? groupObject.grid : null;
    }

    @Override
    protected boolean showFilter() {
        return isList() && groupObject.filter.visible;
    }

    @Override
    protected void changeFilter(ArrayList<GPropertyFilter> conditions) {
        formController.changeFilter(groupObject, conditions);
    }

    public void changeGroups(List<GPropertyDraw> properties, List<GGroupObjectValue> columnKeys, int aggrProps, Integer pageSize, GPropertyGroupType aggrType) {
        formController.changeMode(groupObject, true, properties, columnKeys, aggrProps, aggrType, pageSize, false, null, GListViewType.PIVOT);
    }
    public void changePageSize(int pageSize) {
        formController.changeMode(groupObject, false, null, null, 0, null, pageSize, false, null, null);
    }

    public void focusProperty(GPropertyDraw property) {
        if(property.grid) {
            GTableView table = this.table;
            table.focus();
            table.focusProperty(property);
        } else {
            panel.focusProperty(property);
        }
    }

    public void changeSettings() {
        if(table instanceof GGridTable) {
            GGridTable gridTable = (GGridTable) table;
            GUserPreferencesDialog dialog = new GUserPreferencesDialog(gridTable, this, formController.hasCanonicalName()) {
                @Override
                public void preferencesChanged() {
                    updateSettingsButton();
                }
            };
            dialog.showDialog();
        } else {
            if(table instanceof GPivot) {
                GPivot pivotTable = (GPivot) table;
                pivotTable.switchSettings();
                updateSettingsButton();
            }
        }
    }

    private void updateSettingsButton() {
        if(settingsButton != null) {
            if (table instanceof GGridTable) {
                GGridTable gridTable = (GGridTable) table;
                settingsButton.showBackground(gridTable.hasUserPreferences() || gridTable.generalPreferencesSaved() || gridTable.userPreferencesSaved());
            } else if (table instanceof GPivot) {
                GPivot pivotTable = (GPivot) table;
                settingsButton.showBackground(pivotTable.isSettings());
            }
        }
    }

//    private static void updateTooltip(GGridTable table) {
//        String tooltip = messages.formGridPreferences() + " (";
//        if (table.userPreferencesSaved()) {
//            tooltip += messages.formGridPreferencesSavedForCurrentUser();
//        } else if (table.generalPreferencesSaved()) {
//            tooltip += messages.formGridPreferencesSavedForAllUsers();
//        } else {
//            tooltip += messages.formGridPreferencesNotSaved();
//        }
//
//        setTitle(tooltip + ")");
//    }

}
