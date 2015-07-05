package generation;

import static machine.Op.Instruction.*;
import static machine.Op.Operator.*;
import static machine.Op.Register.*;
import grammar.CracklBaseVisitor;
import grammar.CracklParser.AndExprContext;
import grammar.CracklParser.ArrayAssignStatContext;
import grammar.CracklParser.ArrayDeclContext;
import grammar.CracklParser.ArrayDeclInitContext;
import grammar.CracklParser.ArrayExprContext;
import grammar.CracklParser.ArrayIndexExprContext;
import grammar.CracklParser.AssignDerefContext;
import grammar.CracklParser.AssignStatContext;
import grammar.CracklParser.BlockStatContext;
import grammar.CracklParser.CompExprContext;
import grammar.CracklParser.ConstBoolExprContext;
import grammar.CracklParser.ConstNumExprContext;
import grammar.CracklParser.ConstTextExprContext;
import grammar.CracklParser.DeclContext;
import grammar.CracklParser.ExprContext;
import grammar.CracklParser.ForkStatContext;
import grammar.CracklParser.FuncCallContext;
import grammar.CracklParser.FuncCallStatContext;
import grammar.CracklParser.FuncDeclContext;
import grammar.CracklParser.IdExprContext;
import grammar.CracklParser.IfStatContext;
import grammar.CracklParser.JoinStatContext;
import grammar.CracklParser.LockDeclContext;
import grammar.CracklParser.LockStatContext;
import grammar.CracklParser.MainFuncStatContext;
import grammar.CracklParser.MainfuncContext;
import grammar.CracklParser.NotExprContext;
import grammar.CracklParser.OperatorExprContext;
import grammar.CracklParser.OrExprContext;
import grammar.CracklParser.PrintExprStatContext;
import grammar.CracklParser.ProgramContext;
import grammar.CracklParser.PtrAssignContext;
import grammar.CracklParser.PtrDeclContext;
import grammar.CracklParser.PtrDeclNormalContext;
import grammar.CracklParser.PtrDerefExprContext;
import grammar.CracklParser.PtrRefExprContext;
import grammar.CracklParser.RetContext;
import grammar.CracklParser.SprockellIdExprContext;
import grammar.CracklParser.StatContext;
import grammar.CracklParser.UnlockStatContext;
import grammar.CracklParser.WhileStatContext;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
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
import analysis.Type;

public class Generator extends CracklBaseVisitor<Op> {

	public static final boolean DEBUG_OTHER = true;
	public static final boolean DEBUG_REG = true;

	public ArrayList<Line> program = new ArrayList<Line>();
	
	private Stack<Register> regStack = new Stack<Register>();
	private Stack<Register> freeRegisters = new Stack<Register>();
	private HashMap<String, Function> functionTable; //Function name -> Function
	private Scope currentScope = null;
	private Result result = null;
	private HashMap<FuncCallContext, Integer> functionPlaceholders = new HashMap<FuncCallContext, Integer>(); //ctx -> line number of line where jump needs to happen

	/**Edge-case where e.g. return address and parameters are already being pushed on the stack.
	 * It indicates how many spaces to skip *additionally* **/
	private int pushedDuringFunctionCallSetup = 0;

	private int branchJoinEndprog = -1;

	public static final int STACK_SIZE = 128;
	public static final int STACK_START = 0; //ehh...
	public static final int STACK_END = STACK_START + STACK_SIZE - 1;

	public static final int LOCAL_HEAP_SIZE = 1024;
	public static final int LOCAL_HEAP_START = 0;
	public static final int LOCAL_HEAP_END = STACK_END + LOCAL_HEAP_SIZE - 1;

	public static final int GLOBAL_HEAP_START = LOCAL_HEAP_END + 1;
	public static final int GLOBAL_HEAP_END = 16 * 1000 * 1000 - 1;
	public static final int GLOBAL_HEAP_SIZE = GLOBAL_HEAP_START - GLOBAL_HEAP_END;
	private static final char STRING_TERMINATOR = '$';

	// Location of the heap pointer (points to next free space on heap
	final int MEMADDR_LOCAL_HP = 32;
	final int MEMADDR_GLOBAL_HP = 8096;

	public Generator(Result result, HashMap<String, Function> functionTable) {
		this.result = result;
		this.functionTable = functionTable;
		freeRegisters.addAll(Op.gpRegisters);
	}
	

	@Override
	public Op visitFuncDecl(FuncDeclContext ctx)
	{
		// func #funcStat
		// func: FUNC retType ID LPAR params RPAR LCURL stat* ret RCURL;
		Scope newScope = result.getScope(ctx);
		doAssert(newScope.getPreviousScope() == this.currentScope);
		this.currentScope = newScope;
		debug("newScope: \n" + currentScope);

		functionTable.get(ctx.ID().getText()).startLine = program.size();

		List<StatContext> stats = ctx.stat();
		// add instructions to reserve space for local variables on the stack
		int toPop = addReserveForLocalVariables(stats);

		// insert function's instructions into program (duh)
		for (StatContext stat : stats) {
			visit(stat);
		}

		// add instructions to remove parameters from stack, and restore PC with return address
		// RETURN: add instructions to remove parameters from stack, restore PC with return address, and put returnvalue on the stack
		visit(ctx.ret());
		Reg rReturnValue = popReg();

		// add instructions to "pop" the local variables from the stack
		addDecrSp(toPop);

		// Pop parameters and get the return address
		Reg rReturnAddress = getFreeReg();
		addDecrSp(functionTable.get(ctx.ID().getText()).params.size());
		add(Pop, rReturnAddress);
		add(Push, rReturnValue);

		add(Jump, ind(rReturnAddress)); // TODO: maybe can be absolute if I have a proper function table I think...
		freeReg(rReturnAddress);
		freeReg(rReturnValue);

		this.currentScope = this.currentScope.getPreviousScope();

		return null;
	}

	@Override
	public Op visitRet(RetContext ctx)
	{
		// push return value on stack
		visit(ctx.expr());
		Reg rReturnValue = popReg();
		pushReg(rReturnValue);
		return null;
	}
	
	public Register[] addPushAllRegisters(){
		EnumSet<Register> usedRegisters = Op.gpRegisters.clone();
		boolean removeAll = usedRegisters.removeAll(freeRegisters);
		doAssert(removeAll);

		Register[] inUse = usedRegisters.toArray(new Register[usedRegisters.size()]);

		for(int i = 0; i<inUse.length; i++){
			add(Push, reg(inUse[i]));
		}
		return inUse;
	}
	
	public void addPopPreservedRegisters(Register[] preservedRegisters){
		for(int i = preservedRegisters.length-1; i>=0; i--){
			add(Pop, reg(preservedRegisters[i]));
		}
		
	}
	
	@Override
	public Op visitFuncCall(FuncCallContext ctx)
	{
		// funcCall: ID LPAR expr RPAR;
		//first pushing all registers on the stack for preservation
		Register[] preservedRegisters = addPushAllRegisters();
		
		//add instructions to push return address on the stack: current PC + skipConstant
		Reg rReturnAddress = getFreeReg();
		int returnAddressLine = addPlaceholder("Return address line for: "+ctx.ID().getText());
		doAssert(pushedDuringFunctionCallSetup == 0);
		add(Push, rReturnAddress);
		pushedDuringFunctionCallSetup = 1;

		//add instructions to push arguments on the stack
		List<ExprContext> exprs = ctx.expr();
		for (ExprContext expr : exprs) {
			visit(expr);
			Reg rExpr = popReg();
			add(Push, rExpr);
			pushedDuringFunctionCallSetup++; 
			freeReg(rExpr);
		}
		
		pushedDuringFunctionCallSetup = 0;
		
		//add instructions to Call the function!!!
		functionPlaceholders.put(ctx, addPlaceholder("Jump to absolute function start address of : "+ctx.ID().getText()));
		//	will be similar to : add(Jump, abs(functionTable.get(ctx.ID().getText()).startLine));

		changeAt(returnAddressLine, Const, constOp(program.size()), rReturnAddress); //return just after the Jump instruction
		freeReg(rReturnAddress);
		
		
		//pop returnvalue of the stack and put it in some register
		Reg rReturnValue = getFreeReg();
		add(Pop, rReturnValue);

		addPopPreservedRegisters(preservedRegisters);
		
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
		// mainfunc: MAIN LCURL stat* RCURL;
		Scope newScope = result.getScope(ctx);
		doAssert(newScope.getPreviousScope() == this.currentScope);
		this.currentScope = newScope;
		debug("newScope: \n" + currentScope);
		
		//All threads except spid=0 should wait
		if(result.numberOfSprockells>1){
			Reg rIsNotMain = getFreeReg();
			Reg rJumpToRelease = getFreeReg();
			Reg rRetry = rIsNotMain;
			add(Compute, operator(Equal), reg(SPID), reg(Zero), rIsNotMain);
			int jumpToMainLine = addPlaceholder("Jump to main");

			int retryLine = program.size();
			add(Read, memAddr(result.getStaticGlobals().get("threadsReleasedJump")));
			add(Receive, rJumpToRelease);
			add(Compute, operator(Equal), rJumpToRelease, reg(Zero), rRetry);
			add(Branch, rRetry, abs(retryLine));
			add(Jump, ind(rJumpToRelease));

			int mainLine = program.size();
			changeAt(jumpToMainLine, Branch, rIsNotMain, abs(mainLine));

			// Write number of sprockells to join --TODO: make sure this part only gets executed by spid=0!
			int unjoinedThreadsAddr = result.getStaticGlobals().get("unjoinedThreads");
			int unjoinedThreadsLockAddr = result.getStaticGlobals().get("unjoinedThreadsLock");
			Reg rUnjoinedThreads = getFreeReg();
			add(Const, constOp(result.numberOfSprockells), rUnjoinedThreads);
			addObtainLock("unjoinedThreadsLock");
			add(Write, rUnjoinedThreads, memAddr(unjoinedThreadsAddr));
			addReleaseLock("unjoinedThreadsLock");

			freeReg(rJumpToRelease);
		freeReg(rRetry);
		freeReg(rUnjoinedThreads);
		}

		List<StatContext> stats = ctx.stat();
		int toPop = addReserveForLocalVariables(stats);

		for (StatContext child : stats) {
			visit(child);
		}

		Reg rPop = getFreeReg();
		add(Const, constOp(toPop), rPop);
		add(Compute, operator(Add), reg(Op.Register.SP), rPop, reg(Op.Register.SP));

		freeReg(rPop);
		currentScope = currentScope.getPreviousScope();
		doAssert(currentScope.getPreviousScope() == null);

		add(EndProg); // make sure no 'random' code is executed
		return null;
	}
	
	/**
	 * Adds instructions to effectively pop n variables from the stack
	 */
	private void addDecrSp(int toPop)
	{
		Reg r1 = getFreeReg();
		add(Const, constOp(toPop), r1);
		add(Compute, operator(Add), reg(Op.Register.SP), r1, reg(Op.Register.SP));
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
		Reg rLoc = null;
		if (ctx.PTRASSIGN() != null) {
			rLoc = addReferVariableIntoReg(ctx.ID(1).getText());
		}
		else {
			rLoc = reg(Zero); // null-pointer
		}
		addSave(ctx.ID(0).getText(), rLoc);
		freeReg(rLoc);
		return null;
	}

	/**
	 * Add instructions to get the reference to 'variable', and put it inside a register
	 * @param variable - Name of which variable's memory address to store in a register
	 * @return Reg containing the reference to the variable
	 */
	private Reg addReferVariableIntoReg(String variable)
	{
		Reg rLoc;
		rLoc = getFreeReg();
		MemoryLocation assignLoc = currentScope.getMemLoc(variable);
		if (assignLoc.isGlobal()) {
			int totalOffset = assignLoc.getTotalOffset();
			add(Const, constOp(totalOffset), rLoc);
		}
		else {
			if (assignLoc.isOnStack()) {
				int totalOffset = currentScope.getStackOffset(variable);
				totalOffset += pushedDuringFunctionCallSetup;
				add(Const, constOp(totalOffset), rLoc);
				add(Compute, operator(Add), reg(SP), rLoc, rLoc);
				// throw new IllegalArgumentException("No pointers to stack variables allowed!");
			}
			else {
				int totalOffset = assignLoc.getTotalOffset();
				add(Const, constOp(totalOffset), rLoc);
			}
		}
		return rLoc;
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
		// doAssert(memoryAddress >= 0 && memoryAddress < GLOBAL_HEAP_END);
		Reg rLoc = getFreeReg();
		addLoadInto(id, rLoc);

		Reg rCmp = getFreeReg();
		Reg rConst = getFreeReg();
		add(Const, constOp(LOCAL_HEAP_SIZE), rConst);
		add(Compute, operator(GtE), rLoc, rConst, rCmp);
		int branchLine = addPlaceholder("deref branchLine");

		// from local memory
		add(Load, deref(rLoc), rLoc);
		int jumpToEndLine = addPlaceholder("deref jump to end");
		int elseLine = program.size();
		changeAt(branchLine, Branch, rCmp, abs(elseLine));

		// from global memory
		add(Read, deref(rLoc));
		add(Receive, rLoc);

		int nextEnterLine = program.size();

		changeAt(jumpToEndLine, Jump, abs(nextEnterLine));

		freeReg(rCmp);
		freeReg(rConst);
		// (no need to push, this is handled in visitPtrDerefExpr)

		return rLoc;
	}

	@Override
	public Op visitAssignDeref(AssignDerefContext ctx)
	{
		visit(ctx.expr());
		Reg rExpr = popReg();
		String target = ctx.derefTarget().ID().getText();

		Reg rLoc = getFreeReg();
		addLoadInto(target, rLoc);

		Reg rCmp = getFreeReg();
		Reg rConst = getFreeReg();
		add(Const, constOp(LOCAL_HEAP_SIZE), rConst);
		add(Compute, operator(GtE), rLoc, rConst, rCmp);
		int branchLine = addPlaceholder("deref branchLine");

		// from local memory
		add(Store, rExpr, deref(rLoc));
		int jumpToEndLine = addPlaceholder("deref jump to end");
		int elseLine = program.size();
		changeAt(branchLine, Branch, rCmp, abs(elseLine));

		// from global memory
		add(Write, rExpr, deref(rLoc));

		int nextEnterLine = program.size();

		changeAt(jumpToEndLine, Jump, abs(nextEnterLine));

		freeReg(rCmp);
		freeReg(rConst);
		freeReg(rExpr);
		freeReg(rLoc);
		// (no need to push, this is handled in visitPtrDerefExpr)

		return null;
	}

	private void addSave(String variable, Reg reg)
	{
		MemoryLocation loc = currentScope.getMemLoc(variable);
		if (loc.isLocal()) {
			if (loc.isOnStack()) {
				// relative
				Reg rLoc = getFreeReg();
				int stackOffset = currentScope.getStackOffset(variable);
				stackOffset+=pushedDuringFunctionCallSetup; //Edge-case where e.g. return address and parameters are already being pushed on the stack...
				add(Const, constOp(stackOffset), rLoc);
				add(Compute, operator(Add), reg(SP), rLoc, rLoc);
				add(Store, reg, deref(rLoc));
				freeReg(rLoc);
			}
			else {
				// absolute
				add(Store, reg, addr(loc.getScopeOffset(), loc.getVarOffset()));
			}
		}
		else if (loc.isGlobal()) {
			add(Write, reg, addr(loc.getScopeOffset(), loc.getVarOffset())); // store start address on stack at variable
		}
		else {
			throw new IllegalArgumentException("Neither global nor local!");
		}
	}

	/**
	 * @return memory address, depending on whether it's on stack etc...
	 */
	public int addLoadInto(String variable, Reg reg)
	{
		MemoryLocation loc = currentScope.getMemLoc(variable);
		if (loc.isLocal()) {
			if(loc.isOnStack()){
				//relative
				Reg rLoc = getFreeReg();
				int stackOffset = currentScope.getStackOffset(variable);
				stackOffset+=pushedDuringFunctionCallSetup; //Edge-case where e.g. return address and parameters are already being pushed on the stack...
				add(Const, constOp(stackOffset), rLoc);
				add(Compute, operator(Add), reg(SP), rLoc, rLoc);
				add(Load, deref(rLoc), reg);
				freeReg(rLoc);
				return stackOffset;
			}else{
				//on local heap, absolute
				add(Load, memAddr(loc.getTotalOffset()), reg);
				return loc.getTotalOffset();
			}
		}
		else if (loc.isGlobal()) {
			add(Read, memAddr(loc.getTotalOffset()));
			add(Receive, reg);
			return loc.getTotalOffset();
		}
		else {
			throw new IllegalArgumentException("Neither global nor local!");
		}
	}
	
	@Override
	public Op visitPtrRefExpr(PtrRefExprContext ctx)
	{
		// | REF ID #ptrRefExpr
		Reg rAddr = addReferVariableIntoReg(ctx.ID().getText());
		pushReg(rAddr);
		return null;
	}
	
	@Override
	public Op visitConstTextExpr(ConstTextExprContext ctx)
	{
		Reg rArrayPointer = addGetGlobalHeappointer();
		Reg rOne = getFreeReg();
		add(Const, constOp(1), rOne);
		String text = ctx.STRING().getText();
		text = text.substring(1, text.length()-1);
		text = text + STRING_TERMINATOR;
		char[] chars = text.toCharArray();
		for (char c : chars) {
			Reg rChar = getFreeReg();
			add(Const, constOp(c), rChar);
			add(Write, rChar, deref(rArrayPointer));
			add(Compute, operator(Add), rOne, rArrayPointer, rArrayPointer);
			freeReg(rChar);
		}
		
		addSaveGlobalHeappointer(rArrayPointer);
		//Decremen rArrayPointer again, because we want to 'return' the BASE address
		add(Const, constOp(chars.length), rOne); 
		add(Compute, operator(Sub), rArrayPointer, rOne, rArrayPointer);
		pushReg(rArrayPointer);
		freeReg(rOne);
		return null;
	}
	
	@Override
	public Op visitArrayExpr(ArrayExprContext ctx)
	{
		Reg rArrayPointer = addGetGlobalHeappointer();
		Reg rOne = getFreeReg();
		add(Const, constOp(1), rOne);
		int i = 0;
		for (i = 0; i < ctx.expr().size(); i++) {
			visit(ctx.expr(i));
			Reg rExpr = popReg();
			add(Write, rExpr, deref(rArrayPointer));
			add(Compute, operator(Add), rOne, rArrayPointer, rArrayPointer);
			freeReg(rExpr);
		}
		freeReg(rOne);
		pushReg(rArrayPointer);
		return null;
	}

	/**
	 * Write back heappointer
	 */
	private void addSaveLocalHeappointer(Reg rHp)
	{
		add(Write, rHp, memAddr(MEMADDR_LOCAL_HP));
	}

	private void addSaveGlobalHeappointer(Reg rHp)
	{
		add(Write, rHp, memAddr(MEMADDR_GLOBAL_HP));
	}

	@Override
	public Op visitArrayDecl(ArrayDeclContext ctx)
	{
		visit(ctx.expr()); // write the to be allocated size to register
		Reg rArraySize = popReg();
		addAllocateGlobal(rArraySize, ctx.ID().getText());
		return null;
	}

	/**
	 * Add instructions to retrieve the base address of some array, given a variable name Leaks a register!
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
	 * Adds instructions to allocate an array on the GLOBAL heap Note: it consumes a registers, which is free'd implicitly!
	 * 
	 * @param rArraySize *            - Register containing the size of the array
	 * @param variableName *            - Where (which variable) to store the array starting pointer
	 */
	private void addAllocateGlobal(Reg rArraySize, String variableName)
	{
		// Retrieve heappointer
		Reg rHeapPointer = addGetGlobalHeappointer();

		// Store current heappointer at at variable
		addSave(variableName, rHeapPointer);

		// Increase and write back changed heappointer
		addIncrementHeappointer(rArraySize, rHeapPointer);
	}
//
	/**
	 * Increments and writes back the heap pointer Note: it consumes two registers, both of which are free'd implicitly
	 * @param rArraySize - Register containing the size of the array
	 * @param rHeapPointer - Register containing the current end of the heap
	 */
	private void addIncrementHeappointer(Reg rArraySize, Reg rHeapPointer)
	{
		add(Compute, operator(Add), rArraySize, rHeapPointer, rHeapPointer);
		add(Write, rHeapPointer, memAddr(MEMADDR_GLOBAL_HP));// maybe TODO: test and set?
		freeReg(rArraySize);
		freeReg(rHeapPointer);
	}

	/**
	 * Add instructions to put the current heap pointer (end of heap) in a register
	 * 
	 * @return register containing the read pointer
	 */
	private Reg addGetLocalHeappointer()
	{
		Reg rHeapPointer = getFreeReg();
		add(Load, memAddr(MEMADDR_LOCAL_HP), rHeapPointer);
		return rHeapPointer;
	}

	private Reg addGetGlobalHeappointer()
	{
		Reg rHeapPointer = getFreeReg();
		add(Read, memAddr(MEMADDR_GLOBAL_HP));
		add(Receive, rHeapPointer);
		return rHeapPointer;
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
		add(Compute, operator(Equal), r1, reg(Zero), r1);
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
		add(Compute, operator(Equal), continueReg, reg(Zero), continueReg);
		freeReg(continueReg);
		int branchLine = addPlaceholder("whileBranch");
		visit(ctx.stat()); // body
		add(Jump, abs(evalLine));
		int nextEnterLine = program.size();
		changeAt(branchLine, Branch, continueReg, abs(nextEnterLine));
		return null;
	}

	@Override
	public Op visitOperatorExpr(OperatorExprContext ctx)
	{
		visit(ctx.expr(0));
		visit(ctx.expr(1));
		Reg r1 = popReg();
		Reg r2 = popReg();
		Operator operator = Op.getOperatorByString(ctx.OPERATOR().getText());
		add(Compute, operator(operator),r2 , r1, r1);
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
		else if (ctx.GTE() != null) {
			operator = Operator.GtE;
		}
		else if (ctx.LTE() != null) {
			operator = Operator.LtE;
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
	
	@Override
	public Op visitSprockellIdExpr(SprockellIdExprContext ctx)
	{
		pushReg(reg(Register.SPID));
		return null;
	}

	private void freeReg(Reg r)
	{
		debugReg("Free : " + r.name);
		doAssert(!regStack.contains(r.reg));
		doAssert(!freeRegisters.contains(r.reg));
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
		debugReg("currently free : " + freeRegisters);
		Register reg = freeRegisters.pop();
		debugReg("Get: " + reg);
		return reg(reg);
	}

	@Override
	public Op visit(ParseTree tree)
	{
		debug("----- visiting : " + tree.getText());
		return super.visit(tree);
	}

	// type ID (ASSIGN expr)? SEMI #decl
	@Override
	public Op visitDecl(DeclContext ctx)
	{
		if(ctx.type().arrayType().ARRAY()!=null){
			Reg rArrayPointer = addGetGlobalHeappointer();
			addSave(ctx.ID().getText(), rArrayPointer);
			freeReg(rArrayPointer);

			visit(ctx.expr()); // visitArrayExpr: i.e. multiple expressions, e.g. [3,6,2]
			rArrayPointer = popReg(); // new heap end

			// Write back rArrayPointer to the heapEnd pointer
			addSaveGlobalHeappointer(rArrayPointer);
			freeReg(rArrayPointer);
		}
		else {
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
		}
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
		freeReg(r1);
		return null;
	}

	@Override
	public Op visitPrintExprStat(PrintExprStatContext ctx)
	{
			int ASCII_NEWLINE = (int) '\n';
			int NUM_OFFSET_ASCII = 48;
		//Print string or Number
		if (result.getType(ctx.expr()) == Type.TEXT) {
			//PRINT STRING
			visit(ctx.expr());
			Reg rStringPointer = popReg();

			int evalLine = program.size();

			Reg rChar = getFreeReg();
			Reg rContinue = getFreeReg();
			Reg rTermChar = getFreeReg();
			add(Const, constOp(STRING_TERMINATOR), rTermChar);
			add(Read, deref(rStringPointer));
			add(Receive, rChar);
			add(Compute, operator(Equal), rChar, rTermChar, rContinue); // these can be optimized if \0 terminated
			freeReg(rContinue);
			int branchLine = addPlaceholder("outBranch");

			// --startbody-------------

			add(Write, rChar, MemAddr.StdIO); // write char

			// increment pointer
			Reg rOne = getFreeReg();
			add(Const, constOp(1), rOne);
			add(Compute, operator(Add), rStringPointer, rOne, rStringPointer);

			freeReg(rStringPointer);
			freeReg(rChar);
			freeReg(rOne);
			// --endbody-------------

			add(Jump, abs(evalLine));
			int nextEnterLine = program.size();
			changeAt(branchLine, Branch, rContinue, abs(nextEnterLine));
			freeReg(rTermChar);
		}
		else {
			//PRINT NUMBER
			visit(ctx.expr());
			Reg rNum = popReg();
			Reg rRest = getFreeReg();

			//First find 'length' of number
			Reg rDiv = getFreeReg();
			Reg rRetry = getFreeReg();
			Reg rTen = getFreeReg();
			add(Const, constOp(10), rTen);

			//dowhile(rNum `div` rDiv > 10)
			add(Const, constOp(1), rDiv);
			int retryFindMaxLine = program.size();
			add(Compute, operator(Div), rNum, rDiv, rRest);
			add(Compute, operator(GtE), rRest, rTen, rRetry); 
			add(Compute, operator(Mul),rDiv, rTen, rDiv);
			add(Branch, rRetry, abs(retryFindMaxLine));

			freeReg(rRest);
			freeReg(rRetry);
			rRetry = rTen;

			Reg rNumOffset = getFreeReg();
			add(Const, constOp(NUM_OFFSET_ASCII), rNumOffset);
			Reg rChar = getFreeReg();

			//dowhile(rDiv > 10): print head(tail (number))
			int continuePrintingLine = program.size();
			// --startbody-------------
			add(Const, constOp(10), rTen);
			add(Compute, operator(Div), rDiv, rTen, rDiv); 
			add(Compute, operator(Div), rNum, rDiv, rChar); //remove tail
			add(Compute, operator(Mod), rChar, rTen, rChar); //get head

			add(Compute, operator(Add), rChar, rNumOffset, rChar);
			add(Write, rChar, MemAddr.StdIO); // write char
			// --endbody-------------

			add(Compute, operator(GtE), rDiv, rTen, rTen); 
			add(Branch, rTen, abs(continuePrintingLine));
			freeReg(rNum);
			freeReg(rDiv);
			freeReg(rTen);
			freeReg(rChar);
			freeReg(rNumOffset);
		}
		Reg rNewline = getFreeReg();
		add(Const, constOp(ASCII_NEWLINE), rNewline);
		add(Write, rNewline, MemAddr.StdIO);
		freeReg(rNewline);
		return null;
	}

	private void copyReg(Reg rNum, Reg rRest)
	{
		add(Push, rNum);
		add(Pop, rRest);
	}


	@Override
	public Op visitBlockStat(BlockStatContext ctx)
	{
		Scope newScope = result.getScope(ctx);
		doAssert(newScope.getPreviousScope() == this.currentScope);
		this.currentScope = newScope;
		debug("newScope: \n"+currentScope);

		List<StatContext> stats = ctx.stat();

		if (currentScope.getPreviousScope() != null) {
			int toPop = addReserveForLocalVariables(stats);
			for (StatContext child : stats) {
				visit(child);
			}
			addDecrSp(toPop);
		}
		else {
			for (StatContext child : stats) {
				visit(child);
			}
		}
		currentScope = currentScope.getPreviousScope();
		return null;
	}

	private void reserveStackSpace(String s)
	{
		if(currentScope.getMemLoc(s).isOnStack()){
			debug(String.format("Reserved stack space %s", s));
			add(Push, reg(RegReserved));
		}else{
			throw new IllegalAccessError("Can't reserve space for non-stack variable...");
		}
	}

	/**
	 * Prints a debug message to the output
	 */
	private void debug(String s)
	{
		if (DEBUG_OTHER) {
			System.out.println(s);
		}
	}
	
	private void debugReg(String s)
	{
		if (DEBUG_REG) {
			System.out.println(s);
		}
	}
	
	@Override
	public Op visitForkStat(ForkStatContext ctx)
	{
		int address = result.getStaticGlobals().get("threadsReleasedJump");
		add(Write, reg(PC), memAddr(address));
		return null;
	}
	
	@Override
	public Op visitJoinStat(JoinStatContext ctx)
	{
		// first get, decrement and save number of unjoined threads
		addObtainLock("unjoinedThreadsLock"); // Make sure this operation is locked!

		Reg rOne = getFreeReg();
		add(Const, constOp(1), rOne);
		Reg rUnjoined = getFreeReg();
		add(Read, memAddr(result.getStaticGlobals().get("unjoinedThreads")));
		add(Receive, rUnjoined);
		add(Compute, operator(Sub), rUnjoined, rOne, rUnjoined);
		add(Write, rUnjoined, memAddr(result.getStaticGlobals().get("unjoinedThreads")));
		addReleaseLock("unjoinedThreadsLock");
		// add(Compute, operator(And), reg(SPID), rUnjoined, rUnjoined);
		// add(Branch, reg(SPID), ind(reg(PC)));

		// Now terminate if SPID != 0
		branchJoinEndprog = addPlaceholder("Jump to endProg join");
		//	add(Branch, reg(SPID), rel(0)); // should terminate the sprockell right?

		// SPID = 0 should continue checking the counter in a dowhile

		int spid0RetryLine = program.size();
		add(Read, memAddr(result.getStaticGlobals().get("unjoinedThreads")));
		add(Receive, rUnjoined);
		add(Branch, rUnjoined, abs(spid0RetryLine));

		freeReg(rOne);
		freeReg(rUnjoined);
		return null;
	}

	/**
	 * Adds instructions to wait until the lock with 'name' is obtained
	 */
	private void addObtainLock(String name)
	{
		Reg rLockAddress = getFreeReg();
		int lockAddress = result.getStaticGlobals().get(name);
		add(Const, constOp(lockAddress), rLockAddress);

		Reg rIsLocked = getFreeReg();
		int retryLine = program.size();
		add(TestAndSet, deref(rLockAddress));
		add(Receive, rIsLocked);
		add(Compute, operator(Equal), reg(Zero), rIsLocked, rIsLocked);
		add(Branch, rIsLocked, abs(retryLine));
		freeReg(rLockAddress);
		freeReg(rIsLocked);
	}
	
	private void addReleaseLock(String name){
		Reg rLockAddress = getFreeReg();
		int lockAddress = result.getStaticGlobals().get(name);
		add(Const, constOp(lockAddress), rLockAddress);

		add(Write, reg(Zero), deref(rLockAddress));
		freeReg(rLockAddress);
	}
	
	@Override
	public Op visitLockDecl(LockDeclContext ctx)
	{
		//Just zeroing the lock value and write the pointer to the variable
		Reg rLockPointer = getFreeReg();
		Const lockAddr = constOp(result.getStaticGlobals().get(ctx.ID().getText()));
		add(Const, lockAddr, rLockPointer);
		add(Write,reg(Zero) ,deref(rLockPointer) );
		addSave(ctx.ID().getText(), rLockPointer);
		freeReg(rLockPointer);
		return null;
	}
	
	@Override
	public Op visitLockStat(LockStatContext ctx)
	{
		String name = ctx.ID().getText();
		addObtainLock(name);
		return null;
	}

	@Override
	public Op visitUnlockStat(UnlockStatContext ctx)
	{
		String name = ctx.ID().getText();
		addReleaseLock(name);
		return null;
	}
	
	@Override
	public Op visitProgram(ProgramContext ctx)
	{
		//First reserve global heap space for static variables, e.g. locks should always be at the same location
		int numberOfStaticGlobals = result.numberOfStaticGlobals;
		Reg rHeappointer = addGetGlobalHeappointer();
		Reg rStaticGlobals = getFreeReg();
		add(Const, constOp(numberOfStaticGlobals), rStaticGlobals);
		addIncrementHeappointer(rStaticGlobals, rHeappointer);
		
		List<ParseTree> unordered = new ArrayList<ParseTree>(ctx.stat().children);
		List<ParseTree> reordered = new ArrayList<ParseTree>(unordered.size());
		//REORDER: DECLARATIONS FIRST
		for (ParseTree parseTree : unordered) {
			if(parseTree instanceof DeclContext 
					|| parseTree instanceof PtrDeclContext ||
					parseTree instanceof ArrayDeclContext || parseTree instanceof ArrayDeclInitContext){
				reordered.add(parseTree);
			}
		}
		//REORDER: MAIN THEN
		for (ParseTree parseTree : unordered) {
			if(parseTree instanceof MainFuncStatContext ){
				reordered.add(parseTree);
				unordered.remove(parseTree);
				break;
			}
		}
		
		//REORDER: ADD REST
		reordered.addAll(unordered);
		ctx.stat().children = reordered;
		
		//int jumpLine = addPlaceholder("Jump to main function here...");
		for (ParseTree child : ctx.children) {
			visit(child);
		}

		//replace all function jump placeholders with actual start addresses
		for (Iterator<FuncCallContext> it = functionPlaceholders.keySet().iterator(); it.hasNext();) {
			FuncCallContext fcCtx = it.next();
			String name = fcCtx.ID().getText();
			Function function = functionTable.get(name);
			changeAt(functionPlaceholders.get(fcCtx), Jump, abs(function.startLine));
		}
		
		if(branchJoinEndprog != -1){
			//at the join statement, the 'other' sprockells should jump to endprog
			changeAt(branchJoinEndprog, Branch, reg(SPID), abs(program.size())); 
		}
		
		add(EndProg); //Just to be sure in the case main didn't add one...
		doAssert(freeRegisters.containsAll(Op.gpRegisters));
		doAssert(regStack.isEmpty());
		return null;
	}

	@Override
	public Op visitAndExpr(AndExprContext ctx)
	{
		visit(ctx.expr(0));
		visit(ctx.expr(1));
		Reg r1 = popReg();
		Reg r2 = popReg();
		add(Compute, operator(And), r1, r2, r1);
		freeReg(r2);
		pushReg(r1);
		return null;
	}
	
	@Override
	public Op visitOrExpr(OrExprContext ctx)
	{
		visit(ctx.expr(0));
		visit(ctx.expr(1));
		Reg r1 = popReg();
		Reg r2 = popReg();
		add(Compute, operator(Or), r1, r2, r1);
		freeReg(r2);
		pushReg(r1);
		return null;
	}
	
	@Override
	public Op visitNotExpr(NotExprContext ctx)
	{
		visit(ctx.expr());
		Reg r1 = popReg();
		add(Compute, operator(Equal), r1, reg(Zero), r1);
		pushReg(r1);
		return null;
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

	public Operand.Target.Rel rel(int relJump)
	{
		return new Operand.Target.Rel(relJump);
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
