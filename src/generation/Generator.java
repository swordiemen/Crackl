package generation;

import static machine.Op.Instruction.Compute;
import static machine.Op.Instruction.Const;
import static machine.Op.Instruction.Pop;
import static machine.Op.Instruction.Push;
import static machine.Op.Operator.Add;
import static machine.Op.Reg.RegA;
import static machine.Op.Reg.RegB;
import static machine.Op.Reg.RegC;
import grammar.CracklBaseVisitor;
import grammar.CracklParser.AddExprContext;
import grammar.CracklParser.AndExprContext;
import grammar.CracklParser.AssignStatContext;
import grammar.CracklParser.BlockStatContext;
import grammar.CracklParser.DeclContext;
import grammar.CracklParser.ProgramContext;
import grammar.CracklParser.StatContext;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import machine.Op;
import machine.Op.Reg;
import machine.Operand;

public class Generator extends CracklBaseVisitor<Op>{
	
	public static final boolean DEBUG = true;
	ArrayList<Line> program = new ArrayList<Line>();

	@Override
	public Op visitAddExpr(AddExprContext ctx)
	{
		visit(ctx.expr(0));
		visit(ctx.expr(1));
		popInto(RegA, RegB);
		add(Compute, operator(Add), reg(RegA), reg(RegB), reg(RegC));
		add(Push, reg(RegC));
		return null;
	}
	
	// type ID (ASSIGN expr)? SEMI         		#decl
	@Override
	public Op visitDecl(DeclContext ctx)
	{
		//TODO: for now initializes to 0, should be caught during checker phase
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
	
	@Override
	// target ASSIGN expr SEMI             		#assignStat
	public Op visitAssignStat(AssignStatContext ctx)
	{
		/**
		visit(ctx.expr());
		popInto(RegA);
		int memOffset = currentScope.getOffset(ctx.target().ID());
		//note: can be either some local scope, or the heap/global scope (allocated and will not be cleaned up 'automatically' by popping)
		add(Write, reg(RegA), memAddr(currentScope.getBaseAddress(), memOffset));
		**/
		return null;
	}
	
	@Override
	public Op visitBlockStat(BlockStatContext ctx)
	{
		List<StatContext> stats = ctx.stat();
		
		//first iteration: check for variables that are to be declared in this block, and reserve some stack space
		//this sort of simulates 'moving variables declarations to the top of the scope'
		for (StatContext child : stats) {
			if(child instanceof DeclContext)
			{
				//reserve some space on the stack, to allow for future write
				/**
				reserveStackSpace(((DeclContext)child).ID());
				**/
			}
		}
		
		for (StatContext child: stats) {
			visit(child);
		}
		return null;
	}
	
	private void reserveStackSpace(String s)
	{
		debug(String.format("Reserved stack space %s", s));
	}
	
	private void debug(String s)
	{
		if(DEBUG)
		{
			program.add(new DebugLine(s));
		}
	}
	
	@Override
	public Op visitProgram(ProgramContext ctx)
	{
		for (ParseTree child : ctx.children) {
			visit(child);
		}
		return null;
	}
	
	@Override
	public Op visitAndExpr(AndExprContext ctx)
	{
		/**

		visit(ctx.getChild(0));
		visit(ctx.getChild(1));
		popInto(RegA, RegB);
		add(Compute, operator(And))
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