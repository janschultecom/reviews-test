package com.janschulte.reviews.api

import cats.effect.IO
import io.chrisdavenport.log4cats.Logger
import org.http4s.{EntityDecoder, Response, Status}

/**
  * Miscellaneous testing utils
  */
object TestUtils {

  def check[A](actual: IO[Response[IO]],
               expectedStatus: Status,
               expectedBody: Option[A]
              )(
                implicit ev: EntityDecoder[IO, A]
              ): Boolean = {
    val actualResp = actual.unsafeRunSync
    val statusCheck = actualResp.status == expectedStatus
    val bodyCheck = expectedBody.fold[Boolean](
      actualResp.body.compile.toVector.unsafeRunSync.isEmpty)( // Verify Response's body is empty.
      expected => {
        val x = actualResp.as[A].unsafeRunSync
        x == expected
      }
    )
    statusCheck && bodyCheck
  }

  val ignoreLogger = new Logger[IO] {
    override def error(t: Throwable)(message: => String): IO[Unit] = IO.pure(())

    override def warn(t: Throwable)(message: => String): IO[Unit] = IO.pure(())

    override def info(t: Throwable)(message: => String): IO[Unit] = IO.pure(())

    override def debug(t: Throwable)(message: => String): IO[Unit] = IO.pure(())

    override def trace(t: Throwable)(message: => String): IO[Unit] = IO.pure(())

    override def error(message: => String): IO[Unit] = IO.pure(())

    override def warn(message: => String): IO[Unit] = IO.pure(())

    override def info(message: => String): IO[Unit] = IO.pure(())

    override def debug(message: => String): IO[Unit] = IO.pure(())

    override def trace(message: => String): IO[Unit] = IO.pure(())
  }

}
