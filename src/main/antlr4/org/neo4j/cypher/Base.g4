/**
 * Base rules
 */
grammar Base;

identifier
    : IDENTIFIER
    ;

TRUE options { caseInsensitive=true; } : 'TRUE';
FALSE options { caseInsensitive=true; }: 'FALSE';
NULL options { caseInsensitive=true; }: 'NULL';
IS options { caseInsensitive=true; }: 'IS';
IN options { caseInsensitive=true; }: 'IN';
AND options { caseInsensitive=true; }: 'AND';
OR options { caseInsensitive=true; }: 'OR';
XOR options { caseInsensitive=true; }: 'XOR';
NOT options { caseInsensitive=true; }: 'NOT';
LP: '(';
RP: ')';
LSB: '[';
RSB: ']';
LCB: '{';
RCB: '}';
DOT: '.';
DOTDOT: '..';
PLUS: '+';
MINUS: '-';
SLASH: '/';
STAR: '*';
EQ: '=';
NEQ:'<>';

WS
    : [ \t\r\n]+ -> skip
    ;

IDENTIFIER
    : [a-zA-Z]+[a-zA-Z0-9_]*
    ;

fragment DIGIT
    : [0-9]
    ;

fragment HEX_DIGIT
    : [0-9a-fA-F]
    ;

fragment OCT_DIGIT
    : [0-7]
    ;

DECIMAL_INTEGER
    : DIGIT+
    ;

HEXADECIMAL_INTEGER
    : '0x'HEX_DIGIT+
    ;

OCTAL_INTEGER
    : '0o'OCT_DIGIT+
    ;

EXPONENT
    : ('e'|'E')MINUS?DIGIT+
    ;

FLOAT
    : MINUS?DIGIT*'.'DIGIT+EXPONENT?
    | MINUS?DIGIT+EXPONENT
    ;

fragment QUOTE_SINGLE
    : '\''
    ;

fragment QUOTE_DOUBLE
    : '"'
    ;

fragment STRING_CONTENT
    : .*?
    ;

STRING
    : QUOTE_SINGLE STRING_CONTENT QUOTE_SINGLE
    | QUOTE_DOUBLE STRING_CONTENT QUOTE_DOUBLE
    ;