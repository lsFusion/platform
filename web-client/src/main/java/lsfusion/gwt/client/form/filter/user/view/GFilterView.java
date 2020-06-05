package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.base.view.ResizableFocusPanel;
import lsfusion.gwt.client.base.view.ResizableVerticalPanel;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontMetrics;
import lsfusion.gwt.client.form.design.GFontWidthString;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.controller.GUserFilters;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;

import java.util.*;

import static lsfusion.gwt.client.base.GwtClientUtils.createHorizontalStrut;

public class GFilterView extends ResizableFocusPanel implements GFilterConditionView.UIHandler {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final String ADD = "filtadd.png";
    private static final String APPLY = "filtapply.png";
    private static final String CANCEL = "filtcancel.png";

    private DialogBox filterDialog;

    private ResizableVerticalPanel filterContainer;

    private GUserFilters controller;

    private Map<GPropertyFilter, GFilterConditionView> conditionViews = new LinkedHashMap<>();

    public GFilterView(GUserFilters iController) {
        controller = iController;

        ResizableVerticalPanel mainContainer = new ResizableVerticalPanel();
        setWidget(mainContainer);
        addStyleName("noOutline");

        filterContainer = new ResizableVerticalPanel();

        mainContainer.add(filterContainer);

        Button addConditionButton = new ImageButton(messages.formQueriesFilterAddCondition(), ADD);
        addConditionButton.addClickHandler(event -> addNewCondition());

        Button resetConditionsButton = new ImageButton(messages.formQueriesFilterResetConditions());
        resetConditionsButton.addClickHandler(event -> allRemovedPressed());

        Button applyButton = new ImageButton(messages.ok(), APPLY);
        applyButton.addClickHandler(event -> applyFilter());

        Button cancelButton = new ImageButton(messages.close(), CANCEL);
        cancelButton.addClickHandler(event -> cancelFilter());

        HorizontalPanel leftButtonsPanel = new HorizontalPanel();
        leftButtonsPanel.add(addConditionButton);
        leftButtonsPanel.add(createHorizontalStrut(3));
        leftButtonsPanel.add(resetConditionsButton);
        leftButtonsPanel.addStyleName("flowPanelChildLeftAlign");

        HorizontalPanel rightButtonsPanel = new HorizontalPanel();
        rightButtonsPanel.add(applyButton);
        rightButtonsPanel.add(createHorizontalStrut(3));
        rightButtonsPanel.add(cancelButton);
        rightButtonsPanel.addStyleName("flowPanelChildRightAlign");

        FlowPanel buttonsPanel = new FlowPanel();
        buttonsPanel.add(leftButtonsPanel);
        buttonsPanel.add(rightButtonsPanel);

        mainContainer.add(new HorizontalLineWidget());
        mainContainer.add(buttonsPanel);

        FlowPanel controlPanel = new FlowPanel();
        filterContainer.add(controlPanel);

        sinkEvents(Event.ONKEYDOWN);
    }

    @Override
    public void onBrowserEvent(Event event) {
        if (event.getKeyCode() == KeyCodes.KEY_ESCAPE) {
            GwtClientUtils.stopPropagation(event);
            allRemovedPressed();
        } else {
            super.onBrowserEvent(event);
        }
    }

    public void showDialog(List<GPropertyFilter> conditions, GTableController logicsSupplier, EditEvent keyEvent, GPropertyDraw propertyDraw) {
        if(!conditions.isEmpty()) {
            for (GPropertyFilter condition : conditions) {
                addCondition(condition, logicsSupplier);
            }
            filterDialog = new DialogBox(false, true, new GFilterDialogHeader(messages.formFilterDialogHeader() + " [" + logicsSupplier.getSelectedGroupObject().getCaption() + "]"));
            filterDialog.setGlassEnabled(true);
            filterDialog.setWidget(this);
            filterDialog.center();
            focusOnValue();
            if(keyEvent != null) {
                startEditing(keyEvent, propertyDraw);
            }
        }
    }

    public void hideDialog() {
        controller.filterHidden();
        filterDialog.hide();
    }

    public void allRemovedPressed() {
        controller.allRemovedPressed();
        hideDialog();
    }

    public void addNewCondition() {
        addCondition(controller.getNewCondition(null, null), controller.getLogicsSupplier());
    }

    public void addCondition(GPropertyFilter condition, GTableController logicsSupplier) {
        if(condition != null) {
            GFilterConditionView conditionView = new GFilterConditionView(condition, logicsSupplier, this);
            conditionViews.put(condition, conditionView);
            filterContainer.add(conditionView);
            conditionChanged();
            focusOnValue();
        }
    }

    public void removeCondition(GPropertyFilter condition) {
        filterContainer.remove(conditionViews.get(condition));
        conditionViews.remove(condition);
        conditionChanged();
        focusOnValue();
    }

    @Override
    public void conditionChanged() {
        if(conditionViews.size() == 1) {
            conditionViews.entrySet().iterator().next().getValue().setJunctionVisible(false);
        } else {
            for (GFilterConditionView conditionView : conditionViews.values()) {
                conditionView.setJunctionEnabled(Arrays.asList(conditionViews.values().toArray()).indexOf(conditionView) < conditionViews.size() - 1);
            }
        }
        updateWidth();
    }

    @Override
    public void conditionRemoved(GPropertyFilter condition) {
        removeCondition(condition);
    }

    private void updateWidth() {
        int compareWidth = 0;
        int valueWidth = 0;
        for(GPropertyFilter condition : conditionViews.keySet()) {
            compareWidth = Math.max(compareWidth, getCompareViewWidth(condition.property));
            valueWidth = Math.max(valueWidth, condition.property.getValueWidth(null));
        }
        if(compareWidth > 0) {
            for (GFilterConditionView conditionView : conditionViews.values()) {
                conditionView.setCompareViewWidth(compareWidth);
            }
        }
        if(valueWidth > 0) {
            for (GFilterConditionView conditionView : conditionViews.values()) {
                conditionView.setValueViewWidth(valueWidth);
            }
        }
    }

    public int getCompareViewWidth(GPropertyDraw property) {
        String longestCompare = "";
        for(GCompare compare : property.baseType.getFilterCompares()) {
            if(compare.toString().length() > longestCompare.length())
                longestCompare = compare.toString();
        }
        int width = GFontMetrics.getStringWidth(new GFontWidthString(property.font != null ? property.font : GFont.DEFAULT_FONT, longestCompare));
        return width + 25; //dropdown arrow width
    }

    public void focusOnValue() {
        if (!conditionViews.isEmpty()) {
            // пробегаем по всем ячейкам со значеними, останавливаясь на последней, чтобы сбросить стили выделения в остальных
            for (GFilterConditionView filterView : conditionViews.values()) {
                filterView.focusOnValue();
            }
        }
    }

    public void applyFilter() {
        controller.applyFilters(new ArrayList<>(conditionViews.keySet()), true);
        hideDialog();
    }

    public void cancelFilter() {
        hideDialog();
    }

    public void startEditing(EditEvent keyEvent, GPropertyDraw propertyDraw) {
        if (conditionViews.size() > 0) {
            GFilterConditionView view = conditionViews.values().iterator().next();
            view.setSelectedPropertyDraw(propertyDraw);
            view.startEditing(keyEvent);
        }
    }

    private class HorizontalLineWidget extends Widget {
        public HorizontalLineWidget() {
            super();
            setElement(Document.get().createHRElement());
        }
    }
}
