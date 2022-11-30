package org.neo4j.cypher;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.cypher.internal.ast.Clause;
import org.neo4j.cypher.internal.ast.Query;
import org.neo4j.cypher.internal.ast.factory.neo4j.Neo4jASTFactory;
import org.neo4j.cypher.internal.expressions.Expression;
import org.neo4j.cypher.internal.expressions.PatternPart;
import org.neo4j.cypher.internal.expressions.Variable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.cypher.VisitorHelpers.NoPos;

public class CypherParserTest {

    @ParameterizedTest
    @MethodSource("queries")
    void foo(String query, Query expected) {
        CypherParser parser = parser(query);

        ParseTree tree = parser.query();

        var treeString = tree.toStringTree(parser);
        var ast = tree.accept(new CypherAstProductionVisitor(query));

        System.out.println(query);
        System.out.println(treeString);
        System.out.println(ast);

        assertThat(ast).isEqualTo(expected);
    }

    static Neo4jASTFactory factory = new Neo4jASTFactory("");

    static List<Arguments> queries() {
        return List.of(
                Arguments.of(
                        "MATCH (x), (y) MATCH (z)",
                        query(
                                match(nodePattern("x"), nodePattern("y")),
                                match(nodePattern("z"))
                        )),
                Arguments.of(
                        "MATCH (x), (y) WITH x AS r",
                        query(
                                match(nodePattern("x"), nodePattern("y")),
                                with(Pair.of(variable("x"), "r"))
                        )),
                Arguments.of(
                        "WITH x + y AS r",
                        query(
                                with(Pair.of(add(variable("x"), variable("y")), "r"))
                        )),
                Arguments.of(
                        "WITH x + y AS r RETURN foo AS f, b + c + d AS x",
                        query(
                                with(Pair.of(add(variable("x"), variable("y")), "r")),
                                ret(
                                        Pair.of(variable("foo"), "f"),
                                        Pair.of(add(add(variable("b"), variable("c")), variable("d")), "x")
                                )
                        ))
        );
    }

    static Query query(Clause... clauses) {
        return factory.newSingleQuery(List.of(clauses));
    }

    static Clause match(PatternPart... patterns) {
        return factory.matchClause(NoPos, false, List.of(patterns), NoPos, List.of(), null);
    }

    @SafeVarargs
    static Clause with(Pair<Expression, String>... items) {
        var returnItems = Stream.of(items).map(e -> factory.newReturnItem(NoPos, e.getLeft(), variable(e.getRight()))).toList();
        var ret = factory.newReturnClause(NoPos, false, factory.newReturnItems(NoPos, false, returnItems), List.of(), null, null, null, null, null);
        return factory.withClause(NoPos, ret, null);
    }

    @SafeVarargs
    static Clause ret(Pair<Expression, String>... items) {
        var returnItems = Stream.of(items).map(e -> factory.newReturnItem(NoPos, e.getLeft(), variable(e.getRight()))).toList();
        return factory.newReturnClause(NoPos, false, factory.newReturnItems(NoPos, false, returnItems), List.of(), null, null, null, null, null);
    }

    static PatternPart nodePattern(String variable) {
        return factory.everyPathPattern(List.of(factory.nodePattern(NoPos, variable(variable), null, null, null)));
    }

    static Variable variable(String name) {
        return factory.newVariable(NoPos, name);
    }

    static Expression add(Expression lhs, Expression rhs) {
        return factory.plus(NoPos, lhs, rhs);
    }


    private static CypherParser parser(String string) {
        CharStream input = CharStreams.fromString(string);
        CypherLexer lexer = new CypherLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new CypherParser(tokens);
    }
}
