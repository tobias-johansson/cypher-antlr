package org.neo4j.cypher;

import org.antlr.v4.runtime.ParserRuleContext;
import org.neo4j.cypher.internal.util.InputPosition;

import java.util.List;
import java.util.function.Function;

public class VisitorHelpers {

    public static final InputPosition NoPos = InputPosition.NONE();

    public static InputPosition pos(ParserRuleContext ctx) {
        return NoPos;
    }

    public static <T, R> List<R> visitList(List<T> selection, Function<T, R> visitor) {
        return selection.stream().map(visitor).toList();
    }
}
