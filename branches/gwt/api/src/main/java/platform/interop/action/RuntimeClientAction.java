package platform.interop.action;

import java.io.IOException;

public class RuntimeClientAction extends AbstractClientAction {

    public String command;
    public String[] environment;
    public String directory;

    public boolean waitFor = true;

    public byte[] input;

    public RuntimeClientAction(String command, String[] environment, String directory) {
        
        this.command = command;
        this.environment = environment;
        this.directory = directory;
    }

    @Override
    public Object dispatchResult(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}
