package analysis;

import grammar.CracklBaseListener;
import grammar.CracklParser.AddExprContext;
import grammar.CracklParser.AndExprContext;
import grammar.CracklParser.AssignStatContext;
import grammar.CracklParser.BlockStatContext;
import grammar.CracklParser.CompExprContext;
import grammar.CracklParser.ConstBoolExprContext;
import grammar.CracklParser.ConstNumExprContext;
import grammar.CracklParser.DeclContext;
import grammar.CracklParser.FieldExprContext;
import grammar.CracklParser.FuncCallContext;
import grammar.CracklParser.FuncContext;
import grammar.CracklParser.FuncExprContext;
import grammar.CracklParser.FuncStatContext;
import grammar.CracklParser.IdExprContext;
import grammar.CracklParser.IfStatContext;
import grammar.CracklParser.NotExprContext;
import grammar.CracklParser.OrExprContext;
import grammar.CracklParser.ParExprContext;
import grammar.CracklParser.PrintStatContext;
import grammar.CracklParser.ProgramContext;
import grammar.CracklParser.RetContext;
import grammar.CracklParser.TargetContext;

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
	
	public TypeChecker(){
		types = new ParseTreeProperty<Type>();
		errors = new ArrayList<String>();
		initializedVars = new ArrayList<ParserRuleContext>();
		scopes = new ArrayList<Scope>();
	}
	
	@Override
	public void enterProgram(ProgramContext ctx) {
		scopes.add(new Scope(null));
	}
	
	@Override
	public void exitBlockStat(BlockStatContext ctx) {
		scopes.remove(scopes.size() - 1);
	}
	
	@Override
	public void enterBlockStat(BlockStatContext ctx) {
		Scope lastScope = scopes.get(scopes.size() - 1); 
		Scope scope = new Scope(lastScope);
		scopes.add(scope);
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
	public void exitPrintStat(PrintStatContext ctx) {
		String res = ctx.STRING().getText();
		for(TerminalNode prc : ctx.ID()){
			res += ", " + prc.getText() + " : " + getTypeByString(prc.getText());
		}
		System.out.println(res);
	}

	@Override
	public void exitDecl(DeclContext ctx)
	{
		Type lhsType = Type.get(ctx.type().getText());
		if (ctx.expr() != null) {
			checkType(ctx.expr(), lhsType);
		}
		types.put(ctx, lhsType);
		Scope curScope = scopes.get(scopes.size()-1);
		curScope.put(ctx.ID().getText(), lhsType);
	}
	
	@Override
	public void exitIdExpr(IdExprContext ctx)
	{
		if(!initializedVars.contains(ctx)){
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
				System.out.println(error);
			}
		}else{
			System.out.println("Build succeeded without typecheck errors.");
		}
	}
	
	private boolean hasErrors() {
		return this.errors.size() > 0;
	}

	@Override
	public void exitNotExpr(NotExprContext ctx) {
		if(checkType(ctx.expr(), Type.BOOL)){
			types.put(ctx, Type.BOOL);
		}else{
			types.put(ctx, Type.ERR);
		}
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
		}else{
			types.put(ctx, Type.ERR);
		}
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
		}else{
			types.put(ctx, Type.ERR);
		}
	}
	
	@Override
	public void exitFuncStat(FuncStatContext ctx) {
		types.put(ctx, getType(ctx.func()));
	}
	
	@Override
	public void exitConstBoolExpr(ConstBoolExprContext ctx) {
		types.put(ctx, Type.BOOL);
	}
	
	@Override
	public void exitConstNumExpr(ConstNumExprContext ctx) {
		types.put(ctx, Type.INT);
	}
	
	@Override
	public void exitCompExpr(CompExprContext ctx) {
		if(checkType(ctx.expr(0), Type.INT) && checkType(ctx.expr(1), Type.INT)){
			types.put(ctx, Type.BOOL);
		}else{
			types.put(ctx, Type.ERR);
		}
	}
	
	@Override
	public void exitParExpr(ParExprContext ctx) {
		types.put(ctx, getType(ctx.expr()));
	}
	
	@Override
	public void exitOrExpr(OrExprContext ctx) {
		if(checkType(ctx.expr(0), Type.BOOL) && checkType(ctx.expr(1), Type.BOOL)){
			types.put(ctx, Type.BOOL);
		}else{
			types.put(ctx, Type.ERR);
		}
	}

	private boolean checkType(RuleContext ctx, Type expected)
	{
		Type type = getType(ctx);
		if (!type.equals(expected)) {
			addError(ctx, "Expected type " + expected + ", got " + type);
			return false;
		}
		return true;
	}

	private Type getType(RuleContext ctx)
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
		return type;
	}
	
	private Type getTypeByString(String var)
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

	private void addError(String s)
	{
		errors.add(s);
	}

	private void addError(RuleContext ctx, String error)
	{
		Token start = ((ParserRuleContext) ctx).start;
		String pos = start.getLine() + ":" + start.getCharPositionInLine();
		addError(String.format("%s (%s)", error, pos));
	}

}
