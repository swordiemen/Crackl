package machine;

import java.util.EnumSet;


public class Op {

	public enum Instruction {

		//LocalInsruction
		Const,
		Compute,
		Load,
		Store,
		Branch,
		Jump,
		Push,
		Pop,
		Nop,
		EndProg,

		//SysInstruction
		Read,
		Receive,
		Write,

		//DebugInstruction
		Debug;
	}
	
	public enum Register {
		RegReserved, //to reserve space on the stack
		Zero,
		PC,
		SP,
		SPID,
		RegA,
		RegB,
		RegC,
		RegD,
		RegE
	}
	
	public static final EnumSet<Register> gpRegisters = EnumSet.of(Register.RegA, Register.RegB, Register.RegC, Register.RegD, Register.RegE); 

	public enum Target {
		Abs,
		Rel,
		Ind
	}
	
	public enum MemAddr {
		Addr,
		Deref
	}
	
	public enum Operator {
		Add,
		Sub,
		Mul,
		Div,
		And,
		Or,
		Equal,
		NEq,
		Mod,
		Gt,
		Lt,
		GtE,
		LtE,
		Xor,
		LShift,
		RShift
	}

}
