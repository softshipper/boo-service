package com.sweetsoft

import org.http4s.{HttpRoutes, Response}
import org.http4s.client.Client
import org.http4s.dsl._
import cats.effect._
import fs2.text
import cats._
import cats.implicits._

object Services {

  def apply[F[_] : Defer : Applicative](c: Client[F], fooUrl: String, zooUrl: String)
  : Services[F] = new Services(c, fooUrl, zooUrl)

}

final case class Services[F[_] : Defer : Applicative](c: Client[F], fooUrl: String, zooUrl: String)
  extends Http4sDsl[F] {

  def paths(): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "foo" =>
      call(fooUrl)
    case GET -> Root / "zoo" =>
      call(zooUrl)
  }

  private def call(addr: String): F[Response[F]] =
    c.get(addr |+| "/greet") { res =>
      Applicative[F].pure {
        Response[F]().withEntity {
          res
            .body
            .through(text.utf8Decode)
            .map(msg => "Forwarded through Boo" |+| msg)
        }
      }
    }

}
