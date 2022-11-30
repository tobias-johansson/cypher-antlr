/**
 * Base rules
 */
grammar Base;

identifier
    : IDENTIFIER
    ;

WS
    : [ \t\r\n]+ -> skip
    ;

IDENTIFIER
    : [a-z]+
    ;