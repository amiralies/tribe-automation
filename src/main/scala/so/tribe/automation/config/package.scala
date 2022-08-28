package so.tribe.automation

package object config {
  case class AppConfig(
      http: HttpConfig,
      tribe: TribeConfig,
      mongodb: MongoDBConfig
  )
  case class HttpConfig(host: String, port: Int)
  case class TribeConfig(clientSecret: String)
  case class MongoDBConfig(uri: String, dbName: String)
}
