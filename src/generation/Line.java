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
	
	@Override
	public String toString()
	{
		if(isDebug)
		{
			return String.format("Debug: %s\n", debugText);
		}else
		{
			StringBuilder b = new StringBuilder(instruction.name());
			for (Operand operand : operands) {
				b.append(" -> ");
				b.append(operand.name);
			}
				b.append("\n");
			return b.toString();
		}
	}

}
