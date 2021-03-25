package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;
import lsfusion.gwt.client.form.property.async.GAsyncOpenForm;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.form.view.ModalForm;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.window.GModalityType;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lsfusion.gwt.client.base.GwtClientUtils.findInList;

public abstract class FormsController {
    private final ClientMessages messages = ClientMessages.Instance.get();

    private final FlexTabbedPanel tabsPanel;

    private final List<FormDockable> forms = new ArrayList<>();
    private final List<Integer> formFocusOrder = new ArrayList<>();
    private int focusOrderCount;

    private NativeHashMap<Long, FormContainer> asyncForms = new NativeHashMap<>();

    private final WindowsController windowsController;

    private final GToolbarButton linkEditButton;

    private final GToolbarButton fullScreenButton;
    private boolean fullScreenMode = false;

    public FormsController(WindowsController windowsController) {
        this.windowsController = windowsController;

        GToolbarView toolbarView = new GToolbarView();

        linkEditButton = new GToolbarButton("linkEditMode.png", messages.linkEditModeEnable()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> updateLinkEditMode(!GFormController.isLinkEditMode(), false);
            }
        };
        setCompactSize(linkEditButton);
        toolbarView.addComponent(linkEditButton);

        fullScreenButton = new GToolbarButton(null) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> switchFullScreenMode();
            }
        };
        setCompactSize(fullScreenButton);
        toolbarView.addComponent(fullScreenButton);
        updateFullScreenButton();

        tabsPanel = new FlexTabbedPanel("formsTabBar", toolbarView);

        // unselected (but not removed)
        tabsPanel.setBeforeSelectionHandler(index -> {
            int selectedTab = tabsPanel.getSelectedTab();
            if(selectedTab >= 0) {
                forms.get(selectedTab).onBlur(isRemoving);
                isRemoving = false;
            }
        });
        tabsPanel.setSelectionHandler(index -> {
            forms.get(index).onFocus(isAdding);
            formFocusOrder.set(index, focusOrderCount++);
            isAdding = false;
        });
    }

    private void setCompactSize(GToolbarButton button) {
        button.setSize(StyleDefaults.VALUE_HEIGHT_STRING, StyleDefaults.VALUE_HEIGHT_STRING);
    }

    private boolean isRemoving = false;
    private boolean isAdding = false;

    public void updateLinkEditMode(boolean linkEditMode, boolean linkEditModeWithCtrl) {
        if(!GFormController.isLinkEditModeWithCtrl() && linkEditModeWithCtrl) {
            GFormController.scheduleLinkEditModeStylesTimer(() -> setLinkEditModeStyles(linkEditMode));
        } else {
            GFormController.cancelLinkEditModeStylesTimer();
            setLinkEditModeStyles(linkEditMode);
        }
        linkEditButton.showBackground(linkEditMode);
        GFormController.setLinkEditMode(linkEditMode, linkEditModeWithCtrl);
    }

    private void setLinkEditModeStyles(boolean linkEditMode) {
        linkEditButton.setTitle(linkEditMode ? messages.linkEditModeDisable() : messages.linkEditModeEnable() + " (CTRL)");
        Element globalElement = RootPanel.get().getElement();
        if(linkEditMode)
            globalElement.addClassName("linkEditMode");
        else
            globalElement.removeClassName("linkEditMode");
    }

    public void updateFullScreenButton(){
        fullScreenButton.setTitle(fullScreenMode ? messages.fullScreenModeDisable() : messages.fullScreenModeEnable() + " (ALT+F11)");
        fullScreenButton.setModuleImagePath(fullScreenMode ? "minimize.png" : "maximize.png");
    }

    public void switchFullScreenMode() {
        setFullScreenMode(!fullScreenMode);
    }

    public void setFullScreenMode(boolean fullScreenMode) {
        if (fullScreenMode != this.fullScreenMode) {
            windowsController.setFullScreenMode(fullScreenMode);
            this.fullScreenMode = fullScreenMode;
            updateFullScreenButton();
        }
    }

    public void initRoot(FormsController formsController) {
        GFormController.initKeyEventHandler(RootPanel.get(), formsController, () -> {
            FormContainer currentForm = MainFrame.getCurrentForm();
            if(currentForm != null)
                return currentForm.getForm();
            return null;
        });
    }

    public Widget getView() {
        return tabsPanel;
    }

    public FormContainer openForm(Long requestIndex, GForm form, GModalityType modalityType, boolean forbidDuplicate, Event initFilterEvent, WindowHiddenHandler hiddenHandler) {
        FormDockable duplicateForm = getDuplicateForm(form.sID, forbidDuplicate);
        if(duplicateForm != null) {
            selectTab(duplicateForm);
            return null;
        }

        boolean modal = modalityType.isModalWindow();
        FormContainer formContainer = removeAsyncForm(requestIndex);
        boolean hasAsyncForm = formContainer != null;
        boolean asyncOpened = hasAsyncForm && formContainer instanceof ModalForm == modal;
        if(hasAsyncForm && !asyncOpened) {
            formContainer.hide();
            ensureTabSelected();
        }

        if (!asyncOpened) {
            if(modal) {
                if(openFormTimer != null) {
                    openFormTimer.cancel();
                    openFormTimer = null;
                }
                formContainer = new ModalForm(this, requestIndex, form.getCaption(), false);
            } else {
                formContainer = new FormDockable(this, requestIndex, form.getCaption(), false);
            }

        }
        initForm(formContainer, form, hiddenHandler, modalityType.isDialog(), initFilterEvent);
        if (!asyncOpened || modal)
            formContainer.show();

        return formContainer;
    }

    //size of modal window may change, we don't want flashing, so we use timer
    Timer openFormTimer;
    public void asyncOpenForm(Long requestIndex, GAsyncOpenForm openForm) {
        FormDockable duplicateForm = getDuplicateForm(openForm.canonicalName, openForm.forbidDuplicate);
        if (duplicateForm == null) {
            if (openForm.modal) {
                openFormTimer = new Timer() {
                    @Override
                    public void run() {
                        Scheduler.get().scheduleDeferred(() -> {
                            if (openFormTimer != null) {
                                FormContainer formContainer = new ModalForm(FormsController.this, requestIndex, openForm.caption, true);
                                formContainer.show();
                                asyncForms.put(requestIndex, formContainer);
                                openFormTimer = null;
                            }
                        });
                    }
                };
                openFormTimer.schedule(100);
            } else {
                FormContainer formContainer = new FormDockable(this, requestIndex, openForm.caption, true);
                formContainer.show();
                asyncForms.put(requestIndex, formContainer);
            }
        }
    }

    private FormDockable getDuplicateForm(String canonicalName, boolean forbidDuplicate) {
        if(forbidDuplicate && MainFrame.forbidDuplicateForms) {
            return findForm(canonicalName);
        }
        return null;
    }

    public boolean hasAsyncForm(Long requestIndex) {
        return asyncForms.containsKey(requestIndex);
    }

    public FormContainer removeAsyncForm(Long requestIndex) {
        return asyncForms.remove(requestIndex);
    }

    public void addContextMenuHandler(FormDockable formContainer) {
        formContainer.getTabWidget().addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                GwtClientUtils.stopPropagation(event);

                PopupDialogPanel popup = new PopupDialogPanel();
                final MenuBar menuBar = new MenuBar(true);
                menuBar.addItem(new MenuItem(messages.closeAllTabs(), () -> {
                    popup.hide();
                    closeAllTabs();
                }));
                GwtClientUtils.showPopupInWindow(popup, menuBar, event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
            }
        }, ContextMenuEvent.getType());
    }

    public void initForm(FormContainer<?> formContainer, GForm form, WindowHiddenHandler hiddenHandler, boolean dialog, Event initFilterEvent) {
        formContainer.initForm(this, form, () -> {
            formContainer.hide();

            hiddenHandler.onHidden();
        }, dialog, initFilterEvent);
    }

    public void selectTab(FormDockable dockable) {
        tabsPanel.selectTab(forms.indexOf(dockable));
    }

    public void selectTab(String formCanonicalName) {
        FormDockable form = findForm(formCanonicalName);
        if(form != null)
            selectTab(form);
    }

    public FormDockable findForm(String formCanonicalName) {
        return findInList(forms, dockable -> !dockable.async && dockable.getForm().getForm().sID.equals(formCanonicalName));
    }

    public void addDockable(FormDockable dockable) {
        forms.add(dockable);
        formFocusOrder.add(null);

        tabsPanel.add(dockable.getContentWidget(), dockable.getTabWidget());
        assert !isAdding;
        isAdding = true;
        selectTab(dockable);
        assert !isAdding;
    }

    public void removeDockable(FormDockable dockable) {
        int index = forms.indexOf(dockable);
        boolean isTabSelected = forms.get(tabsPanel.getSelectedTab()).equals(dockable);

        if (isTabSelected) {
            assert !isRemoving;
            isRemoving = true;
        }

        tabsPanel.remove(index);
        assert !isRemoving; // checking that the active tab is closing

        forms.remove(index);
        formFocusOrder.remove(index);
    }

    private void closeAllTabs() {
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            private int size = forms.size();
            @Override
            public boolean execute() {
                if (MainFrame.isModalPopup())
                    return true;

                if (size > 0 && size <= forms.size()) { // check if some tab not closed by user while running "closeAllTabs"
                    FormDockable lastTab = forms.get(size - 1);
                    selectTab(lastTab);
                    lastTab.closePressed();
                    size--;
                    return true;
                }

                return false;
            }
        }, 20);
    }

    public void ensureTabSelected() {
        int size;
        if(tabsPanel.getSelectedTab() < 0 && (size = forms.size()) > 0) {
            FormDockable lastFocusedForm = null;
            int maxOrder = 0;
            int formOrder;
            for(int i=0;i<size;i++) {
                formOrder = formFocusOrder.get(i);
                if (lastFocusedForm == null || formOrder > maxOrder) {
                    lastFocusedForm = forms.get(i);
                    maxOrder = formOrder;
                }
            }
            selectTab(lastFocusedForm);
        }
    }

    public void resetWindowsLayout() {
        setFullScreenMode(false);
        windowsController.resetLayout();
    }

    public abstract void executeNavigatorAction(GNavigatorAction action, NativeEvent nativeEvent);

    public abstract void executeNotificationAction(String actionSID, int type);

}
