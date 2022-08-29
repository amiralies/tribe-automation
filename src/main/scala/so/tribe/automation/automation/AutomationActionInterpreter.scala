package so.tribe.automation.automation

import domain._
import domain.EventDesc.{EvPostCreated, EvSpaceCreated}
import so.tribe.automation.Utils

private[automation] object AutomationActionInterpreter {
  def makeEnv(eventDesc: EventDesc): Env = {
    eventDesc match {
      case EvPostCreated(title, content) =>
        Map("title" -> title, "content" -> content)
      case EvSpaceCreated(spaceName) => Map("spaceName" -> spaceName)
    }
  }

  def interpret(action: Action, env: Env): Effect = {
    action match {
      case Action.AcHttpPostRequest(url, jsonBody) =>
        Effect.EffHttpPostRequest(
          supplyVariables(url, env),
          supplyVariables(jsonBody, env)
        )

      case Action.AcSendNotifToAll(message) =>
        Effect.EffSendNotifToAll(supplyVariables(message, env))
    }
  }

  private def supplyVariables(str: String, env: Env): String =
    env.toList.foldLeft(str)((acc, fieldPair) => {
      val (fieldName, fieldValue) = fieldPair
      val pat = Utils.mustachePatternForField(fieldName)
      pat.replaceAllIn(acc, fieldValue)
    })
}
