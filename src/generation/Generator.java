package generation;

import static machine.Op.Instruction.*;
import static machine.Op.Operator.*;
import static machine.Op.Register.*;
import grammar.CracklBaseVisitor;
import grammar.CracklParser.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import machine.Op;
import machine.Op.Operator;
import machine.Op.Register;
import machine.Operand;
import machine.Operand.Const;
import machine.Operand.MemAddr;
import machine.Operand.Reg;

public class Generator extends CracklBaseVisitor<Op>{
	
	public static final boolean DEBUG = true;
	ArrayList<Line> program = new ArrayList<Line>();
	Stack<Register> regStack = new Stack<Register>();
	Stack<Register> freeRegisters = new Stack<Register>();
	Scope currentScope = null;
	Result result = null;
	
	public Generator(Result result){
		this.result = result;
		freeRegisters.addAll(Op.gpRegisters);
	}

	@Override
	public Op visitAddExpr(AddExprContext ctx)
	{
		visit(ctx.expr(0));
		visit(ctx.expr(1));
		Reg r1 = popReg();
		Reg r2 = popReg();
		add(Compute, operator(Add), r1, r2, r1);
		freeReg(r2);
		pushReg(r1);
		return null;
	}
	
	@Override
	public Op visitIdExpr(IdExprContext ctx)
	{
		Reg r1 = getFreeReg();
		MemoryLocation loc = currentScope.findMemoryLocation(ctx.ID());
		add(Load, memAddr(loc.baseAddress, loc.offset), r1);
	}
	
	private Reg popReg()
	{
		Reg reg = reg(regStack.pop());
		return reg;
	}

	private void freeReg(Register r)
	{
		assert(!regStack.contains(r));
		freeRegisters.push(r);
	}

	private void freeReg(Reg r)
	{
		assert(!regStack.contains(r.reg));
		freeRegisters.push(r.reg);
	}
	
	private void pushReg(Reg r)
	{
		regStack.push(r.reg);
	}
	
	private Reg getFreeReg()
	{
		return reg(freeRegisters.pop());
	}
	
	// type ID (ASSIGN expr)? SEMI         		#decl
	@Override
	public Op visitDecl(DeclContext ctx)
	{
		Reg r1 = null;
		//TODO: for now initializes to 0, should be caught during checker phase
		if(ctx.expr() != null)
		{
			visit(ctx.expr());
			r1 = popReg();
		}
		else
		{
			r1 = reg(Zero);
		}
		MemoryLocation loc = currentScope.getMemoryLocation(ctx.ID());
		add(Write, r1, memAddr(loc.baseAddress, loc.offset));
		freeReg(r1);
		return null;
	}
	
	@Override
	public Op visitConstExpr(ConstExprContext ctx)
	{
		Reg r1 = getFreeReg();
		if(ctx.BOOL() != null)
		{
			add(Const, constOp(ctx.BOOL().getText()), r1);
		}else{
			add(Const, constOp(ctx.NUM().getText()), r1);
		}
		pushReg(r1);
		return null;
	}
	
	
	@Override
	// target ASSIGN expr SEMI             		#assignStat
	public Op visitAssignStat(AssignStatContext ctx)
	{
		visit(ctx.expr());
		Reg r1 = popReg();
		int memOffset = currentScope.getOffset(ctx.target().ID());
		//note: can be either some local scope, or the heap/global scope (allocated and will not be cleaned up)
		add(Write, r1, memAddr(currentScope.getBaseAddress(), memOffset));
		return null;
	}
	
	@Override
	public Op visitPrintStat(PrintStatContext ctx)
	{
		String first = ctx.STRING().getText().substring(1, 1);
		System.out.println("print statement with: "+first);
		Reg r1 = getFreeReg();
		add(Const, constOp(first), r1);
		add(Write, r1, MemAddr.StdIO);
		return null;
	}
	
	@Override
	public Op visitBlockStat(BlockStatContext ctx)
	{
		Scope newScope = result.getScope(ctx);
		assert(newScope.previousScope == this.currentScope);
		this.currentScope = newScope;

		List<StatContext> stats = ctx.stat();
		
		//first iteration: check for variables that are to be declared in this block, and reserve some stack space
		//this sort of simulates 'moving variables declarations to the top of the scope'
		//Type checking makes sure we don't use unassigned and undeclared variables
		for (StatContext child : stats) {
			if(child instanceof DeclContext)
			{
				//reserve some space on the stack, to allow for future write
				reserveStackSpace(((DeclContext)child).ID().getText());
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
		add(Push, reg(RegReserved));
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
		visit(ctx.expr(0));
		visit(ctx.expr(1));
		Reg r1 = popReg();
		Reg r2 = popReg();
		add(Compute, operator(Add), r1, r2, r1);
		freeReg(r2);
		pushReg(r1);
		return null;
	}
	
	private void popInto(Reg... regs)
	{
		for (Reg reg : regs) {
			add(Pop, reg);
		}
	}
	
	private void add(Op.Instruction op, Operand... args){
		Line line = new Line(op, args);
		program.add(line);
	}
	
	private static Reg reg(Register r)
	{
		return  new Reg(r);
	}

	private static Operand.Operator operator(Op.Operator operator)
	{
		Operand o = new Operand(Operand.Type.Reg);
		return o.new Operator(operator);
	}

	private static MemAddr memAddr(int base, int offset)
	{
		return new MemAddr(base, offset);
	}
	
	private static Const constOp(String s)
	{
		Operand o = new Operand(Operand.Type.Const);
		if(s.equalsIgnoreCase("false"))
		{
			return o.new Const(false);
		}
		else if(s.equalsIgnoreCase("true"))
		{
			return o.new Const(true);
		}else
		{
			return o.new Const(Integer.parseInt(s));
		}
	}

	
}