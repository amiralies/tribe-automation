package so.tribe.automation.persist.mongo

import reactivemongo.api.bson.collection.BSONCollection

trait MongoCollections {
  val automations: BSONCollection
}

private[mongo] object MongoCollections {
  val automationsName = "automations"
}
