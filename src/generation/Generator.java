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
import grammar.CracklParser.ExprContext;
import grammar.CracklParser.FuncCallContext;
import grammar.CracklParser.FuncCallStatContext;
import grammar.CracklParser.FuncDeclContext;
import grammar.CracklParser.IdExprContext;
import grammar.CracklParser.IfStatContext;
import grammar.CracklParser.MainfuncContext;
import grammar.CracklParser.PrintExprStatContext;
import grammar.CracklParser.ProgramContext;
import grammar.CracklParser.PtrAssignContext;
import grammar.CracklParser.PtrDeclContext;
import grammar.CracklParser.PtrDeclNormalContext;
import grammar.CracklParser.PtrDerefExprContext;
import grammar.CracklParser.PtrRefExprContext;
import grammar.CracklParser.RetContext;
import grammar.CracklParser.StatContext;
import grammar.CracklParser.WhileStatContext;

import java.util.ArrayList;
import java.util.HashMap;
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

public class Generator extends CracklBaseVisitor<Op> {

	public static final boolean DEBUG = false;
	ArrayList<Line> program = new ArrayList<Line>();
	Stack<Register> regStack = new Stack<Register>();
	Stack<Register> freeRegisters = new Stack<Register>();
	HashMap<String, Integer> functionTable = new HashMap<String, Integer>(); //Function name -> Instruction line number
	private int mainFunctionLine = -1; //TODO: actually see if the main code can be moved to the top, without breaking jumps in e.g. while/if-else etc
	Scope currentScope = null;
	Result result = null;

	public static final int STACK_SIZE = 128;
	public static final int STACK_START = 0; //ehh...
	public static final int STACK_END = STACK_START + STACK_SIZE - 1;

	public static final int LOCAL_HEAP_SIZE = 1024;
	public static final int LOCAL_HEAP_START = STACK_START;
	public static final int LOCAL_HEAP_END = STACK_START + LOCAL_HEAP_SIZE - 1;

	public static final int GLOBAL_HEAP_START = LOCAL_HEAP_END + 1;
	public static final int GLOBAL_HEAP_END = 16 * 1000 * 1000 - 1;
	public static final int GLOBAL_HEAP_SIZE = GLOBAL_HEAP_START - GLOBAL_HEAP_END;

	public ArrayList<Line> getProgram()
	{
		return program;
	}

	public Generator(Result result) {
		this.result = result;
		freeRegisters.addAll(Op.gpRegisters);
	}
	
	/**
	@Override
	public Op visitParams(ParamsContext ctx)
	{
		//	params: ''|(type ID COMMA)* type ID;
		
		//NOTE: this is part of building the function table, maybe this should go to a separate class?
		int size = ctx.type().size();
		for(int i = 0; i<size; i++)
		{
			TypeContext typeCtx = ctx.type(i);
			TerminalNode idCtx = ctx.ID(i);
		}
		return null;
	}
	**/
	
	@Override
	public Op visitFuncDecl(FuncDeclContext ctx)
	{
		// func	#funcStat
		//func: FUNC retType ID LPAR params RPAR LCURL stat* ret RCURL;
		Scope newScope = result.getScope(ctx);
		doAssert(newScope.getScope() == this.currentScope);
		this.currentScope = newScope;
		
		
		//add instructions to push parameters on the stack BULLSHIT
		//visit(ctx.params());
		
		functionTable.put(ctx.ID().getText(), program.size());
		
		List<StatContext> stats = ctx.stat();
		//add instructions to reserve space for local variables on the stack
		int toPop = addReserveForLocalVariables(stats);

		//insert function's instructions into program (duh)
		for (StatContext stat : stats) {
			visit(stat);
		}
		
		//add instructions to "pop" the local variables from the stack
		addDecrSp(toPop);

		//add instructions to remove parameters from stack, and restore PC with return address
		visit(ctx.ret());

		this.currentScope = this.currentScope.getScope();
		return null;
	}
	
	@Override
	public Op visitRet(RetContext ctx)
	{
		//add instructions to remove parameters from stack, restore PC with return address, and put returnvalue on the stack
		//TODO::: !!!! ONLY ONE ARGUMENT FOR NOW, GOTTA MAKE A PROPER FUNCTION TABLE FIRST
		
		//Pop parameters and get the return address
		Reg rReturnAddress = getFreeReg();
		add(Pop, reg(Zero)); //TODO: for(i<function.args.length){...
		add(Pop, rReturnAddress);
		
		//push return value on stack
		visit(ctx.expr());
		Reg rReturnValue = popReg();
		add(Push, rReturnValue);
		freeReg(rReturnValue);
		add(Jump, ind(rReturnAddress)); //TODO: maybe can be absolute if I have a proper function table I think... 
		freeReg(rReturnAddress);
		return null;
	}
	
	@Override
	public Op visitFuncCall(FuncCallContext ctx)
	{
		// funcCall: ID LPAR expr RPAR;
		
		
		//add instructions to push return address on the stack: current PC + skipConstant
		Reg rReturnAddress = getFreeReg();
		int returnAddressLine = addPlaceholder("Return address line for: "+ctx.ID().getText());
		add(Push, rReturnAddress);

		//add instructions to push arguments on the stack
		List<ExprContext> exprs = ctx.expr();
		for (ExprContext expr : exprs) {
			visit(expr);
			Reg rExpr = popReg();
			add(Push, rExpr);
			freeReg(rExpr);
		}
		
		//add instructions to Call the function!!!!!!!!!!!!!!!!AAAAA WHOOOO!!!!
		add(Jump, abs(functionTable.get(ctx.ID().getText())));

		/**
		int returnAddressOffset = program.size()-startSize + 2; //+2 for Compute and Push instructions
		changeAt(constLine, Const, constOp(returnAddressOffset), rReturnAddress); //lazy reuse...
		freeReg(rReturnAddress);
		**/
		changeAt(returnAddressLine, Const, constOp(program.size()), rReturnAddress); //return just after the Jump instruction
		freeReg(rReturnAddress);
		
		//pop returnvalue of the stack and put it in some register
		Reg rReturnValue = getFreeReg();
		add(Pop, rReturnValue);
		
		pushReg(rReturnValue);
		return null;
	}
	
	@Override
	public Op visitFuncCallStat(FuncCallStatContext ctx)
	{
		visit(ctx.funcCall());
		freeReg(popReg()); //only make sure that the register is not used anymore, since this is a statement (no assignment or anything)
		return null;
	}
	
	@Override
	public Op visitMainfunc(MainfuncContext ctx)
	{
		//	mainfunc: MAIN LCURL stat* RCURL;
		this.mainFunctionLine = program.size(); //TODO: actually see if the main code can be moved to the top, without breaking jumps in e.g. while/if-else etc
		List<StatContext> stats = ctx.stat();
		for (StatContext stat : stats) {
			visit(stat);
		}
		add(EndProg); //make sure no 'random' code is executed
		return null;
	}
	
	
	/**
	 * Adds instructions to effectively pop n variables from the stack
	 */
	private void addDecrSp(int toPop)
	{
		Reg r1 = getFreeReg();
		add(Const, constOp(toPop), r1);
		add(Compute, operator(Op.Operator.Sub), reg(Op.Register.SP), r1, reg(Op.Register.SP));
		freeReg(r1);
	}

	private int addReserveForLocalVariables(List<StatContext> stats)
	{
		// first iteration: check for variables that are to be declared in this
		// block, and reserve some stack space
		// this sort of simulates 'moving variables declarations to the top of
		// the scope'
		// Type checking makes sure we don't use unassigned and undeclared
		// variables
		int toPop = 0;
		for (StatContext child : stats) {
			if (child instanceof DeclContext) {
				// reserve some space on the stack, to allow for future write
				reserveStackSpace(((DeclContext) child).ID().getText());
				toPop++;
			}
			else if (child instanceof PtrDeclContext) {
				reserveStackSpace(((PtrDeclContext) child).ID(0).getText());
				toPop++;
			}
			else if (child instanceof PtrDeclNormalContext) {
				reserveStackSpace(((PtrDeclNormalContext) child).ID(0).getText());
				toPop++;
			}
		}
		return toPop;
	}
	

	@Override
	public Op visitPtrDecl(PtrDeclContext ctx)
	{
		// | PTRTYPE type ID (PTRASSIGN ID)? SEMI #ptrDecl
		// #int a => b;
		Reg r1 = null;
		if (ctx.PTRASSIGN() != null) {
			r1 = getFreeReg();
			MemoryLocation assignLoc = currentScope.getMemLoc(ctx.ID(1).getText());
			// if(assignLoc.isGlobal()){
			add(Store, constOp(assignLoc.getScopeOffset() + assignLoc.getVarOffset()));
			// }else{
			// add(Store,constOp( assignLoc.getScopeOffset()+
			// assignLoc.getVarOffset()));
			// }
		}
		else {
			r1 = reg(Zero); // null-pointer
		}
		MemoryLocation loc = currentScope.getMemLoc(ctx.ID(0).getText());
		add(Store, r1, addr(loc.getScopeOffset(), loc.getVarOffset()));
		freeReg(r1);
		return null;
	}

	@Override
	public Op visitPtrDeclNormal(PtrDeclNormalContext ctx)
	{
		// | PTRTYPE type ID ASSIGN ID SEMI #ptrDeclNormal
		// #int a = b; where b is ptr
		MemoryLocation targetLoc = currentScope.getMemLoc(ctx.ID(0).getText());
		MemoryLocation sourceLoc = currentScope.getMemLoc(ctx.ID(1).getText());

		Reg rSourceAddr = getFreeReg();
		add(Const, constOp(sourceLoc.getScopeOffset() + sourceLoc.getVarOffset()), rSourceAddr);
		add(Store, rSourceAddr, addr(targetLoc.getScopeOffset(), targetLoc.getVarOffset()));
		freeReg(rSourceAddr);
		return null;
	}

	@Override
	public Op visitPtrAssign(PtrAssignContext ctx)
	{
		// | ID PTRASSIGN ID SEMI #ptrAssign
		// a => b; where b is value and a is ptr
		MemoryLocation targetLoc = currentScope.getMemLoc(ctx.ID(0).getText());
		MemoryLocation sourceLoc = currentScope.getMemLoc(ctx.ID(1).getText());

		Reg rSourceAddr = getFreeReg();
		add(Const, constOp(sourceLoc.getScopeOffset() + sourceLoc.getVarOffset()), rSourceAddr);
		add(Store, rSourceAddr, addr(targetLoc.getScopeOffset(), targetLoc.getVarOffset()));
		freeReg(rSourceAddr);
		return null;
	}

	@Override
	public Op visitPtrDerefExpr(PtrDerefExprContext ctx)
	{
		// DEREF ID #ptrDerefExpr
		Reg rLoc = addDeref(ctx.ID().getText());
		pushReg(rLoc); // is actually rReceiveValue...
		return null;
	}

	public Reg addDeref(String id)
	{
		MemoryLocation idLoc = currentScope.getMemLoc(id);
		int memoryAddress = idLoc.getScopeOffset() + idLoc.getVarOffset();

		doAssert(memoryAddress >= 0 && memoryAddress < GLOBAL_HEAP_END);
		Reg rLoc = getFreeReg();
		add(Const, constOp(idLoc.getScopeOffset() + idLoc.getVarOffset()), rLoc);

		if (memoryAddress <= LOCAL_HEAP_END) {
			if (memoryAddress > STACK_END) {
				// deref from local heap
				add(Load, deref(rLoc));
			}
			else {
				// deref from stack (same as from local heap...)
				add(Load, deref(rLoc));
			}

		}
		else if (memoryAddress <= GLOBAL_HEAP_END) {
			// deref from global heap
			add(Read, deref(rLoc));
			add(Receive, rLoc);
		}
		else {
			throw new IllegalArgumentException("Heap size exeeded!!! " + memoryAddress);
		}
		return rLoc;
	}

	private void addSave(String variable, Reg reg)
	{
		MemoryLocation loc = currentScope.getMemLoc(variable);
		if (loc.isLocal()) {
			if(loc.isOnStack()){
				//relative
				Reg rLoc = getFreeReg();
				add(Const, constOp(loc.getTotalOffsetRev()), rLoc);
				add(Compute, operator(Operator.Add), reg(SP), rLoc, rLoc);
				add(Store, reg, deref(rLoc));
				freeReg(rLoc);
			}else{
				//absolute
				add(Store, reg, addr(loc.getScopeOffset(), loc.getVarOffset())); 
			}
		}
		else if (loc.isGlobal()) {
			add(Write, reg, addr(loc.getScopeOffset(), loc.getVarOffset())); // store // start // address // on // stack // at // variable
		}
		else {
			throw new IllegalArgumentException("Neither global nor local!");
		}
	}

	public void addLoadInto(String variable, Reg reg)
	{
		MemoryLocation loc = currentScope.getMemLoc(variable);
		//System.out.println("addLoadInto : "+variable + loc.isLocal());
		if (loc.isLocal()) {
			if(loc.isOnStack()){
				//relative
				Reg rLoc = getFreeReg();
				add(Const, constOp(loc.getTotalOffsetRev()), rLoc);
				add(Compute, operator(Operator.Add), reg(SP), rLoc, rLoc);
				add(Load, deref(rLoc), reg);
				freeReg(rLoc);
			}else{
				System.out.println("ON HEAP !!!!");
				//on local heap, absolute
				add(Load, addr(loc.getScopeOffset(), loc.getVarOffset()), reg);
			}
		}
		else if (loc.isGlobal()) {
			add(Read, memAddr(loc.getVarOffset()));
			add(Receive, reg);
		}
		else {
			throw new IllegalArgumentException("Neither global nor local!");
		}

	}

	@Override
	public Op visitPtrRefExpr(PtrRefExprContext ctx)
	{
		// | REF ID #ptrRefExpr
		MemoryLocation memLoc = currentScope.getMemLoc(ctx.ID().getText());
		Reg rAddr = getFreeReg();
		add(Const, constOp(memLoc.getScopeOffset() + memLoc.getVarOffset()), rAddr);
		pushReg(rAddr);
		return null;
	}

	@Override
	public Op visitArrayDeclInit(ArrayDeclInitContext ctx)
	{
		Reg rOne = getFreeReg();
		add(Const, constOp(1), rOne);

		Reg rArrayPointer = addGetHeappointer();
		addSave(ctx.ID().getText(), rArrayPointer);

		int i = 0;
		for (i = 0; i < ctx.expr().size(); i++) {
			visit(ctx.expr(i));
			Reg rExpr = popReg();
			add(Write, rExpr, deref(rArrayPointer));
			add(Compute, operator(Add), rOne, rArrayPointer, rArrayPointer);
			freeReg(rExpr);
		}

		// Write back rArrayPointer to the heapEnd pointer
		add(Write, rArrayPointer, memAddr(MEMADDR_HEAP_POINTER));

		freeReg(rOne);
		freeReg(rArrayPointer);
		return null;
	}

	@Override
	public Op visitArrayDecl(ArrayDeclContext ctx)
	{
		visit(ctx.expr()); // write the to be allocated size to register
		Reg rArraySize = popReg();
		addAllocateArray(rArraySize, ctx.ID().getText());
		return null;
	}

	/**
	 * Add instructions to retrieve the base address of some array, given a variable name Leaks a register!
	 * 
	 * @param variable
	 * @return register containing the base address (on the heap) of an array
	 */
	private Reg addGetArrayPointer(String variable)
	{
		MemoryLocation loc = currentScope.getMemLoc(variable);
		Reg rArrayPointer = getFreeReg();
		add(Read, memAddr(loc.getScopeOffset() + loc.getVarOffset()));
		add(Receive, rArrayPointer);
		return rArrayPointer;
	}

	/**
	 * Adds instructions to allocate an array on the heap Note: it consumes a registers, which is free'd implicitly!
	 * 
	 * @param rArraySize
	 *            - Register containing the size of the array
	 * @param variableName
	 *            - Where (on the stack) to store the array starting pointer
	 */
	private void addAllocateArray(Reg rArraySize, String variableName)
	{
		// Retrieve heappointer
		Reg rHeapPointer = addGetHeappointer();

		// Store current heappointer at at variable (on the stack, or on the
		// heap only if global)
		addSave(variableName, rHeapPointer);
		// MemoryLocation loc = currentScope.getMemLoc(variableName);
		// add(Store, rHeapPointer, addr(loc.getScopeOffset(),
		// loc.getVarOffset()));

		// Increase and write back changed heappointer
		addAllocate(rArraySize, rHeapPointer);
	}

	// Location of the heap pointer (points to next free space on global heap),
	// located on the global memory
	final int MEMADDR_HEAP_POINTER = GLOBAL_HEAP_START;

	/**
	 * Add instructions to put the current heap pointer (end of heap) in a register
	 * 
	 * @return register containing the read pointer
	 */
	private Reg addGetHeappointer()
	{
		Reg rHeapPointer = getFreeReg();
		add(Read, memAddr(MEMADDR_HEAP_POINTER));
		add(Receive, rHeapPointer);
		return rHeapPointer;
	}

	/**
	 * Increments and writes back the heap pointer Note: it consumes two registers, both of which are free'd implicitly
	 * 
	 * @param rArraySize
	 *            - Register containing the size of the array
	 * @param rHeapPointer
	 *            - Register containing the current end of the heap
	 */
	private void addAllocate(Reg rArraySize, Reg rHeapPointer)
	{
		add(Compute, operator(Add), rArraySize, rHeapPointer, rHeapPointer);
		add(Write, rHeapPointer, memAddr(MEMADDR_HEAP_POINTER));// maybe TODO:
																// test and set?
		freeReg(rArraySize);
		freeReg(rHeapPointer);
	}

	@Override
	public Op visitArrayAssignStat(ArrayAssignStatContext ctx)
	{
		// target LSQ expr RSQ ASSIGN expr SEMI #arrayAssignStat
		visit(ctx.expr(1));
		Reg rRhValue = popReg();

		// Get offset into array
		visit(ctx.expr(0));
		Reg rOffsetFromArrayBase = popReg();

		// Get array base address
		Reg rHeapPointer = getFreeReg();
		addLoadInto(ctx.target().ID().getText(), rHeapPointer);

		// Add array offset with array base
		add(Compute, operator(Add), rHeapPointer, rOffsetFromArrayBase, rHeapPointer);
		freeReg(rOffsetFromArrayBase);

		// Write rhs value to memory
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
		addLoadInto(ctx.ID().getText(), rArrayPointer);
		add(Compute, operator(Add), regIndex, rArrayPointer, regIndex);

		add(Read, deref(regIndex));
		add(Receive, rArrayPointer);

		pushReg(rArrayPointer); // is actually rReceiveValue...
		freeReg(regIndex);
		return null;
	}

	@Override
	public Op visitIfStat(IfStatContext ctx)
	{
		visit(ctx.expr());
		Reg r1 = popReg();
		add(Compute, operator(Operator.Equal), r1, reg(Zero), r1);
		freeReg(r1); // TODO: Maybe free it AFTER some other operation
		int branchLine = addPlaceholder();
		if (ctx.ELSE() != null) {
			visit(ctx.stat(0)); // may be a block or a single stat
			int ifEndLine = addPlaceholder("ifEnd");
			visit(ctx.stat(1)); // may be a block or a single stat
			int nextEnterLine = program.size(); // next block in cfg
			changeAt(ifEndLine, Jump, abs(nextEnterLine));
			int elseLine = ifEndLine + 1; // skip over the 'jump'
			changeAt(branchLine, Branch, r1, abs(elseLine));
		}
		else {
			visit(ctx.stat(0));
			int nextEnterLine = program.size();
			// negation of the expression value (because Branch jumps if true).
			// This way it's easier to implement the control flow.
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
		visit(ctx.stat()); // body
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
		addLoadInto(ctx.ID().getText(), r1);
		pushReg(r1);
		return null;
	}

	public void changeAt(int lineNr, Op.Instruction op, Operand... args)
	{
		if (program.get(lineNr) instanceof Label) {
			program.set(lineNr, new Line(op, args));
		}
		else {
			throw new ClassCastException("changeAt: NOT A LABEL!!!");
		}
	}

	public int addPlaceholder(String s)
	{
		int location = program.size();
		program.add(new Label(s));
		return location;
	}

	public int addPlaceholder()
	{
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
		System.out.println("Free : " + r.name);
		doAssert(!regStack.contains(r.reg));
		if (Op.gpRegisters.contains(r.reg)) {
			freeRegisters.push(r.reg);
		}
	}

	private void pushReg(Reg r)
	{
		regStack.push(r.reg);
	}

	private Reg getFreeReg()
	{
		System.out.println("currently free : " + freeRegisters);
		Register reg = freeRegisters.pop();
		System.out.println("Get: " + reg);
		return reg(reg);
	}

	@Override
	public Op visit(ParseTree tree)
	{
		System.out.println("----- visiting : " + tree.getText());
		return super.visit(tree);
	}

	// type ID (ASSIGN expr)? SEMI #decl
	@Override
	public Op visitDecl(DeclContext ctx)
	{
		Reg r1 = null;
		if (ctx.expr() != null) {
			visit(ctx.expr());
			r1 = popReg();
		}
		else {
			r1 = reg(Zero);
		}
		addSave(ctx.ID().getText(), r1);
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
	// target ASSIGN expr SEMI #assignStat
	public Op visitAssignStat(AssignStatContext ctx)
	{
		visit(ctx.expr());
		Reg r1 = popReg();
		addSave(ctx.target().ID().getText(), r1);
		// note: can be either some local scope, or the heap/global scope
		// (allocated and will not be cleaned up)
		freeReg(r1);
		return null;
	}

	@Override
	public Op visitPrintExprStat(PrintExprStatContext ctx)
	{
		int NUM_OFFSET_ASCII = 48;
		int ASCII_NEWLINE = 12;
		int ASCII_BACKSPACE = 10;
		visit(ctx.expr());
		Reg r1 = popReg();
		Reg r2 = getFreeReg();
		add(Const, constOp("" + NUM_OFFSET_ASCII), r2);
		add(Compute, operator(Add), r1, r2, r1);
		add(Write, r1, MemAddr.StdIO);
		add(Const, constOp(ASCII_NEWLINE), r1);
		add(Write, r1, MemAddr.StdIO);
		// add(Const, constOp(ASCII_BACKSPACE), r1);
		// add(Write, r1, MemAddr.StdIO);
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
		int toPop = 0;

		// first iteration: check for variables that are to be declared in this
		// block, and reserve some stack space
		// this sort of simulates 'moving variables declarations to the top of
		// the scope'
		// Type checking makes sure we don't use unassigned and undeclared
		// variables
		for (StatContext child : stats) {
			if (child instanceof DeclContext) {
				// reserve some space on the stack, to allow for future write
				reserveStackSpace(((DeclContext) child).ID().getText());
				toPop++;
			}
			else if (child instanceof PtrDeclContext) {
				reserveStackSpace(((PtrDeclContext) child).ID(0).getText());
				toPop++;
			}
			else if (child instanceof PtrDeclNormalContext) {
				reserveStackSpace(((PtrDeclNormalContext) child).ID(0).getText());
				toPop++;
			}
		}

		for (StatContext child : stats) {
			visit(child);
		}

		Reg rPop = getFreeReg();
		add(Const, constOp(toPop), rPop);
		add(Compute, operator(Op.Operator.Sub), reg(Op.Register.SP), rPop, reg(Op.Register.SP));

		freeReg(rPop);
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
		if (DEBUG) {
			program.add(new DebugLine(s));
		}
	}

	@Override
	public Op visitProgram(ProgramContext ctx)
	{
		int jumpLine = addPlaceholder("Jump to main function here...");
		for (ParseTree child : ctx.children) {
			visit(child);
		}
		changeAt(jumpLine, Jump, abs(mainFunctionLine));
		for (int i = 0; i < 5; i++) {
			add(Nop);
		}
		add(EndProg); //Just to be sure in the case main didn't add one...
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

	private void addAt(int lineNr, Op.Instruction op, Operand... args)
	{
		Line line = new Line(op, args);
		program.add(lineNr, line);
	}

	private void add(Op.Instruction op, Operand... args)
	{
		Line line = new Line(op, args);
		program.add(line);
	}

	private static void doAssert(boolean b)
	{
		if (!b) {
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

	public Operand.Target.Ind ind(Reg r)
	{
		return new Operand.Target.Ind(r);

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
		if (s.equalsIgnoreCase("false")) {
			return new Operand.Const(false);
		}
		else if (s.equalsIgnoreCase("true")) {
			return new Operand.Const(true);
		}
		else {
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