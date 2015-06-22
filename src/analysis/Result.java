package analysis;

import java.util.HashSet;
import java.util.Set;

import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.*;

public class Result {
	private Set<ParserRuleContext> nodes;	// a tool for iteration
	private ParseTreeProperty<Type> types;
	private ParseTreeProperty<Integer> offsets;
	private ParseTreeProperty<Scope> scopes;
	
	public Result(){
		nodes = new HashSet<ParserRuleContext>();
		types = new ParseTreeProperty<Type>();
		offsets = new ParseTreeProperty<Integer>();
		scopes = new ParseTreeProperty<Scope>();
	}
	
	public ParseTreeProperty<Type> getTypes(){
		return types;
	}
	
	public ParseTreeProperty<Integer> getOffsets(){
		return offsets;
	}
	
	public ParseTreeProperty<Scope> getScopes(){
		return scopes;
	}
	
	public void addScope(ParserRuleContext ctx, Scope scope){
		scopes.put(ctx, scope);
	}
	
	public Scope getScope(ParserRuleContext ctx){
		return scopes.get(ctx);
	}
	
	public void addNode(ParserRuleContext ctx){
		nodes.add(ctx);
	}
	
	public Set<ParserRuleContext> getNodes(){
		return nodes;
	}
	
	public void addType(ParserRuleContext ctx, Type type){
		types.put(ctx, type);
	}
	
	public void addOffset(ParserRuleContext ctx, int offset){
		offsets.put(ctx, offset);
	}
	
	public int getOffset(ParserRuleContext ctx){
		return offsets.get(ctx);
	}
	
	public Type getType(ParserRuleContext ctx){
		return types.get(ctx);
	}
}
