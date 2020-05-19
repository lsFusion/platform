package lsfusion.gwt.client.form.design.view.flex;

import com.google.gwt.event.logical.shared.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.view.FixFlexBasisComposite;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.design.view.BeforeSelectionTabHandler;

import java.util.ArrayList;
import java.util.List;

public class FlexTabbedPanel extends FixFlexBasisComposite implements IndexedPanel, HasBeforeSelectionHandlers<Integer>, HasSelectionHandlers<Integer>, RequiresResize, ProvidesResize {
    private final FlexPanel panel;
    private final List<BeforeSelectionTabHandler> beforeSelectionTabHandlers = new ArrayList<>();
    protected TabBar tabBar;
    protected TabDeck deck;

    public FlexTabbedPanel() {
        FlexTabBar tabBar = new FlexTabBar();
        TabbedDeckPanel deck = new TabbedDeckPanel();

        panel = new FlexPanel(true);
        panel.add(tabBar, GFlexAlignment.STRETCH);
        panel.addFill(deck);

        this.tabBar = tabBar;
        this.deck = deck;

        tabBar.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
            @Override
            public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
                onBeforeTabSelected(event);
            }
        });
        tabBar.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                FlexTabbedPanel.this.onTabSelected(event);
            }
        });

        initWidget(panel);

        setStyleName("gwt-TabPanel");
        deck.setStyleName("gwt-TabPanelBottom");
    }

    private void onBeforeTabSelected(BeforeSelectionEvent<Integer> event) {
        BeforeSelectionEvent<Integer> panelEvent = BeforeSelectionEvent.fire(this, event.getItem());
        if (panelEvent != null && panelEvent.isCanceled()) {
            event.cancel();
        }
    }

    public void addBeforeSelectionTabHandler(BeforeSelectionTabHandler handler) {
        beforeSelectionTabHandlers.add(handler);
    }

    private void onTabSelected(SelectionEvent<Integer> event) {
        int tabIndex = event.getSelectedItem();
        for(BeforeSelectionTabHandler beforeSelectionTabHandler : beforeSelectionTabHandlers)
            beforeSelectionTabHandler.onBeforeSelection(tabIndex);
        deck.showWidget(tabIndex);
        SelectionEvent.fire(this, tabIndex);
    }

    @Override
    public HandlerRegistration addBeforeSelectionHandler(BeforeSelectionHandler<Integer> handler) {
        return addHandler(handler, BeforeSelectionEvent.getType());
    }

    @Override
    public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> handler) {
        return addHandler(handler, SelectionEvent.getType());
    }

    @Override
    public void onResize() {
        if (isVisible()) {
            deck.onResize();
        }
    }

    /**
     * Adds a widget to the tab panel. If the Widget is already attached to the
     * TabPanel, it will be moved to the right-most index.
     * @param w       the widget to be added
     * @param tabText the text to be shown on its tab
     */
    public void add(Widget w, String tabText) {
        insert(w, tabText, getWidgetCount());
    }

    /**
     * Adds a widget to the tab panel. If the Widget is already attached to the
     * TabPanel, it will be moved to the right-most index.
     * @param w       the widget to be added
     * @param tabText the text to be shown on its tab
     * @param asHTML  <code>true</code> to treat the specified text as HTML
     */
    public void add(Widget w, String tabText, boolean asHTML) {
        insert(w, tabText, asHTML, getWidgetCount());
    }

    /**
     * Adds a widget to the tab panel. If the Widget is already attached to the
     * TabPanel, it will be moved to the right-most index.
     * @param w         the widget to be added
     * @param tabWidget the widget to be shown in the tab
     */
    public void add(Widget w, Widget tabWidget) {
        insert(w, tabWidget, getWidgetCount());
    }

    public void insert(Widget widget, String tabText, int beforeIndex) {
        insert(widget, tabText, false, beforeIndex);
    }

    public void insert(Widget widget, String tabText, boolean asHTML, int beforeIndex) {
        // Delegate updates to the TabBar to our DeckPanel implementation
        insert(widget, asHTML ? new HTML(tabText, false) : new Label(tabText, false), beforeIndex);
    }

    public void insert(Widget widget, Widget tabWidget, int beforeIndex) {
        // Check to see if the TabPanel already contains the Widget. If so,
        // remove it and see if we need to shift the position to the left.
        int ind = getWidgetIndex(widget);
        if (ind != -1) {
            remove(ind);
            if (ind < beforeIndex) {
                beforeIndex--;
            }
        }
        tabBar.insertTab(tabWidget, beforeIndex);
        deck.insert(widget, beforeIndex);
    }

    @Override
    public boolean remove(int index) {
        if (index != -1) {
            tabBar.removeTab(index);
            return deck.remove(index);
        }

        return false;
    }

    @Override
    public Widget getWidget(int index) {
        return deck.getWidget(index);
    }

    @Override
    public int getWidgetCount() {
        return deck.getWidgetCount();
    }

    @Override
    public int getWidgetIndex(Widget widget) {
        return deck.getWidgetIndex(widget);
    }

    public int getSelectedTab() {
        return tabBar.getSelectedTab();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void selectTab(int index) {
        tabBar.selectTab(index);
    }

    public void setTabCaption(int index, String caption) {
        tabBar.setTabText(index, caption);
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getTabBarHeight() {
        return tabBar.asWidget().getOffsetHeight() + 5; //little extra for borders, etc.
    }
}
