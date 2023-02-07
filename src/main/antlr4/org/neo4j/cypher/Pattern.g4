/**
 * The Cypher pattern sub-language
 */
grammar Pattern;
import Expression;

graphPattern
    : graphPatternPart (',' graphPatternPart)*
    ;

graphPatternPart
    : (var=variable '=' )? pathPattern
    ;

pathPattern
    : nodePattern (relPattern pathPattern)*
    ;

nodePattern
    : LP nodePatternBody RP
    ;

relPattern
    : '<'? MINUS (LSB relPatternBody RSB)? MINUS '>'?
    ;

nodePatternBody
    : var=variable? (':' lab=labelExpression)? (map=mapExpression|parameterExpression)?
    ;

relPatternBody
    : var=variable? (':' lab=labelExpression)? length=relPatternVarlength? (map=mapExpression|parameterExpression)?
    ;

relPatternVarlength
    : '*' positiveDecimalInteger? '..' positiveDecimalInteger?
    | '*' positiveDecimalInteger?
    ;