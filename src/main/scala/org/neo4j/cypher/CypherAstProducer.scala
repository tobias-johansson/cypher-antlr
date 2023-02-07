package org.neo4j.cypher

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.neo4j.cypher.CypherParser.CallClauseContext
import org.neo4j.cypher.CypherParser.ClauseContext
import org.neo4j.cypher.CypherParser.GraphPatternContext
import org.neo4j.cypher.CypherParser.ListContext
import org.neo4j.cypher.CypherParser.MatchClauseContext
import org.neo4j.cypher.CypherParser.OptionalMatchClauseContext
import org.neo4j.cypher.CypherParser.ProjectionsContext
import org.neo4j.cypher.CypherParser.QueryContext
import org.neo4j.cypher.CypherParser.ReturnClauseContext
import org.neo4j.cypher.CypherParser.UnwindClauseContext
import org.neo4j.cypher.CypherParser.WithClauseContext
import org.neo4j.cypher.internal.ast.AscSortItem
import org.neo4j.cypher.internal.ast.Clause
import org.neo4j.cypher.internal.ast.DescSortItem
import org.neo4j.cypher.internal.ast.Limit
import org.neo4j.cypher.internal.ast.Match
import org.neo4j.cypher.internal.ast.OrderBy
import org.neo4j.cypher.internal.ast.ProcedureResult
import org.neo4j.cypher.internal.ast.ProcedureResultItem
import org.neo4j.cypher.internal.ast.Query
import org.neo4j.cypher.internal.ast.Return
import org.neo4j.cypher.internal.ast.ReturnItems
import org.neo4j.cypher.internal.ast.SingleQuery
import org.neo4j.cypher.internal.ast.Skip
import org.neo4j.cypher.internal.ast.SortItem
import org.neo4j.cypher.internal.ast.UnresolvedCall
import org.neo4j.cypher.internal.ast.Unwind
import org.neo4j.cypher.internal.ast.Where
import org.neo4j.cypher.internal.ast.With
import org.neo4j.cypher.internal.expressions.Add
import org.neo4j.cypher.internal.expressions.And
import org.neo4j.cypher.internal.expressions.ContainerIndex
import org.neo4j.cypher.internal.expressions.DecimalDoubleLiteral
import org.neo4j.cypher.internal.expressions.Divide
import org.neo4j.cypher.internal.expressions.Equals
import org.neo4j.cypher.internal.expressions.Expression
import org.neo4j.cypher.internal.expressions.False
import org.neo4j.cypher.internal.expressions.FunctionInvocation
import org.neo4j.cypher.internal.expressions.FunctionName
import org.neo4j.cypher.internal.expressions.In
import org.neo4j.cypher.internal.expressions.IsNull
import org.neo4j.cypher.internal.expressions.ListLiteral
import org.neo4j.cypher.internal.expressions.ListSlice
import org.neo4j.cypher.internal.expressions.MapExpression
import org.neo4j.cypher.internal.expressions.Multiply
import org.neo4j.cypher.internal.expressions.Namespace
import org.neo4j.cypher.internal.expressions.Not
import org.neo4j.cypher.internal.expressions.NotEquals
import org.neo4j.cypher.internal.expressions.Null
import org.neo4j.cypher.internal.expressions.Or
import org.neo4j.cypher.internal.expressions.Parameter
import org.neo4j.cypher.internal.expressions.Pattern
import org.neo4j.cypher.internal.expressions.ProcedureName
import org.neo4j.cypher.internal.expressions.Property
import org.neo4j.cypher.internal.expressions.PropertyKeyName
import org.neo4j.cypher.internal.expressions.SignedDecimalIntegerLiteral
import org.neo4j.cypher.internal.expressions.SignedHexIntegerLiteral
import org.neo4j.cypher.internal.expressions.SignedOctalIntegerLiteral
import org.neo4j.cypher.internal.expressions.StringLiteral
import org.neo4j.cypher.internal.expressions.Subtract
import org.neo4j.cypher.internal.expressions.True
import org.neo4j.cypher.internal.expressions.Variable
import org.neo4j.cypher.internal.util.InputPosition
import org.neo4j.cypher.internal.util.symbols.CTAny

import scala.jdk.CollectionConverters.ListHasAsScala

trait Utils {

  def pos(c: ParserRuleContext): InputPosition =
    pos(c.getStart)

  def pos(token: Token): InputPosition =
    InputPosition(token.getStartIndex, token.getLine, token.getCharPositionInLine)

  def opt[T](in: T): Option[T] =
    Option(in)

  def seq[T](cs: java.util.List[T]): Seq[T] =
    cs.asScala.toSeq

  def unknown(c: ParserRuleContext): Nothing = {
    val cls = c.getClass.getSimpleName
    val text = c.getText
    throw new IllegalArgumentException(s"Unknown parse tree element: $cls($text)")
  }

  type ->[A, B] = (A, B)
}

object CypherAstProducer extends Utils {

  def query(c: QueryContext): Query =
    Query(part = singleQuery(c))(pos(c))

  private def singleQuery(c: QueryContext): SingleQuery =
    SingleQuery(clauses = seq(c.clause()).map(clause))(pos(c))

  private def returnItems(c: ProjectionsContext): ReturnItems =
    ReturnItems(includeExisting = false, items = Seq.empty)(pos(c))

  private def clause(c: ClauseContext): Clause = c match {
    case c: MatchClauseContext =>
      Match(
        optional = false,
        pattern = pattern(c.graphPattern()),
        hints = Seq.empty,
        where = opt(c.whereSubclause()).map(where)
      )(pos(c))

    case c: OptionalMatchClauseContext =>
      Match(
        optional = true,
        pattern = pattern(c.graphPattern()),
        hints = Seq.empty,
        where = opt(c.whereSubclause()).map(where)
      )(pos(c))

    case c: ReturnClauseContext =>
      Return(
        distinct = false,
        returnItems = returnItems(c.projections()),
        orderBy = None,
        skip = None,
        limit = None
      )(pos(c))

    case c: UnwindClauseContext =>
      val projection = c.aliasedProjection()
      Unwind(
        expression = expr(projection.expression()),
        variable = variable(projection.variable())
      )(pos(c))

    case c: WithClauseContext =>
      With(
        distinct = c.distinct != null,
        returnItems = returnItems(c.projections()),
        orderBy = opt(c.orderBy).map(orderBy),
        skip = opt(c.skip).map(skip),
        limit = opt(c.limit).map(limit),
        where = opt(c.where).map(where)
      )(pos(c))

    case c: CallClauseContext =>
      val nameParts = seq(c.name.identifier()).map(ident)
      UnresolvedCall(
        procedureNamespace = Namespace(nameParts.init.toList)(pos(c)),
        procedureName = ProcedureName(nameParts.last)(pos(c)),
        declaredArguments = opt(c.arguments).map(as => seq(as.args.expression()).map(expr)),
        declaredResult = opt(c.`yield`).map(procResult),
        yieldAll = false
      )(pos(c))

    case x => unknown(x)
  }

  private def procResult(c: CypherParser.YieldSubclauseContext): ProcedureResult =
    ProcedureResult(items = seq(c.names.identifier()).map(procResultItem).toIndexedSeq, where = None)(pos(c))

  private def procResultItem(c: CypherParser.IdentifierContext) =
    ProcedureResultItem(variable = variable(c))(pos(c))

  private def orderBy(c: CypherParser.OrderBySubclauseContext): OrderBy =
    OrderBy(seq(c.orderByItem()).map(sortItem))(pos(c))

  private def sortItem(c: CypherParser.OrderByItemContext): SortItem = c.desc match {
    case null => AscSortItem(expr(c.expression()))(pos(c))
    case _    => DescSortItem(expr(c.expression()))(pos(c))
  }

  private def skip(c: CypherParser.SkipSubclauseContext): Skip =
    Skip(expr(c.expression()))(pos(c))

  private def limit(c: CypherParser.LimitSubclauseContext): Limit =
    Limit(expr(c.expression()))(pos(c))

  private def expr(c: CypherParser.ExpressionContext): Expression = c match {
    case c: CypherParser.VariableReferenceContext => Variable(c.getText)(pos(c))
    case c: CypherParser.ParameterContext      => Parameter(ident(c.parameterExpression().identifier()), CTAny)(pos(c))
    case c: CypherParser.EqualityContext       => Equals(expr(c.lhs), expr(c.rhs))(pos(c))
    case c: CypherParser.NonEqualityContext    => NotEquals(expr(c.lhs), expr(c.rhs))(pos(c))
    case c: CypherParser.NotContext            => Not(expr(c.exp))(pos(c))
    case c: CypherParser.OrContext             => Or(expr(c.lhs), expr(c.rhs))(pos(c))
    case c: CypherParser.AndContext            => And(expr(c.lhs), expr(c.rhs))(pos(c))
    case c: CypherParser.AdditionContext       => Add(expr(c.lhs), expr(c.rhs))(pos(c))
    case c: CypherParser.SubtractionContext    => Subtract(expr(c.lhs), expr(c.rhs))(pos(c))
    case c: CypherParser.DivisionContext       => Divide(expr(c.lhs), expr(c.rhs))(pos(c))
    case c: CypherParser.MultiplicationContext => Multiply(expr(c.lhs), expr(c.rhs))(pos(c))
    case c: CypherParser.InContext             => In(expr(c.lhs), expr(c.rhs))(pos(c))
    case c: CypherParser.ListIndexContext      => ContainerIndex(expr(c.list), expr(c.index))(pos(c))
    case c: CypherParser.ListSliceContext      => ListSlice(expr(c.list), exprOpt(c.start), exprOpt(c.end))(pos(c))
    case c: CypherParser.IsNullContext         => IsNull(expr(c.exp))(pos(c))
    case c: CypherParser.PropertyContext       => Property(expr(c.exp), propKey(c.prop))(pos(c))
    case c: CypherParser.ListContext           => list(c)
    case c: CypherParser.MapContext            => map(c)
    case c: CypherParser.FunctionContext       => function(c)
    case c: CypherParser.LiteralContext        => literal(c.literalExpression())

    case x => unknown(x)
  }

  private def list(c: ListContext) =
    ListLiteral(exprSeq(c.listExpression().expression()))(pos(c))

  private def map(c: CypherParser.MapContext) =
    MapExpression(seq(c.mapExpression().mapExpressionPart()).map(mapItem))(pos(c))

  private def function(c: CypherParser.FunctionContext): FunctionInvocation = {
    val nameParts = seq(c.functionCall().name.identifier()).map(ident)
    FunctionInvocation(
      namespace = Namespace(nameParts.init.toList)(pos(c)),
      functionName = FunctionName(nameParts.last)(pos(c)),
      distinct = false,
      args = exprSeq(c.functionCall().args.expression()).toIndexedSeq
    )(pos(c))
  }

  private def mapItem(c: CypherParser.MapExpressionPartContext): PropertyKeyName -> Expression =
    propKey(c.identifier()) -> expr(c.expression())

  private def propKey(c: CypherParser.IdentifierContext): PropertyKeyName =
    PropertyKeyName(ident(c))(pos(c))

  private def literal(c: CypherParser.LiteralExpressionContext): Expression = c match {
    case c: CypherParser.NullLiteralContext               => Null()(pos(c))
    case c: CypherParser.OctalIntegerLiteralContext       => SignedOctalIntegerLiteral(c.getText)(pos(c))
    case c: CypherParser.StringLiteralContext             => StringLiteral(c.getText)(pos(c)) // FIXME includes quotes
    case c: CypherParser.HexadecimalIntegerLiteralContext => SignedHexIntegerLiteral(c.getText)(pos(c))
    case c: CypherParser.FloatLiteralContext              => DecimalDoubleLiteral(c.getText)(pos(c))
    case c: CypherParser.BooleanLiteralContext            => if (c.TRUE() != null) True()(pos(c)) else False()(pos(c))
    case c: CypherParser.DecimalIntegerLiteralContext     => SignedDecimalIntegerLiteral(c.getText)(pos(c))
  }

  private def ident(c: CypherParser.IdentifierContext): String =
    c.IDENTIFIER().getText

  private def exprOpt(c: CypherParser.ExpressionContext): Option[Expression] =
    opt(c).map(expr)

  private def exprSeq(cs: java.util.List[CypherParser.ExpressionContext]): Seq[Expression] =
    seq(cs).map(expr)

  private def variable(c: CypherParser.VariableContext): Variable =
    Variable(c.getText)(pos(c))

  private def variable(c: CypherParser.IdentifierContext): Variable =
    Variable(ident(c))(pos(c))

  private def pattern(c: GraphPatternContext): Pattern =
    Pattern(Seq.empty)(pos(c))

  private def where(c: CypherParser.WhereSubclauseContext): Where =
    Where(expr(c.expression()))(pos(c))

}
