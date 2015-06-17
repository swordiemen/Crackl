import Sprockell.Sprockell
import Sprockell.System
import Sprockell.TypesEtc

prog :: [Instruction]
prog = [  
		  Const 72 RegA
		, Write RegA stdio

		, Const 69 RegA
		, Write RegA stdio

		, Const 76 RegA
		, Write RegA stdio

		, Const 76 RegA
		, Write RegA stdio

		, Const 79 RegA
		, Write RegA stdio

		, Const 2 RegB
		, Const 3 RegC
		, Push RegB
		, Push RegC
		, Pop RegD
		, Pop RegE
		, Compute Mul RegD RegE RegA
		, Const 48 RegB --number offset utf16
		, Compute Add RegA RegB RegA
		, Write RegA stdio


        , EndProg
	]

main = run 1 prog
