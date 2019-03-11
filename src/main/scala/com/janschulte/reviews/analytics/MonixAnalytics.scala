package com.janschulte.reviews.analytics

import java.io.{BufferedReader, File, FileInputStream, InputStreamReader}
import java.time.LocalDate

import cats.effect.Sync
import cats.implicits._
import com.janschulte.reviews.model.Codecs.reviewDecoder
import com.janschulte.reviews.model.{Asin, BestRatedResponse, InfluencersResponse, Review}
import io.circe.parser._
import monix.eval.{TaskLift, TaskLike}
import monix.reactive.Observable

final case class Avg(totalVotes: Int, totalRating: Double)

final case class Rating(asin: Asin, avg: Avg)

/**
  * [[AnalyticsAlgebra]] implementation using monix streams and file-based reviews.
  */
class MonixAnalytics[F[_]](file: File)
                          (implicit S: Sync[F], TL: TaskLift[F], TLi: TaskLike[F])
  extends AnalyticsAlgebra[F] {

  /**
    * Utility method for conveniently parsing the reviews file.
    */
  private def withReviews[O](file: File)(use: Observable[Review] => Observable[O]): Observable[O] = {

    val resource: Observable[BufferedReader] = Observable.resourceF {
      S.delay(new BufferedReader(new InputStreamReader(new FileInputStream(file))))
    } { in => S.delay(in.close()) }

    resource.flatMap { in =>
      val reviews = Observable
        .repeatEval(in.readLine())
        .takeWhile(_ != null)
        .map(text => decode[Review](text.trim).leftMap(_.fillInStackTrace()))
        .collect {
          case Right(review) => review
        }

      use(reviews)
    }
  }

  override def bestRated(start: LocalDate, end: LocalDate, limit: Int, minNumberReviews: Int): F[List[BestRatedResponse]] = {

    val streamOfRatings: Observable[Rating] = withReviews(file) { reviews =>
      reviews
        .groupBy(review => review.asin)
        .map { g =>
          g.filter(MonixAnalytics.inBetween(start, end))
            .foldLeft(Avg(0, 0.0))((avg, review) => Avg(avg.totalVotes + 1, avg.totalRating + review.overall))
            .map(avg => Rating(g.key, avg))
        }
        .flatten
        .filter(rating => rating.avg.totalVotes >= minNumberReviews)
        .take(limit.toLong)
    }

    streamOfRatings
      .map(MonixAnalytics.calculateAverageRating)
      .toListL
      .to
  }

  override def influencers(typ: String, minHelpfulVotes: Int, helpfulPercentage: Double, searchPhrases: List[String])
  : F[List[InfluencersResponse]] = {

    val streamOfReviews: Observable[Observable[Review]] = withReviews(file) { reviews =>
      reviews
        .groupBy(review => review.reviewerId)
        .map { g =>
          g.filter(MonixAnalytics.influencerReview(minHelpfulVotes, helpfulPercentage))
            .foldLeft(0)((acc, _) => acc + 1)
            .filter(numberOfReviews => numberOfReviews >= minHelpfulVotes)
            .repeat
            // Unfortunately it is not possible to reuse the grouped stream here, so one has to stream the reviews again.
            .zipMap(withReviews(file) { r => r.filter(r => r.reviewerId == g.key) })((_, review) => review)
            .filter(MonixAnalytics.containsPhrase(searchPhrases))
        }
    }

    streamOfReviews
      .flatten
      .map(MonixAnalytics.convertToInfluencers)
      .toListL
      .to
  }
}

/**
  * Utility functions used in the stream processing.
  */
object MonixAnalytics {

  val influencerReview: (Int, Double) => Review => Boolean = (minHelpfulVotes, helpfulPercentage) =>
    r => {
      val calculatedPercentage = if (r.helpful.total == 0) 0.0 else r.helpful.helpful / r.helpful.total.toDouble
      (r.helpful.helpful >= minHelpfulVotes) && (calculatedPercentage >= helpfulPercentage)
    }

  val inBetween: (LocalDate, LocalDate) => Review => Boolean = (start, end) => review => {
    (review.reviewTime.isEqual(start) || review.reviewTime.isAfter(start)) &&
      (review.reviewTime.isEqual(end) || review.reviewTime.isBefore(end))
  }

  val calculateAverageRating: Rating => BestRatedResponse = rating => {
    val averageRating = if (rating.avg.totalRating == 0) 0 else {
      rating.avg.totalRating / rating.avg.totalVotes
    }
    BestRatedResponse(rating.asin, averageRating)
  }
  val containsPhrase: List[String] => Review => Boolean =
    searchPhrases => review => searchPhrases.exists(phrase => review.reviewText.unwrap.contains(phrase))

  val convertToInfluencers: Review => InfluencersResponse = review => {
    InfluencersResponse(review.reviewerId, review.reviewerName, review.summary)
  }
}