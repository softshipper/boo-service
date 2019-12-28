package com.sweetsoft

import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import org.http4s.{Request, Response}
import org.http4s.implicits._
import org.http4s.server.blaze._
import org.http4s.client.blaze._
import org.http4s.client._
import org.http4s.server.Router

import scala.concurrent.ExecutionContext.global

object Main extends IOApp {


  private def fooUrl: IO[String] = IO {
    sys.env("FOO-SERVICE")
  }

  private def zooUrl: IO[String] = IO {
    sys.env("ZOO-SERVICE")
  }

  private def router[F[_] : Sync](c: Client[F], fooUrl: String, booUrl: String)
  : Kleisli[F, Request[F], Response[F]] =
    Router("/" -> Services[F](c, fooUrl, booUrl).paths()).orNotFound


  private def server[F[_] : Timer : ConcurrentEffect](fooUrl: String, booUrl: String)
  : F[ExitCode] =

    BlazeClientBuilder[F](global).resource.use { client =>

      BlazeServerBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(router(client, fooUrl, booUrl))
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }

  def run(args: List[String]): IO[ExitCode] =

    for {
      f <- fooUrl
      z <- zooUrl
      e <- server(f, z)
    } yield e

}
