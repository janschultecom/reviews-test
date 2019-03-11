package com.janschulte.reviews.analytics

import java.time.LocalDate

import com.janschulte.reviews.model.{BestRatedResponse, InfluencersResponse}

/**
  * Algebra for data analysis on reviews
  * @tparam F
  */
trait AnalyticsAlgebra[F[_]] {

  /**
    * Retrieves the best rated products in a given time period
    * @param start Start date (inclusive)
    * @param end End date (inclusive)
    * @param limit Maximum number of items to be returned
    * @param minNumberReviews Minimum number of reviews a product must to have in order to be considered
    * @return List of best rated products
    */
  def bestRated(start: LocalDate, end: LocalDate, limit: Int, minNumberReviews: Int): F[List[BestRatedResponse]]

  /**
    * Retrieves the reviews by influencers that contains at least one of the phrases.
    * Influencers must have at least one review where the total amount of votes > minNumberReviews and
    * the ratio between helpful votes and total votes is greater or equal to helpfulPercentage
    * @param typ Search type, currently ignored
    * @param minHelpfulVotes Minimum amount of review votes
    * @param helpfulPercentage Minimum percentage of helpful to total votes
    * @param searchPhrases List of phrases, at least one must match
    * @return List of influencer reviews that match the criteria
    */
  def influencers(typ: String, minHelpfulVotes:Int, helpfulPercentage:Double, searchPhrases:List[String])
  : F[List[InfluencersResponse]]

}

