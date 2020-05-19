package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DatePicker;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.view.ResizableVerticalPanel;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.classes.data.GDateType;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.NativeEditEvent;
import lsfusion.gwt.client.view.StyleDefaults;

import java.text.ParseException;
import java.util.Date;

import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;
import static com.google.gwt.dom.client.BrowserEvents.KEYPRESS;

public class DateGridCellEditor extends PopupBasedGridCellEditor {

    private static final DateTimeFormat format = GwtSharedUtils.getDefaultDateFormat();

    protected DatePicker datePicker;
    protected TextBox editBox;

    public DateGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, Style.TextAlign.RIGHT);

        datePicker.addValueChangeHandler(new ValueChangeHandler<Date>() {
            @Override
            public void onValueChange(ValueChangeEvent<Date> event) {
                onDateChanged(event);
            }
        });

        editBox.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
                if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
                    onEnterPressed();
                }
            }
        });
    }

    @Override
    protected Widget createPopupComponent() {
        ResizableVerticalPanel panel = new ResizableVerticalPanel();

        editBox = new TextBox();
        editBox.addStyleName("dateTimeEditorBox");
        editBox.setHeight(StyleDefaults.VALUE_HEIGHT_STRING);
        panel.add(editBox);

        datePicker = new DatePicker();
        panel.add(datePicker);

        return panel;
    }

    @Override
    public void startEditing(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        String input = null;
        boolean selectAll = true;
        if (editEvent instanceof NativeEditEvent) {
            NativeEvent nativeEvent = ((NativeEditEvent) editEvent).getNativeEvent();
            String eventType = nativeEvent.getType();
            if (KEYDOWN.equals(eventType) && GKeyStroke.isDeleteKeyEvent(nativeEvent)) {
                input = "";
                selectAll = false;
            } else if (KEYPRESS.equals(eventType)) {
                input = String.valueOf((char)nativeEvent.getCharCode());
                selectAll = false;
            }
        }

        Date oldDate = valueAsDate(oldValue);

        if (oldDate != null) {
            datePicker.setValue(oldDate);
            datePicker.setCurrentMonth(oldDate);
        } else {
            datePicker.setValue(new Date());
        }
        editBox.setValue(
                input != null ? input : formatToString(oldDate != null ? oldDate : new Date())
        );

        super.startEditing(editEvent, context, parent, oldDate);

        editBox.getElement().focus();
        if (selectAll) {
            editBox.setSelectionRange(0, editBox.getValue().length());
        } else {
            editBox.setSelectionRange(editBox.getValue().length(), 0);
        }
    }

    protected String formatToString(Date date) {
        return format.format(date);
    }

    protected Date valueAsDate(Object value) {
        if (value == null) {
            return null;
        }
        return ((GDateDTO) value).toDate();
    }

    protected void onDateChanged(ValueChangeEvent<Date> event) {
        commitEditing(GDateDTO.fromDate(event.getValue()));
    }

    protected void onEnterPressed() {
        try {
            commitEditing(parseString(editBox.getValue()));
        } catch (ParseException ignored) {
        }
    }

    protected Object parseString(String value) throws ParseException {
        return GDateType.instance.parseString(value, property.pattern);
    }
}
