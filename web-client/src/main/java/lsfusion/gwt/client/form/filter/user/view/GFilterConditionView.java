package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.form.filter.user.*;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GFilterConditionView extends FlexPanel implements GFilterValueView.GFilterValueListener {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    public interface UIHandler {
        void conditionChanged();
        void conditionRemoved(GPropertyFilter condition);
        void applyFilter();
    }

    private static final String DELETE = "filtdel.png";
    private static final String SETTINGS = "userPreferences.png";

    private Label propertyLabel;
    private Label negationLabel;
    private Label compareLabel;
    private GFilterValueView valueView;
    private Label junctionLabel;
    
    private GToolbarButton settingsButton;
    private Widget settingsReplacement;

    private PopupDialogPanel popup;
    private GFilterConditionListBox propertyView;
    private CheckBox negationView;
    private GFilterConditionListBox compareView;
    private GFilterConditionListBox filterValues;
    private GFilterConditionListBox junctionView;
    private GToolbarButton deleteButton;
    
    private boolean toolsVisible;
    private boolean junctionVisible = false;

    public GPropertyFilter condition;
    private UIHandler handler;

    private Map<GFilterValue, GFilterValueView> valueViews;

    public GFilterConditionView(GPropertyFilter iCondition, GTableController logicsSupplier, final UIHandler handler, boolean toolsVisible) {
        this.condition = iCondition;
        this.handler = handler;

        valueViews = new HashMap<>();
        GDataFilterValue dataValue = condition.value instanceof GDataFilterValue ? (GDataFilterValue) condition.value : new GDataFilterValue();
        valueViews.put(dataValue, new GDataFilterValueView(this, dataValue, condition.property, logicsSupplier));
        GObjectFilterValue objectValue = condition.value instanceof GObjectFilterValue ? (GObjectFilterValue) condition.value : new GObjectFilterValue();
        valueViews.put(objectValue, new GObjectFilterValueView(this, objectValue, logicsSupplier));
        GPropertyFilterValue propertyValue = condition.value instanceof GPropertyFilterValue ? (GPropertyFilterValue) condition.value : new GPropertyFilterValue();
        valueViews.put(propertyValue, new GPropertyFilterValueView(this, propertyValue, logicsSupplier));
        

        propertyLabel = new Label(condition.property.getNotEmptyCaption());
        addCentered(propertyLabel);
        
        negationLabel = new Label(messages.formFilterConditionViewNot());
        negationLabel.setVisible(false);
        addCentered(negationLabel);
        
        compareLabel = new Label(condition.compare.toString());
        compareLabel.setVisible(false);
        addCentered(compareLabel);

        valueView = valueViews.get(condition.value);
        if (valueView != null) {
            addCentered(valueView);
            valueView.propertySet(condition);
        }

        junctionLabel = new Label();
        addCentered(junctionLabel);
        
        settingsButton = new GToolbarButton(SETTINGS, "settings") {
            @Override
            public ClickHandler getClickHandler() {
                return clickEvent -> {
                    popup.setPopupPosition(settingsButton.getAbsoluteLeft(), settingsButton.getAbsoluteTop() + StyleDefaults.COMPONENT_HEIGHT);
                    popup.show();
                };
            }
        };
        addCentered(settingsButton);
        
        settingsReplacement = GwtClientUtils.createHorizontalStrut(StyleDefaults.COMPONENT_HEIGHT);
        addCentered(settingsReplacement);
        
        
        popup = new PopupDialogPanel();
        FlexPanel popupContent = new FlexPanel();
        popup.add(popupContent);

        propertyView = new GFilterConditionListBox();
        propertyView.addStyleName("customFontPresenter");
        for (Pair<Column, String> column : logicsSupplier.getSelectedColumns())
            propertyView.add(column.first, column.second);
        propertyView.addChangeHandler(event -> {
            Column selectedItem = (Column)propertyView.getSelectedItem();
            condition.property = selectedItem.property;
            condition.columnKey = selectedItem.columnKey;

            propertyLabel.setText(selectedItem.property.getNotEmptyCaption());

            filterChanged();
        });

        if (condition.property != null) {
            setSelectedPropertyDraw(condition.property, condition.columnKey);
        }
        popupContent.addCentered(propertyView);

        negationView = new CheckBox(messages.formFilterConditionViewNot());
        negationView.addStyleName("checkBoxFilter");
        negationView.addValueChangeHandler(event -> {
            condition.negation = negationView.getValue();

            negationLabel.setVisible(condition.negation);
            updateCompareLabelVisibility();

            handler.conditionChanged();
        });
        negationView.addClickHandler(event -> handler.conditionChanged());
        negationView.setValue(condition.negation);
        popupContent.addCentered(negationView);

        compareView = new GFilterConditionListBox();
        compareView.addStyleName("customFontPresenter");
        compareView.add((Object[]) GCompare.values());
        compareView.addChangeHandler(event -> {
            condition.compare = (GCompare) compareView.getSelectedItem();

            compareLabel.setText(condition.compare.toString());
            updateCompareLabelVisibility();

            handler.conditionChanged();
        });
        compareView.setItems(condition.property.baseType.getFilterCompares());
        compareView.setSelectedItem(condition.compare);
        popupContent.addCentered(compareView);

        filterValues = new GFilterConditionListBox();
        filterValues.addStyleName("customFontPresenter");
        filterValues.add(dataValue, objectValue, propertyValue);
        filterValues.addChangeHandler(event -> {
            condition.value = (GFilterValue) filterValues.getSelectedItem();
            filterChanged();
        });
        filterValues.setSelectedItem(condition.value);
        popupContent.addCentered(filterValues);

        junctionView = new GFilterConditionListBox();
        junctionView.addStyleName("customFontPresenter");
        junctionView.add(new Object[]{messages.formFilterConditionViewAnd(), messages.formFilterConditionViewOr()});
        junctionView.addChangeHandler(event -> {
            condition.junction = junctionView.getSelectedIndex() == 0;

            junctionLabel.setText(junctionView.getSelectedItemText());

            handler.conditionChanged();
        });
        junctionView.setSelectedIndex(condition.junction ? 0 : 1);
        popupContent.addCentered(junctionView);

        deleteButton = new GToolbarButton(DELETE, messages.formQueriesFilterRemoveCondition()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    popup.hide();
                    handler.conditionRemoved(condition);
                };
            }
        };
        popupContent.addCentered(deleteButton);
        
        setToolsVisible(toolsVisible);
    }

    private void filterChanged() {
        if (valueView != null) {
            remove(valueView);
        }
        valueView = valueViews.get(condition.value);
        if (valueView != null) {
            add(valueView, getWidgetIndex(junctionLabel), GFlexAlignment.CENTER);
            valueView.propertyChanged(condition);
        }
        
        GCompare oldCompare = (GCompare) compareView.getSelectedItem();
        GCompare[] filterCompares = condition.property.baseType.getFilterCompares();
        compareView.setItems(filterCompares);
        if (Arrays.asList(filterCompares).contains(oldCompare)) {
            compareView.setSelectedItem(oldCompare);
        } else {
            compareView.setSelectedItem(condition.property.getDefaultCompare());
            condition.compare = (GCompare) compareView.getSelectedItem();

            compareLabel.setText(condition.compare.toString());
            updateCompareLabelVisibility();
        }

        handler.conditionChanged();
    }
    
    private void updateCompareLabelVisibility() {
        compareLabel.setVisible(negationLabel.isVisible() || condition.compare != condition.property.getDefaultCompare());
    }

    public void setJunctionVisible(boolean visible) {
        junctionVisible = visible;
        junctionView.setVisible(visible && toolsVisible);
    }

    public void setToolsVisible(boolean visible) {
        toolsVisible = visible;
        negationView.setVisible(visible);
        compareView.setVisible(visible);
        filterValues.setVisible(visible);
        junctionView.setVisible(junctionVisible && visible);
        deleteButton.setVisible(visible);
        settingsButton.setVisible(visible);
        settingsReplacement.setVisible(!visible);
    }

    public void setSelectedPropertyDraw(GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        if (propertyDraw != null)
            propertyView.setSelectedItem(new Column(propertyDraw, columnKey));
    }

    @Override
    public void valueChanged() {
        handler.conditionChanged();
    }

    public void focusOnValue() {
        valueView.focusOnValue();
    }

    public void startEditing(Event keyEvent) {
        valueView.startEditing(keyEvent);
    }
}
