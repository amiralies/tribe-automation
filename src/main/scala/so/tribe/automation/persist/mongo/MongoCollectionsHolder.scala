package so.tribe.automation.persist.mongo

import zio._
import so.tribe.automation.config.ConfigService

trait MongoCollectionsHolder {
  val collections: MongoCollections
}

case class MongoCollectionsHolderImpl(collections: MongoCollections)
    extends MongoCollectionsHolder

object MongoCollectionsHolderImpl {
  val layer = ZLayer {
    for {
      configService <- ZIO.service[ConfigService]
      collections <- MongoConnectionGenerator.getCollections(
        configService.access.mongodb.uri,
        configService.access.mongodb.dbName
      )
    } yield MongoCollectionsHolderImpl(collections)
  }
}
