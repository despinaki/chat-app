package com.example.chatapp

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {
  def run(args: List[String]) : IO[ExitCode] = {
    val helloWorldService = HttpRoutes.of[IO] {
      case GET -> Root / "hello" / name =>
        Ok(s"Hello, $name.")
    }
    val httpApp = Router("/" -> helloWorldService).orNotFound
    BlazeServerBuilder[IO](global).bindHttp(8080, "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
