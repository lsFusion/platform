package platform.server.logics;

import platform.base.BaseUtils;
import platform.base.Result;
import platform.server.classes.ValueClass;
import platform.server.logics.linear.LP;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyInterfaceImplement;
import platform.server.logics.property.PropertyMapImplement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyUtils {
    public static ValueClass[] getValueClasses(boolean mapReturns, LP[] dataProperties, int[][] mapInterfaces) {
        Map<Integer, ValueClass> mapClasses = new HashMap<Integer, ValueClass>();
        for (int i = 0; i < dataProperties.length; ++i) {
            LP dataProperty = dataProperties[i];
            int[] mapPropInterfaces = mapInterfaces[i];
            if (mapPropInterfaces == null) {
                mapPropInterfaces = BaseUtils.consecutiveInts(dataProperty.listInterfaces.size() + (mapReturns ? 1 : 0));
            }

            Result<ValueClass> result = new Result<ValueClass>();
            ValueClass[] propClasses = dataProperty.getCommonClasses(result);
            for (int j = 0; j < mapPropInterfaces.length; ++j) {
                ValueClass valueClass = (mapReturns && j == mapPropInterfaces.length - 1)
                                        ? result.result
                                        : propClasses[j];


                int thisIndex = mapPropInterfaces[j];

                ValueClass definedValueClass = mapClasses.get(thisIndex);
                if (definedValueClass != null) {
                    if (valueClass.isCompatibleParent(definedValueClass)) {
                        valueClass = definedValueClass;
                    } else {
                        assert definedValueClass.isCompatibleParent(valueClass);
                    }
                }

                mapClasses.put(thisIndex, valueClass);
            }
        }


        ValueClass classes[] = new ValueClass[mapClasses.size()];
        for (int i = 0; i < mapClasses.size(); ++i) {
            assert mapClasses.containsKey(i);
            classes[i] = mapClasses.get(i);
            assert classes[i] != null;
        }

        return classes;
    }

    public static Object[] getUParams(LP[] props, int exoff) {
        int intNum = props[0].listInterfaces.size();
        Object[] params = new Object[props.length * (1 + intNum + exoff)];
        for (int i = 0; i < props.length; i++) {
            if (exoff > 0)
                params[i * (1 + intNum + exoff)] = 1;
            params[i * (1 + intNum + exoff) + exoff] = props[i];
            for (int j = 1; j <= intNum; j++)
                params[i * (1 + intNum + exoff) + exoff + j] = j;
        }
        return params;
    }

    public static Object[] directLI(LP prop) {
        return getUParams(new LP[]{prop}, 0);
    }

    // считывает "линейные" имплементации
    public static List<LI> readLI(Object[] params) {
        List<LI> result = new ArrayList<LI>();
        for (int i = 0; i < params.length; i++)
            if (params[i] instanceof Integer)
                result.add(new LII((Integer) params[i]));
            else {
                LMI impl = new LMI((LP) params[i]);
                for (int j = 0; j < impl.mapInt.length; j++)
                    impl.mapInt[j] = (Integer) params[i + j + 1];
                i += impl.mapInt.length;
                result.add(impl);
            }
        return result;
    }

    public static Object[] writeLI(List<LI> linearImpl) {
        Object[][] objectLI = new Object[linearImpl.size()][];
        for (int i = 0; i < linearImpl.size(); i++)
            objectLI[i] = linearImpl.get(i).write();
        int size = 0;
        for (Object[] li : objectLI)
            size += li.length;
        Object[] result = new Object[size];
        int i = 0;
        for (Object[] li : objectLI)
            for (Object param : li)
                result[i++] = param;
        return result;
    }

    public static <T extends PropertyInterface> List<PropertyInterfaceImplement<T>> mapLI(List<LI> linearImpl, List<T> interfaces) {
        List<PropertyInterfaceImplement<T>> result = new ArrayList<PropertyInterfaceImplement<T>>();
        for (LI impl : linearImpl)
            result.add(impl.map(interfaces));
        return result;
    }

    public static <T extends PropertyInterface> List<PropertyInterfaceImplement<T>> readImplements(List<T> listInterfaces, Object... params) {
        return mapLI(readLI(params), listInterfaces);
    }

    protected static int getIntNum(Object[] params) {
        int intNum = 0;
        for (Object param : params)
            if (param instanceof Integer)
                intNum = Math.max(intNum, (Integer) param);
        return intNum;
    }

    public static <T extends PropertyInterface, P extends PropertyInterface> PropertyImplement<T, PropertyInterfaceImplement<P>> mapImplement(LP<T> property, List<PropertyInterfaceImplement<P>> propImpl) {
        int mainInt = 0;
        Map<T, PropertyInterfaceImplement<P>> mapping = new HashMap<T, PropertyInterfaceImplement<P>>();
        for (PropertyInterfaceImplement<P> implement : propImpl) {
            mapping.put(property.listInterfaces.get(mainInt), implement);
            mainInt++;
        }
        return new PropertyImplement<T, PropertyInterfaceImplement<P>>(property.property, mapping);
    }

    static ValueClass[] overrideClasses(ValueClass[] commonClasses, ValueClass[] overrideClasses) {
        ValueClass[] classes = new ValueClass[commonClasses.length];
        int ic = 0;
        for (ValueClass common : commonClasses) {
            ValueClass overrideClass;
            if (ic < overrideClasses.length && ((overrideClass = overrideClasses[ic]) != null)) {
                classes[ic++] = overrideClass;
                assert !overrideClass.isCompatibleParent(common);
            } else
                classes[ic++] = common;
        }
        return classes;
    }

    // Linear Implement
    static abstract class LI {
        abstract <T extends PropertyInterface<T>> PropertyInterfaceImplement<T> map(List<T> interfaces);

        abstract Object[] write();

        abstract Object[] compare(LP compare, BusinessLogics BL, int intOff);
    }

    static class LII extends LI {
        int intNum;

        LII(int intNum) {
            this.intNum = intNum;
        }

        <T extends PropertyInterface<T>> PropertyInterfaceImplement<T> map(List<T> interfaces) {
            return interfaces.get(intNum - 1);
        }

        Object[] write() {
            return new Object[]{intNum};
        }

        Object[] compare(LP compare, BusinessLogics BL, int intOff) {
            return new Object[]{compare, intNum, intNum + intOff};
        }
    }

    static class LMI<P extends PropertyInterface> extends LI {
        LP<P> lp;
        int[] mapInt;

        LMI(LP<P> lp) {
            this.lp = lp;
            this.mapInt = new int[lp.listInterfaces.size()];
        }

        <T extends PropertyInterface<T>> PropertyInterfaceImplement<T> map(List<T> interfaces) {
            PropertyMapImplement<P, T> mapRead = new PropertyMapImplement<P, T>(lp.property);
            for (int i = 0; i < lp.listInterfaces.size(); i++)
                mapRead.mapping.put(lp.listInterfaces.get(i), interfaces.get(mapInt[i] - 1));
            return mapRead;
        }

        Object[] write() {
            Object[] result = new Object[mapInt.length + 1];
            result[0] = lp;
            for (int i = 0; i < mapInt.length; i++)
                result[i + 1] = mapInt[i];
            return result;
        }

        Object[] compare(LP compare, BusinessLogics BL, int intOff) {
            int lmiLen = mapInt.length;
            Object[] common = new Object[lmiLen * 2 + 1];
            Object[] shift = new Object[lmiLen + 1];
            shift[0] = lp;
            for (int j = 1; j <= lmiLen; j++) {
                shift[j] = j + lmiLen;
                common[j] = mapInt[j - 1];
                common[j + lmiLen] = mapInt[j - 1] + intOff;
            }
            common[0] = BL.LM.addJProp(compare, BaseUtils.add(directLI(lp), shift));
            return common;
        }
    }
}
