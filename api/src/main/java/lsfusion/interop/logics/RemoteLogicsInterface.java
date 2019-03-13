package lsfusion.interop.logics;

import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.navigator.NavigatorInfo;
import lsfusion.interop.session.SessionInfo;
import lsfusion.interop.action.ReportPath;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.PendingRemoteInterface;
import lsfusion.interop.session.RemoteSessionInterface;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface RemoteLogicsInterface extends PendingRemoteInterface {

    // obsolete

    // main interface

    // authentication
    AuthenticationToken authenticateUser(String userName, String password) throws RemoteException;

    // stateful interfaces
    RemoteNavigatorInterface createNavigator(AuthenticationToken token, NavigatorInfo navigatorInfo) throws RemoteException;
    RemoteSessionInterface createSession(AuthenticationToken token, SessionInfo sessionInfo) throws RemoteException;

    // RESTful interfaces
    // external requests (interface is similar to RemoteSessionInterface but with token)
    ExternalResponse exec(AuthenticationToken token, SessionInfo sessionInfo, String action, ExternalRequest request) throws RemoteException;
    ExternalResponse eval(AuthenticationToken token, SessionInfo sessionInfo, boolean action, Object paramScript, ExternalRequest request) throws RemoteException;

    // separate methods, because used really often (and don't need authentication)
    long generateID() throws RemoteException;
    void ping() throws RemoteException;
    void sendPingInfo(String computerName, Map<Long, List<Long>> pingInfoMap) throws RemoteException;

    List<ReportPath> saveAndGetCustomReportPathList(String formSID, boolean recreate) throws RemoteException;
}