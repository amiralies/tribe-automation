package so.tribe.automation.automation

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._
import domain._

class AutomationActionInterpreterSpec extends AnyFlatSpec with Matchers {

  it should "makeEnv should create correct env for post created event" in {
    val eventDesc = EventDesc.EvPostCreated("this is title", "this is content")

    AutomationActionInterpreter.makeEnv(eventDesc) shouldBe Map(
      "content" -> "this is content",
      "title" -> "this is title"
    )
  }

  it should "makeEnv should create correct env for space created event" in {
    val eventDesc = EventDesc.EvSpaceCreated("this is space name")

    AutomationActionInterpreter.makeEnv(eventDesc) shouldBe Map(
      "spaceName" -> "this is space name"
    )
  }

  it should "interpret should create proper effects for simple actions" in {

    val effect = AutomationActionInterpreter.interpret(
      Action.AcSendNotifToAll("Hi {{title}}, {{content}}"),
      Map("title" -> "foo", "content" -> "great stuff")
    )

    effect shouldBe Some(Effect.EffSendNotifToAll("Hi foo, great stuff"))
  }

}
