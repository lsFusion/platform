package platform.server.logics;

import platform.server.data.classes.ConcreteClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.logics.properties.Property;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.filter.CompareValue;
import platform.server.where.Where;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class ObjectValue implements CompareValue {

    public abstract String getString(SQLSyntax syntax);

    public abstract boolean isString(SQLSyntax syntax);

    public abstract SourceExpr getExpr();

    public abstract Object getValue();

    public static ObjectValue getValue(Object value, ConcreteClass objectClass) {
        if(value==null)
            return NullValue.instance;
        else
            return new DataObject(value, objectClass);
    }

    public SourceExpr getSourceExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends SourceExpr> classSource, TableModifier<? extends TableChanges> modifier) throws SQLException {
        return getExpr();
    }
    
    public boolean classUpdated(GroupObjectImplement classGroup) {return false;}
    public boolean objectUpdated(GroupObjectImplement classGroup) {return false;}
    public boolean dataUpdated(Collection<Property> changedProps) {return false;}
    public void fillProperties(Set<Property> properties) {}
    public boolean isInInterface(GroupObjectImplement classGroup) {return true;}

    public abstract Where order(SourceExpr expr, boolean desc, Where orderWhere);

}
