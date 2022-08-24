package so.tribe.automation

import zio._
import zio.Console._

object MyApp extends ZIOAppDefault {

  override def run = logic

  val logic = printLine("Hello world")
}
