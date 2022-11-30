/**
 * The Cypher expression sub-language
 */
grammar Expression;
import Base;

expression
    : lhs=expression '+' rhs=expression #addition
    | variable                          #variableReference
    ;

variable
    : identifier
    ;