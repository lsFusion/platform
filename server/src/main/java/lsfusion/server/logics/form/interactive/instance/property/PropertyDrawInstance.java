package lsfusion.server.logics.form.interactive.instance.property;

import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.property.ClassViewType;
import lsfusion.interop.form.property.PropertyReadType;
import lsfusion.server.logics.form.interactive.instance.CellInstance;
import lsfusion.server.logics.form.interactive.instance.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.order.OrderInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.physics.admin.authentication.policy.SecurityPolicy;
import lsfusion.server.base.context.ThreadLocalContext;
import lsfusion.server.data.SQLCallable;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.form.struct.property.ActionPropertyObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.value.NullValueProperty;
import lsfusion.server.logics.property.PropertyInterface;

import java.sql.SQLException;

// представление св-ва
public class PropertyDrawInstance<P extends PropertyInterface> extends CellInstance<PropertyDrawEntity> implements PropertyReaderInstance {

    public ActionPropertyObjectInstance getEditAction(String actionId, InstanceFactory instanceFactory, SQLCallable<Boolean> checkReadOnly, ImSet<SecurityPolicy> securityPolicies) throws SQLException, SQLHandledException {
        ActionPropertyObjectEntity<?> editAction = entity.getEditAction(actionId, checkReadOnly, securityPolicies);
        if(editAction!=null)
            return instanceFactory.getInstance(editAction);
        return null;
    }

    private PropertyObjectInstance<?, ?> propertyObject;
    
    public PropertyObjectInstance<?, ?> getValueProperty() {
        return propertyObject;
    }

    public boolean isInInterface(final ImSet<GroupObjectInstance> classGroups, boolean any) {
        return getValueProperty().isInInterface(classGroups, any);
    }

    public OrderInstance getOrder() {
        return (CalcPropertyObjectInstance) getValueProperty();
    }
    
    public boolean isCalcProperty() {
        return getValueProperty() instanceof CalcPropertyObjectInstance;
    }

    // в какой "класс" рисоваться, ессно один из Object.GroupTo должен быть ToDraw
    public GroupObjectInstance toDraw; // не null, кроме когда без параметров в FormInstance проставляется

    public ClassViewType getGroupClassView() {
        return toDraw != null ? toDraw.curClassView : ClassViewType.PANEL;
    }

    private final ImOrderSet<GroupObjectInstance> columnGroupObjects;
    public ImSet<GroupObjectInstance> getColumnGroupObjects() {
        return columnGroupObjects.getSet();
    }
    public ImOrderSet<GroupObjectInstance> getOrderColumnGroupObjects() {
        return columnGroupObjects;
    }
    public ImSet<GroupObjectInstance> getColumnGroupObjectsInGridView() {
        return getColumnGroupObjects().filterFn(new SFunctionSet<GroupObjectInstance>() {
            public boolean contains(GroupObjectInstance element) {
                return element.curClassView.isGrid();
            }
        });
    }

    public Type getType() {
        return entity.getType();
    }

    // предполагается что propertyCaption ссылается на все из propertyObject но без toDraw (хотя опять таки не обязательно)
    public final CalcPropertyObjectInstance<?> propertyCaption;
    public final CalcPropertyObjectInstance<?> propertyShowIf;
    public final CalcPropertyObjectInstance<?> propertyReadOnly;
    public final CalcPropertyObjectInstance<?> propertyFooter;
    public final CalcPropertyObjectInstance<?> propertyBackground;
    public final CalcPropertyObjectInstance<?> propertyForeground;

    // извращенное множественное наследование
    public CaptionReaderInstance captionReader = new CaptionReaderInstance();
    public ShowIfReaderInstance showIfReader = new ShowIfReaderInstance();
    public FooterReaderInstance footerReader = new FooterReaderInstance();
    public ReadOnlyReaderInstance readOnlyReader = new ReadOnlyReaderInstance();
    public BackgroundReaderInstance backgroundReader = new BackgroundReaderInstance();
    public ForegroundReaderInstance foregroundReader = new ForegroundReaderInstance();

    public HiddenReaderInstance hiddenReader = new HiddenReaderInstance();

    public PropertyDrawInstance(PropertyDrawEntity<P> entity,
                                PropertyObjectInstance<?, ?> propertyObject,
                                GroupObjectInstance toDraw,
                                ImOrderSet<GroupObjectInstance> columnGroupObjects,
                                CalcPropertyObjectInstance<?> propertyCaption,
                                CalcPropertyObjectInstance<?> propertyShowIf,
                                CalcPropertyObjectInstance<?> propertyReadOnly,
                                CalcPropertyObjectInstance<?> propertyFooter,
                                CalcPropertyObjectInstance<?> propertyBackground,
                                CalcPropertyObjectInstance<?> propertyForeground) {
        super(entity);
        this.propertyObject = propertyObject;
        this.toDraw = toDraw;
        this.columnGroupObjects = columnGroupObjects;
        this.propertyCaption = propertyCaption;
        this.propertyShowIf = propertyShowIf;
        this.propertyReadOnly = propertyReadOnly;
        this.propertyFooter = propertyFooter;
        this.propertyBackground = propertyBackground;
        this.propertyForeground = propertyForeground;
    }

    public CalcPropertyObjectInstance getPropertyObjectInstance() {
        return getDrawInstance();
    }

    public CalcPropertyObjectInstance<?> getDrawInstance() {
        return getValueProperty().getDrawProperty();
    }

    public byte getTypeID() {
        return PropertyReadType.DRAW;
    }

    public ClassViewType getForceViewType() {
        return entity.forceViewType;
    }

    public String toString() {
        return propertyObject.toString();
    }

    public PropertyDrawEntity getEntity() {
        return entity;
    }

    @Override
    public Object getProfiledObject() {
        return entity;
    }

    // заглушка чтобы на сервере ничего не читать
    public class HiddenReaderInstance implements PropertyReaderInstance {

        public CalcPropertyObjectInstance getPropertyObjectInstance() {
            return new CalcPropertyObjectInstance<>(NullValueProperty.instance, MapFact.<PropertyInterface, ObjectInstance>EMPTY());
        }

        public byte getTypeID() {
            return PropertyDrawInstance.this.getTypeID();
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        @Override
        public Object getProfiledObject() {
            return NullValueProperty.instance;
        }
    }

    public class ShowIfReaderInstance implements PropertyReaderInstance {

        public CalcPropertyObjectInstance getPropertyObjectInstance() {
            return propertyShowIf;
        }

        public byte getTypeID() {
            return PropertyReadType.SHOWIF;
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        public PropertyDrawInstance<P> getPropertyDraw() {
            return PropertyDrawInstance.this;
        }

        @Override
        public Object getProfiledObject() {
            return entity.propertyShowIf;
        }
    }

    public class CaptionReaderInstance implements PropertyReaderInstance {
        public CalcPropertyObjectInstance getPropertyObjectInstance() {
            return propertyCaption;
        }

        public byte getTypeID() {
            return PropertyReadType.CAPTION;
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        @Override
        public String toString() {
            return ThreadLocalContext.localize("{logics.property.caption}") + "(" + PropertyDrawInstance.this.toString() + ")";
        }

        @Override
        public Object getProfiledObject() {
            return entity.propertyCaption;
        }
    }

    public class FooterReaderInstance implements PropertyReaderInstance {
        public CalcPropertyObjectInstance getPropertyObjectInstance() {
            return propertyFooter;
        }

        public byte getTypeID() {
            return PropertyReadType.FOOTER;
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        @Override
        public String toString() {
            return ThreadLocalContext.localize("{logics.property.footer}") + "(" + PropertyDrawInstance.this.toString() + ")";
        }

        @Override
        public Object getProfiledObject() {
            return entity.propertyFooter;
        }
    }

    public class ReadOnlyReaderInstance implements PropertyReaderInstance {
        public CalcPropertyObjectInstance getPropertyObjectInstance() {
            return propertyReadOnly;
        }

        public byte getTypeID() {
            return PropertyReadType.READONLY;
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        @Override
        public String toString() {
            return ThreadLocalContext.localize("{logics.property.readonly}") + "(" + PropertyDrawInstance.this.toString() + ")";
        }

        @Override
        public Object getProfiledObject() {
            return entity.propertyReadOnly;
        }
    }

    public class BackgroundReaderInstance implements PropertyReaderInstance {
        public CalcPropertyObjectInstance getPropertyObjectInstance() {
            return propertyBackground;
        }

        public byte getTypeID() {
            return PropertyReadType.CELL_BACKGROUND;
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        @Override
        public String toString() {
            return ThreadLocalContext.localize("{logics.background}") + "(" + PropertyDrawInstance.this.toString() + ")";
        }

        @Override
        public Object getProfiledObject() {
            return entity.propertyBackground;
        }
    }

    public class ForegroundReaderInstance implements PropertyReaderInstance {
        public CalcPropertyObjectInstance getPropertyObjectInstance() {
            return propertyForeground;
        }

        public byte getTypeID() {
            return PropertyReadType.CELL_FOREGROUND;
        }

        public int getID() {
            return PropertyDrawInstance.this.getID();
        }

        @Override
        public String toString() {
            return ThreadLocalContext.localize("{logics.foreground}") + "(" + PropertyDrawInstance.this.toString() + ")";
        }

        @Override
        public Object getProfiledObject() {
            return entity.propertyForeground;
        }
    }
}