grammar LsfLogics;

@header {
	package platform.server;

	import platform.interop.ClassViewType;
	import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
	import platform.server.data.Union;
	import platform.server.data.expr.query.PartitionType;
	import platform.server.form.entity.GroupObjectEntity;
	import platform.server.form.entity.PropertyObjectEntity;
	import platform.server.form.view.ComponentView;
	import platform.server.form.view.GroupObjectView;
	import platform.server.form.view.PropertyDrawView;
	import platform.server.logics.linear.LP;
	import platform.server.logics.property.PropertyFollows;
	import platform.server.logics.scripted.*;
	import platform.server.logics.scripted.ScriptingFormEntity.MappedProperty;
	import platform.server.logics.scripted.ScriptedFormView.InsertPosition;

	import java.awt.*;
	import java.util.ArrayList;
	import java.util.Collections;
	import java.util.List;
	import java.util.Stack;

	import static platform.interop.form.layout.SingleSimplexConstraint.*;
}

@lexer::header { 
	package platform.server; 
	import platform.server.logics.scripted.ScriptingLogicsModule;
}

@lexer::members {
	public ScriptingLogicsModule self;
	public ScriptingLogicsModule.State parseState;
	
	@Override
	public void emitErrorMessage(String msg) {
		if (parseState == ScriptingLogicsModule.State.GROUP) { 
			self.getErrLog().write(msg + "\n");
		}
	}
	
	@Override
	public String getErrorMessage(RecognitionException e, String[] tokenNames) {
		return self.getErrLog().getErrorMessage(this, super.getErrorMessage(e, tokenNames), e);
	}
	
	@Override
	public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
		self.getErrLog().displayRecognitionError(this, "error", tokenNames, e);
	}
}

@members {
	public ScriptingLogicsModule self;
	public ScriptingLogicsModule.State parseState;

	public boolean inParseState(ScriptingLogicsModule.State parseState) {
		return this.parseState == parseState;
	}

	public boolean inGroupParseState() {
		return inParseState(ScriptingLogicsModule.State.GROUP);
	}

	public boolean inClassParseState() {
		return inParseState(ScriptingLogicsModule.State.CLASS);
	}

	public boolean inPropParseState() {
		return inParseState(ScriptingLogicsModule.State.PROP);
	}

	public void setObjectProperty(Object propertyReceiver, String propertyName, Object propertyValue) throws ScriptingErrorLog.SemanticErrorException {
		if (inPropParseState()) {
			$designStatement::design.setObjectProperty(propertyReceiver, propertyName, propertyValue);
		}
    }

	public List<GroupObjectEntity> getGroupObjectsList(List<String> ids) throws ScriptingErrorLog.SemanticErrorException {
		if (inPropParseState()) {
			return $formStatement::form.getGroupObjectsList(ids);
		}
		return null;
	}

	public MappedProperty getPropertyWithMapping(String name, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
		if (inPropParseState()) {
			return $formStatement::form.getPropertyWithMapping(name, mapping);
		}
		return null;
	}

	@Override
	public void emitErrorMessage(String msg) {
		if (parseState == ScriptingLogicsModule.State.GROUP) { 
			self.getErrLog().write(msg + "\n");
		}
	}

	@Override
	public String getErrorMessage(RecognitionException e, String[] tokenNames) {
		return self.getErrLog().getErrorMessage(this, super.getErrorMessage(e, tokenNames), e);
	}

	@Override
	public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
		self.getErrLog().displayRecognitionError(this, "error", tokenNames, e);
	}
}

@rulecatch {
	catch(RecognitionException re) {
		if (re instanceof ScriptingErrorLog.SemanticErrorException) {
			throw re;
		} else {
			reportError(re);
			recover(input,re);
		}
	}
}

script	
	:	moduleHeader importDirective* statement* EOF
	;


moduleHeader
@after {
	if (inGroupParseState()) {
		self.setModuleName($name.text);
	}
}
	:	'MODULE' name=ID ';'
	;

importDirective
@after {
	if (inGroupParseState()) {
		self.addImportedModule($moduleName.text);
	}
}
	:	'IMPORT' moduleName=ID ';'
	;


statement
	:	(	classStatement
		|	groupStatement
		|	propertyStatement
		|	constraintStatement
		|	followsStatement
		|	writeOnChangeStatement
		|	tableStatement
		|	indexStatement
		|	formStatement
		|	designStatement
		|	emptyStatement
		)	
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// CLASS STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////


classStatement 
@init {
	List<String> classParents = new ArrayList<String>();
	boolean isAbstract = false;
	boolean isStatic = false;
	List<String> instanceNames = new ArrayList<String>();
	List<String> instanceCaptions = new ArrayList<String>();
}
@after {
	if (inClassParseState()) {
		self.addScriptedClass($nameCaption.name, $nameCaption.caption, isAbstract, isStatic, instanceNames, instanceCaptions, classParents);
	}
}
	:	'CLASS' ('ABSTRACT' {isAbstract = true;} | 'STATIC' {isStatic = true;})?
		nameCaption=simpleNameWithCaption 
		(
			'{'
				firstInstData=simpleNameWithCaption { instanceNames.add($firstInstData.name); instanceCaptions.add($firstInstData.caption); }
				(',' nextInstData=simpleNameWithCaption { instanceNames.add($nextInstData.name); instanceCaptions.add($nextInstData.caption); })*
			'}'
			(parents=classParentsList ';' { classParents = $parents.list; })?	
		|	
			(parents=classParentsList { classParents = $parents.list; })? ';'
		)
	;	  

classParentsList returns [List<String> list] 
	:	':' parentList=nonEmptyCompoundIdList { $list = $parentList.ids; }
	; 

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// GROUP STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////
groupStatement
@init {
	String parent = null;
}
@after {
	if (inGroupParseState()) {
		self.addScriptedGroup($groupNameCaption.name, $groupNameCaption.caption, parent);
	}
}
	:	'GROUP' groupNameCaption=simpleNameWithCaption 
		(':' parentName=compoundID { parent = $parentName.text; })?
		';'
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////// FORM STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

formStatement
scope {
	ScriptingFormEntity form;
}
@after {
	if (inPropParseState()) {
		self.addScriptedForm($formStatement::form);
	}
}
	:	declaration=formDeclaration { $formStatement::form = $declaration.form; }
		(	formGroupObjectsList 
		|	formFiltersList
		| 	formPropertiesList
		|	filterGroupDeclaration
		|	formOrderByList
		)*
		';'
	;


formDeclaration returns [ScriptingFormEntity form]
@after {
	if (inPropParseState()) {
		$form = self.createScriptedForm($formNameCaption.name, $formNameCaption.caption);
	}
}
	:	'FORM' 
		formNameCaption=simpleNameWithCaption 
	;


formGroupObjectsList // needs refactoring
@init {
	List<List<String>> names = new ArrayList<List<String>>();
	List<List<String>> classNames = new ArrayList<List<String>>(); 
	List<ClassViewType> groupViewType = new ArrayList<ClassViewType>();
	List<Boolean> isInitType = new ArrayList<Boolean>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedGroupObjects(names, classNames, groupViewType, isInitType);
	}
}
	:	'OBJECTS'
	    groupElement=formGroupObjectDeclaration { names.add($groupElement.objectNames); classNames.add($groupElement.classIds); 
												  groupViewType.add($groupElement.type); isInitType.add($groupElement.isInitType); }
		(',' groupElement=formGroupObjectDeclaration { names.add($groupElement.objectNames); classNames.add($groupElement.classIds); 
													   groupViewType.add($groupElement.type); isInitType.add($groupElement.isInitType); })*
	;


formGroupObjectDeclaration returns [List<String> objectNames, List<String> classIds, ClassViewType type, boolean isInitType]
@init {
	$objectNames = new ArrayList<String>();
	$classIds = new ArrayList<String>();
}
	:	(decl=formSingleGroupObjectDeclaration { $objectNames.add($decl.name); $classIds.add($decl.className); }
	|	('(' 
		objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classIds.add($objDecl.className); }	
		(',' objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classIds.add($objDecl.className); })+	
		')'))
		(viewType=formGroupObjectViewType { $type = $viewType.type; $isInitType = $viewType.isInitType; })?
	; 


formGroupObjectViewType returns [ClassViewType type, boolean isInitType]
	: 	('INIT' {$isInitType = true;} | 'FIXED' {$isInitType = false;})
	  	('PANEL' {$type = ClassViewType.PANEL;} | 'HIDE' {$type = ClassViewType.HIDE;} | 'GRID' {$type = ClassViewType.GRID;})
	;


formSingleGroupObjectDeclaration returns [String name, String className] 
	:	foDecl=formObjectDeclaration { $name = $foDecl.name; $className = $foDecl.className; }
	;


formObjectDeclaration returns [String name, String className] 
	:	(objectName=ID { $name = $objectName.text; } '=')?	
		id=classId { $className = $id.text; }
	; 
	
	
formPropertiesList
@init {
	List<String> properties = new ArrayList<String>();
	List<List<String>> mapping = new ArrayList<List<String>>();
	FormPropertyOptions commonOptions = null;
	List<FormPropertyOptions> options = new ArrayList<FormPropertyOptions>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedPropertyDraws(properties, mapping, commonOptions, options);
	}
}
	: 'PROPERTIES' '(' objects=idList ')' opts=formPropertyOptionsList list=formPropertiesNamesList
		{
			commonOptions = $opts.options;
			properties = $list.properties;
			mapping = Collections.nCopies(properties.size(), $objects.ids);
			options = $list.options;
		}
	| 'PROPERTIES' opts=formPropertyOptionsList mappedList=formMappedPropertiesList
		{
			commonOptions = $opts.options;
			properties = $mappedList.properties;
			mapping = $mappedList.mapping;
			options = $mappedList.options;
		}
	;	

formPropertyOptionsList returns [FormPropertyOptions options]
@init {
	$options = new FormPropertyOptions();
}
	:
	(	'READONLY' { $options.setReadOnly(true); }
	|	'EDITABLE' { $options.setReadOnly(false); }
	|	'COLUMNS' '(' ids=nonEmptyIdList ')' { $options.setColumns(getGroupObjectsList($ids.ids)); }
	|	'SHOWIF' mappedProp=formMappedProperty { $options.setShowIf(getPropertyWithMapping($mappedProp.name, $mappedProp.mapping)); }
	|	'HIGHLIGHTIF' propObj=formPropertyObject { $options.setHighlightIf($propObj.property); }
	|	'HEADER' propObj=formPropertyObject { $options.setHeader($propObj.property); }
	|	'FOOTER' propObj=formPropertyObject { $options.setFooter($propObj.property); }
	)*
	;


formMappedPropertiesList returns [List<String> properties, List<List<String>> mapping, List<FormPropertyOptions> options]
@init {
	$properties = new ArrayList<String>();
	$mapping = new ArrayList<List<String>>();
	$options = new ArrayList<FormPropertyOptions>();
}
	:	mappedProp=formMappedProperty opts=formPropertyOptionsList { $properties.add($mappedProp.name); $mapping.add($mappedProp.mapping); $options.add($opts.options); }
		(',' mappedProp=formMappedProperty opts=formPropertyOptionsList { $properties.add($mappedProp.name); $mapping.add($mappedProp.mapping); $options.add($opts.options); })*
	;

formPropertyObject returns [PropertyObjectEntity property = null]
	: mappedProperty=formMappedProperty
		{
			if (inPropParseState()) {
				$property = $formStatement::form.addPropertyObject(mappedProperty.name, mappedProperty.mapping);
			}
		}
	;

formMappedProperty returns [String name, List<String> mapping]
	:   pname=formPropertyName { $name = $pname.text; }
	    '('
		    objects=idList { $mapping = $objects.ids; }
	    ')'
	;


formPropertiesNamesList returns [List<String> properties, List<FormPropertyOptions> options]
@init {
	$properties = new ArrayList<String>();
	$options = new ArrayList<FormPropertyOptions>();
}
	:	pname=formPropertyName opts=formPropertyOptionsList { $properties.add($pname.text); $options.add($opts.options); }
		(
			',' pname=formPropertyName opts=formPropertyOptionsList { $properties.add($pname.text); $options.add($opts.options); }
		)*
	;


formPropertyName 
	: compoundID | ('OBJVALUE') | ('SELECTION') | ('ADDOBJ')
	;


formFiltersList
@init {
	List<String> propertyNames = new ArrayList<String>();
	List<List<String>> propertyMappings = new ArrayList<List<String>>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedFilters(propertyNames, propertyMappings);
	}
}
	:   'FILTERS'
	    decl=formFilterDeclaration { propertyNames.add($decl.name); propertyMappings.add($decl.mapping);}
	    (',' decl=formFilterDeclaration { propertyNames.add($decl.name); propertyMappings.add($decl.mapping);})*
	;
	
filterGroupDeclaration
@init {
	String filterGroupSID = null;
	List<String> captions = new ArrayList<String>();
	List<String> keystrokes = new ArrayList<String>();
	List<String> properties = new ArrayList<String>();
	List<List<String>> mappings = new ArrayList<List<String>>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedRegularFilterGroup(filterGroupSID, captions, keystrokes, properties, mappings);
	}
}
	:	'FILTERGROUP' sid=ID { filterGroupSID = $sid.text; }
		(
			'FILTER' caption=stringLiteral keystroke=stringLiteral filter=formFilterDeclaration
			{
				captions.add($caption.val);
				keystrokes.add($keystroke.val);
				properties.add($filter.name);
				mappings.add($filter.mapping);
			}
		)+
	;

	
formFilterDeclaration returns [String name, List<String> mapping] 
	: 'NOT' 'NULL' propDecl=formMappedProperty { $name = $propDecl.name; $mapping = $propDecl.mapping; }
	;
	

formOrderByList
@init {
	boolean ascending = true;
	List<String> properties = new ArrayList<String>();
	List<Boolean> orders = new ArrayList<Boolean>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedDefaultOrder(properties, orders);
	}
}
	: 'ORDER' 'BY' orderedProp=formPropertyWithOrder { properties.add($orderedProp.id); orders.add($orderedProp.order); }
		(',' orderedProp=formPropertyWithOrder { properties.add($orderedProp.id); orders.add($orderedProp.order); } )*
	;
	
formPropertyWithOrder returns [String id, boolean order = true]
	:	ID { $id = $ID.text; } ('ASC' | 'DESC' { $order = false; })?
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// PROPERTY STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

propertyStatement
@init {
	LP<?> property = null;
	boolean isData = false;
	List<String> context = new ArrayList<String>();
	boolean dynamic = true;
}
	:	declaration=propertyDeclaration { if ($declaration.paramNames != null) { context = $declaration.paramNames; dynamic = false; }}
		'=' 
		(	def=contextIndependentPD[false] { property = $def.property; isData = $def.isData; }  
		|	expr=propertyExpression[context, dynamic] { property = $expr.property; }
		)
		settings=commonPropertySettings[property, $declaration.name, $declaration.caption, context, isData]
		';'
	;


propertyDeclaration returns [String name, String caption, List<String> paramNames]
	:	propNameCaption=simpleNameWithCaption { $name = $propNameCaption.name; $caption = $propNameCaption.caption; }
		('(' paramList=idList ')' { $paramNames = $paramList.ids; })? 
	;


propertyExpression[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
	:	pe=andPE[context, dynamic] { $property = $pe.property; $usedParams = $pe.usedParams; }
	;


andPE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	List<LP<?>> props = new ArrayList<LP<?>>();
	List<List<Integer>> allUsedParams = new ArrayList<List<Integer>>();
	List<Boolean> nots = new ArrayList<Boolean>();
}
@after {
	if (inPropParseState()) {
		ScriptingLogicsModule.LPWithParams result = self.addScriptedAndProp(nots, props, allUsedParams);				
		$property = result.property;
		$usedParams = result.usedParams;
	}
}
	:	firstExpr=equalityPE[context, dynamic] { props.add($firstExpr.property); allUsedParams.add($firstExpr.usedParams); }
		((('AND') | ('IF')) { nots.add(false); }
		('NOT' { nots.set(nots.size()-1, true); })?
		nextExpr=equalityPE[context, dynamic] { props.add($nextExpr.property); allUsedParams.add($nextExpr.usedParams); })*
	;
		

equalityPE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	LP<?> leftProp = null;
	LP<?> rightProp = null;
	List<Integer> lUsedParams = null, rUsedParams = null;
	String op = null;
}
@after {
	if (inPropParseState() && op != null) {
		ScriptingLogicsModule.LPWithParams result =
			self.addScriptedEqualityProp(op, leftProp, lUsedParams, rightProp, rUsedParams);
		$property = result.property;
		$usedParams = result.usedParams;
	} else {
		$property = leftProp;
		$usedParams = lUsedParams;
	}
}
	:	lhs=relationalPE[context, dynamic] { leftProp = $lhs.property; lUsedParams = $lhs.usedParams; }
		(operand=EQ_OPERAND { op = $operand.text; }
		rhs=relationalPE[context, dynamic] { rightProp = $rhs.property; rUsedParams = $rhs.usedParams; })?
	;


relationalPE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	LP<?> leftProp = null;
	LP<?> rightProp = null;
	LP<?> mainProp = null;
	List<Integer> lUsedParams = null, rUsedParams = null;
	String op = null;
}
@after {
	if (inPropParseState())
	{
		if (op != null) {
			ScriptingLogicsModule.LPWithParams result =
				self.addScriptedRelationalProp(op, leftProp, lUsedParams, rightProp, rUsedParams);
			$property = result.property;
			$usedParams = result.usedParams;
		} else if (mainProp != null) {
			$property = self.addScriptedTypeExprProp(mainProp, leftProp, lUsedParams);
			$usedParams = lUsedParams;
		} else {
			$property = leftProp;
			$usedParams = lUsedParams;
		}
	}	
}
	:	lhs=additivePE[context, dynamic] { leftProp = $lhs.property; lUsedParams = $lhs.usedParams; }
		(
			(operand=REL_OPERAND { op = $operand.text; }
			rhs=additivePE[context, dynamic] { rightProp = $rhs.property; rUsedParams = $rhs.usedParams; }) 
		|	def=typePropertyDefinition { mainProp = $def.property; }
		)?
	;


additivePE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	List<LP<?>> props = new ArrayList<LP<?>>();
	List<List<Integer>> allUsedParams = new ArrayList<List<Integer>>();
	List<String> ops = new ArrayList<String>();
}
@after {
	if (inPropParseState()) {
		ScriptingLogicsModule.LPWithParams result = self.addScriptedAdditiveProp(ops, props, allUsedParams);				
		$property = result.property;
		$usedParams = result.usedParams;
	}
}
	:	firstExpr=multiplicativePE[context, dynamic] { props.add($firstExpr.property); allUsedParams.add($firstExpr.usedParams); }
		( (operand=PLUS | operand=MINUS) { ops.add($operand.text); }
		nextExpr=multiplicativePE[context, dynamic] { props.add($nextExpr.property); allUsedParams.add($nextExpr.usedParams); })*
	;
		
	
multiplicativePE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	List<LP<?>> props = new ArrayList<LP<?>>();
	List<List<Integer>> allUsedParams = new ArrayList<List<Integer>>();
	List<String> ops = new ArrayList<String>();
}
@after {
	if (inPropParseState()) {
		ScriptingLogicsModule.LPWithParams result = self.addScriptedMultiplicativeProp(ops, props, allUsedParams);				
		$property = result.property;
		$usedParams = result.usedParams;
	}
}
	:	firstExpr=simplePE[context, dynamic] { props.add($firstExpr.property); allUsedParams.add($firstExpr.usedParams); }
		(operand=MULT_OPERAND { ops.add($operand.text); }
		nextExpr=simplePE[context, dynamic] { props.add($nextExpr.property); allUsedParams.add($nextExpr.usedParams); })*
	;
	
		 
	 
simplePE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
	:	'(' expr=propertyExpression[context, dynamic] ')' { $property = $expr.property; $usedParams = $expr.usedParams; } 
	|	primitive=expressionPrimitive[context, dynamic] { $property = $primitive.property; $usedParams = $primitive.usedParams; } 
	|	uexpr=unaryMinusPE[context, dynamic] { $property = $uexpr.property; $usedParams = $uexpr.usedParams; }
	;

	
unaryMinusPE[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams] 	
@after {
	if (inPropParseState()) {
		$property = self.addScriptedUnaryMinusProp($property, $usedParams);
	}
}
	:	MINUS expr=simplePE[context, dynamic] { $property = $expr.property; $usedParams = $expr.usedParams; }
	;		 
	

expressionPrimitive[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
	:	(paramName=parameter {
			if (inPropParseState())
				$usedParams = Collections.singletonList(self.getParamIndex($paramName.text, $context, $dynamic));
		 })
	|	(expr=contextDependentPD[context, dynamic] { $property = $expr.property; $usedParams = $expr.usedParams; })
	;

propertyDefinition[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
	:	propertyExpr=contextDependentPD[context, dynamic] { $property = $propertyExpr.property; $usedParams = $propertyExpr.usedParams; } 
	|	propertyExprI=contextIndependentPD[true] { $property = $propertyExprI.property; $usedParams = new ArrayList<Integer>(); }
	;

contextDependentPD[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
	:	joinDef=joinPropertyDefinition[context, dynamic] { $property = $joinDef.property; $usedParams = $joinDef.usedParams; } 
	|	unionDef=unionPropertyDefinition[context, dynamic] { $property = $unionDef.property; $usedParams = $unionDef.usedParams; } 
	|	partDef=partitionPropertyDefinition[context, dynamic] { $property = $partDef.property; $usedParams = $partDef.usedParams; }
	|	constDef=literal { $property = $constDef.property; $usedParams = new ArrayList<Integer>(); }
	;

contextIndependentPD[boolean innerPD] returns [LP property, boolean isData]
	: 	dataDef=dataPropertyDefinition[innerPD] { $property = $dataDef.property; $isData = true; } 
	|	formulaProp=formulaPropertyDefinition { $property = $formulaProp.property; $isData = false; }
	|	groupDef=groupPropertyDefinition { $property = $groupDef.property; $isData = false; } 
	|	typeDef=typePropertyDefinition { $property = $typeDef.property; $isData = false; }
	;

joinPropertyDefinition[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@after {
	if (inPropParseState()) {
		ScriptingLogicsModule.LPWithParams result = self.addScriptedJProp($mainPropObj.property, $exprList.props, $exprList.usedParams);
		$property = result.property;
		$usedParams = result.usedParams;
	}
}
	:	mainPropObj=propertyObject 
		'('
		exprList=propertyExpressionList[context, dynamic] 
		')'
	;




groupPropertyDefinition returns [LP property]
@init {
	List<LP<?>> paramProps = new ArrayList<LP<?>>();
	List<List<Integer>> usedParams = new ArrayList<List<Integer>>();
	boolean isSGProp = true;
	List<String> groupContext = new ArrayList<String>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedGProp(isSGProp, paramProps, usedParams);
	}
}
	:	'GROUP' (('SUM') { isSGProp = true; } | ('MAX') { isSGProp = false; })
		prop=propertyExpression[groupContext, true] { paramProps.add($prop.property); usedParams.add($prop.usedParams); }
		'BY'
		exprList=nonEmptyPropertyExpressionList[groupContext, true] 
		{ paramProps.addAll($exprList.props); usedParams.addAll($exprList.usedParams); }
	;



partitionPropertyDefinition[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	List<List<Integer>> usedSubParams = new ArrayList<List<Integer>>();
	List<LP<?>> paramProps = new ArrayList<LP<?>>();
	PartitionType type = null;
	int groupExprCnt;
	boolean ascending = true;
	boolean useLast = true;
}
@after {
	if (inPropParseState()) {
		ScriptingLogicsModule.LPWithParams result = 
			self.addScriptedOProp(type, ascending, useLast, groupExprCnt, paramProps, usedSubParams);
		$property = result.property;
		$usedParams = result.usedParams;	
	}
}
	:	'PARTITION' ('SUM' {type = PartitionType.SUM;} | 'PREV' {type = PartitionType.PREVIOUS;})
		expr=propertyExpression[context, dynamic] { paramProps.add($expr.property); usedSubParams.add($expr.usedParams); }
		(	'BY'
			exprList=nonEmptyPropertyExpressionList[context, dynamic] { paramProps.addAll($exprList.props); usedSubParams.addAll($exprList.usedParams); }
		)?
		{ groupExprCnt = paramProps.size() - 1; }
		(	'ORDER' ('DESC' { ascending = false; } )?				
			orderList=nonEmptyPropertyExpressionList[context, dynamic] { paramProps.addAll($orderList.props); usedSubParams.addAll($orderList.usedParams); }
		)? 
		('WINDOW' 'EXCEPTLAST' { useLast = false; })?
	;


dataPropertyDefinition[boolean innerPD] returns [LP property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedDProp($returnClass.text, $paramClassNames.ids, innerPD);
	}
}
	:	'DATA'
		returnClass=classId 
		'('
			paramClassNames=classIdList 
		')'
	;



unionPropertyDefinition[List<String> context, boolean dynamic] returns [LP property, List<Integer> usedParams]
@init {
	Union type = null;
}
@after {
	if (inPropParseState()) {
		ScriptingLogicsModule.LPWithParams result = self.addScriptedUProp(type, $exprList.props, $exprList.usedParams);
		$property = result.property;
		$usedParams = result.usedParams;	
	}
}
	:	'UNION'
		(('MAX' {type = Union.MAX;}) | ('SUM' {type = Union.SUM;}) | ('OVERRIDE' {type = Union.OVERRIDE;}) | ('XOR' { type = Union.XOR;}) | ('EXCLUSIVE' {type = Union.EXCLUSIVE;}))
		exprList=nonEmptyPropertyExpressionList[context, dynamic] 
	;


formulaPropertyDefinition returns [LP property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedSFProp($className.text, $formulaText.text);
	}
}
	:	'FORMULA' className=classId formulaText=STRING_LITERAL 
	;


typePropertyDefinition returns [LP property] 
@init {
	boolean bIs = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedTypeProp($clsId.text, bIs);
	}	
}
	:	('IS' { bIs = true; } | 'AS')
		clsId=classId 
	;


propertyObject returns [LP property, String propName, List<String> innerContext]
@init {
	List<String> newContext = new ArrayList<String>(); 
}
	:	name=compoundID	{ if (inPropParseState())
							{$property = self.findLPByCompoundName($name.text); $propName = $name.text;}
						}
	|	'[' 
			(expr=propertyExpression[newContext, true] { $property = $expr.property; $innerContext = newContext; } 
		|	def=contextIndependentPD[true] { $property = $def.property; })
		']' 
	;


commonPropertySettings[LP property, String propertyName, String caption, List<String> namedParams, boolean isData] 
@init {
	String groupName = null;
	boolean isPersistent = false;	
}
@after {
	if (inPropParseState()) {
		self.addSettingsToProperty(property, propertyName, caption, namedParams, groupName, isPersistent, isData);	
	}
} 
	: 	('IN' name=compoundID { groupName = $name.text; })?
		('PERSISTENT' { isPersistent = true; })?
	;


////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// CONSTRAINT STATEMENT //////////////////////////
////////////////////////////////////////////////////////////////////////////////

constraintStatement 
@init {
	boolean checked = false;
}
@after {
	if (inPropParseState()) { 
		self.addScriptedConstraint($expr.property, checked, $message.text);	
	}
}
	:	'CONSTRAINT' ('CHECKED' { checked = true; })? 
		expr=propertyExpression[new ArrayList<String>(), true] 	
		'MSG' message=STRING_LITERAL 
		';'
	;


////////////////////////////////////////////////////////////////////////////////
///////////////////////////////// FOLLOWS STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

followsStatement
@init {
	List<String> context;
	String mainProp;
	List<List<Integer>> usedParams = new ArrayList<List<Integer>>();
	List<LP<?>> props = new ArrayList<LP<?>>();
	List<Integer> options = new ArrayList<Integer>();
}
@after {
	if (inPropParseState()) {
		self.addScriptedFollows(mainProp, context.size(), options, props, usedParams);
	}
}
	:	prop=propertyWithNamedParams { mainProp = $prop.name; context = $prop.params; }
		'=>'
		firstExpr=propertyExpression[context, false] ('RESOLVE' type=followsResolveType)?
		{
			props.add($firstExpr.property); usedParams.add($firstExpr.usedParams);
			options.add(type == null ? PropertyFollows.RESOLVE_ALL : $type.type);
		}
		(',' nextExpr=propertyExpression[context, false] ('RESOLVE' type=followsResolveType)?
			{
		     	props.add($nextExpr.property); usedParams.add($nextExpr.usedParams);
		     	options.add(type == null ? PropertyFollows.RESOLVE_ALL : $type.type);
			}
		)*
		';'
	;

followsResolveType returns [Integer type]
	:	lit=LOGICAL_LITERAL	{ $type = $lit.text.equals("TRUE") ? PropertyFollows.RESOLVE_TRUE : PropertyFollows.RESOLVE_FALSE; }
	|	'ALL'			{ $type = PropertyFollows.RESOLVE_ALL; }
	|	'NOTHING'		{ $type = PropertyFollows.RESOLVE_NOTHING; }
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// CHANGE STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

writeOnChangeStatement
@init {
	boolean old = false;
	boolean anyChange = true;
	List<String> context;
}
@after {
	if (inPropParseState()) { 
		self.addScriptedWriteOnChange($mainProp.name, context.size(), old, anyChange, $valueExpr.property, $valueExpr.usedParams, $changeExpr.property, $changeExpr.usedParams);
	}
}
	:	mainProp=propertyWithNamedParams { context = $mainProp.params; }
		'<-'
		('OLD' { old = true; })?
		valueExpr=propertyExpression[context, false] 
		'ON' ('CHANGE' | 'ASSIGN' { anyChange = false; }) 
		changeExpr=propertyExpression[context, false] 
		';'
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// TABLE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

tableStatement 
	:	'T' ';';


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// INDEX STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

indexStatement
	:	'I' ';';


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// DESIGN STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

designStatement returns [ScriptedFormView formView]
scope {
	ScriptedFormView design;
}
@init {
	boolean applyDefault = false;
}
@after {
}
	: 	'DESIGN'
		nameWithCaption=simpleNameWithCaption
		('FROM' 'DEFAULT' {applyDefault = true;} )?
		{
			if (inPropParseState()) {
				$designStatement::design = $formView = self.createScriptedFormView($nameWithCaption.name, $nameWithCaption.caption, applyDefault);
			}
		}
		componentStatementBody[formView, formView == null ? null : formView.mainContainer]
	;

componentStatementBody [Object propertyReceiver, ComponentView parentComponent]
	: '{'
		( setObjectPropertyStatement[propertyReceiver]
		| positionComponentsStatement[parentComponent]
		| setupComponentStatement
		| setupGroupObjectStatement
		| addComponentStatement[parentComponent]
		| removeComponentStatement
		| emptyStatement )*
	  '}'
	| emptyStatement
	;

emptyStatement
	: ';'	
	;

setupComponentStatement
	: comp=componentSelector[true] componentStatementBody[$comp.component, $comp.component]
	;

setupGroupObjectStatement
@init {
	GroupObjectView groupObject = null;
}
	: 'GROUP'
	  '('
		ID
		{
			if (inPropParseState()) {
				groupObject = $designStatement::design.getGroupObject($ID.text);
			}
		}
	  ')'	'{'
                ( setObjectPropertyStatement[groupObject]
                | emptyStatement
                )*
            '}'
	;

addComponentStatement[ComponentView parentComponent]
@init {
	boolean hasPosition = false;
	ComponentView insComp = null;
}
	: 'ADD' insSelector=componentSelector[false] { insComp = $insSelector.component; }
		( posDefinition=addPositionDefinition posSelector=componentSelector[true] { hasPosition = true; } )?
		{
			if (inPropParseState()) {
				insComp = $designStatement::design.addComponent($insSelector.sid,
																insComp,
																hasPosition ? $posDefinition.position : InsertPosition.IN,
																hasPosition ? $posSelector.component : $parentComponent);
			}
		}
   	    componentStatementBody[insComp, insComp]
	;

removeComponentStatement
@init {
	boolean cascade = false;
}
	: 'REMOVE' compSelector=componentSelector[true] ('CASCADE' { cascade = true; } )? ';'
		{
			if (inPropParseState()) {
				$designStatement::design.removeComponent($compSelector.component, cascade);
			}
		}
	;

componentSelector[boolean hasToExist] returns [String sid, ComponentView component]
	: 'PARENT' '(' child=componentSelector[true] ')'
		{
			if (inPropParseState()) {
				$designStatement::design.getParentContainer($child.component);
			}
		}
	| 'PROPERTY' '(' prop=propertySelector ')' { $component = $prop.propertyView; }
	| mid=multiCompoundID
		{
			if (inPropParseState()) {
				$sid = $mid.sid;
				$component = $designStatement::design.getComponentBySID($sid, hasToExist);
			}
		}
	;


propertySelector returns [PropertyDrawView propertyView = null]
	: pname=formPropertyName
		{
			if (inPropParseState()) {
				$propertyView = $designStatement::design.getPropertyView($pname.text);
			}
		}
	| mappedProp=formMappedProperty
		{
			if (inPropParseState()) {
				$propertyView = $designStatement::design.getPropertyView($mappedProp.name, $mappedProp.mapping);
			}
		}
	;

positionComponentsStatement[ComponentView parentComponent]
@init {
	boolean hasSecondComponent = false;
}
	: 'POSITION' compSelector1=componentSelector[true] constrDefinition=constraintDefinition ( compSelector2=componentSelector[true]  { hasSecondComponent = true; } )? ';'
    	{
		if (inPropParseState()) {
	    		$designStatement::design.addIntersection($compSelector1.component,
	    							 					$constrDefinition.constraint,
	    							 					hasSecondComponent ? $compSelector2.component : parentComponent);
	    	}
    	}
	;

constraintDefinition returns [DoNotIntersectSimplexConstraint constraint]
	: 'TO' 'THE' 'LEFT' { $constraint = TOTHE_LEFT; }
	| 'TO' 'THE' 'RIGHT' { $constraint = TOTHE_RIGHT; }
	| 'TO' 'THE' 'BOTTOM' { $constraint = TOTHE_BOTTOM; }
	| 'TO' 'THE' 'RIGHTBOTTOM' { $constraint = TOTHE_RIGHTBOTTOM; }
	| 'TO' 'NOT' 'INTERSECT' { $constraint = DO_NOT_INTERSECT; }
	;

addPositionDefinition returns [InsertPosition position]
	: 'IN' { $position = InsertPosition.IN; }
	| 'BEFORE' { $position = InsertPosition.BEFORE; }
	| 'AFTER' { $position = InsertPosition.AFTER; }
	;


setObjectPropertyStatement[Object propertyReceiver] returns [String id, Object value]
	: ID '=' componentPropertyValue ';'  { setObjectProperty($propertyReceiver, $ID.text, $componentPropertyValue.value); }
	;

componentPropertyValue returns [Object value]
	: c=colorLiteral { $value = $c.val; }
	| s=stringLiteral { $value = $s.val; }
	| i=intLiteral { $value = $i.val; }
	| d=doubleLiteral { $value = $d.val; }
	| dim=dimensionLiteral { $value = $dim.val; }
	| b=booleanLiteral { $value = $b.val; }
	;

colorLiteral returns [Color val]
	: c=COLOR_LITERAL { $val = Color.decode($c.text); }
	;

stringLiteral returns [String val]
	: s=STRING_LITERAL { $val = self.transformStringLiteral($s.text); }
	;

intLiteral returns [int val]
@init {
	boolean isMinus = false;
}
	: (MINUS {isMinus=true;})? ui=uintLiteral  { $val = (isMinus ? -1 : 1) * Integer.parseInt($ui.text); }
	;

doubleLiteral returns [double val]
@init {
	boolean isMinus = false;
}
	: (MINUS {isMinus=true;})? ud=udoubleLiteral { $val = (isMinus ? -1 : 1) * Double.parseDouble($ud.text); }
	;

booleanLiteral returns [boolean val]
	: bool=LOGICAL_LITERAL { $val = Boolean.valueOf($bool.text); }
	;

dimensionLiteral returns [Dimension val]
	: '(' x=intLiteral ',' y=intLiteral ')' { $val = new Dimension(Integer.parseInt($x.text), Integer.parseInt($y.text)); }
	;

////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////// COMMON /////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

propertyWithNamedParams returns [String name, List<String> params]
	:	propName=compoundID { $name = $propName.text; }
		'('
		list=idList { $params = $list.ids; }
		')'
	;

parameter
	:	ID | NUMBERED_PARAM
	;


simpleNameWithCaption returns [String name, String caption] 
	:	simpleName=ID { $name = $simpleName.text; }
		(captionStr=STRING_LITERAL { $caption = $captionStr.text; })?
	;
	
idList returns [List<String> ids] 
@init {
	ids = new ArrayList<String>();	
} 
	:	(neIdList=nonEmptyIdList { ids = $neIdList.ids; })?
	;

classIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	((firstClassName=classId { ids.add($firstClassName.text); })
		(',' className=classId { ids.add($className.text); })*)?
	;

compoundIdList returns [List<String> ids] 
@init {
	ids = new ArrayList<String>();	
} 
	:	(neIdList=nonEmptyCompoundIdList { ids = $neIdList.ids; })?
	;

nonEmptyIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>(); 
}
	:	firstId=ID	{ $ids.add($firstId.text); }
		(',' nextId=ID	{ $ids.add($nextId.text); })*
	;

nonEmptyCompoundIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	firstId=compoundID	{ $ids.add($firstId.text); }
		(',' nextId=compoundID	{ $ids.add($nextId.text); })*
	;

parameterList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	(firstParam=parameter	 { $ids.add($firstParam.text); }
		(',' nextParam=parameter { $ids.add($nextParam.text); })* )?
	;


propertyExpressionList[List<String> context, boolean dynamic] returns [List<LP<?>> props, List<List<Integer>> usedParams] 
@init {
	$props = new ArrayList<LP<?>>();
	$usedParams = new ArrayList<List<Integer>>(); 
}
	:	(neList=nonEmptyPropertyExpressionList[context, dynamic] { $props = $neList.props; $usedParams = $neList.usedParams; })?
	;
	

nonEmptyPropertyExpressionList[List<String> context, boolean dynamic] returns [List<LP<?>> props, List<List<Integer>> usedParams]
@init {
	$props = new ArrayList<LP<?>>();
	$usedParams = new ArrayList<List<Integer>>(); 
}
	:	first=propertyExpression[context, dynamic] { $props.add($first.property); $usedParams.add($first.usedParams); }
		(',' next=propertyExpression[context, dynamic] { $props.add($next.property); $usedParams.add($next.usedParams);})* 
	; 

literal returns [LP property]
@init {
	ScriptingLogicsModule.ConstType cls = null;
	String text = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addConstantProp(cls, text);	
	}
}
	: 	strInt=uintLiteral	{ cls = ScriptingLogicsModule.ConstType.INT; text = $strInt.text; }
	|	strReal=udoubleLiteral	{ cls = ScriptingLogicsModule.ConstType.REAL; text = $strReal.text; }
	|	str=STRING_LITERAL	{ cls = ScriptingLogicsModule.ConstType.STRING; text = $str.text; }  
	|	str=LOGICAL_LITERAL	{ cls = ScriptingLogicsModule.ConstType.LOGICAL; text = $str.text; }
	|	strEnum=strictCompoundID{ cls = ScriptingLogicsModule.ConstType.ENUM; text = $strEnum.text; } 
	;
	
classId 
	:	compoundID | PRIMITIVE_TYPE
	;

compoundID
	:	(ID '.')? ID
	;

strictCompoundID
	:	ID '.' ID
	;
	
multiCompoundID returns [String sid]
	:	id=ID { $sid = $id.text; } ('.' cid=ID { $sid = $sid + "." + $cid.text; } )*
	;

udoubleLiteral
	:	POSITIVE_DOUBLE_LITERAL
	; 
		
uintLiteral
	:	UINT_LITERAL
	;		



/////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////// LEXER //////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////
	
fragment NEWLINE	:	'\r'?'\n'; 
fragment SPACE		:	(' '|'\t');
fragment STR_LITERAL_CHAR	: '\\\'' | ~('\r'|'\n'|'\'');	 // overcomplicated due to bug in ANTLR Works
fragment DIGITS		:	('0'..'9')+;
fragment HEX_DIGIT	: 	'0'..'9' | 'a'..'f' | 'A'..'F';

PRIMITIVE_TYPE  :	'INTEGER' | 'DOUBLE' | 'LONG' | 'BOOLEAN' | 'DATE' | 'STRING[' DIGITS ']' | 'ISTRING[' DIGITS ']';
LOGICAL_LITERAL :	'TRUE' | 'FALSE';		
ID          	:	('a'..'z'|'A'..'Z')('a'..'z'|'A'..'Z'|'_'|'0'..'9')*;
WS				:	(NEWLINE | SPACE) { $channel=HIDDEN; };
STRING_LITERAL	:	'\'' STR_LITERAL_CHAR* '\'';
COLOR_LITERAL 	:	'#' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
COMMENTS		:	('//' .* '\n') { $channel=HIDDEN; };
UINT_LITERAL 	:	DIGITS;
POSITIVE_DOUBLE_LITERAL	: 	DIGITS '.' DIGITS;	  
NUMBERED_PARAM	:	'$' DIGITS;
EQ_OPERAND		:	('==') | ('!=');
REL_OPERAND		: 	('<') | ('>') | ('<=') | ('>=');
MINUS			:	'-';
PLUS			:	'+';
MULT_OPERAND	:	('*') | ('/');