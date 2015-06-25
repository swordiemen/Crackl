package generation;

import static machine.Op.Instruction.*;
import static machine.Op.Operator.Add;
import static machine.Op.Register.*;
import grammar.CracklBaseVisitor;
import grammar.CracklParser.AddExprContext;
import grammar.CracklParser.AndExprContext;
import grammar.CracklParser.ArrayAssignStatContext;
import grammar.CracklParser.ArrayDeclContext;
import grammar.CracklParser.ArrayDeclInitContext;
import grammar.CracklParser.ArrayIndexExprContext;
import grammar.CracklParser.AssignStatContext;
import grammar.CracklParser.BlockStatContext;
import grammar.CracklParser.CompExprContext;
import grammar.CracklParser.ConstBoolExprContext;
import grammar.CracklParser.ConstNumExprContext;
import grammar.CracklParser.DeclContext;
import grammar.CracklParser.IdExprContext;
import grammar.CracklParser.IfStatContext;
import grammar.CracklParser.PrintExprStatContext;
import grammar.CracklParser.ProgramContext;
import grammar.CracklParser.StatContext;
import grammar.CracklParser.WhileStatContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import machine.Op;
import machine.Op.Operator;
import machine.Op.Register;
import machine.Operand;
import machine.Operand.Const;
import machine.Operand.Deref;
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
	
	@Override
	public Op visitArrayDeclInit(ArrayDeclInitContext ctx)
	{
		Reg rOne = getFreeReg();
		add(Const, constOp(1), rOne);

		Reg rArrayPointer = addGetHeappointer();
		MemoryLocation loc = currentScope.getMemLoc(ctx.ID().getText());
		add(Store, rArrayPointer, addr(loc.getScopeOffset(), loc.getVarOffset())); //store start address on stack at variable
		
		int i = 0;
		for(i = 0; i<ctx.expr().size(); i++){
			visit(ctx.expr(i));
			Reg rExpr = popReg();
			add(Write, rExpr, deref(rArrayPointer));
			add(Compute, operator(Add), rOne, rOne, rArrayPointer);
			freeReg(rExpr);
		}
		
		//Write back rArrayPointer to the heapEnd pointer
		add(Write, rArrayPointer, memAddr(MEMADDR_HEAP_POINTER));

		freeReg(rOne);
		freeReg(rArrayPointer);
		return null;
	}
	
	@Override
	public Op visitArrayDecl(ArrayDeclContext ctx)
	{
		visit(ctx.expr()); //write the to be allocated size to register
		Reg rArraySize = popReg();
		addAllocateArray(rArraySize, ctx.ID().getText());
		return null;
	}
	
	/**
	 * Add instructions to retrieve the base address of some array, given a variable name
	 * Leaks a register!
	 * @param variable
	 * @return register containing the base address (on the heap) of an array
	 */
	private Reg addGetArrayPointer(String variable){
		MemoryLocation loc = currentScope.getMemLoc(variable);
		Reg rArrayPointer = getFreeReg();
		add(Read, memAddr(loc.getScopeOffset()+loc.getVarOffset()));
		add(Receive, rArrayPointer);
		return rArrayPointer;
	}

	/**
	 * Adds instructions to allocate an array on the heap
	 * Note: it consumes a registers, which is free'd implicitly!
	 * @param rArraySize - Register containing the size of the array
	 * @param variableName - Where (on the stack) to store the array starting pointer
	 */
	private void addAllocateArray(Reg rArraySize, String variableName)
	{
		//Retrieve heappointer
		Reg rHeapPointer = addGetHeappointer();
		
		//Store current heappointer at at variable (on the stack)
		MemoryLocation loc = currentScope.getMemLoc(variableName);
		add(Store, rHeapPointer, addr(loc.getScopeOffset(), loc.getVarOffset()));
		
		//Increase and write back changed heappointer
		addAllocate(rArraySize, rHeapPointer);
	}
	
	
	// Location of the heap pointer (at end of heap)
	final int MEMADDR_HEAP_POINTER = 1000; // TODO: some nice place

	/**
	 * Add instructions to put the current heap pointer (end of heap) in a register
	 * @return register containing the read pointer
	 */
	private Reg addGetHeappointer(){
		Reg rHeapPointer = getFreeReg();
		add(Read, memAddr(MEMADDR_HEAP_POINTER));
		add(Receive, rHeapPointer);
		return rHeapPointer;
	}
	
	/**
	 * Increments and writes back the heap pointer
	 * Note: it consumes two registers, both of which are free'd implicitly
	 * @param rArraySize - Register containing the size of the array
	 * @param rHeapPointer - Register containing the current end of the heap
	 */
	private void addAllocate(Reg rArraySize, Reg rHeapPointer)
	{
		add(Compute, operator(Add), rArraySize, rHeapPointer, rHeapPointer);
		add(Write, rHeapPointer, memAddr(MEMADDR_HEAP_POINTER));//maybe TODO: test and set?
		freeReg(rArraySize);
		freeReg(rHeapPointer);
	}
	
	@Override
	public Op visitArrayAssignStat(ArrayAssignStatContext ctx)
	{
		visit(ctx.expr(1));
		Reg rRhValue = popReg();

		//Get offset into array
		visit(ctx.expr(0));
		Reg rOffsetFromArrayBase = popReg();

		//Get array base address
		Reg rHeapPointer = getFreeReg();
		MemoryLocation loc = currentScope.getMemLoc(ctx.target().ID().getText());
		add(Load, addr(loc.getScopeOffset(), loc.getVarOffset()), rHeapPointer);
		
		//Add array offset with array base 
		add(Compute, operator(Add), rHeapPointer, rOffsetFromArrayBase, rHeapPointer);
		freeReg(rOffsetFromArrayBase);

		//Write rhs value to memory
		add(Write, rRhValue, deref(rHeapPointer));
		freeReg(rRhValue);

		freeReg(rHeapPointer);
		return null;
	}
	
	@Override
	public Op visitArrayIndexExpr(ArrayIndexExprContext ctx)
	{
		visit(ctx.expr());
		Reg regIndex = popReg();

		Reg rArrayPointer = getFreeReg();
		MemoryLocation loc = currentScope.getMemLoc(ctx.ID().getText());
		add(Load, addr(loc.getScopeOffset(), loc.getVarOffset()), rArrayPointer);
		add(Compute, operator(Add), regIndex, rArrayPointer, regIndex);

		add(Read, deref(regIndex));
		add(Receive, rArrayPointer);

		pushReg(rArrayPointer); //is actually rReceiveValue...
		freeReg(regIndex);
		return null;
	}
	
	@Override
	public Op visitIfStat(IfStatContext ctx)
	{
		visit(ctx.expr());
		Reg r1 = popReg();
		add(Compute, operator(Operator.Equal), r1, reg(Zero), r1);
		freeReg(r1); //TODO: Maybe free it AFTER some other operation
		int branchLine = addPlaceholder();
		if(ctx.ELSE()!=null)
		{
			visit(ctx.stat(0)); // may be a block or a single stat
			int ifEndLine = addPlaceholder("ifEnd");
			visit(ctx.stat(1)); // may be a block or a single stat
			int nextEnterLine = program.size(); // next block in cfg
			changeAt(ifEndLine, Jump, abs(nextEnterLine));
			int elseLine = ifEndLine + 1; //skip over the 'jump'
			changeAt(branchLine, Branch, r1, abs(elseLine));
		}else{
			visit(ctx.stat(0)); 
			int nextEnterLine = program.size();
			//negation of the expression value (because Branch jumps if true). This way it's easier to implement the control flow.
			changeAt(branchLine, Branch, r1, abs(nextEnterLine));
		}
		
		return null;
	}
	
	
	@Override
	public Op visitWhileStat(WhileStatContext ctx)
	{
        int evalLine = program.size();
        visit(ctx.expr());
        Reg continueReg = popReg(); 
        add(Compute, operator(Operator.Equal), continueReg, reg(Zero), continueReg);
        freeReg(continueReg);
        int branchLine = addPlaceholder("whileBranch");
        visit(ctx.stat()); //body
        add(Jump, abs(evalLine));
        int nextEnterLine = program.size();
        changeAt(branchLine, Branch, continueReg, abs(nextEnterLine));
        return null;
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
	public Op visitCompExpr(CompExprContext ctx)
	{
		Operator operator;
		if (ctx.EQ() != null) {
			operator = Operator.Equal;
		}
		else if (ctx.LT() != null) {
			operator = Operator.Lt;
		}
		else if (ctx.NE() != null) {
			operator = Operator.NEq;
		}
		else if (ctx.GT() != null) {
			operator = Operator.Gt;
		}
		else {
			throw new NullPointerException("Comparator not found");
		}

		visit(ctx.expr(0));
		Reg r1 = popReg();
		visit(ctx.expr(1));
		Reg r2 = popReg();

		add(Compute, operator(operator), r1, r2, r1);
		pushReg(r1);
		freeReg(r2);
		return null;
	}
	
	@Override
	public Op visitIdExpr(IdExprContext ctx)
	{
		Reg r1 = getFreeReg();
		MemoryLocation loc = currentScope.getMemLoc(ctx.ID().getText());
		//System.out.println("memory access: "+ loc);
		add(Load, addr(loc.getScopeOffset(), loc.getVarOffset()), r1);
		pushReg(r1);
		return null;
	}

	public void changeAt(int lineNr, Op.Instruction op, Operand... args){
		if(program.get(lineNr) instanceof Label){
			program.set(lineNr, new Line(op, args));
		}else{
			throw new ClassCastException("changeAt: NOT A LABEL!!!");
		}
	}

	public int addPlaceholder(String s ){
		int location = program.size();
		program.add(new Label(s));
		return location;
	}
	
	public int addPlaceholder(){
		return addPlaceholder("Placeholder!");
	}
	
	private Reg popReg()
	{
		Reg reg = reg(regStack.pop());
		return reg;
	}

	private void freeReg(Register r)
	{
		doAssert(!regStack.contains(r));
		freeRegisters.push(r);
	}

	private void freeReg(Reg r)
	{
		//System.out.println("Free : "+r.name);
		doAssert(!regStack.contains(r.reg));
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
		//System.out.println("currently free : "+freeRegisters);
		Register reg = freeRegisters.pop();
		//System.out.println("Get: " +reg);
		return reg(reg);
	}
	
	@Override
	public Op visit(ParseTree tree)
	{
		//System.out.println("----- visiting : "+tree.getText());
		return super.visit(tree);
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
		add(Store, r1, addr(loc.getScopeOffset(), loc.getVarOffset()));
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
		add(Store, r1, addr(loc.getScopeOffset(), loc.getVarOffset()));
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
		doAssert(newScope.getScope() == this.currentScope);
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
			}else if(child instanceof ArrayDeclContext){
				//reserveStackSpace(((ArrayDeclContext)child).ID().getText());
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
		doAssert(freeRegisters.containsAll(Op.gpRegisters));
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
	
	private void addAt(int lineNr, Op.Instruction op, Operand... args){
		Line line = new Line(op, args);
		program.add(lineNr, line);
	}

	private void add(Op.Instruction op, Operand... args){
		Line line = new Line(op, args);
		program.add(line);
	}

	private static void doAssert(boolean b){
		if(!b){
			throw new IllegalArgumentException("ASSERTION FAILED!");
		}
	}
	
	private static Reg reg(Register r)
	{
		return new Operand.Reg(r);
	}

	public Operand.Target.Abs abs(int lineNumber)
	{
		return new Operand.Target.Abs(lineNumber);
		
	}

	private static Operand.Operator operator(Op.Operator operator)
	{
		return new Operand.Operator(operator);
	}

	private static MemAddr memAddr(int absolute)
	{
		return new Operand.MemAddr(absolute);
	}

	private static Operand.Addr addr(int base, int offset)
	{
		return new Operand.Addr(base, offset);
	}

	private static Deref deref(Reg reg)
	{
		return new Operand.Deref(reg);
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

	private static Const constOp(int i)
	{
		{
			return new Operand.Const(i);
		}
	}
	
}