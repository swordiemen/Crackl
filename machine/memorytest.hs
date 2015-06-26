import Sprockell.Sprockell
import Sprockell.System
import Sprockell.TypesEtc

prog :: [Instruction]
prog = [ 
	Push Zero,
	Const 1 RegA,
	Const 3 RegB,
	Store RegA (Addr 10),
	Write RegB (Addr 10),
	Load (Addr 10) RegC,
	Read (Addr 10),
	Receive RegD,
	Write RegC stdio,
	Write RegD stdio,
	Nop,
	Nop,
	Nop,
	Nop,
	Nop,
	Nop,
	Nop,
	Nop,
	Nop,
	Nop,
	EndProg	]
main = run 1 prog