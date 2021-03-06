package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.property.LP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;

import java.util.List;

public class ModuleIndirectLocalsFinder extends ModuleLocalsFinder {
    @Override
    protected boolean accepted(LogicsModule module, LP<?> property, List<ResolveClassSet> signature) {
        return SignatureMatcher.isSoftCompatible(module.getParamClasses(property), signature);
    }
}
