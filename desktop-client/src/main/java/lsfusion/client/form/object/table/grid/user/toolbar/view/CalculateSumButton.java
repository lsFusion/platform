package lsfusion.client.form.object.table.grid.user.toolbar.view;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.StartupProperties;

import javax.swing.*;
import java.math.BigDecimal;
import java.text.NumberFormat;

public abstract class CalculateSumButton extends ToolbarGridButton {
    private static final String SUM_ICON_PATH = "sum.png";

    public CalculateSumButton() {
        super(SUM_ICON_PATH, ClientResourceBundle.getString("form.queries.calculate.sum"));
    }

    public abstract void addListener();

    public void showPopupMenu(String caption, Object sum) {
        JPopupMenu menu = new JPopupMenu();
        JLabel label;
        JPanel panel = new JPanel();

        if (sum != null) {
            label = new JLabel(ClientResourceBundle.getString("form.queries.sum.result") + " [" + caption + "]: ");
            JTextField field = new JTextField(15);
            field.setHorizontalAlignment(JTextField.RIGHT);
            field.setText(format(sum));
            panel.add(label);
            panel.add(field);
        } else {
            label = new JLabel(ClientResourceBundle.getString("form.queries.unable.to.calculate.sum") + " [" + caption + "]");
            panel.add(label);
        }

        menu.add(panel);
        menu.setLocation(getLocation());
        menu.show(this, menu.getLocation().x + getWidth(), menu.getLocation().y);
    }

    public String format(Object number) {
        NumberFormat nf = NumberFormat.getNumberInstance();
        if(number instanceof BigDecimal)
            nf.setMaximumFractionDigits(((BigDecimal) number).scale());
        if (StartupProperties.dotSeparator)
            return nf.format(number).replace(',', '.');
        else
            return nf.format(number);
    }
}
