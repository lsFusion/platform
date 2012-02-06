package platform.server.logics.property.actions;

import platform.server.classes.ValueClass;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;

public class ApplyActionProperty extends ActionProperty {

    BusinessLogics BL;

    public ApplyActionProperty(BusinessLogics BL) {
        super("apply", "Применить изменения", new ValueClass[] {} );

        this.BL = BL;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        context.applyChanges(BL);
    }
}
