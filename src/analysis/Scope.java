package analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scope {
	private int size;
	private int globSize;
	private Map<String, Type> types;
	private Map<String, Integer> offsets;
	private Map<String, Integer> globalOffsets;
	private Scope prevScope;
	private ArrayList<String> initVars;
	private Map<String, Boolean> varToIsGlobal;

	/**
	 * Creates a new Scope with a given previous scope.
	 * @param s The scope which is one level above this (null if this is the top scope).
	 */
	public Scope(Scope s){
		types = new HashMap<String, Type>();
		offsets = new HashMap<String, Integer>();
		initVars = new ArrayList<String>();
		varToIsGlobal = new HashMap<String, Boolean>();
		globalOffsets = new HashMap<String, Integer>();
		prevScope = s;
		size = 0;
		globSize = 0;
	}

	/**
	 * Returns the Scope which is above this level.
	 * @return <b>prevScope</b> The Scope which is one level above this Scope, or null if this is the top Scope.
	 */
	public Scope getScope(){
		return prevScope;
	}

	/**
	 * Returns the list of initialized variables of this scope.
	 * @return initVars
	 */
	public ArrayList<String> getInitVars(){
		return initVars;
	}

	/**
	 * Returns true if <code>var</code> is initialized in this scope.
	 * @param var The variable to be checked.
	 * @return bool 
	 */
	public boolean isInitialized(String var){
		return initVars.contains(var);
	}

	/**
	 * Adds an initialized variable to this Scope.
	 * @param var The variable to be added.
	 */
	public void addInitVar(String var){
		initVars.add(var);
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
	 * @param glob Whether the variable is global.
	 */
	public void put(String var, Type type, boolean glob){
		if(!exists(var)){
			types.put(var, type);
			if(glob){
				globalOffsets.put(var, globSize);
				globSize += type.getSize();
			}else{
				offsets.put(var, size);
				size += type.getSize();
			}
			varToIsGlobal.put(var, glob);
		}else{
			System.out.println("Already exists "+var);
		}
	}

	/**
	 * Returns the address of this scope relative to address 0.
	 * @return <b>baseAddress</b> The baseAddress of this Scope.
	 */
	public int getBaseAddress(){
		if(prevScope == null){ //top level Scope
			return 0;
		}
		return prevScope.getLocalSize() + prevScope.getBaseAddress();
	}

	public int getGlobalBaseAddress(){
		if(prevScope == null){
			return 0;
		}
		return prevScope.getGlobalSize() + prevScope.getGlobalBaseAddress();
	}

	/**
	 * Returns the size of this Scope.
	 * @return <b>size</b> The size of this Scope.
	 */
	private int getLocalSize() {
		return size;
	}

	public int getGlobalSize(){
		return globSize;
	}

	/**
	 * Returns the type of a variable, or <i>null</i> if it doesn't exist in this Scope.
	 * @param var The variable whose type wants to be known.
	 * @return <b>type</b> The type of the variable.
	 */
	public Type getType(String var){
		Type type = types.get(var);
		return type;
	}

	/**
	 * Returns the offset of a variable, or <i>null</i> if it doesn't exist in this Scope.
	 * @param var The variable whose type wants to be known.
	 * @return <b>offset</b> The offset of the variable.
	 */
	public Integer getOffset(String var){	//Integer instead of int, since it can return null.
		int res = -1;
		if(isGlobal(var)){
			res = globalOffsets.get(var);
		}else{
			res = offsets.get(var);
		}
		return res;
	}

	public Boolean isGlobal(String var){
		if(varToIsGlobal.containsKey(var)){
			return varToIsGlobal.get(var);
		}else{
			return null;
		}
	}

	/**
	 * Returns the MemoryLocation of a variable. If it isn't found in this Scope, continues
	 * searching in the previous Scope. If prevScope is null, and the variable isn't found, returns <i>null</i>.
	 * @param var The variable whose MemoryLocation wants to be known.
	 * @return <b>memLoc</b> The MemoryLocation of the variable.
	 */
	public MemoryLocation getMemLoc(String var){
		MemoryLocation memLoc = null;
		boolean global = isGlobal(var);
		Integer offset;
		if(global){
			offset = globalOffsets.get(var);
		}else{
			offset = offsets.get(var);
		}
		if(offset != null){
			memLoc = new MemoryLocation(this, getOffset(var), isGlobal(var));
		}else if(prevScope == null && offset == null){
			return null;
		}else{
			return prevScope.getMemLoc(var);
		}
		return memLoc;
	}

}
