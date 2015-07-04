package analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class Result {
	private Set<ParserRuleContext> nodes;	// a tool for iteration
	private ParseTreeProperty<Type> types;
	private ParseTreeProperty<Integer> offsets;
	private ParseTreeProperty<Scope> scopes;
	public int numberOfSprockells = 1; //how many sprockells the program will run on (default: 1)
	private HashMap<String, Integer> staticGlobals; //variables and offsets of variables such as locks, which should have a static position in global memory
	public int numberOfStaticGlobals = 0;
	
	/**
	 * Creates a new Result class.
	 */
	public Result(){
		nodes = new HashSet<ParserRuleContext>();
		types = new ParseTreeProperty<Type>();
		offsets = new ParseTreeProperty<Integer>();
		scopes = new ParseTreeProperty<Scope>();
		staticGlobals = new HashMap<String, Integer>();

		addStaticGlobal("threadsReleasedJump"); //instruction where the sprockells will jump to. While this is 0 they will wait.
		addStaticGlobal("unjoinedThreads"); //how many threads haven't reached join() yet
		addStaticGlobal("unjoinedThreadsLock"); //lock on the 'unjoinedTheads' counter
	}
	
	/**
	 * Returns a ParseTreeProperty, with Contexts mapped to Types (if applicable).
	 * @return types
	 */
	public ParseTreeProperty<Type> getTypes(){
		return types;
	}
	
	/**
	 * Returns a ParseTreeProperty, with Contexts mapped to their offsets (if applicable).
	 * @return
	 */
	public ParseTreeProperty<Integer> getOffsets(){
		return offsets;
	}
	
	/**
	 * Returns a ParseTreeProperty, with Contexts mapped to Scopes (if applicable).
	 * @return
	 */
	public ParseTreeProperty<Scope> getScopes(){
		return scopes;
	}
	
	/**
	 * Adds a scope to this result.
	 * @param ctx The (Block)context of the scope.
	 * @param scope The scope to be added.
	 */
	public void addScope(ParserRuleContext ctx, Scope scope){
		scopes.put(ctx, scope);
	}
	
	/**
	 * Returns the scope of a certain (Block)Context
	 * @param ctx The context of which the Scope wants to be known.
	 * @return <b>scope</b> The scope belonging to the ctx.
	 */
	public Scope getScope(ParserRuleContext ctx){
		return scopes.get(ctx);
	}
	
	/**
	 * Adds a context to this result's list of contexts.
	 * @param ctx
	 */
	public void addNode(ParserRuleContext ctx){
		nodes.add(ctx);
	}
	
	/**
	 * Returns a list of nodes that this Result knows of.
	 * @return <b>nodes</b> The list of nodes.
	 */
	public Set<ParserRuleContext> getNodes(){
		return nodes;
	}
	
	/**
	 * Adds a context's type (if applicable) to this Result's type map.
	 * @param ctx The context of which the type is to be added.
	 * @param type The type of the context (variable, expression).
	 */
	public void addType(ParserRuleContext ctx, Type type){
		types.put(ctx, type);
	}
	
	/**
	 * Adds a context's offset (if applicable) to this Result's offset map.
	 * @param ctx The context of which the offset is to be added.
	 * @param offset The offset of the context (variable).
	 */
	public void addOffset(ParserRuleContext ctx, int offset){
		offsets.put(ctx, offset);
	}
	
	/**
	 * Returns a offset given a context.
	 * @param ctx The context of which the offset wants to be known.
	 * @return <b>offset</b> The offset of the context.
	 */
	public int getOffset(ParserRuleContext ctx){
		return offsets.get(ctx);
	}
	
	public void addStaticGlobal(String var){
		staticGlobals.put(var, numberOfStaticGlobals++);
	}
	
	/**
	 * Returns a type given a context.
	 * @param ctx The context of which the type wants to be known.
	 * @return <b>type</b> The offset of the context.
	 */
	public Type getType(ParserRuleContext ctx){
		return types.get(ctx);
	}

	public HashMap<String, Integer> getStaticGlobals()
	{
		return this.staticGlobals;
	}
}
