package analysis;

public class Array extends Type {
	
	private int size;
	private int length;
	private Type type;

	public Array(Types t) {
		super(Types.Array);
		this.type = new Type(t);
	}
	
	public Array(Type t){
		super(Types.Array);
		this.type = t;
	}
	
	public void setType(Type t){
		type = t;
	}
	
	public Type getTypeObj(){
		return type;
	}
	
	
	/**
	 * Returns true if <code>obj</code> is an array with the same content types and length.
	 */
	public boolean equals(Object obj){
		return obj instanceof Array && ((Array) obj).getLength() == this.getLength() && ((Array) obj).getTypeObj().equals(this.getTypeObj());
	}
	
	public void setLength(int length){
		this.length = length;
		this.size = length * type.getSize();
	}
	
	public int getLength(){
		return length;
	}
	
	@Override
	public boolean isArray(){
		return true;
	}
	
	@Override
	public int getSize() {
		//return Integer.SIZE/Byte.SIZE;
		return 1;
	}
	
	public String toString(){
		return "["+type.toString()+"]";
	}

	public void setSize(int si) {
		size = si;
		length = size/type.getSize();
	}

}
