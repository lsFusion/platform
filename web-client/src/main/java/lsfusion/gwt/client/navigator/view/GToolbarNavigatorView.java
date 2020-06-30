package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.AppImageButton;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.base.view.ResizableHorizontalPanel;
import lsfusion.gwt.client.base.view.ResizableVerticalPanel;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.GNavigatorFolder;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GToolbarNavigatorWindow;

import java.util.Set;

public class GToolbarNavigatorView extends GNavigatorView {
    private static final int PADDING_H = 7;
    private static final int PADDING_STEP = 15;
    private CellPanel panel;
    private boolean vertical;
    private HasVerticalAlignment.VerticalAlignmentConstant alignmentY;
    private HasAlignment.HorizontalAlignmentConstant alignmentX;
    private boolean verticalTextAlign;

    public GToolbarNavigatorView(GToolbarNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, navigatorController);
        alignmentX = window.getAlignmentX();
        alignmentY = window.getAlignmentY();
        verticalTextAlign = window.hasVerticalTextPosition();

        vertical = window.isVertical();
        panel = vertical ? new ResizableVerticalPanel() : new ResizableHorizontalPanel();
        SimplePanel toolbarContainer = new SimplePanel(panel);
        if (vertical) {
            toolbarContainer.setStyleName("verticalToolbar");
            panel.setWidth("100%");
        } else {
            toolbarContainer.setStyleName("horizontalToolbar");
            panel.setHeight("100%");
        }
        setComponent(toolbarContainer);
    }

    @Override
    public void refresh(Set<GNavigatorElement> newElements) {
        panel.clear();

        for (GNavigatorElement element : newElements) {
            if (!element.containsParent(newElements)) {
                addElement(element, newElements, 0);
            }
        }
    }

    private void addElement(final GNavigatorElement element, Set<GNavigatorElement> newElements, int step) {
        final ImageButton button = new AppImageButton(element.image, element.caption, verticalTextAlign, !vertical);
        Style buttonStyle = button.getElement().getStyle();
        buttonStyle.setPaddingTop(5, Style.Unit.PX);
        buttonStyle.setPaddingBottom(5, Style.Unit.PX);
        buttonStyle.setPaddingLeft(PADDING_H, Style.Unit.PX);
        buttonStyle.setPaddingRight(PADDING_H, Style.Unit.PX);
        
        button.addMouseUpHandler(new MouseUpHandler() {
            @Override
            public void onMouseUp(MouseUpEvent event) {
                TooltipManager.get().hideTooltip();
                selected = element;
                navigatorController.update();
                navigatorController.openElement(element, event.getNativeEvent());
            }
        });

        button.addMouseOverHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent event) {
                TooltipManager.get().showTooltip(event.getClientX(), event.getClientY(), new TooltipManager.TooltipHelper() {
                    @Override
                    public String getTooltip() {
                        return element.getTooltipText();
                    }

                    @Override
                    public boolean stillShowTooltip() {
                        return button.isAttached() && button.isVisible();
                    }
                });
            }
        });

        button.addMouseOutHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent event) {
                TooltipManager.get().hideTooltip();
            }
        });

        button.addMouseMoveHandler(new MouseMoveHandler() {
            @Override
            public void onMouseMove(MouseMoveEvent event) {
                TooltipManager.get().updateMousePosition(event.getClientX(), event.getClientY());
            }
        });

        button.addMouseDownHandler(new MouseDownHandler() {
            @Override
            public void onMouseDown(MouseDownEvent event) {
                TooltipManager.get().hideTooltip();
            }
        });

        button.setHeight("auto");
        button.addStyleName("toolbarNavigatorButton");
        if (element instanceof GNavigatorFolder && element.equals(selected)) {
            button.addStyleName("toolbarSelectedNavigatorButton");
        }
        if (vertical) {
            button.setWidth("100%");
            button.getElement().getStyle().setPaddingLeft(PADDING_H + PADDING_STEP * step, Style.Unit.PX);
        }

        panel.add(button);
        panel.setCellVerticalAlignment(button, alignmentY);
        panel.setCellHorizontalAlignment(button, alignmentX);

        if ((element.window != null) && (!element.window.equals(window))) {
            return;
        }
        for (GNavigatorElement childEl: element.children) {
            if (newElements.contains(childEl)) {
                addElement(childEl, newElements, step + 1);
            }
        }
    }

    @Override
    public GNavigatorElement getSelectedElement() {
        return selected;
    }

    @Override
    public int getHeight() {
        return panel.getOffsetHeight();
    }

    @Override
    public int getWidth() {
        return panel.getOffsetWidth();
    }
}
