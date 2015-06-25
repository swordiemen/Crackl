package analysis;

import java.util.Map;

public class Type {

	public enum Types {
		Bool, Int, Err, Array, Pointer
	}
	
	
	public Map<Types, Integer> sizeMap;
	public static final Type BOOL = new Type(Types.Bool);
	public static final Type INT = new Type(Types.Int);
	public static final Type ERR = new Type(Types.Bool);
	//protected Array array;

	public Types type;

	public Type(Types t) {
		this.type = t;
	}
	public Type getTypeObj(){
		return this;
	}
	
	public Types getType(){
		return type;
	}
	
	/**
	 * Returns the size of this Type.
	 * @return <b>size</b> The size of this Type.
	 */
	public int getSize(){
		switch(this.type){
		case Bool:
//			return Integer.SIZE/Byte.SIZE;
			return 1;
		case Int:
//			return Integer.SIZE/Byte.SIZE;
			return 1;
//		case Array:
//			return array.getSize();
		case Err:
			return 0;
		default:
			throw new IllegalArgumentException("Cannot get the size of an unknown type.");
		}
	}

	@Override
	public boolean equals(Object obj)
	{	
		return obj instanceof Type &&
				((Type) obj).type.equals(this.type) && 
				(isArray() == ((Type) obj).isArray()) &&
				(isPointer() == ((Type) obj).isPointer());	
	}

	public boolean isArray(){
		return type == Types.Array;
	}
	
	public boolean isPointer(){
		return type == Types.Pointer;
	}
	
	/**
	 * Returns a new Type from a String.
	 * @param type
	 * @return
	 */
	public static Type get(String type)
	{
		switch (type)
			{
			case "boolean":
				return BOOL;

			case "int":
				return INT;
				
			case "err":
				return ERR;

			default:
				throw new IllegalArgumentException(type);
			}
	}
	
	public String toString(){
		switch(this.type){
		case Bool:
			return "boolean";
		case Int:
			return "integer";
		case Err:
			return "Invalid type";
		default:
			return "Error in toString() of Type.java";
		}
	}
	
//	public void setArray(Array array) {
//		this.array = array;
//	}
	
//	public Array getArray(){
//		return array;
//	}

}