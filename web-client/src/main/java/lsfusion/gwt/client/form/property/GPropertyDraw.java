package lsfusion.gwt.client.form.property;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.base.ImageHolder;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.classes.GActionType;
import lsfusion.gwt.client.classes.GClass;
import lsfusion.gwt.client.classes.GObjectType;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.classes.data.GLongType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontMetrics;
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.object.table.controller.GTableController;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.view.FormatCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.panel.view.PanelRenderer;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.StyleDefaults;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class GPropertyDraw extends GComponent implements GPropertyReader, Serializable {
    public int ID;
    public String sID;
    public String namespace;
    public String caption;
    public String canonicalName;
    public String propertyFormName;

    public String toolTip;
    public String tableName;
    public String[] interfacesCaptions;
    public GClass[] interfacesTypes;
    public String creationScript;
    public String creationPath;
    public String formPath;

    public GGroupObject groupObject;
    public String columnsName;
    public ArrayList<GGroupObject> columnGroupObjects;

    public boolean grid;

    public GType baseType;
    public String pattern;
    public String defaultPattern;
    public GClass returnClass;

    public GType changeWYSType;
    public GType changeType;

    public AddRemove addRemove;
    public boolean askConfirm;
    public String askConfirmMessage;

    public boolean hasEditObjectAction;
    public boolean hasChangeAction;

    public GEditBindingMap editBindingMap;

    public ImageHolder imageHolder;
    public Boolean focusable;
    public boolean checkEquals;
    public GPropertyEditType editType = GPropertyEditType.EDITABLE;

    public boolean echoSymbols;
    public boolean noSort;
    public GCompare defaultCompare;

    public ArrayList<GInputBindingEvent> bindingEvents = new ArrayList<>();
    public boolean showChangeKey;

    public boolean hasKeyBinding() {
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GKeyInputEvent)
                return true;
        return false;
    }
    public String getKeyBindingText() {
        assert hasKeyBinding();
        String result = "";
        for(GInputBindingEvent bindingEvent : bindingEvents)
            if(bindingEvent.inputEvent instanceof GKeyInputEvent) {
                result = (result.isEmpty() ? "" : result + ",") + ((GKeyInputEvent) bindingEvent.inputEvent).keyStroke;
            }
        return result;
    }

    public boolean drawAsync;

    public GCaptionReader captionReader;
    public GShowIfReader showIfReader;
    public GFooterReader footerReader;
    public GReadOnlyReader readOnlyReader;
    public GBackgroundReader backgroundReader;
    public GForegroundReader foregroundReader;

    // for pivoting
    public String formula;
    public ArrayList<GPropertyDraw> formulaOperands;

    public String aggrFunc;
    public ArrayList<GLastReader> lastReaders;
    public boolean lastAggrDesc;

    public GPropertyDraw quickFilterProperty;

    public int charWidth;
    public int charHeight;

    public int valueWidth = -1;
    public int valueHeight = -1;

    public boolean panelCaptionAbove;
    
    public boolean columnKeysVertical;
    
    public GFlexAlignment valueAlignment;

    public Boolean editOnSingleClick;

    public boolean hide;

    private transient CellRenderer cellRenderer;
    
    public boolean notNull;

    // eventually gets to PropertyDrawEntity.getEventAction (which is symmetrical to this)
    public String getEventSID(Event editEvent) {
        String actionSID = null;
        if (editBindingMap != null) { // property bindings
            actionSID = editBindingMap.getEventSID(editEvent);
        }
        // not sure that it should be done like this since enter is used as a tab
//        if (actionSID == null && baseType instanceof GActionType && GKeyStroke.isEnterKeyEvent(editEvent)) {
//            return GEditBindingMap.CHANGE;
//        }
        if (actionSID == null) {
            actionSID = GEditBindingMap.getDefaultEventSID(editEvent, changeType == null ? null : changeType.getEditEventFilter());
        }
        return actionSID;
    }
    public boolean isFilterChange(Event editEvent) {
        return GEditBindingMap.isDefaultFilterChange(editEvent, baseType.getEditEventFilter());
    }

    public static class AddRemove implements Serializable {
        public GObject object;
        public boolean add;

        public AddRemove() {}

        public AddRemove(GObject object, boolean add) {
            this.object = object;
            this.add = add;
        }
    }

    public GPropertyDraw(){}

    public void update(GTableController controller, Map<GGroupObjectValue, Object> values, boolean updateKeys) {
        throw new UnsupportedOperationException();
    }

    public PanelRenderer createPanelRenderer(GFormController form, GGroupObjectValue columnKey) {
        return baseType.createPanelRenderer(form, this, columnKey);
    }

    public CellRenderer getCellRenderer() {
        if (cellRenderer == null) {
            cellRenderer = baseType.createGridCellRenderer(this);
        }
        return cellRenderer;
    }

    public void setUserPattern(String pattern) {
        if(baseType instanceof GFormatType) {
            this.pattern = pattern != null ? pattern : defaultPattern;

            CellRenderer renderer = getCellRenderer();
            if (renderer instanceof FormatCellRenderer) {
                ((FormatCellRenderer) renderer).updateFormat();
            } else
                assert false;
        }
    }

    public Object parseChangeValueOrNull(String s) {
        if (s == null || changeWYSType == null) {
            return null;
        }
        try {
            return changeWYSType.parseString(s, pattern);
        } catch (ParseException pe) {
            return null;
        }
    }

    public boolean canUseChangeValueForRendering() {
        return changeType != null && baseType.getClass() == changeType.getClass();
    }

    @Override
    public int getGroupObjectID() {
        return groupObject != null ? groupObject.ID : -1;
    }

    public String getCaptionOrEmpty() {
        return caption == null ? "" : caption;
    }

    public String getDynamicCaption(Object caption) {
        return caption == null ? "" : caption.toString().trim();
    }

    public String getEditCaption(String caption) {
        if (caption == null) {
            caption = this.caption;
        }

        if(showChangeKey && hasKeyBinding())
            caption += " (" + getKeyBindingText() + ")";
        return caption;
    }

    public String getEditCaption() {
        return getEditCaption(caption);
    }

    public String getNotEmptyCaption() {
        if (caption == null || caption.trim().length() == 0) {
            return getMessages().propertyEmptyCaption();
        } else {
            return caption;
        }
    }

    private static ClientMessages getMessages() {
        return ClientMessages.Instance.get();
    }
    
    public static final String TOOL_TIP_FORMAT =
            "<html><b>%s</b><br>%s";

    public static String getDetailedToolTipFormat() {
        return  "<hr>" +
                "<b>" + getMessages().propertyTooltipCanonicalName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipTable() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipObjects() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipSignature() + ":</b> %s (%s)<br>" +
                "<b>" + getMessages().propertyTooltipScript() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipPath() + ":</b> %s<br>" +
                "<hr>" +
                "<b>" + getMessages().propertyTooltipFormPropertyName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipFormPropertyDeclaration() + ":</b> %s" +
                "</html>";
    }  
    
    public static String getDetailedActionToolTipFormat() {
        return  "<hr>" +
                "<b>sID:</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipObjects() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipPath() + ":</b> %s<br>" +
                "<hr>" +
                "<b>" + getMessages().propertyTooltipFormPropertyName() + ":</b> %s<br>" +
                "<b>" + getMessages().propertyTooltipFormPropertyDeclaration() + ":</b> %s" +
                "</html>";
    }
    
    public static String getChangeKeyToolTipFormat() {
        return "<hr><b>" + getMessages().propertyTooltipHotkey() + ":</b> %s<br>";
    }
            
    public String getTooltipText(String caption) {
        String propCaption = GwtSharedUtils.nullTrim(!GwtSharedUtils.isRedundantString(toolTip) ? toolTip : caption);
        String keyBindingText = hasKeyBinding() ? GwtSharedUtils.stringFormat(getChangeKeyToolTipFormat(), getKeyBindingText()) : null;

        if (!MainFrame.showDetailedInfo) {
            return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT, propCaption, keyBindingText);
        } else {
            String ifaceObjects = GwtSharedUtils.toString(", ", interfacesCaptions);
            String scriptPath = creationPath != null ? creationPath.replace("\n", "<br>") : "";
            String scriptFormPath = formPath != null ? formPath.replace("\n", "<br>") : "";
            
            if (baseType instanceof GActionType) {
                return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT + getDetailedActionToolTipFormat(),
                        propCaption, keyBindingText, canonicalName, ifaceObjects, scriptPath, propertyFormName, scriptFormPath);
            } else {
                String tableName = this.tableName != null ? this.tableName : "&lt;none&gt;";
                String returnClass = this.returnClass.toString();
                String ifaceClasses = GwtSharedUtils.toString(", ", interfacesTypes);
                String script = creationScript != null ? escapeHTML(creationScript).replace("\n", "<br>") : "";
                
                return GwtSharedUtils.stringFormat(TOOL_TIP_FORMAT + getDetailedToolTipFormat(),
                        propCaption, keyBindingText, canonicalName, tableName, ifaceObjects, returnClass, ifaceClasses,
                        script, scriptPath, propertyFormName, scriptFormPath);
            }
        }
    }

    private String escapeHTML(String value) {
        return value.replace("<", "&lt;").replace(">", "&gt;");
    }

    public ImageDescription getImage() {
        return getImage(true);
    }

    public ImageDescription getImage(boolean enabled) {
        ImageDescription image = imageHolder != null ? imageHolder.getImage() : null;
        if (!enabled && image != null && image.url != null) {
            int dotInd = image.url.lastIndexOf(".");
            if (dotInd != -1) {
                return new ImageDescription(image.url.substring(0, dotInd) + "_Disabled" + image.url.substring(dotInd), image.width, image.height);
            }
        }
        return image;
    } 

    public boolean isReadOnly() {
        return editType == GPropertyEditType.READONLY;
    }

    public boolean isEditableNotNull() {
        return notNull && !isReadOnly();
    }

    public double getFlex() {
        if (flex == -2) {
            return getValueWidth(null);
        }
        return flex;
    }

    public GFlexAlignment getAlignment() {
        return alignment;
    }
    
    public Style.TextAlign getTextAlignStyle() {
        if (valueAlignment != null) {
            switch (valueAlignment) {
                case START:
                    return Style.TextAlign.LEFT;
                case CENTER:
                case STRETCH:
                    return Style.TextAlign.CENTER;
                case END:
                    return Style.TextAlign.RIGHT;
            }
        }
        return null;
    }

    // padding has to be included for grid column for example, and not for panel property (since flex, width, min-width, etc. doesn't include padding)
    public int getValueWidthWithPadding(GFont parentFont) {
        return getValueWidth(parentFont) + getCellRenderer().getWidthPadding() * 2;
    }

    public int getValueWidth(GFont parentFont) {
        if (valueWidth != -1) {
            return valueWidth;
        }

        GFont font = this.font != null ? this.font : parentFont;

        String widthString = null;
        if(widthString == null && charWidth != 0)
            widthString = GwtSharedUtils.replicate('0', charWidth);
        if(widthString != null)
            return baseType.getFullWidthString(widthString, font);

        return baseType.getDefaultWidth(font, this);
    }

    public Object getFormat() {
        return (baseType instanceof GObjectType ? GLongType.instance : ((GFormatType)baseType)).getFormat(pattern);
    }

    public int getValueHeight(GFont parentFont) {
        if (valueHeight != -1) {
            return valueHeight;
        }

        // we don't set padding to cell or button, but count them to have visual padding
        int insets = StyleDefaults.CELL_VERTICAL_PADDING * 2;
        GFont usedFont = font != null ? font : parentFont;
        int lines = charHeight == 0 ? baseType.getDefaultCharHeight() : charHeight;
        int height;
        if ((usedFont != null && usedFont.size > 0) || lines > 1) {
            int lineHeight = GFontMetrics.getSymbolHeight(font);
            height = lineHeight * lines + insets;
        } else {
            height = StyleDefaults.VALUE_HEIGHT;
        }
        
        final ImageDescription image = getImage();
        if (image != null && image.height >= 0) {
            height = Math.max(image.height + insets, height);
        }
        return height;
    }

    public LinkedHashMap<String, String> getContextMenuItems() {
        return editBindingMap == null ? null : editBindingMap.getContextMenuItems();
    }

    @Override
    public int hashCode() {
        return ID;
    }

    @Override
    public String toString() {
        return "GPropertyDraw{" +
                "sID='" + sID + '\'' +
                ", caption='" + caption + '\'' +
                ", baseType=" + baseType +
                ", changeType=" + changeType +
                ", imagePath='" + imageHolder.getDefaultImage() + '\'' +
                ", focusable=" + focusable +
                ", checkEquals=" + checkEquals +
                ", editType=" + editType +
                '}';
    }
}
