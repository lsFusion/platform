package lsfusion.gwt.client.form.filter.user.view;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.filter.user.GPropertyFilter;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class GFilterValueView extends ResizableSimplePanel {
    protected GFilterValueListener listener;

    public GFilterValueView(GFilterValueListener listener) {
        this.listener = listener;
    }

    public interface GFilterValueListener {
        void valueChanged();
    }

    public void focusOnValue() {
    }

    public void propertySet(GPropertyFilter condition) {}
    public void propertyChanged(GPropertyFilter condition) {}

    public void startEditing(Event keyEvent) {
        focusOnValue();
    }

    public int getWidth(GPropertyDraw property) {
        return getElement().getClientWidth();
    }

    public void setWidth(int width) {
        setWidth(width + "px");
    }
}
