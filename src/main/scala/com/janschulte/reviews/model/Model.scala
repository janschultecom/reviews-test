package com.janschulte.reviews.model

import java.time.LocalDate


final case class Asin(unwrap: String)

final case class ReviewerId(unwrap: String)

final case class ReviewerName(unwrap: String)

final case class ReviewText(unwrap: String)

final case class HelpfulVotes(helpful: Int, total: Int)

final case class ReviewSummary(unwrap: String)



final case class BestRatedRequest(start: LocalDate, end: LocalDate, limit: Int, minNumberReviews: Int)

final case class BestRatedResponse(asin: Asin, averageRating: Double)

final case class InfluencersRequest(typ: String, minHelpfulVotes:Int, helpfulPercentage:Double, searchPhrases:List[String])

final case class InfluencersResponse(reviewerId: ReviewerId, reviewerName: Option[ReviewerName], summary: ReviewSummary)

final case class Review(reviewerId: ReviewerId,
                        asin: Asin,
                        reviewerName: Option[ReviewerName],
                        helpful: HelpfulVotes,
                        reviewText:ReviewText,
                        overall: Double,
                        summary: ReviewSummary,
                        timestamp: Long,
                        reviewTime: LocalDate)

final case class ApiError(error: String)