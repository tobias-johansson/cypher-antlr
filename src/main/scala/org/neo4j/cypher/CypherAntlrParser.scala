package org.neo4j.cypher

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.neo4j.cypher.internal.ast.Query

import scala.util.Try

object CypherAntlrParser {

  private def makeParser(string: String): CypherParser = {
    val input = CharStreams.fromString(string)
    val lexer = new CypherLexer(input)
    lexer.addErrorListener(ThrowingErrorListener(string))
    val tokens = new CommonTokenStream(lexer)
    val parser = new CypherParser(tokens)
    parser.addErrorListener(ThrowingErrorListener(string))
    parser
  }

  def parse(text: String): Try[Result] = Try {
    val parser = makeParser(text)
    val tree = parser.query
    val treeString = tree.toStringTree(parser)
    val ast = CypherAstProducer.query(tree)
    Result(text, treeString, ast)
  }

  case class Result(
    text: String,
    treeString: String,
    ast: Query
  ) {

    override def toString: String = {
      val input = text.linesIterator.map("  " + _).mkString("\n")
      s"""query:
         |$input
         |treeString: $treeString
         |ast: $ast
         |""".stripMargin
    }
  }

  case class ThrowingErrorListener(string: String) extends BaseErrorListener {

    override def syntaxError(
      recognizer: Recognizer[_, _],
      offendingSymbol: Any,
      line: Int,
      column: Int,
      msg: String,
      e: RecognitionException
    ): Unit = {
      val lines = string.linesIterator.toList
      val (before, after) = lines.splitAt(line)
      val indent = " " * column
      val pointer = s"$indent^- $msg ($line:$column)"
      val visualLines = before ++ Seq(pointer) ++ after
      val visual = visualLines.mkString("\n")
      val message =
        s"""Parse failed:
           |
           |$visual""".stripMargin

      throw new ParseCancellationException(message)
    }
  }

}
