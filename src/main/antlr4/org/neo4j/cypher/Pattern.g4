/**
 * The Cypher pattern sub-language
 */
grammar Pattern;
import Expression;

graphPattern
    : pathPattern (',' pathPattern)*
    ;

pathPattern
    : nodePattern
    ;

nodePattern
    : '(' variable ')'
    ;