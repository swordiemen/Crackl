package generation;

import machine.Op;
import machine.Operand;

public class Line {
	
	Op.Instruction instruction;
	Operand[] operands;

	public Line(Op.Instruction instruction, Operand... operands)
	{
		this.instruction = instruction;
		this.operands = operands;
	}

}
