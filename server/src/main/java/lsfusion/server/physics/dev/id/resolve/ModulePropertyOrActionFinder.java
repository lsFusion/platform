package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;

import java.util.ArrayList;
import java.util.List;

public abstract class ModulePropertyOrActionFinder<T extends LAP<?, ?>> implements ModuleFinder<T, List<ResolveClassSet>> {
    @Override
    public List<T> resolveInModule(LogicsModule module, String simpleName, List<ResolveClassSet> signature) {
        List<T> result = new ArrayList<>();
        for (T property : getSourceList(module, simpleName)) {
            if (accepted(module, property, signature)) {
                result.add(property);
            }
        }
        return result;
    }
    
    protected abstract Iterable<T> getSourceList(LogicsModule module, String name);
    
    protected abstract boolean accepted(LogicsModule module, T property, List<ResolveClassSet> signature);
}
