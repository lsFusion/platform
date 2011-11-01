package platform.gwt.utils;

import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.Iterator;
import java.util.List;

public class GwtUtils {
    public static <K> int relativePosition(K element, List<K> comparatorList, List<K> insertList) {
        int ins = 0;
        int ind = comparatorList.indexOf(element);

        Iterator<K> icp = insertList.iterator();
        while (icp.hasNext() && comparatorList.indexOf(icp.next()) < ind) {
            ins++;
        }
        return ins;
    }

    public static String rtrim(String string) {
        if (string == null) return "";

        int len = string.length();
        while (len > 0 && string.charAt(len - 1) == ' ') len--;
        return string.substring(0, len);
    }

    public static String toHtml(String plainString) {
        if (plainString == null) {
            return "";
        }
        return SimpleHtmlSanitizer.sanitizeHtml(plainString).asString().replaceAll("(\r\n|\n\r|\r|\n)", "<br />");
    }

    public static void removeLoaderFromHostedPage() {
        RootPanel p = RootPanel.get("loadingWrapper");
        if (p != null) {
            RootPanel.getBodyElement().removeChild(p.getElement());
        }
    }
}
