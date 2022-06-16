package net.timerboard.routefinder

import cats.implicits._
import cats.effect.Async
import fs2.io.file.{Files, Path}
import io.circe.Codec
import net.andimiller.hedgehogs.{Edge, Node}
import net.andimiller.hedgehogs.circe._

import java.nio.file.{Path => JPath}

case class Config(
    nodes: Vector[Node[Long, String]],
    edges: Vector[Edge[Long, Int]]
)

object Config {
  implicit val codec: Codec[Config] =
    io.circe.generic.semiauto.deriveCodec[Config]

  def load[F[_]: Async: Files](path: JPath): F[Config] =
    Files[F]
      .readAll(Path.fromNioPath(path))
      .through(fs2.text.utf8.decode)
      .compile
      .foldMonoid
      .flatMap { s =>
        Async[F].fromEither(
          io.circe.parser.parse(s)
        )
      }
      .flatMap { j =>
        Async[F].fromEither(
          j.as[Config]
        )
      }

}
