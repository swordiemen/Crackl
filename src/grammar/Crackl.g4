grammar Crackl;

program: PROGRAM_START stat;

stat: type ARRAY? ID  (ASSIGN expr)? SEMI  		#decl
	| type LSQ expr RSQ ID SEMI					#arrayDecl 
    | target ASSIGN expr SEMI             		#assignStat
    | target LSQ expr RSQ ASSIGN expr SEMI      #arrayAssignStat
    | IF LPAR expr RPAR stat (ELSE stat)? 		#ifStat 
    | WHILE LPAR expr RPAR stat           		#whileStat 
    | FOR LPAR ID ASSIGN expr SEMI
               expr SEMI
               ID ASSIGN expr RPAR stat   		#forStat 
    | LCURL stat* RCURL                 		#blockStat
    | func										#funcStat
    | PRINT LPAR expr RPAR SEMI 				#printExprStat
     ;
    
func: retType ID LPAR params RPAR LCURL stat* ret? RCURL;    

ret: 'return ' expr SEMI;
params: ''|(type ID COMMA)* type ID;
    
target: ID;

type: INTTYPE | BOOLTYPE;
retType: type | VOID;

funcCall: ID LPAR expr RPAR;

expr: expr DOT ID                   #fieldExpr
    | funcCall						#funcExpr
    | LSQ (expr? | 
    	expr (COMMA expr)*) RSQ		#arrayExpr
    | ID LSQ expr RSQ				#arrayIndexExpr
    | NOT expr                      #notExpr
    | expr (PLUS | MINUS) expr      #addExpr
    | expr AND expr                 #andExpr
    | expr OR  expr                 #orExpr
    | expr (LT | GT | EQ | NE) expr #compExpr
    | LPAR expr RPAR                #parExpr
    | NUM				            #constNumExpr
    | BOOL							#constBoolExpr
    | ID                            #idExpr
    ;

INTTYPE: 'int';
BOOLTYPE: 'boolean';


BOOL: 'true' | 'false';
ARRAY: LSQ RSQ;

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
FUNCTION: 'func';
VOID: 'void';


fragment LETTER: [a-zA-Z];
fragment DIGIT: [0-9];

ID: LETTER (LETTER | DIGIT)*;
NUM: DIGIT+;
STRING: '"' (~["\\] | '\\'.)* '"';

WS: [ \t\r\n]+ -> skip;