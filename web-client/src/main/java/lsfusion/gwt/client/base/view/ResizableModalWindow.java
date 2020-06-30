package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.PopupPanel;
import lsfusion.gwt.client.base.GwtClientUtils;

public class ResizableModalWindow extends ResizableWindow {

    private ModalMask modalMask;
//    private HandlerRegistration nativePreviewHandlerReg;

    @Override
    public void show() {
        if (modalMask == null) {
            modalMask = new ModalMask();
            modalMask.show();
        }

//        breaks resize and not sure what it's for
//        nativePreviewHandlerReg = Event.addNativePreviewHandler(this::previewNativeEvent);

        super.show();
    }

    @Override
    public void hide() {
        super.hide();

//        if (nativePreviewHandlerReg != null) { // как-то словили NPE
//            nativePreviewHandlerReg.removeHandler();
//            nativePreviewHandlerReg = null;
//        }

        if (modalMask != null) {
            modalMask.hide();
            modalMask = null;
        }
    }

//    private boolean eventTargetsPopup(NativeEvent event) {
//        EventTarget target = event.getEventTarget();
//        return Element.is(target) && getElement().isOrHasChild(Element.as(target));
//    }
//
//    private void previewNativeEvent(Event.NativePreviewEvent event) {
//        // If the event has been canceled or consumed, ignore it
//        if (event.isCanceled() || event.isConsumed()) {
//            return;
//        }
//
//        // If the event targets the popup, consume it
//        Event nativeEvent = Event.as(event.getNativeEvent());
//        if (eventTargetsPopup(nativeEvent)) {
//            event.consume();
//        } else {
//            // Cancel the event if it doesn't target the modal popup.
//            event.cancel();
//        }
//    }

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
