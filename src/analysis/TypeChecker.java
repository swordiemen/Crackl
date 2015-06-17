package analysis;

import grammar.CracklBaseListener;
import grammar.CracklParser.AssignStatContext;
import grammar.CracklParser.DeclContext;
import grammar.CracklParser.FuncContext;
import grammar.CracklParser.IdExprContext;
import grammar.CracklParser.RetContext;

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
	public void exitRet(RetContext ctx)
	{
		types.put(ctx, types.get(ctx.expr()));
	}

	private void checkType(RuleContext ctx, Type expected)
	{
		Type type = getType(ctx);
		if (!type.equals(expected)) {
			addError(ctx, "Expected type " + expected + ", got " + type);
		}
	}

	private Type getType(RuleContext ctx)
	{
		Type type = types.get(ctx);
		if (type == null) {
			addError("Not declared" + ctx);
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
