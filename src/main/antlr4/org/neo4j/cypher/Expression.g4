/**
 * The Cypher expression sub-language
 */
grammar Expression;
import Base;

expression
    : lhs=expression PLUS rhs=expression        #addition
    | lhs=expression MINUS rhs=expression       #subtraction
    | lhs=expression SLASH rhs=expression       #division
    | lhs=expression STAR rhs=expression        #multiplication
    | lhs=expression EQ rhs=expression          #equality
    | lhs=expression NEQ rhs=expression         #nonEquality
    | lhs=expression IN rhs=expression          #in
    | lhs=expression AND rhs=expression         #and
    | lhs=expression OR rhs=expression          #or
    | lhs=expression XOR rhs=expression         #or
    | LP exp=expression RP                      #parenthesis
    | list=expression LSB index=expression RSB  #listIndex
    | list=expression LSB start=expression? DOTDOT end=expression? RSB  #listSlice
    | exp=expression DOT prop=identifier        #property
    | exp=expression IS NULL                    #isNull
    | NOT exp=expression                        #not
    | literalExpression                         #literal
    | listExpression                            #list
    | mapExpression                             #map
    | parameterExpression                       #parameter
    | functionCall                              #function
    | variable                                  #variableReference
    ;

literalExpression
    : NULL #nullLiteral
    | (TRUE | FALSE) #booleanLiteral
    | decimalInteger #decimalIntegerLiteral
    | hexadecimalInteger #hexadecimalIntegerLiteral
    | octalInteger #octalIntegerLiteral
    | FLOAT #floatLiteral
    | STRING #stringLiteral
    ;

decimalInteger
    : MINUS? DECIMAL_INTEGER
    ;

positiveDecimalInteger
    : DECIMAL_INTEGER
    ;

hexadecimalInteger
    : MINUS? HEXADECIMAL_INTEGER
    ;

octalInteger
    : MINUS? OCTAL_INTEGER
    ;

listExpression
    : LSB (expression (',' expression)*)? RSB
    ;

mapExpression
    : LCB (mapExpressionPart (',' mapExpressionPart)*)? RCB
    ;

mapExpressionPart
    : identifier ':' expression
    ;

parameterExpression
    : '$' identifier
    ;

functionCall
    : name=qualifiedIdentifier LP distinct='DISTINCT'? args=expressionSequence? RP
    | name=qualifiedIdentifier LP '*' RP
    ;

qualifiedIdentifier
    : identifier (DOT identifier)*
    ;

expressionSequence
    : expression (',' expression)*
    ;

variable
    : identifier
    ;

labelExpression
    : identifier
    | legacyLabelConjuctionExpression
    | legacyLabelDisjuctionExpression
    | labelExpression '&' labelExpression
    | labelExpression '|' labelExpression
    ;

legacyLabelConjuctionExpression
    : identifier
    | identifier ':' legacyLabelConjuctionExpression
    ;

legacyLabelDisjuctionExpression
    : identifier
    | identifier '|:' legacyLabelDisjuctionExpression
    ;
