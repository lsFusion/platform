package lsfusion.gwt.client.form.object.table.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.filter.user.controller.GUserFilters;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.panel.controller.GPanelController;
import lsfusion.gwt.client.form.object.table.GToolbar;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;
import lsfusion.gwt.client.form.property.GFooterReader;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class GAbstractTableController implements GTableController {
    protected final GFormController formController;
    protected final GPanelController panel;
    protected final GToolbarView toolbarView;
    public GUserFilters filter;

    public GAbstractTableController(GFormController formController, GToolbar toolbar, boolean isList) {
        this.formController = formController;

        panel = new GPanelController(formController);

        if (toolbar == null || !toolbar.visible || !isList) {
            toolbarView = null;
        } else {
            toolbarView = new GToolbarView();
            getFormLayout().addBaseComponent(toolbar, toolbarView, null);
        }
    }

    public GFormLayout getFormLayout() {
        return formController.formLayout;
    }

    public void addToToolbar(Widget tool) {
        if (toolbarView != null) {
            toolbarView.addComponent(tool);
        }
    }
    
    public void addToolbarSeparator() {
        if (toolbarView != null) {
            toolbarView.addSeparator();
        }
    }

    public void addFilterButton() {
        if (showFilter()) {
            filter = new GUserFilters(this) {
                @Override
                public void remoteApplyQuery() {
                    changeFilter(new ArrayList<>(getConditions()));
                }

                @Override
                public void filterHidden() {
                    focusFirstWidget();
                }
            };

            addToToolbar(filter.getToolbarButton());
        }
    }

    @Override
    public List<GObject> getObjects() {
        return formController.getObjects();
    }

    @Override
    public List<GPropertyDraw> getPropertyDraws() {
        return formController.getPropertyDraws();
    }

    protected boolean showFilter() {
        return true;
    }

    public void quickEditFilter(EditEvent editEvent, GPropertyDraw propertyDraw, GGroupObjectValue columnKey) {
        filter.quickEditFilter(editEvent, propertyDraw, columnKey);
    }

    public void replaceFilter() {
        if (filter != null) {
            filter.addConditionPressed(true);
        }
    }

    public void addFilter() {
        filter.addConditionPressed(false);
    }

    public void removeFilters() {
        filter.allRemovedPressed();
    }

    protected abstract void changeFilter(List<GPropertyFilter> conditions);
    public abstract boolean focusFirstWidget();
    public abstract GComponent getGridComponent();

    @Override
    public void updateFooterValues(GFooterReader reader, Map<GGroupObjectValue, Object> values) {
    }

    // вызов focus() у getFocusHolderElement() грида по какой-то причине приводит к подскролливанию нашего скролла
    // (если грид заключён в скролл и не влезает по высоте) до первого ряда таблицы, скрывая заголовок (видимо вызывается scrollIntoView(), 
    // который, кстати, продолжает вызываться и при последующих изменениях фокуса в IE).
    // поэтому крутим все скроллы-предки вверх при открытии формы.
    // неоднозначное решение, т.к. вовсе необязательно фокусный компонент находится вверху скролла, но пока должно хватать. 
    public void scrollToTop() {
        GComponent gridComponent = getGridComponent();
        if (gridComponent != null) {
            scrollToTop(gridComponent.container);
        }
    }

    private void scrollToTop(GContainer container) {
        if (container != null) {
            if (container.isScroll()) {
                Element childElement = getFormLayout().getContainerView(container).getView().getElement().getFirstChildElement();
                if (childElement != null && childElement.getScrollTop() != 0) {
                    childElement.setScrollTop(0);
                }
            }
            scrollToTop(container.container);
        }
    }

    @Override
    public void setContainerCaption(GContainer container, String caption) {
        formController.setContainerCaption(container, caption);
    }
}
