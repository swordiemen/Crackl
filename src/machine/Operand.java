package machine;


public class Operand {
	

	public static enum Type {
		Reg,
		Memaddr,
		Target,
		Operator,
		Const,

		//Jumps (absolute, relative, indirect/register)
		Abs,
		Rel,
		Ind, Addr, Deref;
	}
	
	public String name;
	public Type type;

	public Operand(Type t, String n) {
		this.name = n;
		this.type = t;
	}

	public Operand(Type t) {
		this(t, "unnamed_operand");
	}

	public String toDebugString()
	{
		return String.format("Operand (type=%s, name=%s", type.toString(), name);
	}

	@Override
	public String toString()
	{
		return name;
	}

	public static class Operator extends Operand {

		public final Op.Operator operator;

		public Operator(Op.Operator operator) {
			super(Type.Operator, operator.toString());
			this.operator = operator;
		}
	}

	public abstract static class Target extends Operand {

		public Target(Type t, String val) {
			super(t, val);
		}

		@Override
		public String toString()
		{
			return String.format("(%s %s)", this.type, this.name);
		}

		public static class Abs extends Target {

			public Abs(int line) {
				super(Type.Abs, "" + line);
			}
		}

		public static class Rel extends Target {
			public Rel(int rel) {
				super(Type.Rel, "" + rel);
			}
		}

		public static class Ind extends Target {
			public Ind(Reg reg) {
				super(Type.Ind, "" + reg);
			}
		}

	}

	public static class Reg extends Operand {

		public final Op.Register reg;

		public Reg(Op.Register reg) {
			super(Type.Reg, reg.toString());
			this.reg = reg;
		}

		@Override
		public String toString()
		{
			if (name.equals(Op.Register.RegReserved.toString())) {
				return Op.Register.Zero.toString();
			}
			else {
				return reg.toString();
			}
		}
	}

	public static class Const extends Operand {
		public final int value;

		public Const(int v) {
			super(Type.Const, Integer.toString(v));
			value = v;
		}

		public Const(boolean b) {
			super(Type.Const, Integer.toString(b ? 1 : 0));
			value = b ? 1 : 0;
		}
	}

	/**
	 * Absolute Memory location
	 *
	 */
	public static class MemAddr extends Operand {

		public static final MemAddr StdIO = new MemAddr(0x1000000);
		public final int absAddress;

		public MemAddr(int absolute) {
			super(Type.Memaddr, String.format("(Addr %d)", absolute));
			this.absAddress = absolute;
		}

		@Override
		public String toString()
		{
			String result = String.format("(Addr %d)", absAddress);
			return result;
		}
	}

	public static class Addr extends Operand {

		public final int absAddress;

		public Addr(int base, int offset) {
			super(Type.Addr, String.format("(Addr %d)", base + offset));
			this.absAddress = base + offset;
		}

		@Override
		public String toString()
		{
			String result = String.format("(Addr %d)", absAddress);
			return result;
		}
	}

	public static class Deref extends Operand {

		public final Reg reg;

		public Deref(Reg reg) {
			super(Type.Deref, String.format("(Deref (%s))", reg.name));
			this.reg = reg;
		}

		@Override
		public String toString()
		{
			String result = String.format("(Deref (%s))", reg.name);
			return result;
		}
	}

}
