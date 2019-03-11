package com.janschulte.reviews.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import cats.effect.Sync
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Decoder, Encoder}
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf
import cats.implicits._

object Codecs {

  private val germanDateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

  val decodeDateGerman: Decoder[LocalDate] = Decoder.decodeString.emap { dateStr =>
    Either.catchNonFatal(LocalDate.parse(dateStr, germanDateFormat)).leftMap(t => s"Invalid date: ${t.getMessage}")
  }

  private val amazonDateFormat = DateTimeFormatter.ofPattern("MM d, yyyy")

  val decodeDateAmazon: Decoder[LocalDate] = Decoder.decodeString.emap { dateStr =>
    Either.catchNonFatal(LocalDate.parse(dateStr, amazonDateFormat)).leftMap(t => s"Invalid date: ${t.getMessage}")
  }

  implicit val asinDecoder: Decoder[Asin] = Decoder.decodeString.map(Asin.apply)
  implicit val asinEncoder: Encoder[Asin] = Encoder.encodeString.contramap(_.unwrap)

  implicit val reviewerIdDecoder: Decoder[ReviewerId] = Decoder.decodeString.map(ReviewerId.apply)
  implicit val reviewerIdEncoder: Encoder[ReviewerId] = Encoder.encodeString.contramap(_.unwrap)

  implicit val reviewerNameDecoder: Decoder[ReviewerName] = Decoder.decodeString.map(ReviewerName.apply)
  implicit val reviewerNameEncoder: Encoder[ReviewerName] = Encoder.encodeString.contramap(_.unwrap)

  implicit val reviewTextDecoder: Decoder[ReviewText] = Decoder.decodeString.map(ReviewText.apply)
  implicit val helpfulVotesDecoder: Decoder[HelpfulVotes] = Decoder.decodeArray[Int].emap {
    case Array(helpful, total) => Right(HelpfulVotes(helpful, total))
    case otherwise => Left(s"Invalid helpful: $otherwise")
  }
  implicit val reviewSummaryDecoder: Decoder[ReviewSummary] = Decoder.decodeString.map(ReviewSummary.apply)
  implicit val reviewSummaryEncoder: Encoder[ReviewSummary] = Encoder.encodeString.contramap(_.unwrap)

  implicit val bestRatedReqDecoder: Decoder[BestRatedRequest] = Decoder
    .forProduct4[BestRatedRequest, LocalDate, LocalDate, Int, Int]("start", "end", "limit", "min_number_reviews")(
    (start, end, limit, minNumReviews) => BestRatedRequest(start, end, limit, minNumReviews)
  )(decodeDateGerman, decodeDateGerman, Decoder.decodeInt, Decoder.decodeInt)

  implicit def bestRatedReqEntityDecoder[F[_]](implicit S: Sync[F]): EntityDecoder[F, BestRatedRequest] =
    jsonOf[F, BestRatedRequest]

  implicit val bestRatedRespEncoder: Encoder[BestRatedResponse] =
    Encoder.forProduct2[BestRatedResponse, Asin, Double]("asin", "average_rating")(bestRated => {
      (bestRated.asin, bestRated.averageRating)
    })

  implicit val influencersReqDecoder: Decoder[InfluencersRequest] = Decoder
    .forProduct4[InfluencersRequest,String,Int,Double,List[String]](
    "type","min_helpful_votes","helpful_percentage","search_phrases")(
    (typ,minHelpfulVotes,helpfulPercentage,searchPhrases) =>
      InfluencersRequest(typ,minHelpfulVotes,helpfulPercentage,searchPhrases)
  )

  implicit def influencersReqEntityDecoder[F[_]](implicit S: Sync[F]): EntityDecoder[F, InfluencersRequest] =
    jsonOf[F, InfluencersRequest]

  implicit val influencersRespEncoder: Encoder[InfluencersResponse] =
    Encoder.forProduct3[InfluencersResponse,ReviewerId,Option[ReviewerName],ReviewSummary](
      "reviewer_id","reviewer_name","summary")(influencer => {
      (influencer.reviewerId,influencer.reviewerName,influencer.summary)
    })

  implicit val errorEncoder: Encoder[ApiError] = deriveEncoder

  implicit val reviewDecoder: Decoder[Review] = Decoder
    .forProduct9[Review, ReviewerId, Asin, Option[ReviewerName], HelpfulVotes, ReviewText, Double, ReviewSummary, Long, LocalDate](
    "reviewerID", "asin", "reviewerName", "helpful", "reviewText",
    "overall", "summary", "unixReviewTime", "reviewTime"
  )((reviewId, asin, name, helpfulVotes, text, overall, summary, timestamp, reviewTime) =>
    Review(reviewId, asin, name, helpfulVotes, text, overall, summary, timestamp, reviewTime)
  )(reviewerIdDecoder, asinDecoder,Decoder.decodeOption(reviewerNameDecoder), helpfulVotesDecoder, reviewTextDecoder, Decoder.decodeDouble,
    reviewSummaryDecoder, Decoder.decodeLong, decodeDateAmazon)

}
