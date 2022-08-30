package so.tribe.automation.slate
import io.circe._
import io.circe.syntax._

case class Block(
    name: String,
    children: List[Block],
    props: Map[String, String]
)

case class Slate(rootBlock: String, blocks: List[SlateBlock])

case class SlateBlock(
    id: String,
    name: String,
    children: Option[String],
    props: Option[String]
)

object Block {
  def fromXml(node: xml.Node): Block = {
    val name = node.label
    val children = node.child.toList.map(fromXml)
    val props = node.attributes.asAttrMap

    Block(name, children, props)
  }

  def toSlate(block: Block): Slate = {
    val root = assignId(block)
    val allBlocks = flattenBlocks(root).map { b =>
      SlateBlock(
        id = b.id,
        name = b.name,
        children = serilizeChildren(b),
        props = serilizeProps(b.name == "RawText", b.props)
      )
    }

    Slate(root.id, allBlocks)
  }

  private def assignId(b: Block): BlockWithId = {
    val id = genID
    BlockWithId(
      id = id,
      name = b.name,
      children = b.children.map(assignId),
      props = b.props
    )
  }

  // defintly not the most efficent code
  private def flattenBlocks(b: BlockWithId): List[BlockWithId] = {
    b :: b.children.flatMap(flattenBlocks)
  }

  private def serilizeChildren(blockWithId: BlockWithId): Option[String] =
    if (blockWithId.children.isEmpty) {
      None
    } else {
      Some(blockWithId.children.map(_.id).asJson.noSpaces)
    }

  private def serilizeProps(
      isRawText: Boolean,
      props: Map[String, String]
  ): Option[String] =
    if (props.isEmpty) {
      None
    } else {
      if (!isRawText) {
        val propsCleaned = props
          .mapValues({
            case "false" => false.asJson
            case "true"  => true.asJson
            case v       => v.asJson
          })
          .toMap
        Some(JsonObject.fromMap(propsCleaned).asJson.noSpaces)
      } else {
        props.get("value").map(_.asJson.noSpaces)
      }
    }

  private case class BlockWithId(
      id: String,
      name: String,
      children: List[BlockWithId],
      props: Map[String, String]
  )

  private def genID() = java.util.UUID.randomUUID.toString
}

object Slate {
  import io.circe.generic.semiauto.deriveCodec

  implicit val slateBlockCodec = deriveCodec[SlateBlock]
  implicit val slateCodec = deriveCodec[Slate]

  def toJson(slate: Slate) = slate.asJson
}
