package platform.server.logics;

import net.sf.jasperreports.engine.JRException;
import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.apache.log4j.Logger;
import platform.base.BaseUtils;
import platform.server.LsfLogicsLexer;
import platform.server.LsfLogicsParser;
import platform.server.classes.*;
import platform.server.data.Union;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.StoredDataProperty;
import platform.server.logics.property.group.AbstractGroup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * User: DAle
 * Date: 03.06.11
 * Time: 14:54
 */

public class ScriptingLogicsModule extends LogicsModule {

    private final static Logger scriptLogger = Logger.getLogger(ScriptingLogicsModule.class);
    private String scriptName;
    private String code = null;
    private String filename = null;
    private final BusinessLogics<?> BL;
    private final Set<String> importedModules = new HashSet<String>();
    private final ScriptingErrorLog errLog;
    private LsfLogicsParser parser;

    public enum State {GROUP, CLASS, PROP, NAVIGATOR}
    public enum ConstType { INT, REAL, STRING, LOGICAL, ENUM }

    private Map<String, ValueClass> primitiveTypeAliases = BaseUtils.buildMap(
            Arrays.<String>asList("INTEGER", "DOUBLE", "LONG", "DATE", "BOOLEAN"),
            Arrays.<ValueClass>asList(IntegerClass.instance, DoubleClass.instance, LongClass.instance, DateClass.instance, LogicalClass.instance)
    );

    private ScriptingLogicsModule(String scriptName, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        super(scriptName);
        setBaseLogicsModule(baseModule);
        this.scriptName = scriptName;
        this.BL = BL;
        errLog = new ScriptingErrorLog(scriptName);
    }

    public static ScriptingLogicsModule createFromString(String scriptName, String code, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        ScriptingLogicsModule module = new ScriptingLogicsModule(scriptName, baseModule, BL);
        module.code = code;
        return module;
    }

    public static ScriptingLogicsModule createFromFile(String scriptName, String filename, BaseLogicsModule<?> baseModule, BusinessLogics<?> BL) {
        ScriptingLogicsModule module = new ScriptingLogicsModule(scriptName, baseModule, BL);
        module.filename = filename;
        return module;
    }

    private CharStream createStream() throws IOException {
        if (code != null) {
            return new ANTLRStringStream(code);
        } else {
            return new ANTLRFileStream(filename, "UTF-8");
        }
    }

    public ScriptingErrorLog getErrLog() {
        return errLog;
    }

    public LsfLogicsParser getParser() {
        return parser;
    }

    public void addImportedModule(String moduleName) {
        scriptLogger.info("import " + moduleName + ";");
        importedModules.add(moduleName);
    }

    protected LogicsModule findModule(String sid) throws ScriptingErrorLog.SemanticErrorException {
        List<LogicsModule> modules = BL.getLogicModules();
        for (LogicsModule module : modules) {
            if (module.getSID().equals(sid)) {
                return module;
            }
        }
        checkModule(null, sid);
        return null;
    }

    private String transformStringLiteral(String captionStr) {
        String caption = captionStr.replace("\\'", "'");
        caption = caption.replace("\\n", "\n");
        caption = caption.replace("\\r", "\r");
        caption = caption.replace("\\t", "\t");
        return caption.substring(1, caption.length()-1);
    }

    private ValueClass getPredefinedClass(String name) {
        if (primitiveTypeAliases.containsKey(name)) {
            return primitiveTypeAliases.get(name);
        } else if (name.startsWith("STRING[")) {
            name = name.substring("STRING[".length(), name.length() - 1);
            return StringClass.get(Integer.parseInt(name));
        } else if (name.startsWith("ISTRING[")) {
            name = name.substring("ISTRING[".length(), name.length() - 1);
            return InsensitiveStringClass.get(Integer.parseInt(name));
        }
        return null;
    }

    public ValueClass findClassByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
            ValueClass valueClass = getPredefinedClass(name);
            if (valueClass == null) {
                int dotPosition = name.indexOf('.');
                if (dotPosition > 0) {
                    LogicsModule module = findModule(name.substring(0, dotPosition));
                    valueClass = module.getClassByName(name.substring(dotPosition + 1));
                } else {
                    valueClass = getClassByName(name);
                    if (valueClass == null) {
                        for (String importModuleName : importedModules) {
                            LogicsModule module = findModule(importModuleName);
                            if ((valueClass = module.getClassByName(name)) != null) {
                                break;
                            }
                        }
                    }
                }
            }
            checkClass(valueClass, name);
            return valueClass;
    }

    public void addScriptedClass(String className, String captionStr, boolean isAbstract, boolean isStatic,
                                 List<String> instNames, List<String> instCaptions, List<String> parentNames) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedClass(" + className + ", " + (captionStr==null ? "" : captionStr) + ", " + isAbstract + ", " + isStatic + ", " + instNames + ", " + instCaptions + ", " + parentNames + ");");
        checkDuplicateClass(className);
        checkStaticClassConstraints(className, isStatic, isAbstract, instNames, instCaptions);
        checkClassParents(parentNames);

        String caption = (captionStr == null ? className : transformStringLiteral(captionStr));

        CustomClass[] parents;
        if (!isStatic && parentNames.isEmpty()) {
            parents = new CustomClass[] {baseLM.baseClass};
        } else {
            parents = new CustomClass[parentNames.size()];
            for (int i = 0; i < parentNames.size(); i++) {
                String parentName = parentNames.get(i);
                parents[i] = (CustomClass) findClassByCompoundName(parentName);
            }
        }

        assert !(isStatic && isAbstract);
        if (isStatic) {
            String[] captions = new String[instCaptions.size()];
            for (int i = 0; i < instCaptions.size(); i++) {
                captions[i] = (instCaptions.get(i) == null ? null : transformStringLiteral(instCaptions.get(i)));
            }
            addStaticClass(className, caption, instNames.toArray(new String[instNames.size()]), captions, parents);
        } else if (isAbstract) {
            addAbstractClass(className, caption, parents);
        } else {
            addConcreteClass(className, caption, parents);
        }
    }

    private AbstractGroup findGroupByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        AbstractGroup group;
        int dotPosition = name.indexOf('.');
        if (dotPosition > 0) {
            LogicsModule module = findModule(name.substring(0, dotPosition));
            group = module.getGroupByName(name.substring(dotPosition + 1));
        } else {
            group = getGroupByName(name);
            if (group == null) {
                for (String importModuleName : importedModules) {
                    LogicsModule module = findModule(importModuleName);
                    if ((group = module.getGroupByName(name)) != null) {
                        break;
                    }
                }
            }
        }
        checkGroup(group, name);
        return group;
    }

    public LP<?> findLPByCompoundName(String name) throws ScriptingErrorLog.SemanticErrorException {
        LP<?> property;
        int dotPosition = name.indexOf('.');
        if (dotPosition > 0) {
            LogicsModule module = findModule(name.substring(0, dotPosition));
            property = module.getLPByName(name.substring(dotPosition + 1));
        } else {
            property = getLPByName(name);
            if (property == null) {
                for (String importModuleName : importedModules) {
                    LogicsModule module = findModule(importModuleName);
                    if ((property = module.getLPByName(name)) != null) {
                        break;
                    }
                }
            }
        }

        checkProperty(property, name);
        return property;
    }

    public List<String> getNamedParamsList(String propertyName) throws ScriptingErrorLog.SemanticErrorException {
        List<String> paramList;
        int dotPosition = propertyName.indexOf('.');
        if (dotPosition > 0) {
            LogicsModule module = findModule(propertyName.substring(0, dotPosition));
            paramList = module.getNamedParams(module.transformNameToSID(propertyName.substring(dotPosition + 1)));
        } else {
            paramList = getNamedParams(transformNameToSID(propertyName));
            if (paramList == null) {
                for (String importModuleName : importedModules) {
                    LogicsModule module = findModule(importModuleName);
                    if ((paramList = module.getNamedParams(module.transformNameToSID(propertyName))) != null) {
                        break;
                    }
                }
            }
        }
        return paramList;
    }

    private List<String> getNamedParamsList(Object obj) throws ScriptingErrorLog.SemanticErrorException {
        if (obj instanceof LP) {
            return getNamedParams(((LP)obj).property.getSID());
        } else {
            return getNamedParamsList((String) obj);
        }
    }

    public void addScriptedGroup(String groupName, String captionStr, String parentName) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedGroup(" + groupName + ", " + (captionStr==null ? "" : captionStr) + ", " + (parentName == null ? "null" : parentName) + ");");
        checkDuplicateGroup(groupName);
        String caption = (captionStr == null ? groupName : transformStringLiteral(captionStr));
        AbstractGroup parentGroup = (parentName == null ? null : findGroupByCompoundName(parentName));
        addAbstractGroup(groupName, caption, parentGroup);
    }

    public ScriptingFormEntity createScriptedForm(String formName, String caption) {
        scriptLogger.info("createScriptedForm(" + formName + ", " + caption + ");");
        return new ScriptingFormEntity(baseLM.baseElement, this, formName, transformStringLiteral(caption));
    }

    public void addScriptedForm(ScriptingFormEntity form) {
        scriptLogger.info("addScriptedFrom(" + form + ");");
        addFormEntity(form);
    }

    private String toLog(Object obj) {
        return BaseUtils.toCaption(obj);
    }

    public LP<?> addScriptedDProp(String caption, String returnClass, List<String> paramClasses, boolean innerProp) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedDProp(" + returnClass + ", " + paramClasses + ", " + innerProp + ");");

        ValueClass value = findClassByCompoundName(returnClass);
        ValueClass[] params = new ValueClass[paramClasses.size()];
        for (int i = 0; i < paramClasses.size(); i++) {
            params[i] = findClassByCompoundName(paramClasses.get(i));
        }
        if (innerProp) {
            return addDProp(genSID(), caption, value, params);
        } else {
            StoredDataProperty dataProperty = new StoredDataProperty(genSID(), caption, params, value);
            return addProperty(null, new LP<ClassPropertyInterface>(dataProperty));
        }
    }

    public int getParamIndex(String param, List<String> namedParams, boolean dynamic) throws ScriptingErrorLog.SemanticErrorException {
        int index = -1;
        if (namedParams != null) {
            index = namedParams.indexOf(param);
        }
        if (index < 0 && param.startsWith("$")) {
            index = Integer.parseInt(param.substring(1)) - 1;
            if (index < 0 || !dynamic && namedParams != null && index >= namedParams.size()) {
                errLog.emitParamIndexError(parser, index + 1, namedParams == null ? 0 : namedParams.size());
            }
        }
        if (index < 0 && namedParams != null && dynamic) {
            index = namedParams.size();
            namedParams.add(param);
        }
        if (index < 0) {
            errLog.emitParamNotFoundError(parser, param);
        }
        return index;
    }

    public class LPWithParams {
        public LP<?> property;
        public List<Integer> usedParams;

        public LPWithParams(LP<?> property, List<Integer> usedParams) {
            this.property = property;
            this.usedParams = usedParams;
        }
    }

    private boolean isTrivialParamList(List<Object> paramList) {
        int index = 1;
        for (Object param : paramList) {
            if (!(param instanceof Integer) || ((Integer)param) != index) return false;
            ++index;
        }
        return true;
    }

    public void addSettingsToProperty(LP<?> property, String name, List<String> namedParams, String groupName, boolean isPersistent, boolean isData) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addSettingsToProperty(" + property.property.getSID() + ", " + name + ", " + namedParams + ", " + groupName + ", " + isPersistent + ");");
        checkDuplicateProperty(name);
        changePropertyName(property, name);
        AbstractGroup group = (groupName == null ? null : findGroupByCompoundName(groupName));
        addPropertyToGroup(property.property, group);
        if (isData) {
            property.property.markStored(baseLM.tableFactory);
        } else if (isPersistent) {
            addPersistent(property);
        }
        checkPropertyValue(property, name);
        addNamedParams(property.property.getSID(), namedParams);
    }

    private <T extends LP<?>> void changePropertyName(T lp, String name) {
        removeModuleLP(lp);
        lp.property.setSID(transformNameToSID(name));
        lp.property.freezeSID();
        addModuleLP(lp);
    }

    public LPWithParams addScriptedJProp(String caption, LP<?> mainProp, List<LP<?>> paramProps, List<List<Integer>> usedParams) throws ScriptingErrorLog.SemanticErrorException {
        checkParamCount(mainProp, paramProps);
        List<Object> resultParams = getParamsPlainList(paramProps, usedParams);
        LP<?> prop;
        if (isTrivialParamList(resultParams)) {
            prop = mainProp;
        } else {
            scriptLogger.info("addScriptedJProp(" + mainProp.property.getSID() + ", " + resultParams + ");");
            prop = addJProp(caption, mainProp, resultParams.toArray());
        }
        return new LPWithParams(prop, mergeLists(usedParams));
    }

    private LP<?> getRelationProp(String op) {
        if (op.equals("==")) {
            return baseLM.equals2;
        } else if (op.equals("!=")) {
            return baseLM.diff2;
        } else if (op.equals(">")) {
            return baseLM.greater2;
        } else if (op.equals("<")) {
            return baseLM.less2;
        } else if (op.equals(">=")) {
            return baseLM.groeq2;
        } else if (op.equals("<=")) {
            return baseLM.lsoeq2;
        }
        assert false;
        return null;
    }

    private LP<?> getArithProp(String op) {
        if (op.equals("+")) {
            return baseLM.sumDouble2;
        } else if (op.equals("-")) {
            return baseLM.subtractDouble2;
        } else if (op.equals("*")) {
            return baseLM.multiplyDouble2;
        } else if (op.equals("/")) {
            return baseLM.divideDouble2;
        }
        assert false;
        return null;
    }

    public LPWithParams addScriptedEqualityProp(String caption, String op, LP<?> leftProp, List<Integer> lUsedParams,
                                                                           LP<?> rightProp, List<Integer> rUsedParams) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(caption, getRelationProp(op), Arrays.asList(leftProp, rightProp), Arrays.asList(lUsedParams, rUsedParams));
    }

    public LPWithParams addScriptedRelationalProp(String caption, String op, LP<?> leftProp, List<Integer> lUsedParams,
                                                                             LP<?> rightProp, List<Integer> rUsedParams) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(caption, getRelationProp(op), Arrays.asList(leftProp, rightProp), Arrays.asList(lUsedParams, rUsedParams));
    }

    public LPWithParams addScriptedAndProp(String caption, List<Boolean> nots, List<LP<?>> properties, List<List<Integer>> usedParams) throws ScriptingErrorLog.SemanticErrorException {
        assert properties.size() == usedParams.size();
        assert nots.size() + 1 == properties.size();

        LPWithParams curLP = new LPWithParams(properties.get(0), usedParams.get(0));
        if (nots.size() > 0) {
            boolean[] notsArray = new boolean[nots.size()];
            for (int i = 0; i < nots.size(); i++) {
                notsArray[i] = nots.get(i);
            }
            curLP = addScriptedJProp(caption, and(notsArray), properties, usedParams);
        }
        return curLP;
    }

    public LPWithParams addScriptedAdditiveProp(String caption, List<String> operands, List<LP<?>> properties, List<List<Integer>> usedParams) throws ScriptingErrorLog.SemanticErrorException {
        assert properties.size() == usedParams.size();
        assert operands.size() + 1 == properties.size();

        LPWithParams curLP = new LPWithParams(properties.get(0), usedParams.get(0));
        for (int i = 1; i < properties.size(); i++) {
            String op = operands.get(i-1);
            curLP = addScriptedJProp(caption, getArithProp(op), Arrays.asList(curLP.property, properties.get(i)), Arrays.asList(curLP.usedParams, usedParams.get(i)));
        }
        return curLP;
    }


    public LPWithParams addScriptedMultiplicativeProp(String caption, List<String> operands, List<LP<?>> properties, List<List<Integer>> usedParams) throws ScriptingErrorLog.SemanticErrorException {
        assert properties.size() == usedParams.size();
        assert operands.size() + 1 == properties.size();

        LPWithParams curLP = new LPWithParams(properties.get(0), usedParams.get(0));
        for (int i = 1; i < properties.size(); i++) {
            String op = operands.get(i-1);
            curLP = addScriptedJProp(caption, getArithProp(op), Arrays.asList(curLP.property, properties.get(i)), Arrays.asList(curLP.usedParams, usedParams.get(i)));
        }
        return curLP;
    }

    public LP<?> addScriptedUnaryMinusProp(String caption, LP<?> prop, List<Integer> usedParams) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp(caption, baseLM.minusDouble, Arrays.<LP<?>>asList(prop), Arrays.asList(usedParams)).property;
    }

    private List<Integer> mergeLists(List<List<Integer>> lists) {
        Set<Integer> s = new TreeSet<Integer>();
        for (List<Integer> list : lists) {
            s.addAll(list);
        }
        return new ArrayList<Integer>(s);
    }

    private List<Object> getParamsPlainList(List<LP<?>> paramProps, List<List<Integer>> usedParams) throws ScriptingErrorLog.SemanticErrorException {
        List<Integer> allUsedParams = mergeLists(usedParams);
        List<Object> resultParams = new ArrayList<Object>();

        for (int i = 0; i < paramProps.size(); i++) {
            LP<?> property = paramProps.get(i);
            if (property != null) {
                resultParams.add(property);
                for (int paramIndex : usedParams.get(i)) {
                    int localParamIndex = allUsedParams.indexOf(paramIndex);
                    assert localParamIndex >= 0;
                    resultParams.add(localParamIndex + 1);
                }
            } else {
                int localParamIndex = allUsedParams.indexOf(usedParams.get(i).get(0));
                assert localParamIndex >= 0;
                resultParams.add(localParamIndex + 1);
            }
        }
        return resultParams;
    }

    public LP<?> addScriptedGProp(String caption, boolean isSGProp, List<LP<?>> paramProps, List<List<Integer>> usedParams) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedGProp(" + isSGProp + ", " + paramProps + ", " + usedParams + ");");

        List<Object> resultParams = getParamsPlainList(paramProps, usedParams);
        int groupPropParamCount = mergeLists(usedParams).size();
        LP<?> resultProp = null;
        if (isSGProp) {
            resultProp = addSGProp(null, genSID(), false, false, caption, groupPropParamCount, resultParams.toArray());
        } else {
            //resultProp = addMGProp(caption, groupProp, resultParams.toArray());
        }
        return resultProp;
    }

    private List<Object> transformSumUnionParams(List<Object> params) {
        List<Object> newList = new ArrayList<Object>();
        for (Object obj : params) {
            if (obj instanceof LP) {
                newList.add(1);
            }
            newList.add(obj);
        }
        return newList;
    }

    public LP<?> addScriptedUProp(String caption, Union unionType, List<LP<?>> paramProps, List<List<Integer>> usedParams) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addScriptedUProp(" + unionType + ", " + paramProps + ", " + usedParams + ");");
        checkUnionPropertyParams(paramProps);
        List<Object> resultParams = getParamsPlainList(paramProps, usedParams);
        if (unionType == Union.SUM) {
            resultParams = transformSumUnionParams(resultParams);
        }
        return addUProp(null, caption, unionType, resultParams.toArray());
    }

    public LP<?> addConstantProp(ConstType type, String text) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addConstantProp(" + type + ", " + text + ");");

        switch (type) {
            case INT: return addCProp(IntegerClass.instance, Integer.parseInt(text));
            case REAL: return addCProp(DoubleClass.instance, Double.parseDouble(text));
            case STRING: text = transformStringLiteral(text); return addCProp(StringClass.get(text.length()), text);
            case LOGICAL: return addCProp(LogicalClass.instance, text.equals("TRUE"));
            case ENUM: return addStaticClassConst(text);
        }
        return null;
    }

    private LP<?> addStaticClassConst(String name) throws ScriptingErrorLog.SemanticErrorException {
        int pointPos = name.indexOf('.');
        assert pointPos > 0;
        assert name.indexOf('.') == name.lastIndexOf('.');

        String className = name.substring(0, pointPos);
        String instanceName = name.substring(pointPos+1);
        LP<?> resultProp = null;

        ValueClass cls = findClassByCompoundName(className);
        if (cls instanceof StaticCustomClass) {
            StaticCustomClass staticClass = (StaticCustomClass) cls;
            if (staticClass.hasSID(instanceName)) {
                resultProp = addCProp(staticClass, instanceName);
            } else {
                errLog.emitNotFoundError(parser, "static class instance", instanceName);
            }
        } else {
            errLog.emitNonStaticHasInstancesError(parser, className);
        }
        return resultProp;
    }

    public LP<?> addScriptedTypeProp(String className, boolean bIs) throws ScriptingErrorLog.SemanticErrorException {
        scriptLogger.info("addTypeProp(" + className + ", " + (bIs ? "IS" : "AS") + ");");
        if (bIs) {
            return is(findClassByCompoundName(className));
        } else {
            return object(findClassByCompoundName(className));
        }
    }

    public LP<?> addScriptedTypeExprProp(LP<?> mainProp, LP<?> property, List<Integer> usedParams) throws ScriptingErrorLog.SemanticErrorException {
        return addScriptedJProp("", mainProp, Arrays.<LP<?>>asList(property), Arrays.asList(usedParams)).property;
    }

    private void checkGroup(AbstractGroup group, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (group == null) {
            errLog.emitGroupNotFoundError(parser, name);
        }
    }

    private void checkClass(ValueClass cls, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (cls == null) {
            errLog.emitClassNotFoundError(parser, name);
        }
    }

    private void checkProperty(LP<?> lp, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (lp == null) {
            errLog.emitPropertyNotFoundError(parser, name);
        }
    }

    private void checkModule(LogicsModule module, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (module == null) {
            errLog.emitModuleNotFoundError(parser, name);
        }
    }

    private void checkParamCount(LP<?> mainProp, List<LP<?>> paramProps) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.property.interfaces.size() != paramProps.size()) {
            errLog.emitParamCountError(parser, mainProp, paramProps.size());
        }
    }

    private void checkPropertyValue(LP<?> property, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (!property.property.check()) {
            errLog.emitPropertyAlwaysNullError(parser, name);
        }
    }

    private void checkDuplicateClass(String className) throws ScriptingErrorLog.SemanticErrorException {
        if (getClassByName(className) != null) {
            errLog.emitAlreadyDefinedError(parser, "class", className);
        }
    }

    private void checkDuplicateGroup(String groupName) throws ScriptingErrorLog.SemanticErrorException {
        if (getGroupByName(groupName) != null) {
            errLog.emitAlreadyDefinedError(parser, "group", groupName);
        }
    }

    private void checkDuplicateProperty(String propName) throws ScriptingErrorLog.SemanticErrorException {
        if (getLPByName(propName) != null) {
            errLog.emitAlreadyDefinedError(parser, "property", propName);
        }
    }

    private void checkUnionPropertyParams(List<LP<?>> uPropParams) throws ScriptingErrorLog.SemanticErrorException {
        int paramCnt = uPropParams.get(0).property.interfaces.size();
        for (LP<?> lp : uPropParams) {
            if (lp.property.interfaces.size() != paramCnt) {
                errLog.emitUnionPropParamsError(parser);
            }
        }
    }

    private void checkStaticClassConstraints(String className, boolean isStatic, boolean isAbstract, List<String> instNames, List<String> instCaptions) throws ScriptingErrorLog.SemanticErrorException {
        assert instCaptions.size() == instNames.size();
        if (isStatic && isAbstract) {
            errLog.emitAbstractStaticClassError(parser);
        } else if (!isStatic && instNames.size() > 0) {
            errLog.emitNonStaticHasInstancesError(parser, className);
        } else if (isStatic && instNames.size() == 0) {
            errLog.emitStaticHasNoInstancesError(parser, className);
        } else if (isStatic) {
            Set<String> names = new HashSet<String>();
            for (String name : instNames) {
                if (names.contains(name)) {
                    errLog.emitAlreadyDefinedError(parser, "instance", name);
                }
                names.add(name);
            }
        }
    }

    private void checkClassParents(List<String> parents) throws ScriptingErrorLog.SemanticErrorException {
        for (String parentName : parents) {
            ValueClass valueClass = findClassByCompoundName(parentName);
            if (!(valueClass instanceof CustomClass)) {
                errLog.emitBuiltInClassAsParentError(parser, parentName);
            }
            if (valueClass instanceof StaticCustomClass) {
                errLog.emitStaticClassAsParentError(parser, parentName);
            }
        }
    }

    private void parseStep(State state) {
        try {
            LsfLogicsLexer lexer = new LsfLogicsLexer(createStream());
            parser = new LsfLogicsParser(new CommonTokenStream(lexer));

            parser.self = this;
            parser.parseState = state;

            lexer.self = this;
            lexer.parseState = state;

            parser.script();
//            arithLexer lexer = new arithLexer(createStream());
//            arithParser parser = new arithParser(new CommonTokenStream(lexer));
//            parser.program();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initClasses() {
        parseStep(ScriptingLogicsModule.State.CLASS);
    }

    @Override
    public void initTables() {
    }

    @Override
    public void initGroups() {
        parseStep(ScriptingLogicsModule.State.GROUP);
    }

    @Override
    public void initProperties()  {
        parseStep(ScriptingLogicsModule.State.PROP);
    }

    @Override
    public void initIndexes() {
    }

    @Override
    public void initNavigators() throws JRException, FileNotFoundException {
        parseStep(ScriptingLogicsModule.State.NAVIGATOR);
    }

    @Override
    public String getErrorsDescription() {
        return errLog.toString();
    }

    @Override
    public String getNamePrefix() {
        return scriptName;
    }
}
