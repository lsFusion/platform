package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.view.StyleDefaults;

import static lsfusion.gwt.client.view.StyleDefaults.BUTTON_HORIZONTAL_PADDING;

public class SimpleImageButton extends ImageButton {
    public SimpleImageButton(String caption) {
        this(caption, null);
    }

    public SimpleImageButton(String caption, String imagePath) {
        super(caption, imagePath);

        setHeight(StyleDefaults.COMPONENT_HEIGHT_STRING);
        
        Style buttonStyle = getElement().getStyle();
        buttonStyle.setPaddingTop(0, Style.Unit.PX);
        buttonStyle.setPaddingBottom(0, Style.Unit.PX);
        buttonStyle.setPaddingLeft(BUTTON_HORIZONTAL_PADDING, Style.Unit.PX);
        buttonStyle.setPaddingRight(BUTTON_HORIZONTAL_PADDING, Style.Unit.PX);
    }
}
