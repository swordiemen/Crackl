package machine;

import java.util.EnumSet;


public class Op {

	public static final EnumSet<Register> gpRegisters = EnumSet.of(Register.RegA, Register.RegB, Register.RegC, Register.RegD, Register.RegE); 

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
		TestAndSet,

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
	
	public static Operator getOperatorByString(String operator){
		switch(operator){
			case "+": return Operator.Add;
			case "-": return Operator.Sub;
			case "%": return Operator.Mod;
			case "/": return Operator.Div;
			case "*": return Operator.Mul;
			default: throw new IllegalArgumentException("No such operator: "+operator);
		}
	}

}
