package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.view.LabelPropertyRenderer;

public class StringPropertyRenderer extends LabelPropertyRenderer {

    private final boolean echoSymbols;

    public StringPropertyRenderer(ClientPropertyDraw property) {
        super(property);
        echoSymbols = property != null && property.echoSymbols;
    }

    @Override
    protected boolean showNotDefinedString() {
        return true;
    }

    public void setValue(Object value) {
        super.setValue(value);

        if (value != null) {
            getComponent().setText(echoSymbols ? "******" : value.toString());
        } else if (property == null || !property.isEditableNotNull()) {
            getComponent().setText(EMPTY_STRING);
        }
    }
}
