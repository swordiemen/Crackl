package analysis;

public class Type {

	public enum Types {
		Bool, Int
	}

	public static final Type BOOL = new Type(Types.Bool);
	public static final Type INT = new Type(Types.Int);

	public Types type;

	public Type(Types t) {
		this.type = t;
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

			default:
				throw new IllegalArgumentException(text);
			}
	}

}