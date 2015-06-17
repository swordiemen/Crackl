package analysis;

import grammar.CracklBaseListener;
import grammar.CracklParser.AddExprContext;
import grammar.CracklParser.AndExprContext;
import grammar.CracklParser.AssignStatContext;
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
import grammar.CracklParser.NotExprContext;
import grammar.CracklParser.OrExprContext;
import grammar.CracklParser.ParExprContext;
import grammar.CracklParser.ProgramContext;
import grammar.CracklParser.RetContext;
import grammar.CracklParser.TargetContext;

import java.util.ArrayList;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class TypeChecker extends CracklBaseListener {

	ParseTreeProperty<Type> types = new ParseTreeProperty<Type>();
	ArrayList<String> errors = new ArrayList<String>();
	ArrayList<ParserRuleContext> initializedVars = new ArrayList<ParserRuleContext>();

	@Override
	public void exitAssignStat(AssignStatContext ctx)
	{
		Type lhsType = Type.get(ctx.target().getText());
		checkType(ctx.expr(), lhsType);
		initializedVars.add(ctx);
	}

	@Override
	public void exitDecl(DeclContext ctx)
	{
		Type lhsType = Type.get(ctx.type().getText());
		if (ctx.expr() != null) {
			checkType(ctx.expr(), lhsType);
		}
		types.put(ctx, lhsType);
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
		System.out.println(types);
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
		Type type = types.get(ctx);
		if (type == null) {
			addError("Not declared" + ctx);
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
