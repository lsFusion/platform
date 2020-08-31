package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

public class GCustom extends GSimpleStateTableView {
    private final String renderFunction;

    public GCustom(GFormController form, GGridController grid, String renderFunction) {
        super(form, grid);
        this.renderFunction = renderFunction;
    }

    @Override
    protected void render(Element element, Element recordElement, JsArray<JavaScriptObject> list) {
        if (list.length() > 0 && element != null) {
            runFunction(element, list, renderFunction);
        }
    }

    protected native void runFunction(Element element, JavaScriptObject list, String renderFunction)/*-{
        var fn = $wnd[renderFunction];
        if (typeof fn === 'function'){
            fn(element, list);
        }
    }-*/;
}
