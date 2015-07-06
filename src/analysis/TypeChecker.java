package analysis;

import grammar.CracklBaseListener;
import grammar.CracklParser.AndExprContext;
import grammar.CracklParser.ArrayAssignStatContext;
import grammar.CracklParser.ArrayDeclContext;
import grammar.CracklParser.ArrayDeclInitContext;
import grammar.CracklParser.ArrayExprContext;
import grammar.CracklParser.ArrayIndexExprContext;
import grammar.CracklParser.ArrayTypeContext;
import grammar.CracklParser.AssignDerefContext;
import grammar.CracklParser.AssignStatContext;
import grammar.CracklParser.BlockStatContext;
import grammar.CracklParser.CompExprContext;
import grammar.CracklParser.ConstBoolExprContext;
import grammar.CracklParser.ConstNumExprContext;
import grammar.CracklParser.ConstTextExprContext;
//import grammar.CracklParser.ConstTextExprContext;
import grammar.CracklParser.DeclContext;
import grammar.CracklParser.FuncCallContext;
import grammar.CracklParser.FuncDeclContext;
import grammar.CracklParser.FuncDeclStatContext;
import grammar.CracklParser.FuncExprContext;
import grammar.CracklParser.IdExprContext;
import grammar.CracklParser.IfStatContext;
import grammar.CracklParser.LockDeclContext;
import grammar.CracklParser.MainfuncContext;
import grammar.CracklParser.NegExprContext;
import grammar.CracklParser.NotExprContext;
import grammar.CracklParser.NumOfSprockellContext;
import grammar.CracklParser.OrExprContext;
import grammar.CracklParser.OtherOperatorExprContext;
import grammar.CracklParser.ParExprContext;
import grammar.CracklParser.ParamsContext;
import grammar.CracklParser.PrimitiveContext;
import grammar.CracklParser.PrintExprStatContext;
import grammar.CracklParser.ProgramContext;
import grammar.CracklParser.PtrAssignContext;
import grammar.CracklParser.PtrDeclContext;
import grammar.CracklParser.PtrDeclNormalContext;
import grammar.CracklParser.PtrDerefExprContext;
import grammar.CracklParser.PtrRefExprContext;
import grammar.CracklParser.RetContext;
import grammar.CracklParser.SignOperatorExprContext;
import grammar.CracklParser.SprockellIdExprContext;
import grammar.CracklParser.TypeContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import analysis.Type.Types;

public class TypeChecker extends CracklBaseListener {
	public static final String VOIDSTRING = "void";
	
	public static final String FUNC_NOT_IN_TOP_LEVEL_ERROR = "Functions can only be made in the top level scope.";
	public static final String VARIABLE_NOT_INITIALIZED_ERROR = "Variable '%s' is not initialized.";
	public static final String GLOBAL_VARIABLE_NOT_IN_OUTER_SCOPE_ERROR = "Global variable '%s' may only be declared in the top level scope.";
	public static final String FUNCTION_ALREADY_EXISTS_ERROR = "Function '%s' already exists.";
	public static final String VARIABLE_ALREADY_DECLARED_ERROR = "Variable '%s' already declared in this scope!";
	public static final String FUNCTION_VOID_RETURN_ERROR = "Function '%s' has a return expression, but its return type is void (should be just 'return;').";
	public static final String FUNCTION_DOES_NOT_EXIST_ERROR = "Function '%s' does not exist.";
	public static final String ID_NOT_ARRAY_ERROR = "Identifier '%s' is not an array.";
	private static final String COMP_INCOMPARABLE_TYPES = "Variables of the type %s is not comparable with %s.";


	ParseTreeProperty<Type> types;
	ParseTreeProperty< ArrayList<Type>> paramTypes;
	ArrayList<String> errors;
	ArrayList<Scope> scopes;
    public HashMap<String,Function> functions = new HashMap<String,Function>();
    private Function currentFunction;
	Result result;
	HashMap<String, ArrayList<Type>> funcParams;
	HashMap<String, Type> funcTypes;

	public TypeChecker(){
		types = new ParseTreeProperty<Type>();
		paramTypes = new ParseTreeProperty<ArrayList<Type>>(); //no ParseTreeContex, since it's missing keySet().
		errors = new ArrayList<String>();
		scopes = new ArrayList<Scope>();
		result = new Result();
		funcParams = new HashMap<String, ArrayList<Type>>();
		funcTypes = new HashMap<String, Type>();
	}

	@Override
	public void enterParams(ParamsContext ctx)
	{
		Scope newScope = scopes.get(scopes.size()-1);

		List<TypeContext> types = ctx.type();
		funcParams.put(currentFunction.id,new ArrayList<Type>());
		for (int i = 0; i < types.size(); i++) {
			TerminalNode idCtx = ctx.ID(i);
			Type type = getTypeFromContext(ctx.type(i));
			String var = idCtx.getText();

			this.types.put(idCtx, type);
			newScope.put(var, type, false);
			newScope.addInitVar(var);
			result.addType(ctx, type);
			result.addOffset(ctx, newScope.getOffset(var));
			result.addNode(ctx);
			
			currentFunction.params.add(var);
			 funcParams.get(currentFunction.id).add(type);
			
		}
		//funcParams.put(currentFunction.id, currentFunction.params);
	}

	@Override
	public void enterRet(RetContext ctx)
	{
		types.put(ctx, types.get(ctx.expr()));
	}

	@Override
	public void exitBlockStat(BlockStatContext ctx) {
		Scope removeScope = scopes.get(scopes.size() - 1);
		scopes.remove(scopes.size() - 1);
		result.addScope(ctx, removeScope);
		result.addNode(ctx);
	}

	@Override
	public void enterBlockStat(BlockStatContext ctx) {
		Scope lastScope; 
		if(scopes.size() == 0){
			lastScope = null;
		}else{
			lastScope = scopes.get(scopes.size() - 1);
		}

		Scope scope = new Scope(lastScope);
		scopes.add(scope);
	}

	@Override
	public void exitArrayIndexExpr(ArrayIndexExprContext ctx) {
		String idString = ctx.ID().getText();
		if(getTypeByString(idString) instanceof Array){
			Array idType = (Array) getTypeByString(idString);
			if(!checkType(ctx.expr(), Type.INT)){
				addError(ctx,"Index of an array must be an integer.");
			}else{
				types.put(ctx, idType.getTypeObj());
			}
		}else{
			addError(ctx,String.format("Cannot get a value from '%s', since it is not an array.", ctx.ID().getText()));
			types.put(ctx, Type.ERR);
		}
	}

	public void exitArrayAssignStat(ArrayAssignStatContext ctx) {
		isInitialized(ctx.target().getText());
		Type type = getType(ctx.target());
		if(!(type instanceof Array)){
			addError(ctx, String.format(ID_NOT_ARRAY_ERROR, ctx.target().getText()));
		}else{
			Array arrType = (Array) type;
			Type typeOfArray = arrType.getTypeObj();
			//checkType(ctx.expr(0), Type.INT); can also be expr
			checkType(ctx.expr(1), typeOfArray);
		}
	}

	@Override
	public void exitAssignStat(AssignStatContext ctx)
	{
		Scope curScope = scopes.get(scopes.size()-1);
		Type lhsType = getType(ctx.target());
		checkType(ctx.expr(), lhsType);
		curScope.addInitVar(ctx.target().getText());
	}
	
	@Override
	public void exitAssignDeref(AssignDerefContext ctx)
	{
		// e.g. @ptr_a = 5;
		Type targetType = getTypeByString(ctx.derefTarget().ID().getText());
		targetType = targetType.getTypeObj();
		Type exprType = getType(ctx.expr());
		checkType(targetType, exprType, ctx);
		super.exitAssignDeref(ctx);
	}

	public boolean isInitialized(String var){
		for(int i = scopes.size() - 1; i >= 0; i--){
			if(scopes.get(i).isInitialized(var)){
				return true;
			}
		}
		addError(String.format(VARIABLE_NOT_INITIALIZED_ERROR, var));
		return false;
	}

	@Override
	public void exitIfStat(IfStatContext ctx) {
		checkType(ctx.expr(), Type.BOOL);
	}

	@Override
	public void exitPrintExprStat(PrintExprStatContext ctx)
	{
		types.put(ctx, getType(ctx.expr()));
	}

	@Override
	public void exitDecl(DeclContext ctx)
	{
		String var = ctx.ID().getText();
		boolean global = (ctx.GLOBAL() != null);
		if(global && (scopes.size() > 1)){
			addError(ctx, String.format(GLOBAL_VARIABLE_NOT_IN_OUTER_SCOPE_ERROR, var));
		}
		Scope curScope = scopes.get(scopes.size()-1);
		if(curScope.getType(var) != null){
			addError(ctx, String.format(VARIABLE_ALREADY_DECLARED_ERROR, var));
		}else{
			Type lhsType = getTypeFromContext(ctx.type());
			if (ctx.expr() != null) {
				checkType(ctx.expr(), lhsType);
				curScope.addInitVar(var);
			}
			types.put(ctx, lhsType);
			curScope.put(var, lhsType, global);
			result.addType(ctx, lhsType);
			result.addOffset(ctx, curScope.getOffset(var));
			result.addNode(ctx);
		}
	}
	
	@Override
	public void enterMainfunc(MainfuncContext ctx)
	{
		Scope lastScope; 
		if(scopes.size() == 0){
			lastScope = null;
		}else{
			lastScope = scopes.get(scopes.size() - 1);
		}

		Scope scope = new Scope(lastScope);
		scopes.add(scope);
	}
	
	@Override
	public void exitMainfunc(MainfuncContext ctx)
	{
		Scope removeScope = scopes.get(scopes.size() - 1);
		scopes.remove(scopes.size() - 1);
		result.addScope(ctx, removeScope);
		result.addNode(ctx);
	}

	@Override
	public void exitArrayDeclInit(ArrayDeclInitContext ctx) {
		String var = ctx.ID().getText();
		boolean global = (ctx.GLOBAL() != null);
		if(global && (scopes.size() > 1)){
			addError(ctx, String.format(GLOBAL_VARIABLE_NOT_IN_OUTER_SCOPE_ERROR, var));
		}
		Type type = types.get(ctx.expr());
		boolean correct = checkType(type, types.get(ctx.expr()),ctx);
		Array rhsType = (Array) types.get(ctx.expr());
		/**  Causes wrong types??? e.g. [[Integer]]
		if(correct){
			int size = rhsType.getSize();
			rhsType = new Array(type);
			rhsType.setSize(size);
			rhsType.setLength(rhsType.getLength());
		}else{
			addError(ctx,"Incorrect initialization of array.");
		}
		**/ 

		Scope curScope = scopes.get(scopes.size()-1);
		if(curScope.getType(var) != null){
			addError(ctx,String.format(VARIABLE_ALREADY_DECLARED_ERROR, var));
		}else{
			//declaring an array
			//when declaring an array without a given size, an expression of type Array should be given.
			//probably superfluous, since grammar enforces this.
			types.put(ctx, rhsType);
			curScope.put(var, rhsType, global);
			result.addType(ctx, rhsType);
			result.addOffset(ctx, curScope.getOffset(var));
			result.addNode(ctx);
			curScope.addInitVar(var);

		}
	}

	@Override
	public void exitArrayDecl(ArrayDeclContext ctx) {
		String var = ctx.ID().getText();
		boolean global = (ctx.GLOBAL() != null);
		if(global && (scopes.size() > 1)){
			addError(ctx, String.format(GLOBAL_VARIABLE_NOT_IN_OUTER_SCOPE_ERROR, var));
		}
		Scope curScope = scopes.get(scopes.size()-1);
		if(curScope.getType(var) != null){
			addError(ctx,String.format(VARIABLE_ALREADY_DECLARED_ERROR, var));
		}else{
			Array lhsType = new Array(Type.get(ctx.type().getText()));
			if(!checkType(ctx.expr(), Type.INT)){
				addError(ctx,"Array should be declared with an expression of type integer.");
			}else{
				types.put(ctx, lhsType);
				curScope.put(var, lhsType, global);
				result.addType(ctx, lhsType);
				result.addOffset(ctx, curScope.getOffset(var));
				result.addNode(ctx);
				curScope.addInitVar(var); // automatically initialized with default values.
			}
		}
	}


	@Override
	public void exitPtrDecl(PtrDeclContext ctx) {
		boolean global = (ctx.GLOBAL() != null);
		String var = ctx.ID(0).getText();
		if(global && (scopes.size() > 1)){
			addError(ctx, String.format(GLOBAL_VARIABLE_NOT_IN_OUTER_SCOPE_ERROR, var));
		}

		Scope curScope = scopes.get(scopes.size() - 1);
		if(curScope.exists(var)){
			addError(ctx, String.format(VARIABLE_ALREADY_DECLARED_ERROR, var));
		}
		Type lhsType = getTypeFromContext(ctx.type());
		if(ctx.ID().size() > 1){
			String otherVar = ctx.ID(1).getText();
			isInitialized(otherVar);
			if(lhsType instanceof Pointer){
				checkType(((Pointer)lhsType).getTypeObj(), getTypeByString(otherVar), ctx);
			}else{
				addError("Expected a pointer type in the lefthand side, got "+lhsType);
			}
		}
		types.put(ctx, lhsType);
		curScope.put(var, lhsType, global);
		result.addType(ctx, lhsType);
		result.addOffset(ctx, curScope.getOffset(var));
		result.addNode(ctx);
		curScope.addInitVar(var);
	}


	@Override
	public void exitPtrDeclNormal(PtrDeclNormalContext ctx) {
		String var = ctx.ID(0).getText();
		boolean global = (ctx.GLOBAL() != null);
		if(global && (scopes.size() > 1)){
			addError(ctx, String.format(GLOBAL_VARIABLE_NOT_IN_OUTER_SCOPE_ERROR, var));
		}
		Scope curScope = scopes.get(scopes.size() - 1);
		if(curScope.exists(var)){
			addError(ctx, String.format(VARIABLE_ALREADY_DECLARED_ERROR, var));
		}
		
		Pointer pointerType = (Pointer)getTypeFromContext(ctx.type());
		checkType(pointerType, getTypeByString(ctx.ID(1).getText()), ctx);

		types.put(ctx, pointerType);
		curScope.put(var, pointerType, global);
		result.addType(ctx, pointerType);
		result.addOffset(ctx, curScope.getOffset(var));
		result.addNode(ctx);
		curScope.addInitVar(var);
	}

	@Override
	public void exitIdExpr(IdExprContext ctx)
	{
		String var = ctx.getText();
		isInitialized(var);
	}

	@Override
	public void enterFuncDecl(FuncDeclContext ctx)
	{
		Scope lastScope; 
		lastScope = scopes.get(scopes.size() - 1);
		if(lastScope.getPreviousScope() != null){
			addError(ctx, FUNC_NOT_IN_TOP_LEVEL_ERROR);
		}
		assert(lastScope.getPreviousScope() == null); //should be the outer scope! or something

		Scope newScope = new Scope(lastScope, true);
		scopes.add(newScope);
		
		currentFunction = new Function(ctx.ID().getText());
		functions.put(ctx.ID().getText(), currentFunction);
		
		//Moved from exitFuncDecl
		Type retType = Type.get(ctx.retType().getText());
		boolean isVoid = retType == Type.VOID;
		String functionName = ctx.ID().getText();

		//funcParams.put(functionName, paramTypes.get(ctx.params()));
//		
//		for()
		
		funcTypes.put(functionName, retType);
	}

	@Override
	public void exitFuncDecl(FuncDeclContext ctx)
	{
		Type retType = Type.get(ctx.retType().getText());
		boolean isVoid = retType == Type.VOID;
		String functionName = ctx.ID().getText();

		if(isVoid){
			if(ctx.ret().expr() != null){
				addError(ctx, String.format(FUNCTION_VOID_RETURN_ERROR, functionName));
			}
		}
		if(funcTypes.containsKey(ctx)){
			addError(ctx, String.format(FUNCTION_ALREADY_EXISTS_ERROR, functionName));
		}else{
			funcTypes.put(functionName, retType);
			if(!isVoid){
				if(ctx.ret()==null){
					addError(ctx, "Function '" + functionName + "' has no return statement (expects " + retType + ").");
				}else{
					checkType(ctx.ret().expr(), retType);
				}
			}
		}

		Scope removeScope = scopes.get(scopes.size() - 1);
		scopes.remove(scopes.size() - 1);
		result.addScope(ctx, removeScope);
		result.addNode(ctx);
	}
	
	@Override
	public void exitArrayExpr(ArrayExprContext ctx) {
		int i;
		Type type = types.get(ctx.expr(0));
		for(i = 1; i < ctx.expr().size(); i++){
			checkType(type, types.get(ctx.expr(i)), ctx);
		}
		Array arrayType = new Array(type);
		arrayType.setLength(i);
		arrayType.setLength(i * type.getSize());
		types.put(ctx, new Array(type));
	}

	@Override
	public void exitProgram(ProgramContext ctx) {
		if(hasErrors()){
			for (String error : errors) {
				System.err.println(error);
			}
			System.err.println("Build failed.");
		}else{
			//			result.addScope(ctx, scopes.get(0));	//there should be only 1 scope left, namely the global scope.
			//			result.addNode(ctx);
//			System.out.println("Build succeeded without typecheck errors.");
//			for(ParserRuleContext prc : result.getNodes()){
//				//System.out.println("----------------" + prc.getText() + "----------------");
//				Type type = types.get(prc);
//				System.out.println(type);
//				if(type instanceof Array){
//				}
//			}
			//			for(ParserRuleContext prc : result.getNodes()){		// testing 
			//				System.out.println("----------------" + prc.getText() + "----------------");
			//				if(result.getTypes().get(prc) != null){
			//					System.out.println("Type: " + result.getType(prc));
			//				}
			//				if(result.getOffsets().get(prc) != null){
			//					System.out.println("Offset: " + result.getOffset(prc));
			//				}
			//				if(result.getScopes().get(prc) != null){
			//					System.out.println("Scope base: " + result.getScope(prc).getBaseAddress());
			//				}
			//			}
		}
	}

	public boolean hasErrors() {
		return !this.errors.isEmpty();
	}

	public ArrayList<String> getErrors()
	{
		return errors;
	}

	@Override
	public void exitNotExpr(NotExprContext ctx) {
		if(checkType(ctx.expr(), Type.BOOL)){
			types.put(ctx, Type.BOOL);
			result.addType(ctx, Type.BOOL);
		}else{
			types.put(ctx, Type.ERR);
			result.addType(ctx, Type.ERR);
		}
		result.addNode(ctx);
	}
	
	@Override
	public void exitSignOperatorExpr(SignOperatorExprContext ctx)
	{
		if(checkType(ctx.expr(0), Type.INT) && checkType(ctx.expr(1), Type.INT)){
			types.put(ctx, Type.INT);
			result.addType(ctx, Type.INT);
		}else{
			types.put(ctx, Type.ERR);
			result.addType(ctx, Type.ERR);
		}
		result.addNode(ctx);
	}
	
	@Override
	public void exitNegExpr(NegExprContext ctx)
	{
		checkType(ctx.expr(), Type.INT);
		types.put(ctx, Type.INT);
		super.exitNegExpr(ctx);
	}
	
	@Override
	public void exitOtherOperatorExpr(OtherOperatorExprContext ctx)
	{
		if(checkType(ctx.expr(0), Type.INT) && checkType(ctx.expr(1), Type.INT)){
			types.put(ctx, Type.INT);
			result.addType(ctx, Type.INT);
		}else{
			types.put(ctx, Type.ERR);
			result.addType(ctx, Type.ERR);
		}
		result.addNode(ctx);	super.exitOtherOperatorExpr(ctx);
	}

	@Override
	public void exitFuncExpr(FuncExprContext ctx) {
		types.put(ctx, types.get(ctx.funcCall()));
	}

	@Override
	public void exitFuncCall(FuncCallContext ctx) {
		String funcName = ctx.ID().getText();
		Type funcType = funcTypes.get(funcName);
		if(funcType == null){
			addError(ctx, String.format(FUNCTION_DOES_NOT_EXIST_ERROR, funcName));
		}else{
			types.put(ctx, funcType);
			int funcParamsAmount = funcParams.get(funcName).size();
			int actualAmount = ctx.expr().size();
			if(funcParamsAmount != actualAmount){
				addError(ctx, String.format("Invalid amount of arguments for function '%s', expected %d but got %d.", funcName, funcParamsAmount, actualAmount));
			}else{
				for(int i = 0; i < actualAmount; i++){
					Type type = types.get(ctx.expr(i));
					if(type!=null){
						checkType(funcParams.get(funcName).get(i),type , ctx);
					}else{
						System.out.println("Warning: function referenced before declaration: "+funcName);
					}
				}
			}
		}
	}

	@Override
	public void exitAndExpr(AndExprContext ctx) {
		if(checkType(ctx.expr(0), Type.BOOL) && checkType(ctx.expr(1), Type.BOOL)){
			types.put(ctx, Type.BOOL);
			result.addType(ctx, Type.BOOL);
		}else{
			types.put(ctx, Type.ERR);
			result.addType(ctx, Type.ERR);
		}
		result.addNode(ctx);
	}

	@Override
	public void exitPtrAssign(PtrAssignContext ctx) {
		String var = ctx.ID(0).getText();
		if(isInitialized(var)){
			Type idType = getTypeByString(ctx.ID(0).getText());
			Pointer p = (Pointer) getTypeByString(ctx.ID(1).getText());
			checkTypePointer(p, idType, ctx);
		}
	}

	@Override
	public void exitPtrDerefExpr(PtrDerefExprContext ctx) {
		Type type = getTypeByString(ctx.ID().getText());
		if(type instanceof Pointer){
			types.put(ctx, ((Pointer) type).getTypeObj());
		}else{
			addError(ctx, ctx.ID().getText()+" is not a pointer.");
		}
	}
	
	@Override
	public void exitLockDecl(LockDeclContext ctx)
	{
		String var = ctx.ID().getText();
		Scope curScope = scopes.get(scopes.size()-1);
		if(curScope.getPreviousScope() != null){
			addError(ctx, String.format("Locks should not be created in inner scopes"));
		}
		if(curScope.getType(var) != null){
			addError(ctx, String.format(VARIABLE_ALREADY_DECLARED_ERROR, var));
		}else{
			Type lhsType = Type.LOCK;
				curScope.addInitVar(var);
			types.put(ctx, lhsType);
			curScope.put(var, lhsType, true); ///////Todo: maybe global?
			result.addType(ctx, lhsType);
			result.addNode(ctx);
			result.addStaticGlobal(var);
		}
	}

	@Override
	public void exitPtrRefExpr(PtrRefExprContext ctx) {
		Pointer type = new Pointer(getTypeByString(ctx.ID().getText()));
		types.put(ctx, type);
		result.addNode(ctx);
		result.addType(ctx, type);
	}

	/**
	@Override
	public void exitLockStat(LockStatContext ctx) {
		checkType(ctx.expr(), Type.LOCK);
	}

	@Override
	public void exitUnlockStat(UnlockStatContext ctx) {
		checkType(ctx.expr(), Type.LOCK);
	}
	**/

	@Override
	public void exitParams(ParamsContext ctx) {
		for(TypeContext tctx : ctx.type()){
			Type type = getTypeFromContext(tctx);
			if(paramTypes.get(ctx) != null){
				paramTypes.get(ctx).add(type);
			}else{
				ArrayList<Type> list = new ArrayList<Type>();
				list.add(type);
				paramTypes.put(ctx, list);
			}
		}
		
	}
	
	//Get type, pointer/array aware
	private Type getTypeFromContext(TypeContext ctx ){
		Type type;
		if(ctx.arrayType()==null){
			//nested pointers
			type = new Pointer(getTypeFromContext(ctx.type()));
		}else{
			ArrayTypeContext arrayTypeCtx = ctx.arrayType();
			type = getTypeFromArrayContext(arrayTypeCtx);
			if (ctx.PTRTYPE() != null) {
				type = new Pointer(type);
			}
		}
		return type;
	}
	
	private Type getTypeFromArrayContext(ArrayTypeContext ctx){
			Type type;
		if(ctx.primitive()==null){
			type = new Array(getTypeFromContext(ctx.type()));
		}else{
			PrimitiveContext primCtx = ctx.primitive();
			type = Type.get(primCtx.getText());
			if (ctx.ARRAY() != null) {
				type = new Array(type);
			}
		}
		return type;	
	}

	@Override
	public void exitFuncDeclStat(FuncDeclStatContext ctx) {
		//types.put(ctx, getType(ctx.funcDecl()));
	}

	@Override
	public void exitConstBoolExpr(ConstBoolExprContext ctx) {
		types.put(ctx, Type.BOOL);
		result.addType(ctx, Type.BOOL);
		result.addNode(ctx);
	}

	@Override
	public void exitConstTextExpr(ConstTextExprContext ctx)
	{
		types.put(ctx, Type.TEXT);
		result.addType(ctx, Type.TEXT);
		result.addNode(ctx);
	}

	@Override
	public void exitConstNumExpr(ConstNumExprContext ctx) {
		types.put(ctx, Type.INT);
		result.addType(ctx, Type.INT);
		result.addNode(ctx);
	}
	
	@Override
	public void exitCompExpr(CompExprContext ctx)
	{
		Type t2 = getType(ctx.expr(1));
		Type t1 = getType(ctx.expr(0));
		if ((ctx.EQ()!=null && t1.equals(t2) && !t1.equals(Type.VOID) && !t1.equals(Type.ERR))
			|| (checkType(ctx.expr(0), Type.INT) && checkType(ctx.expr(1), Type.INT))){
			types.put(ctx, Type.BOOL);
			result.addType(ctx, Type.BOOL);
		}
		else {
			addError(ctx, String.format(COMP_INCOMPARABLE_TYPES, t1, t2));
			types.put(ctx, Type.ERR);
			result.addType(ctx, Type.ERR);
		}
		result.addNode(ctx);
	}

	@Override
	public void exitParExpr(ParExprContext ctx) {
		types.put(ctx, getType(ctx.expr()));
	}

	@Override
	public void exitOrExpr(OrExprContext ctx) {
		if(checkType(ctx.expr(0), Type.BOOL) && checkType(ctx.expr(1), Type.BOOL)){
			types.put(ctx, Type.BOOL);
			result.addType(ctx, Type.BOOL);
		}else{
			types.put(ctx, Type.ERR);
			result.addType(ctx, Type.ERR);
		}
		result.addNode(ctx);
	}

	/**
	 * Checks if the type of <code>ctx</code> and <code>expected</code> is equal. Adds an
	 * error to this TypeChecker if it is not the case.
	 * @param ctx The context to be checked.
	 * @param expected The expected type.
	 * @return bool Whether the types are equal.
	 */
	public boolean checkType(RuleContext ctx, Type expected)
	{
		boolean res = true;
		Type type = getType(ctx);
		if (!type.equals(expected)) {
			if(expected instanceof Array && type instanceof Array){
				if(((Array) expected).getLength() != ((Array) type).getLength()){
					addError(ctx, "Exptected array of length " + ((Array) expected).getLength() + ", got an array with length " + ((Array) type).getLength() + ".");
				}
			}
			addError(ctx, "Expected type " + expected + ", got " + type);

			res = false;
		}
		return res;
	}

	@Override
	public void exitSprockellIdExpr(SprockellIdExprContext ctx)
	{
		types.put(ctx, Type.INT);
	}
	
	@Override
	public void enterNumOfSprockell(NumOfSprockellContext ctx)
	{
		result.numberOfSprockells = Integer.parseInt(ctx.NUM().getText());
	}
	
	/**
	 * Checks if the type of <code>actual</code> is equal to <code>expected</code>. Adds an error (with Context <code>ctx</code>) to this Typechecker
	 * if the types are not equal.
	 * @param expected The expected type.
	 * @param actual The actual type.
	 * @param ctx The context of this check.
	 * @return bool Whether the types are equal.
	 */
	public boolean checkType(Type expected, Type actual, RuleContext ctx){
		boolean res = true;
		if(!actual.equals(expected)){
			res = false;
			addError(ctx, "Expected type " + expected + ", got " + actual + ".");
		}
		return res;
	}

	/**
	 * Checks if the Type of a pointer <code>p</code> and <code>actual</code> are equal. Adds an error
	 * to this TypeChecker if this is not the case.
	 * @param p The pointer of which the Type should be checked.
	 * @param actual The actual Type.
	 * @param ctx The context of this check.
	 * @return bool Whether the types are equal.
	 */
	public boolean checkTypePointer(Pointer p, Type actual, RuleContext ctx){
		boolean res = true;
		Type pointerType = p.getTypeObj();
		if(!pointerType.equals(actual)){
			addError(ctx, "(ptr) Expected type " + pointerType + ", got " + actual);
			res = false;
		}
		return res;
	}

	/**
	 * Returns the type of a given RuleContext. First checks the current scope, and then goes upward. 
	 * Returns <code>null</code> if the variable has not been found. Also adds an error to this TypeChecker
	 * if that is the case.
	 * @param ctx The context of which the type should be returned.
	 * @return type The type of the context.
	 */
	public Type getType(RuleContext ctx)
	{
		String var = ctx.getText();
		Type type = null;
		boolean found = false;
		if(types.get(ctx) != null){
			found = true;
			type = types.get(ctx);
		}
		for(int i = scopes.size() - 1; i >= 0 && !found; i--){
			Scope curScope = scopes.get(i);
			if(curScope.exists(var)){
				type = curScope.getType(var);
				break;
			}
		}
		if (type == null) {
			addError(ctx,"Not declared: " + ctx.getText());
			type = Type.ERR;
		}
		result.addType((ParserRuleContext) ctx, type);
		return type;
	}

	/**
	 * Returns the Type of a given String (variable name).
	 * 
	 * @param var
	 *            The name of the variable.
	 * @return type The type of the variable (or null if it hasn't been declared).
	 */
	public Type getTypeByString(String var)
	{
		// First check if it perhaps is a number listeral
//		if (var.matches("-?\\d+(\\.\\d+)?")) {
//			return Type.INT;
//		}
//		else {
//
//		}
		Type type = null;
		boolean found = false;
		for (int i = scopes.size() - 1; i >= 0 && !found; i--) {
			Scope curScope = scopes.get(i);
			if (curScope.exists(var)) {
				type = curScope.getType(var);
				found = true;
			}
		}
		if (type == null) {
			addError("Not declared: " + var);
			type = Type.ERR;
		}
		return type;
	}
	
	/**
	 * Adds an error message to this TypeChecker.
	 * @param s The error message.
	 */
	public void addError(String s)
	{
		errors.add(s);
	}

	/**
	 * Adds an error message to this TypeChecker. Also include a Context, so it's possible to know where
	 * exactly the error occurred.
	 * @param ctx The context of the error.
	 * @param error The error message.
	 */
	public void addError(RuleContext ctx, String error)
	{
		Token start = ((ParserRuleContext) ctx).start;
		String pos = start.getLine() + ":" + start.getCharPositionInLine();
		addError(String.format("%s (%s)", error, pos));
	}

	/**
	 * Returns the Result class of this TypeChecker.
	 * @return result The Result class of this TypeChecker.
	 */
	public Result getResult(){
		return result;
	}

}
