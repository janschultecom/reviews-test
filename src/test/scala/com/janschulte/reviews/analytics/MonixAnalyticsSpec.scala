package com.janschulte.reviews.analytics

import java.io.File
import java.time.LocalDate

import cats.effect.IO
import com.janschulte.reviews.model._
import monix.eval.TaskLift
import monix.execution.Scheduler.Implicits.global
import org.specs2.mutable.Spec

import scala.concurrent.duration._

class MonixAnalyticsSpec extends Spec {

  implicit val ioTaskLift: TaskLift[IO] = TaskLift.toIO

  "MonixAnalytics" should {

    val file = new File("src/test/resources/test-reviews.json")
    val analytics = new MonixAnalytics[IO](file)

    "calculate the best rated products (1)" in {

      val expected = List(
        BestRatedResponse(Asin("0700099867"), 4.5),
        BestRatedResponse(Asin("9625990674"), 4.0),
      )

      val start = LocalDate.of(2011, 1, 1)
      val end = LocalDate.of(2011, 12, 31)

      val result = analytics
        .bestRated(start, end, 2, 2)
        .unsafeRunTimed(1.second)

      result must beSome(expected)
    }

    "calculate the best rated products (2)" in {

      val expected = List(
        BestRatedResponse(Asin("0700099867"), 3.3333333333333335)
      )

      val start = LocalDate.of(2011, 1, 1)
      val end = LocalDate.of(2014, 12, 31)

      val result = analytics
        .bestRated(start, end, 2, 5)
        .unsafeRunTimed(1.second)

      result must beSome(expected)
    }


    "calculate the influencer reviews" in {

      val expected = List(
        InfluencersResponse(ReviewerId("B0000000000002"), None, ReviewSummary("Lorem ipsum summary 2")),
        InfluencersResponse(ReviewerId("B0000000000007"), Some(ReviewerName("Name 7")), ReviewSummary("Lorem ipsum summary 7")),
        InfluencersResponse(ReviewerId("B0000000000007"), Some(ReviewerName("Name 7")), ReviewSummary("Lorem ipsum summary 10")),
      )

      val result = analytics
        .influencers("influencer-reviews", 1, 0.3, List("love", "hate"))
        .unsafeRunTimed(1.second)

      result must beSome(expected)
    }
  }
}
