package generation;

import static machine.Op.Instruction.*;
import static machine.Op.Operator.Add;
import static machine.Op.Register.*;
import grammar.CracklBaseVisitor;
import grammar.CracklParser.AddExprContext;
import grammar.CracklParser.AndExprContext;
import grammar.CracklParser.AssignStatContext;
import grammar.CracklParser.BlockStatContext;
import grammar.CracklParser.ConstBoolExprContext;
import grammar.CracklParser.ConstNumExprContext;
import grammar.CracklParser.DeclContext;
import grammar.CracklParser.IdExprContext;
import grammar.CracklParser.IfStatContext;
import grammar.CracklParser.PrintExprStatContext;
import grammar.CracklParser.ProgramContext;
import grammar.CracklParser.StatContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import machine.Op;
import machine.Op.Register;
import machine.Operand;
import machine.Operand.Const;
import machine.Operand.MemAddr;
import machine.Operand.Reg;

import org.antlr.v4.runtime.tree.ParseTree;

import analysis.MemoryLocation;
import analysis.Result;
import analysis.Scope;

public class Generator extends CracklBaseVisitor<Op>{
	
	public static final boolean DEBUG = false;
	ArrayList<Line> program = new ArrayList<Line>();
	Stack<Register> regStack = new Stack<Register>();
	Stack<Register> freeRegisters = new Stack<Register>();
	Scope currentScope = null;
	Result result = null;
	
	public ArrayList<Line> getProgram()
	{
		return program;
	}
	
	public Generator(Result result){
		this.result = result;
		freeRegisters.addAll(Op.gpRegisters);
	}
	
	/**
	@Override
	public Op visitIfStat(IfStatContext ctx)
	{
		visit(ctx.expr());
		Reg r1 = popReg();
		add(Branch, r1, abs(elseLine));
		return super.visitIfStat(ctx);
	}
	**/
	
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
		MemoryLocation loc = currentScope.getMemLoc(ctx.ID().getText());
		System.out.println(loc);
		add(Load, memAddr(loc.getScopeOffset(), loc.getVarOffset()), r1);
		pushReg(r1);
		return null;
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
		System.out.println("Free : "+r.name);
		assert(!regStack.contains(r.reg));
		if(Op.gpRegisters.contains(r.reg))
		{
			freeRegisters.push(r.reg);
		}
	}
	
	private void pushReg(Reg r)
	{
		regStack.push(r.reg);
	}
	
	private Reg getFreeReg()
	{
		System.out.println("currently free : "+freeRegisters);
		Register reg = freeRegisters.pop();
		System.out.println("Get: " +reg);
		return reg(reg);
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
		MemoryLocation loc = currentScope.getMemLoc(ctx.ID().getText());
		add(Store, r1, memAddr(loc.getScopeOffset(), loc.getVarOffset()));
		freeReg(r1);
		return null;
	}
	
	@Override
	public Op visitConstBoolExpr(ConstBoolExprContext ctx)
	{
		Reg r1 = getFreeReg();
		add(Const, constOp(ctx.BOOL().getText()), r1);
		pushReg(r1);
		return null;
	}
	@Override
	public Op visitConstNumExpr(ConstNumExprContext ctx)
	{
		Reg r1 = getFreeReg();
		add(Const, constOp(ctx.NUM().getText()), r1);
		pushReg(r1);
		return null;
	}
	
	@Override
	// target ASSIGN expr SEMI             		#assignStat
	public Op visitAssignStat(AssignStatContext ctx)
	{
		visit(ctx.expr());
		Reg r1 = popReg();
		MemoryLocation loc = currentScope.getMemLoc(ctx.target().ID().getText());
		//note: can be either some local scope, or the heap/global scope (allocated and will not be cleaned up)
		add(Store, r1, memAddr(loc.getScopeOffset(), loc.getVarOffset()));
		freeReg(r1);
		return null;
	}
	
	@Override
	public Op visitPrintExprStat(PrintExprStatContext ctx)
	{
		int NUM_OFFSET_UTF16 = 48;
		visit(ctx.expr());
		Reg r1 = popReg();
		Reg r2 = getFreeReg();
		add(Const, constOp(""+NUM_OFFSET_UTF16), r2);
		add(Compute, operator(Add), r1, r2, r1);
		add(Write, r1, MemAddr.StdIO);
		freeReg(r2);
		freeReg(r1);
		return null;
	}
	
	@Override
	public Op visitBlockStat(BlockStatContext ctx)
	{
		Scope newScope = result.getScope(ctx);
		assert(newScope.getScope() == this.currentScope);
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
		
		currentScope = currentScope.getScope();
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
		for(int i = 0; i<10; i++)
		{
			add(Nop);
		}
		add(EndProg);
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
		return new Operand.Reg(r);
	}

	/**
	public Operand.Abs abs(int lineNumber)
	{
		
	}
	**/

	private static Operand.Operator operator(Op.Operator operator)
	{
		return new Operand.Operator(operator);
	}

	private static MemAddr memAddr(int base, int offset)
	{
		return new MemAddr(base, offset);
	}
	
	private static Const constOp(String s)
	{
		if(s.equalsIgnoreCase("false"))
		{
			return new Operand.Const(false);
		}
		else if(s.equalsIgnoreCase("true"))
		{
			return new Operand.Const(true);
		}else
		{
			return new Operand.Const(Integer.parseInt(s));
		}
	}

	
}