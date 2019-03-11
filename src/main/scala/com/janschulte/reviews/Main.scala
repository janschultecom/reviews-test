package com.janschulte.reviews

import java.io.File

import cats.effect._
import com.janschulte.reviews.analytics.MonixAnalytics
import com.janschulte.reviews.api.Service
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.blaze._
import org.http4s.implicits._
import com.janschulte.reviews.config.ServiceConfig
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import monix.eval._
import monix.execution.Scheduler.Implicits.global
import pureconfig.generic.auto._
import pureconfig.module.catseffect._

object Main extends IOApp {

  implicit val ioTaskLift: TaskLift[IO] = TaskLift.toIO

  override def run(args: List[String]): IO[ExitCode] = args match {
    case file :: Nil =>
      for {
        serviceConfig <- loadConfigF[IO, ServiceConfig]("service")
        analyticsAlgebra = new MonixAnalytics[IO](new File(file))
        logger <- Slf4jLogger.create[IO]
        api = new Service[IO](analyticsAlgebra, logger)
        _ <- ServerBuilder.stream[IO](api.amazon)(serviceConfig)
      } yield ExitCode.Success
    case _ =>
      IO.raiseError[ExitCode](new Throwable("No file name given"))
  }


}

object ServerBuilder {

  def stream[F[_]](api: HttpRoutes[F])(serviceConfig: ServiceConfig)
                  (implicit ce: ConcurrentEffect[F], t: Timer[F]): F[Unit] = {

    val httpApp = Router("/" -> api).orNotFound

    BlazeServerBuilder[F]
      .bindHttp(serviceConfig.port, serviceConfig.host)
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
  }
}
