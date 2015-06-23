package machine;

public class Operand {
	
	public static enum Type {
		Reg,
		Memaddr,
		Target,
		Operator,
		Const;
	}
	
	public String name;
	public Type type;
	
	public Operand(Type t, String n)
	{
		this.name = n;
		this.type = t;
	}
	
	public Operand(Type t) {
		this(t, "unnamed_operand");
	}
	
	@Override
	public String toString()
	{
		return String.format("Operand (type=%s, name=%s", type.toString(), name);
	}

	public static class Operator extends Operand{
		
		public final Op.Operator operator;

		public Operator(Op.Operator operator)
		{
			super(Type.Reg, operator.toString());
			this.operator = operator;
		}
	}
	
	public static class Reg extends Operand{
		
		public final Op.Register reg;

		public Reg(Op.Register reg)
		{
			super(Type.Reg, reg.toString());
			this.reg = reg;
		}
	}
	
	public static class Const extends Operand
	{
		public final int value;
		
		public Const(int v)
		{
			super(Type.Const, Integer.toString(v));
			value = v;
		}
		
		public Const(boolean b)
		{
			super(Type.Const, Boolean.toString(b));
			value = b ? 1 : 0;
		}
	}

	/**
	 * Absolute Memory location
	 * @author willem
	 *
	 */
	public static class MemAddr extends Operand{
		
		public static final MemAddr StdIO = new MemAddr(0, 0x1000000);
		public final int absAddress;

		public MemAddr(int base, int offset)
		{
			super(Type.Memaddr, "@"+(base+offset));
			this.absAddress = base+offset;
		}
	}
}

