package lsfusion.http.provider.session;

import com.google.gwt.core.client.GWT;
import lsfusion.http.LSFAuthenticationToken;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.remote.AuthenticationToken;
import lsfusion.interop.session.RemoteSessionInterface;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// session scoped - one for one browser (! not tab)
public class SessionProviderImpl implements SessionProvider, DisposableBean {

    @Override
    public SessionSessionObject createSession(RemoteLogicsInterface remoteLogics, HttpServletRequest request, String sessionID) throws RemoteException {
        AuthenticationToken lsfToken = LSFAuthenticationToken.getAppServerToken();

        RemoteSessionInterface remoteSession = remoteLogics.createSession(lsfToken, NavigatorProviderImpl.getSessionInfo(request));

        SessionSessionObject sessionSessionObject = new SessionSessionObject(remoteSession);
        addSessionSessionObject(sessionID, sessionSessionObject);
        return sessionSessionObject;
    }

    private final Map<String, SessionSessionObject> currentSessions = new ConcurrentHashMap<>();

    private void addSessionSessionObject(String sessionID, SessionSessionObject sessionSessionObject) {
        currentSessions.put(sessionID, sessionSessionObject);
    }

    @Override
    public SessionSessionObject getSessionSessionObject(String sessionID) {
        return currentSessions.get(sessionID);
    }

    @Override
    public void removeSessionSessionObject(String sessionID) throws RemoteException {
        SessionSessionObject sessionSessionObject = currentSessions.remove(sessionID);
        sessionSessionObject.remoteSession.close();
    }

    @Override
    public void destroy() throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        GWT.log("Destroying session for user " + (auth == null ? "UNKNOWN" : auth.getName()) + "...", new Exception());
        
        for(SessionSessionObject sessionSessionObject : currentSessions.values())
            sessionSessionObject.remoteSession.close();
    }

}