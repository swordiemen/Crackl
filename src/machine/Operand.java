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

	public class Operator extends Operand{
		
		public final Op.Operator operator;

		public Operator(Op.Operator operator)
		{
			super(Type.Reg, operator.toString());
			this.operator = operator;
		}
	}
	
	public class Reg extends Operand{
		
		public final Op.Reg reg;

		public Reg(Op.Reg reg)
		{
			super(Type.Reg, reg.toString());
			this.reg = reg;
		}
	}

	/**
	 * Absolute Memory location
	 * @author willem
	 *
	 */
	public class MemAddr extends Operand{
		
		public final int absAddress;

		public MemAddr(int base, int offset)
		{
			super(Type.Memaddr, "@"+(base+offset));
			this.absAddress = base+offset;
		}
	}
}

