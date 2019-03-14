package lsfusion.server.logics.action;

import lsfusion.server.base.context.ExecutionStack;
import lsfusion.server.logics.action.ExecutionEnvironment;

public interface EnvStackRunnable {

    void run(ExecutionEnvironment env, ExecutionStack stack);

}