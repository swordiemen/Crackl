package analysis;

import grammar.CracklBaseListener;
import grammar.CracklParser.AddExprContext;
import grammar.CracklParser.AndExprContext;
import grammar.CracklParser.ArrayDeclContext;
import grammar.CracklParser.ArrayExprContext;
import grammar.CracklParser.ArrayIndexExprContext;
import grammar.CracklParser.AssignStatContext;
import grammar.CracklParser.BlockStatContext;
import grammar.CracklParser.CompExprContext;
import grammar.CracklParser.ConstBoolExprContext;
import grammar.CracklParser.ConstNumExprContext;
import grammar.CracklParser.DeclContext;
import grammar.CracklParser.FuncCallContext;
import grammar.CracklParser.FuncContext;
import grammar.CracklParser.FuncExprContext;
import grammar.CracklParser.FuncStatContext;
import grammar.CracklParser.IdExprContext;
import grammar.CracklParser.IfStatContext;
import grammar.CracklParser.NotExprContext;
import grammar.CracklParser.OrExprContext;
import grammar.CracklParser.ParExprContext;
import grammar.CracklParser.PrintExprStatContext;
import grammar.CracklParser.ProgramContext;
import grammar.CracklParser.RetContext;

import java.util.ArrayList;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

public class TypeChecker extends CracklBaseListener {

	ParseTreeProperty<Type> types;
	ArrayList<String> errors;
	ArrayList<ParserRuleContext> initializedVars;
	ArrayList<Scope> scopes;
	Result result;

	public TypeChecker(){
		types = new ParseTreeProperty<Type>();
		errors = new ArrayList<String>();
		initializedVars = new ArrayList<ParserRuleContext>();
		scopes = new ArrayList<Scope>();
		result = new Result();
	}

	@Override
	public void exitArrayExpr(ArrayExprContext ctx) {
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

		if(correct){		
			Array arr = new Array(type);
			arr.setSize(size);
			arr.setLength(ctx.expr().size());
			types.put(ctx, arr);
			System.out.println(types.get(ctx).getSize());
		}else{
			types.put(ctx, Type.ERR);
		}
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
				addError("Index of an array must be an integer.");
			}else{
				types.put(ctx, idType.getTypeObj());
			}
		}else{
			addError(String.format("Cannot get a value from '%s', since it is not an array.", ctx.ID().getText()));
			types.put(ctx, Type.ERR);
		}
	}

	@Override
	public void exitAssignStat(AssignStatContext ctx)
	{
		Type lhsType = getType(ctx.target());
		checkType(ctx.expr(), lhsType);
		initializedVars.add(ctx);
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
			addError(String.format("Variable '%s' already declared in this scope!", var));
		}else{
			Type lhsType = Type.get(ctx.type().getText());
			if (ctx.expr() != null) {
				checkType(ctx.expr(), lhsType);
			}
			types.put(ctx, lhsType);
			curScope.put(var, lhsType);
			result.addType(ctx, lhsType);
			result.addOffset(ctx, curScope.getOffset(var));
			result.addNode(ctx);
		}
	}

	@Override
	public void exitArrayDecl(ArrayDeclContext ctx) {
		String var = ctx.ID().getText();
		Scope curScope = scopes.get(scopes.size()-1);
		if(curScope.getType(var) != null){
			addError(String.format("Variable '%s' already declared in this scope!", var));
		}else{
			Array lhsType = new Array(Type.get(ctx.type().getText()));
			lhsType.setLength(Integer.parseInt(ctx.NUM().getText()));
			if (ctx.expr() != null) {
				checkType(ctx.expr(), lhsType);
			}
			types.put(ctx, lhsType);
			curScope.put(var, lhsType);
			result.addType(ctx, lhsType);
			result.addOffset(ctx, curScope.getOffset(var));
			result.addNode(ctx);
		}
	}



	@Override
	public void exitIdExpr(IdExprContext ctx)
	{
		Scope curScope = scopes.get(scopes.size() - 1);
		if(!curScope.exists(ctx.getText())){
			//TODO do something with this
			//addError("Variable " + ctx + "is not initialized.");
		}
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

	private boolean hasErrors() {
		return this.errors.size() > 0;
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
			addError("Not declared: " + ctx.getText());
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
