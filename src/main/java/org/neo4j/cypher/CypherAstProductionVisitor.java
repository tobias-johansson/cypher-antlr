package org.neo4j.cypher;

import org.neo4j.cypher.internal.ast.Clause;
import org.neo4j.cypher.internal.ast.Query;
import org.neo4j.cypher.internal.ast.Return;
import org.neo4j.cypher.internal.ast.ReturnItem;
import org.neo4j.cypher.internal.ast.ReturnItems;
import org.neo4j.cypher.internal.ast.factory.neo4j.Neo4jASTFactory;
import org.neo4j.cypher.internal.expressions.Expression;
import org.neo4j.cypher.internal.expressions.PatternAtom;
import org.neo4j.cypher.internal.expressions.PatternPart;
import org.neo4j.cypher.internal.expressions.Variable;
import org.neo4j.cypher.internal.util.ASTNode;

import java.util.List;

import static org.neo4j.cypher.VisitorHelpers.pos;
import static org.neo4j.cypher.VisitorHelpers.visitList;

class CypherAstProductionVisitor extends CypherBaseVisitor<ASTNode> {

    final Neo4jASTFactory factory;

    public CypherAstProductionVisitor(String query) {
        factory = new Neo4jASTFactory(query);
    }

    @Override
    public Query visitQuery(CypherParser.QueryContext ctx) {
        var clauses = ctx.clause().stream().map(this::visitClause).toList();
        return factory.newSingleQuery(clauses);
    }

    public Clause visitClause(CypherParser.ClauseContext ctx) {
        return switch (ctx) {
            case CypherParser.MatchClauseContext c -> visitMatchClause(c);
            case CypherParser.WithClauseContext c -> visitWithClause(c);
            case CypherParser.ReturnClauseContext c -> visitReturnClause(c);
            default -> throw new IllegalStateException("Unexpected value: " + ctx);
        };
    }

    @Override
    public Clause visitMatchClause(CypherParser.MatchClauseContext ctx) {
        var patterns = visitList(ctx.matchPattern.pathPattern(), this::visitPathPattern);
        return factory.matchClause(pos(ctx), false, patterns, pos(ctx.graphPattern()), List.of(), null);
    }

    @Override
    public Clause visitWithClause(CypherParser.WithClauseContext ctx) {
        var ret = factory.newReturnClause(pos(ctx), false, visitProjections(ctx.items), List.of(), null, null, null, null, null);
        return factory.withClause(pos(ctx), ret, null);
    }

    @Override
    public Return visitReturnClause(CypherParser.ReturnClauseContext ctx) {
        return factory.newReturnClause(pos(ctx), false, visitProjections(ctx.items), List.of(), null, null, null, null, null);
    }

    @Override
    public ReturnItems visitProjections(CypherParser.ProjectionsContext ctx) {
        return factory.newReturnItems(pos(ctx), false, visitList(ctx.projection(), this::visitProjection));
    }

    @Override
    public ReturnItem visitProjection(CypherParser.ProjectionContext ctx) {
        return factory.newReturnItem(pos(ctx), visitExpression(ctx.expression()), visitVariable(ctx.variable()));
    }

    @Override
    public PatternPart visitPathPattern(CypherParser.PathPatternContext ctx) {
        var atoms = List.of(visitNodePattern(ctx.nodePattern()));
        return factory.everyPathPattern(atoms);
    }

    @Override
    public PatternAtom visitNodePattern(CypherParser.NodePatternContext ctx) {
        return factory.nodePattern(pos(ctx), visitVariable(ctx.variable()), null, null, null);
    }

    public Expression visitExpression(CypherParser.ExpressionContext ctx) {
        return switch (ctx) {
            case CypherParser.VariableReferenceContext c -> visitVariable(c.variable());
            case CypherParser.AdditionContext c -> visitAddition(c);
            default -> throw new IllegalStateException("Unexpected value: " + ctx);
        };
    }

    @Override
    public Variable visitVariable(CypherParser.VariableContext ctx) {
        var name = ctx.identifier().getText();
        return factory.newVariable(pos(ctx), name);
    }

    @Override
    public Expression visitAddition(CypherParser.AdditionContext ctx) {
        return factory.plus(pos(ctx), visitExpression(ctx.lhs), visitExpression(ctx.rhs));
    }
}
