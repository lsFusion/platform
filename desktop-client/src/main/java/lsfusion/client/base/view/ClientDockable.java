package lsfusion.client.base.view;

import bibliothek.extension.gui.dock.theme.eclipse.EclipseTabDockAction;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.DefaultMultipleCDockable;
import bibliothek.gui.dock.common.action.predefined.CCloseAction;
import bibliothek.gui.dock.common.event.CDockableAdapter;
import bibliothek.gui.dock.common.event.CFocusListener;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.control.focus.DefaultFocusRequest;
import com.jhlabs.image.PointFilter;
import lsfusion.client.form.controller.FormsController;
import lsfusion.client.form.view.ClientFormDockable;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.effect.BufferedImageOpEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// уничтожаемые формы
public abstract class ClientDockable extends DefaultMultipleCDockable {

    private String canonicalName;

    private Container contentContainer;
    private LockableUI contentLayerUI;
    private JXLayer contentLayer;
    private Component defaultComponent;
    private ClientDockable blockingDockable;

    private final CustomCloseAction closeAction;

    protected ClientDockable(String canonicalName, FormsController formsController) {
        super(formsController.getDockableFactory());

        this.canonicalName = canonicalName;

        setMinimizable(false);
        setMaximizable(true);
        setExternalizable(false);
        setRemoveOnClose(true);
        setCloseable(true);

        putAction(ACTION_KEY_CLOSE, closeAction = new CustomCloseAction(formsController.getControl()));

        addCDockableStateListener(new CDockableAdapter() {
            @Override
            public void visibilityChanged(CDockable dockable) {
                initDefaultComponent();
                if (defaultComponent != null) {
                    removeCDockableStateListener(this);
                }
            }
        });

        addFocusListener(new CFocusListener() {
            @Override
            public void focusGained(CDockable dockable) {
                initDefaultComponent();
                if (defaultComponent != null) {
                    removeFocusListener(this);
                }
            }

            @Override
            public void focusLost(CDockable dockable) {}
        });
    }

    private void initDefaultComponent() {
        if (defaultComponent == null) {
            defaultComponent = focusDefaultComponent();
            FocusTraversalPolicy traversalPolicy = contentContainer.getFocusTraversalPolicy();
            if (traversalPolicy != null) {
                defaultComponent = traversalPolicy.getDefaultComponent(contentContainer);
                if (defaultComponent != null) {
                    defaultComponent.requestFocusInWindow();
                }
            }
        }
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    protected void setContent(Container contentContainer) {
        this.contentContainer = contentContainer;
        this.contentLayerUI = new ShadowLayerUI();
        this.contentLayer = new JXLayer(contentContainer, contentLayerUI);
        contentLayer.setFocusable(false);
        contentLayer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (blockingDockable != null) {
                    blockingDockable.toFront();
                    blockingDockable.requestFocusInWindow();
                }
            }
        });

        getContentPane().add(contentLayer);
    }

    public void blockView() {
        closeAction.setEnabled(false);
        contentLayerUI.setLocked(true);
        contentLayer.updateUI();
    }

    public void unblockView() {
        closeAction.setEnabled(true);
        contentLayerUI.setLocked(false);
        contentLayer.updateUI();

        requestFocusInWindow();
    }

    public void onClosing() {
        setVisible(false);
    }

    // закрываются пользователем
    public void onClosed() {
        getContentPane().removeAll();
    }

    public void onShowingChanged(boolean oldShowing, boolean newShowing) {
        // nothing by default
    }

    public void requestFocusInWindow() {
        // to ensure dockable itself will be focused 
        getControl().getController().setFocusedDockable(new DefaultFocusRequest(intern(), null, true, true, true, true));

        // When user uses desktop client theme with titles in every dockable (with tabs under dockable; other than Eclipse) 
        // the newly opened form receives no focus. Dockable title receives it instead. 
        focusDefaultComponent();
    }

    private Component focusDefaultComponent() {
        FocusTraversalPolicy traversalPolicy = contentContainer.getFocusTraversalPolicy();
        if (traversalPolicy != null) {
            Component defaultComponent = traversalPolicy.getDefaultComponent(contentContainer);
            if (defaultComponent != null) {
                defaultComponent.requestFocusInWindow();
                return defaultComponent;
            }
        }
        return null;
    }

    public void setBlockingDockable(ClientFormDockable blockingDockable) {
        this.blockingDockable = blockingDockable;
    }

    public abstract void onOpened();

    private final static class ShadowLayerUI extends LockableUI {
        public static final double opacity = 0.5;

        public ShadowLayerUI() {
            super(new BufferedImageOpEffect(
                    new PointFilter() {
                        @Override
                        public int filterRGB(int x, int y, int rgb) {
                            int a = rgb & 0xff000000;
                            int r = (rgb >> 16) & 0xff;
                            int g = (rgb >> 8) & 0xff;
                            int b = rgb & 0xff;
                            r = (int) (r * opacity);
                            g = (int) (g * opacity);
                            b = (int) (b * opacity);
                            return a | (r << 16) | (g << 8) | b;
                        }
                    }
            ));
        }
    }

    @EclipseTabDockAction
    private class CustomCloseAction extends CCloseAction {
        public CustomCloseAction(CControl control) {
            super(control);
        }

        @Override
        public void close(CDockable dockable) {
            onClosing();
        }
    }
}
