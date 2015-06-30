grammar Crackl;

program: PROGRAM_START stat;

stat: GLOBAL? type ID  (ASSIGN expr)? SEMI  			#decl
	| GLOBAL? type ARRAY ID ASSIGN 
			LSQ expr (COMMA expr)* RSQ SEMI				#arrayDeclInit
	| GLOBAL? type LSQ expr RSQ ID SEMI					#arrayDecl 
	| GLOBAL? PTRTYPE type ID (PTRASSIGN ID)? SEMI		#ptrDecl
	| GLOBAL? PTRTYPE type ID ASSIGN ID SEMI		#ptrDeclNormal
	| ID PTRASSIGN ID SEMI							#ptrAssign
    | target ASSIGN expr SEMI             				#assignStat
    | target LSQ expr RSQ ASSIGN expr SEMI     			#arrayAssignStat
    | IF LPAR expr RPAR stat (ELSE stat)? 				#ifStat 
    | WHILE LPAR expr RPAR stat           				#whileStat 
    | FOR LPAR ID ASSIGN expr SEMI
               expr SEMI
               ID ASSIGN expr RPAR stat   				#forStat 
    | LCURL stat* RCURL                 				#blockStat
    | funcCall SEMI										#funcCallStat
    | mainfunc											#mainFuncStat
    | funcDecl											#funcDeclStat
    | PRINT LPAR expr RPAR SEMI 						#printExprStat
     ;
    
funcDecl: FUNC retType ID LPAR params RPAR LCURL stat* ret RCURL;
mainfunc: MAIN LCURL stat* RCURL;
global: GLOBAL LCURL  RCURL;

ret: 'return ' expr SEMI;
params: ''|(type ID COMMA)* type ID;
    
target: ID;

type: INTTYPE | BOOLTYPE;
retType: type | VOID;

funcCall: ID LPAR (expr? | (expr (COMMA expr)*)) RPAR;

expr: expr DOT ID                   #fieldExpr
    | funcCall						#funcExpr
    | ID LSQ expr RSQ				#arrayIndexExpr
    | NOT expr                      #notExpr
    | expr (PLUS | MINUS) expr      #addExpr
    | expr AND expr                 #andExpr
    | expr OR  expr                 #orExpr
    | expr (LT | GT | EQ | NE) expr #compExpr
    | LPAR expr RPAR                #parExpr
    | DEREF ID						#ptrDerefExpr
    | REF ID						#ptrRefExpr
    | NUM				            #constNumExpr
    | BOOL							#constBoolExpr
    | ID                            #idExpr
    ;

INTTYPE: 'int';
BOOLTYPE: 'boolean';



BOOL: 'true' | 'false';
ARRAY: LSQ RSQ;

PTRASSIGN: '=>';
PROGRAM_START: 'Program';
LCURL: '{';
RCURL: '}'; 
LSQ: '[';
RSQ: ']';
DOT: '.';
NOT: '!';
PLUS: '+';
MINUS: '-';
AND: '&&';
OR: '||';
LT: '<';
GT: '>';
EQ: '==';
NE: '!=';
LPAR: '(';
RPAR: ')';
SEMI: ';';
ASSIGN: '=';
IF: 'if';
WHILE: 'while';
ELSE: 'else';
FOR: 'for';
PRINT: 'print';
COMMA: ',';
VOID: 'void';
GLOBAL: 'global';
PTRTYPE: '#';
DEREF: '@';
REF: '&';
FSLASH: '/';
FUNC: 'func';
MAIN: 'main';

fragment LETTER: [a-zA-Z];
fragment DIGIT: [0-9];
fragment UNDERSCORE: '_';

ID: LETTER (LETTER | DIGIT | UNDERSCORE)*;
NUM: DIGIT+;
STRING: '"' (~["\\] | '\\'.)* '"';

WS: [ \t\r\n]+ -> skip;
COMMENT: FSLASH FSLASH (~['//'])* '//' -> skip;
