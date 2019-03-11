package com.janschulte.reviews.api

import java.time.LocalDate

import cats.effect.IO
import com.janschulte.reviews.analytics.AnalyticsAlgebra
import com.janschulte.reviews.api.TestUtils._
import com.janschulte.reviews.model.{Asin, BestRatedResponse, InfluencersResponse}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.{Method, Request, Response, Status, Uri}
import org.specs2.mutable.Spec

class ServiceSpec extends Spec {

  "The Service" should {

    val request =
      """{
        |  "start": "15.10.2011",
        |  "end": "01.08.2013",
        |  "limit": 2,
        |  "min_number_reviews": 2
        |}""".stripMargin

    s"return ${Status.Ok} for a successful best rated request" in {

      val analytics: AnalyticsAlgebra[IO] = new AnalyticsAlgebra[IO] {
        override def bestRated(start: LocalDate, end: LocalDate, limit: Int, minNumberReviews: Int)
        : IO[List[BestRatedResponse]] = {
          IO.pure(List(BestRatedResponse(Asin("A123456789"), 3.7)))
        }

        override def influencers(typ: String, minHelpfulVotes: Int, helpfulPercentage: Double, searchPhrases: List[String])
        : IO[List[InfluencersResponse]] = ???
      }

      val service = new Service[IO](analytics, ignoreLogger)

      val response: IO[Response[IO]] = Router("/" -> service.amazon).orNotFound.run(
        Request(method = Method.POST, uri = Uri.uri("/amazon/best-rated")).withEntity(request)
      )

      val expected = """[{"asin":"A123456789","average_rating":3.7}]""".stripMargin

      check(response, Status.Ok, Some(expected))
    }

    s"return ${Status.InternalServerError} for a successful best rated request" in {

      val analytics: AnalyticsAlgebra[IO] = new AnalyticsAlgebra[IO] {
        override def bestRated(start: LocalDate, end: LocalDate, limit: Int, minNumberReviews: Int)
        : IO[List[BestRatedResponse]] = IO.raiseError(new Throwable("Harddisk exploded"))

        override def influencers(typ: String, minHelpfulVotes: Int, helpfulPercentage: Double, searchPhrases: List[String])
        : IO[List[InfluencersResponse]] = ???
      }

      val service = new Service[IO](analytics, ignoreLogger)

      val response: IO[Response[IO]] = Router("/" -> service.amazon).orNotFound.run(
        Request(method = Method.POST, uri = Uri.uri("/amazon/best-rated")).withEntity(request)
      )

      val expected = """{"error":"Something went wrong: Harddisk exploded"}""".stripMargin

      check(response, Status.InternalServerError, Some(expected))
    }

    s"return ${Status.BadRequest} for a successful best rated request" in {

      val analytics: AnalyticsAlgebra[IO] = new AnalyticsAlgebra[IO] {
        override def bestRated(start: LocalDate, end: LocalDate, limit: Int, minNumberReviews: Int)
        : IO[List[BestRatedResponse]] = {
          IO.raiseError(new Throwable("Code unreachable"))
        }

        override def influencers(typ: String, minHelpfulVotes: Int, helpfulPercentage: Double, searchPhrases: List[String])
        : IO[List[InfluencersResponse]] = ???
      }

      val service = new Service[IO](analytics, ignoreLogger)

      val response: IO[Response[IO]] = Router("/" -> service.amazon).orNotFound.run(
        Request(method = Method.POST, uri = Uri.uri("/amazon/best-rated")).withEntity("not a json")
      )

      val expected = """{"error":"Invalid input json: Malformed message body: Invalid JSON"}""".stripMargin

      check(response, Status.BadRequest, Some(expected))
    }

  }

}
