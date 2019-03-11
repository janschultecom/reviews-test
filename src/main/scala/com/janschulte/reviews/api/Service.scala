package com.janschulte.reviews.api

import cats.effect.Sync
import org.http4s._
import org.http4s.dsl.Http4sDsl
import com.janschulte.reviews.model.Codecs._
import cats.data.EitherT
import io.circe.syntax._
import org.http4s.circe._
import cats.implicits._
import com.janschulte.reviews.analytics.AnalyticsAlgebra
import com.janschulte.reviews.model.{ApiError, BestRatedRequest, InfluencersRequest}
import io.chrisdavenport.log4cats.Logger

/**
  * Constructs http routes for the analytics http service
  */
class Service[F[_]](analytics: AnalyticsAlgebra[F], logger: Logger[F])(implicit S: Sync[F])
  extends Http4sDsl[F] {

  val amazon: HttpRoutes[F] = HttpRoutes.of[F] {
    case request@POST -> Root / "amazon" / "best-rated" =>
      request
        .attemptAs[BestRatedRequest]
        .flatMap(apiRequest =>
          EitherT.right[DecodeFailure](
            analytics.bestRated(
              apiRequest.start,
              apiRequest.end,
              apiRequest.limit,
              apiRequest.minNumberReviews)
          )
        )
        .fold(
          error =>
            logger.info("Got invalid best-rated request") *>
              BadRequest(ApiError(s"Invalid input json: ${error.getMessage()}").asJson),
          response =>
            logger.info("Got successful best-rated request") *>
              Ok(response.asJson)
        ).flatten.handleErrorWith {
        error =>
          logger.error("Got an internal server error while serving best-rated request") *>
            InternalServerError(ApiError(s"Something went wrong: ${error.getMessage}").asJson)
      }

    case request@POST -> Root / "amazon" / "influencers" =>
      request
        .attemptAs[InfluencersRequest]
        .flatMap(apiRequest =>
          EitherT.right[DecodeFailure](
            analytics.influencers(
              apiRequest.typ,
              apiRequest.minHelpfulVotes,
              apiRequest.helpfulPercentage,
              apiRequest.searchPhrases
            )
          )
        )
        .fold(
          error =>
            logger.info("Got invalid influencers request") *>
              BadRequest(ApiError(s"Invalid input json: ${error.getMessage()}").asJson),
          response =>
            logger.info("Got successful influencers request") *>
              Ok(response.asJson)
        ).flatten.handleErrorWith {
        error =>
          logger.error("Got an internal server error while serving influencers request") *>
            InternalServerError(ApiError(s"Something went wrong: ${error.getMessage}").asJson)
      }
  }
}
