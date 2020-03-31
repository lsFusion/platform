package lsfusion.client.form.property.cell.classes.view;

import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.rich.RichEditorKit;
import lsfusion.client.form.property.cell.classes.controller.rich.RichEditorPane;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.view.MainFrame;

import javax.swing.*;
import java.awt.*;


public class TextPropertyRenderer extends PropertyRenderer {
    private boolean rich;
    private JEditorPane pane;

    public TextPropertyRenderer(ClientPropertyDraw property, boolean rich) {
        super(property);
        this.rich = rich;

        getComponent().setOpaque(true);
        getComponent().setFont(new Font("Tahoma", Font.PLAIN, MainFrame.getIntUIFontSize(10)));
        getComponent().setEditable(false);
        getComponent().setEditorKitForContentType("text/html", new RichEditorKit());
    }

    public JEditorPane getComponent() {
        if (pane == null) {
            pane = new JEditorPane();
        }
        return pane;
    }

    @Override
    protected boolean showRequiredString() {
        return true;
    }

    @Override
    protected boolean showNotDefinedString() {
        return true;
    }

    public void setValue(Object value) {
        super.setValue(value);
        if (value == null) {
            getComponent().setContentType("text");
            if (property != null && property.isEditableNotNull()) {
                getComponent().setText(REQUIRED_STRING);
            } else {
                getComponent().setText(EMPTY_STRING);
            }
        } else {
            if (rich) {
                getComponent().setContentType("text/html");
                RichEditorPane.setText(getComponent(), value.toString());
            } else {
                getComponent().setContentType("text");
                getComponent().setText(value.toString());
            }
        }
    }
}
