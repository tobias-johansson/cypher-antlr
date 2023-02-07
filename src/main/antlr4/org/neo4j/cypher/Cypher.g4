/**
 * The Neo4j Cypher language
 */
grammar Cypher;
import Expression, Pattern;

query
    : clause+ EOF
    ;

clause
    : 'MATCH'
        pattern=graphPattern
        where=whereSubclause?
        # matchClause
    | 'OPTIONAL' 'MATCH'
        pattern=graphPattern
        where=whereSubclause?
        # optionalMatchClause
    | 'MERGE'
        pattern=graphPattern
        where=whereSubclause?
        # matchClause
    | 'WITH'
        distinct='DISTINCT'?
        items=projections
        where=whereSubclause?
        orderBy=orderBySubclause?
        skip=skipSubclause?
        limit=limitSubclause?
        # withClause
    | 'RETURN'
        distinct='DISTINCT'?
        items=projections
        orderBy=orderBySubclause?
        skip=skipSubclause?
        limit=limitSubclause?
        # returnClause
    | 'UNWIND'
        item=aliasedProjection
        # unwindClause
    | 'CALL'
        name=qualifiedIdentifier
        arguments=callClauseArguments?
        yield=yieldSubclause?
        # callClause
    ;

projections
    : projection (',' projection)*
    ;

projection
    : aliasedProjection
    | unaliasedProjection
    | starProjection
    ;

aliasedProjection
    : expression 'AS' variable
    | expression
    ;

unaliasedProjection
    : expression
    ;

starProjection
    : '*'
    ;

orderBySubclause
    : 'ORDER' 'BY' items=orderByItem+
    ;

orderByItem
    : expression (asc='ASC' | desc='DESC')?
    ;

whereSubclause
    : 'WHERE' expression
    ;

skipSubclause
    : 'SKIP' expression
    ;

limitSubclause
    : 'LIMIT' expression
    ;

callClauseArguments
    : LP args=expressionSequence? RP
    ;

yieldSubclause
    : 'YIELD' names=indentifierSequence
    ;

indentifierSequence
    : identifier (',' identifier)*
    ;