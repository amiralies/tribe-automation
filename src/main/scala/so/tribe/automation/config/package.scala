package so.tribe.automation

package object config {
  case class AppConfig(
      http: HttpConfig,
      tribe: TribeConfig,
  )
  case class HttpConfig(host: String, port: Int)
  case class TribeConfig(clientSecret: String)
}

