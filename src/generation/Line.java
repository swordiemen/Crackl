package generation;

import machine.Op;
import machine.Operand;

public class Line {
	
	Op.Instruction instruction;
	Operand[] operands;

	boolean isDebug = false;
	String debugText;

	public Line(Op.Instruction instruction, Operand... operands)
	{
		this.instruction = instruction;
		this.operands = operands;
	}
	
	public Line(String s)
	{
		isDebug = true;
		debugText = s;
	}

}
