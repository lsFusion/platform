package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.PopupPanel;

public class ResizableModalWindow extends ResizableWindow {
    private ModalMask modalMask;
    private WindowHiddenHandler hiddenHandler;
    private HandlerRegistration nativePreviewHandlerReg;

    public ResizableModalWindow(String caption) {
        this((WindowHiddenHandler)null);
        setCaption(caption);
    }

    public ResizableModalWindow(WindowHiddenHandler ihiddenHandler) {
        this.hiddenHandler = ihiddenHandler;

        addHandlers();
    }

    private void addHandlers() {
        addCloseHandler(new CloseHandler<ResizableWindow>() {
            @Override
            public void onClose(CloseEvent<ResizableWindow> event) {
                if (nativePreviewHandlerReg != null) { // как-то словили NPE
                    nativePreviewHandlerReg.removeHandler();
                    nativePreviewHandlerReg = null;
                }

                if (modalMask != null) {
                    modalMask.hide();
                    modalMask = null;
                }

                if (hiddenHandler != null) {
                    hiddenHandler.onHidden();
                }
            }
        });

        nativePreviewHandlerReg = Event.addNativePreviewHandler(new Event.NativePreviewHandler() {
            @Override
            public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                previewNativeEvent(event);
            }
        });
    }

    public void setWindowHiddenHandler(WindowHiddenHandler hiddenHandler) {
        this.hiddenHandler = hiddenHandler;
    }

    @Override
    protected void attach() {
        if (modalMask == null) {
            modalMask = new ModalMask();
            modalMask.show();
        }
        super.attach();
    }

    private boolean eventTargetsPopup(NativeEvent event) {
        EventTarget target = event.getEventTarget();
        return Element.is(target) && getElement().isOrHasChild(Element.as(target));
    }

    private void previewNativeEvent(Event.NativePreviewEvent event) {
        // If the event has been canceled or consumed, ignore it
        if (event.isCanceled() || event.isConsumed()) {
            return;
        }

        // If the event targets the popup, consume it
        Event nativeEvent = Event.as(event.getNativeEvent());
        if (eventTargetsPopup(nativeEvent)) {
            event.consume();
        } else {
            // Cancel the event if it doesn't target the modal popup.
            event.cancel();
        }
    }

    private final static class ModalMask {
        private final PopupPanel popup;

        private ModalMask() {
            popup = new PopupPanel();
            popup.setGlassEnabled(true);
            popup.getElement().getStyle().setOpacity(0);
        }

        public void show() {
            popup.center();
        }

        public void hide() {
            popup.hide();
        }
    }
}
