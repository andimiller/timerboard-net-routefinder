package net.timerboard.routefinder

import cats.data.Kleisli
import cats.implicits._
import cats.effect._
import net.andimiller.hedgehogs.{Dijkstra, Graph}
import org.http4s.{HttpRoutes, Request, Response}
import scalacache.Cache
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.apispec.openapi.circe.yaml._
import sttp.tapir.redoc.{Redoc, RedocUIOptions}
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics

import scala.concurrent.duration.DurationInt

class Routes[F[_]: Async](
    val api: API,
    graph: Graph[Long, String, Int],
    systems: List[System],
    routesCache: Cache[F, SystemId, Map[Long, Int]]
) {

  val listSystems = api.listSystems.serverLogic[F] { _ =>
    systems.asRight[Unit].pure[F]
  }

  val listRoutes = api.listRoutes.serverLogic[F] { system =>
    routesCache
      .cachingF(system)(1.hour.some) {
        Sync[F].delay {
          Dijkstra
            .multi(graph)(system.value, systems.toSet.map[Long](_.id))
            .view
            .mapValues(_._1)
            .toMap
        }
      }
      .map(_.asRight)
  }

  val docs = Redoc[F](
    "timerboard-net-routefinder",
    api.docs.toYaml,
    RedocUIOptions.default
  )

  val prom = PrometheusMetrics.default[F]()

  val endpoints: HttpRoutes[F] =
    Http4sServerInterpreter[F](
      Http4sServerOptions.customiseInterceptors
        .metricsInterceptor((prom.metricsInterceptor()))
        .corsInterceptor(CORSInterceptor.default[F])
        .options
    ).toRoutes(
      List(listSystems, listRoutes, prom.metricsEndpoint) ++ docs
    )

  import org.http4s.implicits._
  val routes: Kleisli[F, Request[F], Response[F]] = endpoints.orNotFound
}
