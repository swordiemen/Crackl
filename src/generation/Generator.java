package generation;

import static machine.Op.Instruction.*;
import static machine.Op.Operator.*;
import static machine.Op.Reg.RegA;
import static machine.Op.Reg.RegB;
import static machine.Op.Reg.RegC;
import grammar.CracklBaseVisitor;
import grammar.CracklParser.AddExprContext;
import grammar.CracklParser.AssignStatContext;
import grammar.CracklParser.DeclContext;

import java.util.ArrayList;

import machine.Op;
import machine.Op.Reg;
import machine.Operand;

public class Generator extends CracklBaseVisitor<Op>{
	
	ArrayList program = new ArrayList<Line>();

	@Override
	public Op visitAddExpr(AddExprContext ctx)
	{
		visit(ctx.expr(0));
		visit(ctx.expr(1));
		popInto(RegA, RegB);
		add(Compute, operator(Add), reg(RegA), reg(RegB), reg(RegC));
		return null;
	}

	// type ID (ASSIGN expr)? SEMI         		#decl
	@Override
	public Op visitDecl(DeclContext ctx)
	{
		//TODO: for now initializes to 0
		if(ctx.expr() != null)
		{
			popInto(RegA);
		}
		else
		{
			add(Const, constant(0), reg(RegA));
		}
		/**
		int memOffset = currentScope.getOffset(ctx.ID());
		add(Write, reg(RegA), memAddr(currentScope.getBaseAddress(), memOffset));
		**/
		return null;
	}
	
	
	private void popInto(Reg... regs)
	{
		for (Reg reg : regs) {
			add(Pop, reg(reg));
		}
	}
	
	private void add(Op.Instruction op, Operand... args){
		Line line = new Line(op, args);
		program.add(line);
	}
	
	private static Operand.Reg reg(Op.Reg r)
	{
		Operand o = new Operand(Operand.Type.Reg);
		return  o.new Reg(r);
	}

	private static Operand constant(int i)
	{
		Operand o = new Operand(Operand.Type.Const);
		return  o;
	}

	private static Operand.Operator operator(Op.Operator operator)
	{
		Operand o = new Operand(Operand.Type.Reg);
		return o.new Operator(operator);
	}

	private static Operand.MemAddr operator(int base, int offset)
	{
		Operand o = new Operand(Operand.Type.Memaddr);
		return o.new MemAddr(base, offset);
	}
	
}