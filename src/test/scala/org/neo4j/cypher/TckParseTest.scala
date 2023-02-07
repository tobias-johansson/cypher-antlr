package org.neo4j.cypher

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.function.Executable
import org.opencypher.tools.tck.api.CypherTCK
import org.opencypher.tools.tck.api.ExecQuery
import org.opencypher.tools.tck.api.Execute
import org.opencypher.tools.tck.api.ExpectError
import org.opencypher.tools.tck.api.ExpectResult
import org.opencypher.tools.tck.api.Scenario
import org.opencypher.tools.tck.api.Step
import org.opencypher.tools.tck.constants.TCKErrorTypes.SYNTAX_ERROR

import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.Failure
import scala.util.Success
import scala.util.Try

class TckParseTest {

  val categoryFilter: Seq[String] = List()
  //  e.g. List("clauses", "match")

  val excluded: Set[String] = Set()
  // e.g. Set( """clauses/match :: "Match7 - Optional match" :: [25] "Optionally matching self-loops without matches"""" )

  def runScenario(scenario: Scenario): Unit = {

    println(scenario)

    var lastResult: Try[CypherAntlrParser.Result] = Failure(new Exception("no query has been parsed"))

    scenario
      .steps
      .map(parseAction)
      .foreach {

        case Parse(text) =>
          lastResult = CypherAntlrParser.parse(text)

        case ExpectParseFailure =>
          lastResult match {
            case Failure(_) => ()
            case Success(result) =>
              println(result)
              throw new Exception("expected parse failure")
          }

        case ExpectParseSuccess =>
          lastResult match {
            case Success(result) =>
              println(result)
            case Failure(exception) =>
              throw exception
          }

        case NoAction =>
          ()

      }
  }

  sealed trait ParseAction
  case class Parse(text: String) extends ParseAction
  case object ExpectParseFailure extends ParseAction
  case object ExpectParseSuccess extends ParseAction
  case object NoAction extends ParseAction

  private def parseAction(step: Step) = step match {
    case exe: Execute if exe.qt == ExecQuery =>
      Parse(exe.query)
    case _: ExpectResult =>
      ExpectParseSuccess
    case err: ExpectError =>
      isParseError(err) match {
        case true  => ExpectParseFailure
        case false => ExpectParseSuccess
      }
    case _ =>
      NoAction
  }

  private val parseErrorDetails = Set("UnexpectedSyntax", "InvalidRelationshipPattern")

  private def isParseError(err: ExpectError) =
    err.errorType == SYNTAX_ERROR && parseErrorDetails.contains(err.detail)

  @TestFactory
  def runTests(): java.util.Collection[DynamicTest] = {

    val tests = for {
      scenario <- CypherTCK.allTckScenarios
      name = scenario.toString().strip()
      if !excluded.contains(name)
      if scenario.categories.startsWith(categoryFilter)
    } yield name -> DynamicTest.dynamicTest(name, () => runScenario(scenario))

    tests
      .sortBy { case (name, _) => name }
      .map { case (_, test) => test }
      .asJava
  }

}
