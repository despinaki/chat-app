package com.example.chatapp

import cats.effect.{ExitCode, IO, IOApp}
import fs2.concurrent.Queue
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.websocket.WebSocketBuilder
import fs2.Pipe
import org.http4s.websocket.WebSocketFrame

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    for {
      q <- Queue.unbounded[IO, WebSocketFrame]
      res <- startServer(q)
    } yield res

  }

  def startServer(queue: Queue[IO, WebSocketFrame]): IO[ExitCode] = {
    val receive: Pipe[IO, WebSocketFrame, Unit] =
      s => s.map(x => {
        queue.enqueue1(x).unsafeRunSync()
      })

    val helloWorldService = HttpRoutes.of[IO] {
      case GET -> Root / "hello" / name =>
        Ok(s"Hello, $name.")
      case GET -> Root / "ws" =>
        WebSocketBuilder[IO].build(queue.dequeue, receive)
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
