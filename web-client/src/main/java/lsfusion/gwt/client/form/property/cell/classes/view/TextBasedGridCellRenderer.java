package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.table.grid.view.GPivot;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.GridCellRenderer;
import lsfusion.gwt.client.view.MainFrame;

import static lsfusion.gwt.client.base.EscapeUtils.unicodeEscape;
import static lsfusion.gwt.client.view.StyleDefaults.CELL_HORIZONTAL_PADDING;

public abstract class TextBasedGridCellRenderer<T> extends GridCellRenderer<T> {
    protected GPropertyDraw property;

    TextBasedGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    public void renderStatic(Element element, GFont font, boolean isSingle) {
        GPivot.setTableToExcelAlignment(element, property);

        renderStaticContent(element, font);
    }

    protected void renderStaticContent(Element element, GFont font) {
        Style style = element.getStyle();
        // важно оставить множественные пробелы
        style.setWhiteSpace(Style.WhiteSpace.PRE);
        style.setPosition(Style.Position.RELATIVE);
        setPadding(style);

        //нужно для эллипсиса, но подтормаживает рендеринг,
        //оставлено закомменченым просто для справки
//        style.setOverflow(Style.Overflow.HIDDEN);
//        style.setTextOverflow(Style.TextOverflow.ELLIPSIS);
    }

    protected void setPadding(Style style) {
        style.setPaddingRight(CELL_HORIZONTAL_PADDING, Style.Unit.PX);
        style.setPaddingLeft(CELL_HORIZONTAL_PADDING, Style.Unit.PX);

        style.setPaddingBottom(0, Style.Unit.PX);
        style.setPaddingTop(0, Style.Unit.PX);
    }

    protected void setBasedTextFonts(Element element, GFont font, boolean isSingle) {
        if (property.font == null && isSingle) {
            property.font = font;
        }

        if (property.font != null) {
            property.font.apply(element.getStyle());
            GPivot.setTableToExcelFontStyle(element, property.font);
        }
    }

    public void renderDynamic(Element element, GFont font, Object value, boolean isSingle) {
        setBasedTextFonts(element, font, isSingle);
        if (value == null) {
            element.setTitle(property.isEditableNotNull() ? REQUIRED_VALUE : "");
            setInnerText(element, null);
        } else {
            String stringValue = unicodeEscape(format((T) value));
            setInnerText(element, stringValue);
            element.setTitle(property.echoSymbols ? "" : stringValue);
        }
    }

    protected abstract void setInnerText(Element element, String innerText);

    public abstract String format(T value);

    protected String getRequiredStringValue() {
        return MainFrame.showNotDefinedStrings ? REQUIRED_VALUE : "<div class=\"notNullLine\"></div>";
    }
}
