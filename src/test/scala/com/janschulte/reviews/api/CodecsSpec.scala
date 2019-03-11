package com.janschulte.reviews.api

import java.time.LocalDate

import com.janschulte.reviews.model._
import org.specs2.mutable.Spec
import io.circe.parser._
import Codecs._
import io.circe.syntax._

class CodecsSpec extends Spec {

  "Codecs" should {

    "decode a best rated request" in {
      val input = """{
                    |  "start": "15.10.2011",
                    |  "end": "01.08.2013",
                    |  "limit": 2,
                    |  "min_number_reviews": 2
                    |}""".stripMargin

      val expected = BestRatedRequest(
        LocalDate.of(2011,10,15),
        LocalDate.of(2013,8,1),
        2,
        2
      )
      decode[BestRatedRequest](input) must beRight(expected)
    }

    "decode a review" in {

      val input =
        """{"reviewerID": "B1111111111111",
          |"asin": "B00005BOSF",
          |"reviewerName": "John Q. Public",
          |"helpful": [3, 18],
          |"reviewText": "This is an amazing book",
          |"overall": 5.0, "summary": "Awesome",
          |"unixReviewTime": 1319760000,
          |"reviewTime": "07 9, 2012"}""".stripMargin

      val expected = Review(
        ReviewerId("B1111111111111"),
        Asin("B00005BOSF"),
        Some(ReviewerName("John Q. Public")),
        HelpfulVotes(3,18),
        ReviewText("This is an amazing book"),
        5.0,
        ReviewSummary("Awesome"),
        1319760000,
        LocalDate.of(2012,7,9)
      )

      decode[Review](input) must beRight(expected)
    }

    "encode a best rated response" in {
      val input = BestRatedResponse(Asin("A123456789"),3.5)

      val expected = """{"asin":"A123456789","average_rating":3.5}"""
      input.asJson.noSpaces must be_==(expected)
    }
  }

}
