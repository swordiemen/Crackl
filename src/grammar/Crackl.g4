grammar Crackl;

program: PROGRAM_START numOfSprockell? stat;

stat: GLOBAL? type ID  (ASSIGN expr)? SEMI  		#decl
	| LOCK ID SEMI									#lockDecl
	| GLOBAL? type ID ASSIGN expr SEMI				#arrayDeclInit
	| GLOBAL? type LSQ expr RSQ ID SEMI				#arrayDecl 
	| GLOBAL? type ID (PTRASSIGN ID)? SEMI			#ptrDecl
	| GLOBAL? type ID ASSIGN ID SEMI				#ptrDeclNormal
	| ID PTRASSIGN ID SEMI							#ptrAssign
    | target ASSIGN expr SEMI             			#assignStat
    | derefTarget ASSIGN expr SEMI             		#assignDeref
    | target LSQ expr RSQ ASSIGN expr SEMI     		#arrayAssignStat
    | IF LPAR expr RPAR stat (ELSE stat)? 			#ifStat 
    | WHILE LPAR expr RPAR stat           			#whileStat 
    | LCURL stat* RCURL                 			#blockStat
    | funcCall SEMI									#funcCallStat
    | mainfunc										#mainFuncStat
    | funcDecl										#funcDeclStat
    | PRINT LPAR expr RPAR SEMI 					#printExprStat
    | LOCK LPAR ID RPAR SEMI						#lockStat
    | UNLOCK LPAR ID RPAR	SEMI					#unlockStat
    | FORK 											#forkStat
    | JOIN				 							#joinStat
     ;

     
numOfSprockell: (LPAR SPROCKELLCOUNT ASSIGN NUM RPAR);
    
funcDecl: FUNC retType ID LPAR params RPAR LCURL stat* ret RCURL;
mainfunc: MAIN LCURL stat* RCURL;
global: GLOBAL LCURL  RCURL;

ret: 'return' expr? SEMI;
params: ((type ID COMMA)* type ID)?;
    
target: ID;
derefTarget: DEREF ID;

type: PTRTYPE? arrayType | PTRTYPE type;
arrayType: primitive ARRAY? | primitive type;
primitive: INTTYPE | BOOLTYPE | TEXTTYPE;
retType: type | VOID;

funcCall: ID LPAR (expr? | (expr (COMMA expr)*)) RPAR;

expr: ID LSQ expr RSQ				#arrayIndexExpr
    | LSQ (expr (COMMA expr)*)?	RSQ	#arrayExpr
    | NOT expr                      #notExpr
    | SIGNOPERATOR expr 			#negExpr
    | expr OTHEROPERATOR expr      		#otherOperatorExpr
    | expr SIGNOPERATOR expr      		#signOperatorExpr
    | expr AND expr                 #andExpr
    | expr OR  expr                 #orExpr
    | expr (LT | GT | EQ | NE | GTE | LTE) expr #compExpr
	| funcCall						#funcExpr
    | LPAR expr RPAR                #parExpr
    | DEREF ID						#ptrDerefExpr
    | REF ID						#ptrRefExpr
    | NUM				            #constNumExpr
    | BOOL							#constBoolExpr
    | ID                            #idExpr
    | STRING						#constTextExpr
    | SPROCKELLID					#sprockellIdExpr
    ;

INTTYPE: 'int';
BOOLTYPE: 'boolean';
TEXTTYPE: 'text';

OTHEROPERATOR: (MODULO | MULTIPLY | DIVIDE);
SIGNOPERATOR: (MINUS|PLUS);

LOCK: 'lock';
UNLOCK: 'unlock';
BOOL: 'true' | 'false';
ARRAY: LSQ RSQ;
SPROCKELLID: 'getSprockellId()';
CREATELOCK: 'createLock()';
FORK: 'releaseSprockells();';
JOIN: 'joinSprockells();';

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
MODULO: '%';
MULTIPLY: '*';
DIVIDE: '/'; 
AND: '&&';
OR: '||';
GTE: '>=';
LTE: '<=';
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
SPROCKELLCOUNT: 'sprockells';

fragment LETTER: [a-zA-Z];
fragment DIGIT: [0-9];
fragment UNDERSCORE: '_';

ID: LETTER (LETTER | DIGIT | UNDERSCORE)*;
NUM: DIGIT+;
STRING: '"' (~["\\] | '\\'.)* '"';

COMMENT: FSLASH FSLASH (.)*? '\n' -> skip;
WS: [ \t\r\n]+ -> skip;
