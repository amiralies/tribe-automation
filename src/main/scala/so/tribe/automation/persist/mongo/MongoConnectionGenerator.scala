package so.tribe.automation.persist.mongo

import reactivemongo.api.{DB, AsyncDriver, MongoConnection}
import reactivemongo.api.bson.collection.BSONCollection
import zio._

object MongoConnectionGenerator {
  private def createConnection(uri: String, dbName: String): UIO[DB] =
    ZIO.fromFuture { implicit ec =>
      val driver = AsyncDriver()
      for {
        uri <- MongoConnection.fromString(uri)
        connection <- driver.connect(uri)
        db <- connection.database(dbName)
      } yield db
    }.orDie

  def getCollections(uri: String, dbName: String): UIO[MongoCollections] =
    createConnection(uri, dbName).map { db =>
      new MongoCollections {
        override val automations: BSONCollection =
          db.collection[BSONCollection](MongoCollections.automationsName)

      }
    }

}
