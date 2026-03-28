grammar Expr;

prog: stat* EOF;

stat
    : 'var' ID ('=' expr)? ';'                                         #DeclVar
    | ID '=' expr ';'                                                   #Atribuicao
    | 'if' '(' boolExpr ')' '{' stat* '}' elseifClause* elseClause?   #IfStat
    | 'while' '(' boolExpr ')' '{' stat* '}'                           #WhileStat
    | 'print' '(' printArg ')' ';'                                     #PrintStat
    ;

printArg
    : expr      #PrintExpr
    | STRING    #PrintStr
    ;

elseifClause: 'else' 'if' '(' boolExpr ')' '{' stat* '}';
elseClause  : 'else' '{' stat* '}';

expr
    : <assoc=right> O1=expr '^' O2=expr             #Potencia
    | O1=expr OP=('*'|'/') O2=expr                  #MulDiv
    | O1=expr OP=('+'|'-') O2=expr                  #SomaSub
    | '(' NESTED=expr ')'                            #Parenteses
    | NUMBER                                         #Numero
    | ID                                             #UsoVariavel
    | 'input' '(' STRING ')'                        #Input
    ;

boolExpr
    : 'not' B=boolExpr                               #Not
    | O1=boolExpr 'and' O2=boolExpr                 #And
    | O1=boolExpr 'or'  O2=boolExpr                 #Or
    | boolAtom                                       #BoolAtomPassthrough
    ;

boolAtom
    : '(' NESTED=boolExpr ')'                                          #BoolParenteses
    | O1=expr OP=('<='|'>='|'=='|'!='|'<'|'>') O2=expr               #Comparacao
    | 'true'                                                           #BoolTrue
    | 'false'                                                          #BoolFalse
    ;

NUMBER  : [0-9]+ ('.' [0-9]+)?;
ID      : [a-zA-Z_][a-zA-Z_0-9]*;
STRING  : '"' (~["\r\n])* '"';
COMMENT : '//' ~[\r\n]* -> skip;
WS      : [ \t\r\n]+ -> skip;