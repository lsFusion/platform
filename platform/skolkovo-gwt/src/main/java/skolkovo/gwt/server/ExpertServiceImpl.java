package skolkovo.gwt.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import skolkovo.api.remote.SkolkovoRemoteInterface;
import skolkovo.gwt.client.ExpertService;
import skolkovo.gwt.shared.GwtVoteInfo;
import skolkovo.gwt.shared.MessageException;

import java.security.Principal;

public class ExpertServiceImpl extends RemoteServiceServlet implements ExpertService {

    public GwtVoteInfo getVoteInfo(int voteId) throws MessageException {
        try {
            Principal user = getThreadLocalRequest().getUserPrincipal();
            if (user == null) {
                return null;
            }

            return VoteFactory.toGwtVoteInfo(getLogics().getVoteInfo(user.getName(), voteId));
        } catch (Throwable e) {
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
    }

    public void setVoteInfo(GwtVoteInfo voteInfo, int voteId) throws MessageException {
        try {
            Principal user = getThreadLocalRequest().getUserPrincipal();
            if (user != null) {
                getLogics().setVoteInfo(user.getName(), voteId, VoteFactory.toVoteInfo(voteInfo));
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new MessageException(DebugUtil.getInitialCause(e).getMessage());
        }
    }

    private SkolkovoRemoteInterface getLogics() {
        String serverHost = getServletConfig().getInitParameter("serverHost");
        String serverPort = getServletConfig().getInitParameter("serverPort");
        return SkolkovoLogicsClient.getInstance().getLogics(serverHost, serverPort);
    }
}