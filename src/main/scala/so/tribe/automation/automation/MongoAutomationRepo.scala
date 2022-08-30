package so.tribe.automation.automation

import so.tribe.automation.persist.mongo.MongoCollectionsHolder
import zio._
import so.tribe.automation.persist.mongo.MongoUtils
import reactivemongo.api.bson.BSONDocument

case class MongoAutomationRepo(mongoCollectionsHolder: MongoCollectionsHolder)
    extends AutomationRepo {
  import DomainBsonSupport._
  private val automationCollection =
    mongoCollectionsHolder.collections.automations

  override def insert(automation: domain.Automation): UIO[Unit] =
    ZIO.fromFuture { implicit ec =>
      automationCollection
        .insert(ordered = false)
        .one(automation)
        .map(MongoUtils.validateWriteResult("Insert Automation", automation))
    }.orDie

  override def getAllByNetworkIdAndTrigger(
      networkId: String,
      trigger: domain.Trigger
  ): UIO[List[domain.Automation]] = ZIO.fromFuture { implicit ec =>
    val query =
      BSONDocument("networkId" -> networkId, "trigger" -> trigger.entryName)

    automationCollection.find(query).cursor[domain.Automation]().collect[List]()
  }.orDie

  override def deleteById(id: String): UIO[Option[domain.Automation]] =
    ZIO.fromFuture { implicit ec =>
      val selector = BSONDocument("_id" -> id)

      automationCollection
        .findAndRemove(selector)
        .map(_.result[domain.Automation])
    }.orDie

  override def getByNetworkId(
      networkId: String
  ): UIO[List[domain.Automation]] = ZIO.fromFuture { implicit ec =>
    val query = BSONDocument("networkId" -> networkId)

    automationCollection.find(query).cursor[domain.Automation]().collect[List]()
  }.orDie

}

object MongoAutomationRepo {
  val layer = ZLayer.fromFunction(MongoAutomationRepo(_))
}
