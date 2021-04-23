package com.example.chatapp

import cats.effect.{ExitCode, IO, IOApp}
import fs2.concurrent.Queue
import fs2.Stream
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.websocket.WebSocketBuilder
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Close

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    var allQueues: List[Queue[IO, WebSocketFrame]] = List.empty
    val helloWorldService = HttpRoutes.of[IO] {
      case GET -> Root / "hello" / name =>
        Ok(s"Hello, $name.")
      case GET -> Root / "ws" =>
        for {
          queue <- Queue.unbounded[IO, WebSocketFrame]
          _ <- IO {
            allQueues ++= List(queue)
          }
          pipe = (s: Stream[IO, WebSocketFrame]) => s.map(x => {
            println(x)
            x match {
              case Close(_) => allQueues = allQueues.filter(_ == queue)
              case _ => allQueues
                .filter(_ != queue)
                .foreach(_.enqueue1(x).unsafeRunSync())
            }
          })
          response <- WebSocketBuilder[IO].build(queue.dequeue, pipe) //needs stream for sent messages and pipe to consume them

        } yield response
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
