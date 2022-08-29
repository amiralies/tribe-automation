package so.tribe.automation.persist.mongo

import reactivemongo.api.bson.BSONValue
import reactivemongo.api.bson.BSONWriter
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.bson.BSONDocumentHandler
import reactivemongo.api.bson.BSONDocumentHandler
import reactivemongo.api.bson.BSONDocument
import io.circe.Json
import scala.util.Try
import io.circe.bson.{jsonToBson, bsonToJson}
import io.circe.Codec

object MongoUtils {
  def validateWriteResult[T](action: => String, t: => T)(result: WriteResult)(
      implicit writer: BSONWriter[T]
  ): Unit = {
    if (result.writeErrors.isEmpty)
      ()
    else
      throw new Exception(
        action +
          " on " +
          writer.writeOpt(t).map(BSONValue.pretty) +
          " , messages : " + result.writeErrors.map(_.errmsg).mkString("\n")
      )
  }

  def genBsonHandler[T](implicit jsonCodec: Codec[T]) =
    new BSONDocumentHandler[T] {
      import io.circe.syntax._

      override def readDocument(doc: BSONDocument): Try[T] = bsonToJson(
        doc
      ).toTry.flatMap(_.as[T].toTry)

      override def writeTry(t: T): Try[BSONDocument] =
        jsonToBson(t.asJson).toTry.flatMap(_.asTry[BSONDocument])
    }
}
