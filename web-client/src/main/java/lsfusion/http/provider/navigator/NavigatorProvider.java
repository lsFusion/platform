package lsfusion.http.provider.navigator;


import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.interop.logics.LogicsSessionObject;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;

public interface NavigatorProvider {

    String createNavigator(LogicsSessionObject sessionObject, HttpServletRequest request, Integer screenWidth, Integer screenHeight) throws RemoteException;
    NavigatorSessionObject getNavigatorSessionObject(String sessionID) throws SessionInvalidatedException;
    NavigatorSessionObject createOrGetNavigatorSessionObject(String sessionID, LogicsSessionObject sessionObject, HttpServletRequest request) throws RemoteException;
    void removeNavigatorSessionObject(String sessionID) throws RemoteException;

    String getLogicsName(String sessionID) throws SessionInvalidatedException;
    
    String getSessionInfo();
}
