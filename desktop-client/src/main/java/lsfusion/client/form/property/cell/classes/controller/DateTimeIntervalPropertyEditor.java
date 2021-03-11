package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.classes.data.ClientDateIntervalClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.view.MainFrame;

import java.text.SimpleDateFormat;

public class DateTimeIntervalPropertyEditor extends IntervalPropertyEditor {

    public DateTimeIntervalPropertyEditor(Object value, ClientPropertyDraw property) {
        super(property);
        defaultValue = value;

        editingFormat = (SimpleDateFormat) MainFrame.dateTimeFormat;
        setText(editingFormat.format(ClientDateIntervalClass.getDateFromInterval(value, true))
                + " - " + editingFormat.format(ClientDateIntervalClass.getDateFromInterval(value, false)));
    }
}
