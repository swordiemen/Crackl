package analysis;

public class MemoryLocation {
	private Scope scope;
	private int scopeOffset;
	private int varOffset;
	
	/**
	 * Creates a new MemoryLocation with default values.
	 */
	public MemoryLocation(){
		this(new Scope(null), -1);
	}
	
	/**
	 * Creates a new MemoryLocation of a variable with a specified scope, scopeOffset and varOffset
	 * @param s Scope where this variable lives in.
	 * @param vo The offset of the variable relative to the scope.
	 */
	public MemoryLocation(Scope s, int vo){
		setScope(s);
		scopeOffset = s.getBaseAddress();
		setVarOffset(vo);
	}
	
	/**
	 * Returns the scope of this MemoryLocation.
	 * @return scope
	 */
	public Scope getScope() {
		return scope;
	}

	/**
	 * Sets the scope of this MemoryLocation.
	 * @param scope The scope that this MemoryLocation should have.
	 */
	public void setScope(Scope scope) {
		this.scope = scope;
		this.scopeOffset = scope.getBaseAddress();
	}

	/**
	 * Returns the scope offset of this MemoryLocation.
	 * @return scopeOffset.
	 */
	public int getScopeOffset() {
		return scopeOffset;
	}

	/**
	 * Sets the scope of this MemoryLocation.
	 * However, this should not be used, as the scopeOffset is automatically
	 * changed whenever the scope is changed. 
	 * @param scope The scope that this MemoryLocation should have.
	 */
	public void setScopeOffset(int scopeOffset) {
		this.scopeOffset = scopeOffset;
	}

	/**
	 * Returns the offset of the variable's location relative to the scopeOffset.
	 * @return varOffset
	 */
	public int getVarOffset() {
		return varOffset;
	}

	/**
	 * Sets the variableOffset of this memoryLocation, in case a variable has been moved in the memory.
	 * @param varOffset
	 */
	public void setVarOffset(int varOffset) {
		this.varOffset = varOffset;
	}
}
