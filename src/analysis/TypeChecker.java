package analysis;

import grammar.CracklBaseListener;
import grammar.CracklParser.*;

import java.util.ArrayList;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

public class TypeChecker extends CracklBaseListener {

	ParseTreeProperty<Type> types;
	ArrayList<String> errors;
	ArrayList<Scope> scopes;
	Result result;

	public TypeChecker(){
		types = new ParseTreeProperty<Type>();
		errors = new ArrayList<String>();
		scopes = new ArrayList<Scope>();
		result = new Result();
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
			addError(ctx,"Identifier " + ctx.target().getText() + " is not an array.");
		}else{
			Array arrType = (Array) type;
			Type typeOfArray = arrType.getTypeObj();
			checkType(ctx.expr(0), Type.INT);
			checkType(ctx.expr(1), typeOfArray);
		}
	}

	@Override
	public void exitAssignStat(AssignStatContext ctx)
	{
		Scope curScope = scopes.get(scopes.size()-1);
		Type lhsType = getType(ctx.target());
		checkType(ctx.expr(), lhsType);
		curScope.addInitVar(ctx.getText());
	}

	public boolean isInitialized(String var){
		for(int i = scopes.size() - 1; i >= 0; i--){
			if(scopes.get(i).isInitialized(var)){
				return true;
			}
		}
		addError(String.format("Variable '%s' is not initialized.", var));
		return false;
	}

	@Override
	public void exitIfStat(IfStatContext ctx) {
		checkType(ctx.expr(), Type.BOOL);
	}

	@Override
	public void exitPrintExprStat(PrintExprStatContext ctx)
	{
		String res = ctx.expr().getText();
		System.out.println(res);
	}

	@Override
	public void exitDecl(DeclContext ctx)
	{
		String var = ctx.ID().getText();
		Scope curScope = scopes.get(scopes.size()-1);
		if(curScope.getType(var) != null){
			addError(ctx, String.format("Variable '%s' already declared in this scope!", var));
		}else{
			Type lhsType = Type.get(ctx.type().getText());
			if (ctx.expr() != null) {
				checkType(ctx.expr(), lhsType);
				curScope.addInitVar(var);
			}
			types.put(ctx, lhsType);
			curScope.put(var, lhsType);
			result.addType(ctx, lhsType);
			result.addOffset(ctx, curScope.getOffset(var));
			result.addNode(ctx);
		}
	}

	@Override
	public void exitArrayDeclInit(ArrayDeclInitContext ctx) {
		Type type = types.get(ctx.expr(0));
		int size = type.getSize();
		boolean correct = true;
		for(int i = 1; i < ctx.expr().size(); i++){
			if(!checkType(ctx.expr(i), type)){
				correct = false;
				type = Type.ERR;
				break;
			}
			size += types.get(ctx.expr(i)).getSize();
		}
		Array rhsType = null;
		if(correct){		
			rhsType = new Array(type);
			rhsType.setSize(size);
			rhsType.setLength(ctx.expr().size());
		}else{
			addError(ctx,"Incorrect initialization of array.");
		}

		String var = ctx.ID().getText();
		Scope curScope = scopes.get(scopes.size()-1);
		if(curScope.getType(var) != null){
			addError(ctx,String.format("Variable '%s' already declared in this scope!", var));
		}else{
			//declaring an array
			//when declaring an array without a given size, an expression of type Array should be given.
			//probably superfluous, since grammar enforces this.
			types.put(ctx, rhsType);
			curScope.put(var, rhsType);
			result.addType(ctx, rhsType);
			result.addOffset(ctx, curScope.getOffset(var));
			result.addNode(ctx);
			curScope.addInitVar(var);

		}
	}

	@Override
	public void exitArrayDecl(ArrayDeclContext ctx) {
		String var = ctx.ID().getText();
		Scope curScope = scopes.get(scopes.size()-1);
		if(curScope.getType(var) != null){
			addError(ctx,String.format("Variable '%s' already declared in this scope!", var));
		}else{
			Array lhsType = new Array(Type.get(ctx.type().getText()));
			if(!checkType(ctx.expr(), Type.INT)){
				addError(ctx,"Array should be declared with an expression of type integer.");
			}else{
				types.put(ctx, lhsType);
				curScope.put(var, lhsType);
				result.addType(ctx, lhsType);
				result.addOffset(ctx, curScope.getOffset(var));
				result.addNode(ctx);
				curScope.addInitVar(var); // automatically initialized with default values.
			}
		}
	}



	@Override
	public void exitIdExpr(IdExprContext ctx)
	{
		String var = ctx.getText();
		isInitialized(var);
	}

	@Override
	public void exitFunc(FuncContext ctx)
	{
		Type retType = Type.get(ctx.retType().getText());
		types.put(ctx, retType);
		checkType(ctx.ret(), retType);
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
			System.out.println("Build succeeded without typecheck errors.");
			for(ParserRuleContext prc : result.getNodes()){
				System.out.println("----------------" + prc.getText() + "----------------");
				Type type = types.get(prc);
				System.out.println(type);
				if(type instanceof Array){
					System.out.println("HEYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY");
				}
			}
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
		return this.errors.size() > 0;
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
	public void exitRet(RetContext ctx)
	{
		types.put(ctx, types.get(ctx.expr()));
	}

	@Override
	public void exitAddExpr(AddExprContext ctx) {
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
	public void exitFuncExpr(FuncExprContext ctx) {
		types.put(ctx, getType(ctx.funcCall()));
	}

	@Override
	public void exitFuncCall(FuncCallContext ctx) {
		//TODO Params type checken.
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
	public void exitPntAssign(PntAssignContext ctx) {
		String var = ctx.ID().getText();
		if(isInitialized(var)){
			Type idType = getTypeByString(ctx.ID().getText());
			Pointer p = (Pointer) getTypeByString(ctx.target().getText());
			checkTypePointer(p, idType, ctx);
		}
	}

	@Override
	public void exitPntDecl(PntDeclContext ctx) {
		Scope curScope = scopes.get(scopes.size() - 1);
		String var = ctx.ID(0).getText();
		if(curScope.exists(var)){
			addError(ctx, "Variable '" + var + "' is already declared!");
		}
		Type lhsType = Type.get(ctx.type().getText());
		Pointer pointerType = new Pointer(lhsType);
		if(ctx.ID().size() > 1){
			String otherVar = ctx.ID(1).getText();
			isInitialized(otherVar);
			checkType(lhsType, getTypeByString(otherVar), ctx);
		}
		types.put(ctx, pointerType);
		curScope.put(var, pointerType);
		result.addType(ctx, pointerType);
		result.addOffset(ctx, curScope.getOffset(var));
		result.addNode(ctx);
		curScope.addInitVar(var);
	}

	@Override
	public void exitPntDeclNormal(PntDeclNormalContext ctx) {
		Scope curScope = scopes.get(scopes.size() - 1);
		String var = ctx.target().getText();
		if(curScope.exists(var)){
			addError(ctx, "Variable '" + var + "' is already declared!");
		}
		Type lhsType = Type.get(ctx.type().getText());
		Pointer pointerType = new Pointer(lhsType);

		checkType(pointerType, getType(ctx.expr()), ctx);

		types.put(ctx, pointerType);
		curScope.put(var, pointerType);
		result.addType(ctx, pointerType);
		result.addOffset(ctx, curScope.getOffset(var));
		result.addNode(ctx);
		curScope.addInitVar(var);
	}

	@Override
	public void exitPntExpr(PntExprContext ctx) {
		String var = ctx.ID().getText();
		isInitialized(var);
		Type type = getTypeByString(var);
		if(!(type instanceof Pointer)){
			addError(ctx, "Variable " + var + " is not a pointer.");
		}
		types.put(ctx, ((Pointer) getTypeByString(var)).getTypeObj());
	}

	@Override
	public void exitFuncStat(FuncStatContext ctx) {
		types.put(ctx, getType(ctx.func()));
	}

	@Override
	public void exitConstBoolExpr(ConstBoolExprContext ctx) {
		types.put(ctx, Type.BOOL);
		result.addType(ctx, Type.BOOL);
		result.addNode(ctx);
	}

	@Override
	public void exitConstNumExpr(ConstNumExprContext ctx) {
		types.put(ctx, Type.INT);
		result.addType(ctx, Type.INT);
		result.addNode(ctx);
	}

	@Override
	public void exitCompExpr(CompExprContext ctx) {
		if(checkType(ctx.expr(0), Type.INT) && checkType(ctx.expr(1), Type.INT)){
			types.put(ctx, Type.BOOL);
			result.addType(ctx, Type.BOOL);
		}else{
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

	public boolean checkType(Type expected, Type actual, RuleContext ctx){
		boolean res = true;
		if(!actual.equals(expected)){
			res = false;
			addError(ctx, "Expected type " + expected + ", got " + actual + ".");
		}
		return res;
	}

	public boolean checkTypePointer(Pointer p, Type actual, RuleContext ctx){
		boolean res = true;
		Type pointerType = p.getTypeObj();
		if(!pointerType.equals(actual)){
			addError(ctx, "Expected type " + pointerType + ", got " + actual);
			res = false;
		}
		return res;
	}

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

	public Type getTypeByString(String var)
	{
		Type type = null;
		boolean found = false;
		for(int i = scopes.size() - 1; i >= 0 && !found; i--){
			Scope curScope = scopes.get(i);
			if(curScope.exists(var)){
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

	public void addError(String s)
	{
		errors.add(s);
	}

	public void addError(RuleContext ctx, String error)
	{
		Token start = ((ParserRuleContext) ctx).start;
		String pos = start.getLine() + ":" + start.getCharPositionInLine();
		addError(String.format("%s (%s)", error, pos));
	}

	public Result getResult(){
		return result;
	}

}
