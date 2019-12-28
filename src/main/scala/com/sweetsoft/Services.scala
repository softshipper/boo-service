package com.sweetsoft

import org.http4s.{Header, Headers, HttpRoutes, Response}
import org.http4s.client.Client
import org.http4s.dsl._
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
    case req@(GET -> Root / "service") =>
      filterValidServices(req.headers) match {
        case Some(service) =>
          call(createUrl(service))
        case None =>
          BadRequest("The service does not exists!!")
      }
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

  private def containValidServices(header: Header)
  : Boolean =
    header.name == "service" && (header.value == "FOO" || header.value == "ZOO")

  private def filterValidServices(headers: Headers)
  : Option[String] =
    headers.filter(containValidServices).foldLeft(Option.empty[String]) { (_, header) =>
      Some(header.value)
    }

  private def createUrl(serviceName: String): String =
    s"http://${serviceName}"

}
