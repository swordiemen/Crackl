package analysis;

import java.util.HashMap;
import java.util.Map;

public class Scope {
	private int size;
	private Map<String, Type> types;
	private Map<String, Integer> offsets;
	private Scope prevScope;

	/**
	 * Creates a new Scope with a given previous scope.
	 * @param s The scope which is one level above this (null if this is the top scope).
	 */
	public Scope(Scope s){
		types = new HashMap<String, Type>();
		offsets = new HashMap<String, Integer>();
		prevScope = s;
	}

	/**
	 * Returns the Scope which is above this level.
	 * @return <b>prevScope</b> The Scope which is one level above this Scope, or null if this is the top Scope.
	 */
	public Scope getScope(){
		return prevScope;
	}

	/**
	 * Checks if a certain variable exists in this Scope.
	 * @param var The variable to be checked.
	 * @return <b>exists</b> Whether this variable exists.
	 */
	public boolean exists(String var){
		return types.containsKey(var);
	}

	/**
	 * Puts a new variable in this scope. If it already exists, does nothing (should not happen, is managed by TypeChecker).
	 * @param var The variable to be added.
	 * @param type The type of the variable.
	 */
	public void put(String var, Type type){
		if(exists(var)){
			types.put(var, type);
			offsets.put(var, size);
			size += type.getSize();
		}
	}

	/**
	 * Returns the address of this scope relative to address 0.
	 * @return <b>baseAddress</b> The baseAddress of this Scope.
	 */
	public int getBaseAddress(){
		if(prevScope == null){
			return 0;
		}
		return prevScope.getSize() + prevScope.getBaseAddress();
	}

	/**
	 * Returns the size of this Scope.
	 * @return <b>size</b> The size of this Scope.
	 */
	private int getSize() {
		return size;
	}

	/**
	 * Returns the type of a variable, or <i>null</i> if it doesn't exist in this Scope.
	 * @param var The variable whose type wants to be known.
	 * @return <b>type</b> The type of the variable.
	 */
	public Type getType(String var){
		Type type = types.get(var);
//		if(type != null){
//			return type;
//		}else if(prevScope == null && type == null){
//			return null;
//		}else{
//			return prevScope.getType(var)
//		}
		return type;
	}

	/**
	 * Returns the offset of a variable, or <i>null</i> if it doesn't exist in this Scope.
	 * @param var The variable whose type wants to be known.
	 * @return <b>offset</b> The offset of the variable.
	 */
	public Integer getOffset(String var){	//Integer instead of int, since it can return null.
		return offsets.get(var);
	}

	/**
	 * Returns the MemoryLocation of a variable. If it isn't found in this Scope, continues
	 * searching in the previous Scope. If prevScope is null, and the variable isn't found, returns <i>null</i>.
	 * @param var The variable whose MemoryLocation wants to be known.
	 * @return <b>memLoc</b> The MemoryLocation of the variable.
	 */
	public MemoryLocation getMemLoc(String var){
		MemoryLocation memLoc = null;
		Integer offset = offsets.get(var);
		if(offset != null){
			memLoc = new MemoryLocation(this, getOffset(var));
		}else if(prevScope == null && offset == null){
			return null;
		}else{
			return prevScope.getMemLoc(var);
		}
		return memLoc;
	}
}
