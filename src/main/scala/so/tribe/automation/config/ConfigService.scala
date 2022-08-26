package so.tribe.automation.config

import zio.config.typesafe._
import zio.{ZLayer}
import zio.config._
import zio.config.magnolia._

trait ConfigService {
  val access: AppConfig
}

case class ConfigServiceImpl(access: AppConfig) extends ConfigService

object ConfigServiceImpl {
  private val derivedDescriptor = descriptor[AppConfig]

  val layer: ZLayer[Any, ReadError[String], ConfigService] =
    TypesafeConfig
      .fromResourcePath(derivedDescriptor)
      .project(ConfigServiceImpl(_))
}
