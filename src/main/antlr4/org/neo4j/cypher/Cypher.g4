/**
 * The Neo4j Cypher language
 */
grammar Cypher;
import Expression, Pattern;

query
    : clause+ EOF
    ;

clause
    : 'MATCH' matchPattern=graphPattern # matchClause
    | 'WITH' items=projections          # withClause
    | 'RETURN' items=projections        # returnClause
    ;

projections
    : projection (',' projection)*
    ;

projection
    : expression 'AS' variable
    ;
