package analysis;

import java.util.Map;

public class Type {

	public enum Types {
		Bool, Int, Err, Array, Pointer, Void, Text, Lock
	}

	public Map<Types, Integer> sizeMap;
	public static final Type BOOL = new Type(Types.Bool);
	public static final Type INT = new Type(Types.Int);
	public static final Type ERR = new Type(Types.Err);
	public static final Type VOID = new Type(Types.Void);
	public static final Type TEXT = new Type(Types.Text);
	public static final Type LOCK = new Type(Types.Lock);

	public Types type;

	/**
	 * Creates a new Type, given a Types t.
	 * 
	 * @param t
	 *            The Types of this Type.
	 */
	public Type(Types t) {
		this.type = t;
	}

	/**
	 * Returns a new Type object of this Type.
	 * 
	 * @return <code>this</code> if this Type is not an array/pointer. Otherwise, it will return the type the pointer points to, or the
	 *         types of the elements of the array.
	 */
	public Type getTypeObj()
	{
		return this;
	}

	/**
	 * Returns the Types type of this Type.
	 * 
	 * @return type The Types of this Type.
	 */
	public Types getType()
	{
		return type;
	}

	/**
	 * Returns the size of this Type.
	 * 
	 * @return <b>size</b> The size of this Type.
	 */
	public int getSize()
	{
		switch (this.type)
			{
			case Bool:
				// return Integer.SIZE/Byte.SIZE;
				return 1;
			case Int:
				// return Integer.SIZE/Byte.SIZE;
				return 1;
			case Err:
				return 0;
			case Text:
				return 1;
			case Lock:
				return 1;
			default:
				throw new IllegalArgumentException("Cannot get the size of an unknown type.");
			}
	}

	@Override
	public boolean equals(Object obj)
	{
		boolean result = obj instanceof Type;
		result &= ((Type) obj).getType().equals(getType());
		if (this.isArray()) {
			result &= ((Array) this).getTypeObj().equals(((Array) obj).getTypeObj());
		}
		if (this.isPointer()) {
			result &= ((Pointer) this).getTypeObj().equals(((Pointer) obj).getTypeObj());
		}
		return result;
	}

	/**
	 * Returns true if this is an Array.
	 * 
	 * @return bool Whether this is an array.
	 */
	public boolean isArray()
	{
		return type == Types.Array;
	}

	/**
	 * Returns true if this is a pointer.
	 * 
	 * @return bool Whether this is a pointer.
	 */
	public boolean isPointer()
	{
		return type == Types.Pointer;
	}

	/**
	 * Returns a new Type from a String.
	 * 
	 * @param type
	 * @return
	 */
	public static Type get(String type)
	{
		String PTRTYPE = "#";
		if (type.startsWith(PTRTYPE))
			;
		switch (type)
			{
			case "boolean":
				return BOOL;

			case "int":
				return INT;

			case "err":
				return ERR;

			case "void":
				return VOID;

			case "text":
				return TEXT;

			default:
				throw new IllegalArgumentException(type);
			}
	}

	public String toString()
	{
		switch (this.type)
			{
			case Bool:
				return "boolean";
			case Int:
				return "integer";
			case Err:
				return "invalid type";
			case Void:
				return "void";
			case Text:
				return "text";
			default:
				return "Cannot get the toString() of unexpected type " + this.type;
			}
	}

	// public void setArray(Array array) {
	// this.array = array;
	// }

	// public Array getArray(){
	// return array;
	// }

}