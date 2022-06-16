package net.timerboard.routefinder

import cats.implicits._
import cats.effect._
import com.comcast.ip4s.IpAddress
import net.andimiller.hedgehogs.Graph
import org.http4s.ember.server.EmberServerBuilder
import scalacache.caffeine.CaffeineCache

import scala.concurrent.duration.DurationInt
import scala.util.control.NoStackTrace

object Server extends IOApp {

  def serve[F[_]: Async](config: Config) = for {
    graph      <-
      Async[F].fromEither(
        Graph
          .fromIterables(config.nodes, config.edges, bidirectional = true)
          .toEither
          .leftMap(es => new Throwable(es.mkString_("\n")) with NoStackTrace)
      )
    systemIds   = config.nodes.map(_.id).toList
    systems     = config.nodes.map { n => System(n.data, n.id) }.toList
    api         = new API(systemIds)
    routeCache <- CaffeineCache[F, SystemId, Map[Long, Int]]
    routes      = new Routes[F](api, graph, systems, routeCache)
    _          <- EmberServerBuilder
                    .default[F]
                    .withHost(IpAddress.fromString("0.0.0.0").get)
                    .withHttpApp(routes.routes)
                    .build
                    .use { _ =>
                      Async[F].never[Unit]
                    }
  } yield ExitCode.Success

  override def run(args: List[String]): IO[ExitCode] =
    CLI.server.parse(args) match {
      case Left(value)       => IO.println(value).as(ExitCode.Success)
      case Right(configPath) =>
        Config.load[IO](configPath).flatMap(serve[IO])
    }
}
