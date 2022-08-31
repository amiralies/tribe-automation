package so.tribe.automation.automation

import domain._
import domain.EventDesc.{EvPostCreated, EvSpaceCreated}
import domain.Action._
import so.tribe.automation.Utils
import so.tribe.automation.automation.domain.Condition._

private[automation] object AutomationActionInterpreter {
  def makeEnv(eventDesc: EventDesc): Env = {
    eventDesc match {
      case EvPostCreated(title, content) =>
        Trigger.TrPostCreated.fieldNames.collect {
          case k @ "title"   => k -> title
          case k @ "content" => k -> content
        }.toMap
      case EvSpaceCreated(spaceName) =>
        Trigger.TrSpaceCreated.fieldNames.collect { case k @ "spaceName" =>
          k -> spaceName
        }.toMap
    }
  }

  def evalCondition(condition: Condition, env: Env): Boolean =
    condition match {
      case CdEq(fieldName, value)       => env(fieldName) == value
      case CdContains(fieldName, value) => env(fieldName).contains(value)
      case CdAnd(left, right) =>
        evalCondition(left, env) && evalCondition(right, env)
      case CdOr(left, right) =>
        evalCondition(left, env) || evalCondition(right, env)
    }

  def interpret(action: Action, env: Env): Option[Effect] = {
    action match {
      case AcHttpPostRequest(url, jsonBody) =>
        Some(
          Effect.EffHttpPostRequest(
            supplyVariables(url, env),
            supplyVariables(jsonBody, env)
          )
        )

      case AcSendNotifToAll(message) =>
        Some(Effect.EffSendNotifToAll(supplyVariables(message, env)))

      case AcIf(condition, thenBranch, elseBranch) =>
        if (evalCondition(condition, env))
          interpret(thenBranch, env)
        else elseBranch.flatMap(interpret(_, env))
    }
  }

  private def supplyVariables(str: String, env: Env): String =
    env.toList.foldLeft(str)((acc, fieldPair) => {
      val (fieldName, fieldValue) = fieldPair
      val pat = Utils.mustachePatternForField(fieldName)
      pat.replaceAllIn(acc, fieldValue)
    })
}
