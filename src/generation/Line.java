package generation;

import machine.Op;
import machine.Operand;

/**
 * Line models a line of Sprockell code, containing the Instruction that comes at this line, and also it's operands.
 */
public class Line {

	Op.Instruction instruction;
	Operand[] operands;

	boolean isDebug = false;
	String debugText;

	public Line(Op.Instruction instruction, Operand... operands) {
		this.instruction = instruction;
		this.operands = operands;
	}

	public Line(String s) {
		isDebug = true;
		debugText = s;
	}

	@Override
	public String toString()
	{
		if (isDebug) {
			return String.format("Debug: %s\n", debugText);
		}
		else {
			StringBuilder b = new StringBuilder(instruction.toString());
			for (Operand operand : operands) {
				b.append(" ");
				b.append(operand.toString());
			}
			return b.toString();
		}
	}

}
