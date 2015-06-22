package analysis;

import java.util.Map;

public class Type {

	public enum Types {
		Bool, Int, Err
	}
	
	
	public Map<Types, Integer> sizeMap;
	public static final Type BOOL = new Type(Types.Bool);
	public static final Type INT = new Type(Types.Int);
	public static final Type ERR = new Type(Types.Bool);

	public Types type;

	public Type(Types t) {
		this.type = t;
	}
	
	public int getSize(){
		switch(this.type){
		case Bool:
			return Integer.SIZE/Byte.SIZE;
		case Int:
			return Integer.SIZE/Byte.SIZE;
		case Err:
			return 0;
		default:
			throw new IllegalArgumentException("Cannot get the size of an unknown type.");
		}
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof Type && ((Type) obj).type.equals(this.type);
	}

	public static Type get(String text)
	{
		switch (text)
			{
			case "boolean":
				return BOOL;

			case "int":
				return INT;
				
			case "err":
				return ERR;

			default:
				throw new IllegalArgumentException(text);
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

}