grammar Crackl;

program: PROGRAM_START LCURL stat+ RCURL;

stat: type ID (ASSIGN expr)? SEMI         		#decl
    | target ASSIGN expr SEMI             		#assignStat
    | IF LPAR expr RPAR stat (ELSE stat)? 		#ifStat 
    | WHILE LPAR expr RPAR stat           		#whileStat 
    | FOR LPAR ID ASSIGN expr SEMI
               expr SEMI
               ID ASSIGN expr RPAR stat   		#forStat 
    | LCURL stat* RCURL                 		#blockStat
    | func										#funcStat
    | PRINT LPAR STRING (COMMA ID)* RPAR SEMI 	#printStat
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

PROGRAM_START: 'Program';
LCURL: '{';
RCURL: '}'; 
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