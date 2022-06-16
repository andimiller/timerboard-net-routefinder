package net.timerboard.routefinder

import cats.implicits.catsSyntaxOptionId
import io.circe.{Codec => CCodec}
import sttp.tapir._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.json.circe.jsonBody

case class System(name: String, id: Long)
object System                    {
  implicit val codec: CCodec[System] =
    io.circe.generic.semiauto.deriveCodec[System]
  implicit val schema                = Schema.derived[System]
}
case class SystemId(value: Long)
class API(systemIds: List[Long]) {
  implicit val system: Codec[String, SystemId, CodecFormat.TextPlain] =
    Codec.long
      .validate(
        Validator.custom(
          id => ValidationResult.validWhen(systemIds.contains(id)),
          "Id was not a valid Solar System ID".some
        )
      )
      .map(SystemId)(_.value)

  val pathSystem =
    path[SystemId]("system_id").description("The id of a Solar System")

  val listSystems = endpoint
    .name("list_systems")
    .tag("data")
    .description("List all available Solar Systems")
    .in("v1" / "systems")
    .out(
      jsonBody[List[System]].example(List(System("Jita", 30000142L)))
    )

  implicit class ExtraMapSyntax[K, V](m: Map[K, V]) {
    def mapKeys[K2](f: K => K2): Map[K2, V] = m.map { case (k, v) => f(k) -> v }
  }

  val routesResult = jsonBody[Map[String, Int]]
    .description("A Map of System IDs to distance")
    .map(_.mapKeys(_.toLong))(_.mapKeys(_.toString))
    .example(Map(30000142L -> 32))

  val listRoutes   = endpoint
    .name("list_routes")
    .tag("routefinding")
    .description("Find all routes from one origin")
    .in("v1" / "routes" / pathSystem)
    .out(
      routesResult
    )

  val endpoints = List(listSystems, listRoutes)
  val docs      = OpenAPIDocsInterpreter().toOpenAPI(
    endpoints,
    "timerboard-net-routefinder",
    "0.1"
  )

}
