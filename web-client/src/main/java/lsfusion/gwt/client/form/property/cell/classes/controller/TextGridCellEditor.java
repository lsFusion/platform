package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import static lsfusion.gwt.client.view.StyleDefaults.*;

public class TextGridCellEditor extends TextBasedGridCellEditor {
    public TextGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, "textarea");
    }

    @Override
    public void renderDom(Element cellParent, RenderContext renderContext, UpdateContext updateContext) {
        DivElement div = cellParent.appendChild(Document.get().createDivElement());
        div.getStyle().setPaddingRight(8, Style.Unit.PX);
        div.getStyle().setPaddingLeft(0, Style.Unit.PX);
        div.getStyle().setHeight(100, Style.Unit.PCT);

        TextAreaElement textArea = div.appendChild(Document.get().createTextAreaElement());
        textArea.setTabIndex(-1);
        
        textArea.addClassName("textBasedGridCellEditor");

        Style textareaStyle = textArea.getStyle();
        textareaStyle.setBorderWidth(0, Style.Unit.PX);
        textareaStyle.setPaddingTop(CELL_VERTICAL_PADDING, Style.Unit.PX);
        textareaStyle.setPaddingRight(CELL_HORIZONTAL_PADDING, Style.Unit.PX);
        textareaStyle.setPaddingBottom(0, Style.Unit.PX);
        textareaStyle.setPaddingLeft(CELL_HORIZONTAL_PADDING, Style.Unit.PX);
        textareaStyle.setWidth(100, Style.Unit.PCT);
        textareaStyle.setHeight(100, Style.Unit.PCT);
        textareaStyle.setProperty("resize", "none");
        textareaStyle.setWhiteSpace(Style.WhiteSpace.PRE_WRAP);
        textareaStyle.setProperty("wordWrap", "break-word");
        textareaStyle.setOutlineStyle(Style.OutlineStyle.NONE);

        cellParent.getStyle().setPadding(0, Style.Unit.PX);

        setBaseTextFonts(textareaStyle, updateContext);
        textArea.setValue(currentText);
    }

    @Override
    protected String tryParseInputText(String inputText, boolean onCommit) {
        return (inputText == null || inputText.isEmpty()) ? null : inputText;
    }

    @Override
    protected void enterPressed(NativeEvent event, Element parent) {
        if (event.getShiftKey()) {
            super.enterPressed(event, parent);
        }
    }

    @Override
    protected void arrowPressed(NativeEvent event, Element parent, boolean down) {
        //NOP
    }

    @Override
    protected InputElement getInputElement(Element parent) {
        return parent.getFirstChild().getFirstChild().cast();
    }
}
