package com.example.chatapp

import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  def run(args: List[String]) =
    ChatappServer.stream[IO].compile.drain.as(ExitCode.Success)
}
