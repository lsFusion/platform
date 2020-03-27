package lsfusion.client.base.view;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

import static javax.swing.UIManager.*;
import static lsfusion.client.controller.MainController.colorPreferences;
import static lsfusion.client.controller.MainController.colorTheme;

public class SwingDefaults {
    private static Color buttonBackground;
    private static Border buttonBorder;
    private static Border singleCellTableBorder;
    private static Color tableCellBackground;
    private static Color tableCellForeground;
    private static Border tableCellBorder;
    private static Color focusedTableCellBackground;
    private static Border focusedTableCellBorder;
    private static Color focusedTableRowBackground;
    private static Color focusedTableRowForeground;
    private static Color tableSelectionBackground;
    private static Color notDefinedForeground;
    
    private static Font textAreaFont;
    private static Color logPanelErrorColor;
    private static Color logPanelSuccessColor;
    
    public static void reset() {
        buttonBackground = null;
        buttonBorder = null;
        singleCellTableBorder = null;
        tableCellBackground = null;
        tableCellForeground = null;
        tableCellBorder = null;
        focusedTableCellBackground = null;
        focusedTableCellBorder = null;
        focusedTableRowBackground = null;
        focusedTableRowForeground = null;
        tableSelectionBackground = null;
        notDefinedForeground = null;
        
        textAreaFont = null;
        logPanelErrorColor = null;
        logPanelSuccessColor = null;
    }
    
    public static Color getButtonBackground() {
        if (buttonBackground == null) {
            buttonBackground = getColor("Button.background");
        }
        return buttonBackground; 
    }

    public static Border getButtonBorder() {
        if (buttonBorder == null) {
            buttonBorder = getBorder("Button.border");
        }
        return buttonBorder;
    } 

    public static Border getSingleCellTableBorder() {
        if (singleCellTableBorder == null) {
            singleCellTableBorder = getBorder("TextField.border");
        }
        return singleCellTableBorder;
    } 

    public static Color getTableCellBackground() {
        if (tableCellBackground == null) {
            tableCellBackground = getColor("Table.background");
        }
        return tableCellBackground;
    }

    public static Color getTableCellForeground() {
        if (tableCellForeground == null) {
            tableCellForeground = getColor("Table.foreground");
        }
        return tableCellForeground;
    }

    public static Border getTableCellBorder() {
        if (tableCellBorder == null) {
            tableCellBorder = getBorder("Table.cellNoFocusBorder");
        }
        return tableCellBorder;
    }

    public static Color getFocusedTableCellBackground() {
        if (focusedTableCellBackground == null) {
            Color preferredBackground = colorPreferences != null ? colorPreferences.getFocusedCellBackground() : null;
            focusedTableCellBackground = preferredBackground != null ? preferredBackground : getColor("Table.selectionBackground");
        }
        return focusedTableCellBackground;
    }

    public static Border getFocusedTableCellBorder() {
        if (focusedTableCellBorder == null) {
            Color preferredColor = colorPreferences != null ? colorPreferences.getFocusedCellBorderColor() : null;
            if (preferredColor != null) {
                focusedTableCellBorder = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(preferredColor), BorderFactory.createEmptyBorder(1, 2, 1, 2)); 
            } else {
                focusedTableCellBorder = getBorder("Table.focusCellHighlightBorder");
            }
        }
        return focusedTableCellBorder; 
    }

    public static Color getFocusedTableRowBackground() {
        if (focusedTableRowBackground == null) {
            Color preferredBackground = colorPreferences != null ? colorPreferences.getSelectedRowBackground() : null;
            focusedTableRowBackground = preferredBackground != null ? preferredBackground : getColor("Table.selectionBackground");
        }
        return focusedTableRowBackground; 
    }

    public static Color getFocusedTableRowForeground() {
        if (focusedTableRowForeground == null) {
            focusedTableRowForeground = getColor("Table.selectionForeground");
        }
        return focusedTableRowForeground; 
    }

    public static Color getTableSelectionBackground() {
        if (tableSelectionBackground == null) {
            Color preferredBackground = colorPreferences != null ? colorPreferences.getSelectedCellBackground() : null;
            tableSelectionBackground = preferredBackground != null ? preferredBackground : getColor("Table.selectionInactiveBackground");
        }
        return tableSelectionBackground; 
    }

    public static Color getNotDefinedForeground() {
        if (notDefinedForeground == null) {
            notDefinedForeground = getColor("TextField.inactiveForeground");
            if (!colorTheme.isLight()) {
                notDefinedForeground = notDefinedForeground.darker(); // dark LAF returns the same color as enabled text field 
            }
        }
        return notDefinedForeground;
    }

    public static Font getTextAreaFont() {
        if (textAreaFont == null) {
            textAreaFont = getFont("TextArea.font");
        }
        return textAreaFont;
    }
    
    public static Color getLogPanelErrorColor() {
        if (logPanelErrorColor == null) {
            logPanelErrorColor = colorTheme.isLight() ? new Color(255, 182, 182) : new Color(64, 0, 0);
        }
        return logPanelErrorColor;
    }
    
    public static Color getLogPanelSuccessColor() {
        if (logPanelSuccessColor == null) {
            // for some reason log panel refuses to draw ColorUIResource color
            logPanelSuccessColor = new Color(getColor("Component.focusColor").getRGB());
            if (!colorTheme.isLight()) {
                logPanelSuccessColor = logPanelSuccessColor.darker();
            }
        }
        return logPanelSuccessColor;
    }
    
    
    // ----------- not cached properties ----------- //
    
    public static Color getTitledBorderTitleColor() {
        // Trying to be close to default titled border color.
        // As we don't know what TitledBorder.border contains, use Separator.foreground (as FlatLaf does).
        Color borderColor = getColor("Separator.foreground");
        if (colorTheme.isLight()) {
            return borderColor.darker();
        } else {
            return borderColor.brighter();
        }
    }
    
    public static int getCellHeight() {
        return 19;
    } 
    
    public static int getSingleCellTableIntercellSpacing() {
        // to have right height and to be able to draw table border in editor
        return 2;
    }
}
