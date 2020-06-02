/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package repositories

import java.util.UUID

import javax.inject.{Inject, Singleton}
import models.errors.{DatabaseError, Errors, NotFoundError}
import models.{ApprovalProcessReview, RequestOutcome}
import play.api.libs.json.{Format, JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.ImplicitBSONHandlers._
import repositories.formatters.ApprovalProcessReviewFormatter
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ApprovalProcessReviewRepository {
  def save(review: ApprovalProcessReview): Future[RequestOutcome[UUID]]
  def getByIdVersionAndType(id: String, version: Int, reviewType: String): Future[RequestOutcome[ApprovalProcessReview]]
}

@Singleton
class ApprovalProcessReviewRepositoryImpl @Inject() (implicit mongoComponent: ReactiveMongoComponent)
    extends ReactiveRepository[ApprovalProcessReview, UUID](
      collectionName = "approvalProcessReviews",
      mongo = mongoComponent.mongoConnector.db,
      domainFormat = ApprovalProcessReviewFormatter.mongoFormat,
      idFormat = implicitly[Format[UUID]]
    )
    with ApprovalProcessReviewRepository {

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq("ocelotId" -> IndexType.Ascending, "version" -> IndexType.Ascending, "reviewType" -> IndexType.Ascending),
      name = Some("review-secondary-Index"),
      unique = true
    )
  )

  def save(review: ApprovalProcessReview): Future[RequestOutcome[UUID]] = {

    insert(review)
      .map { _ =>
        Right(review.id)
      }
      //$COVERAGE-OFF$
      .recover {
        case error =>
          logger.error(s"Attempt to persist process ${review.id} to collection published failed with error : ${error.getMessage}")
          Left(Errors(DatabaseError))
      }
    //$COVERAGE-ON$
  }

  def getByIdVersionAndType(id: String, version: Int, reviewType: String): Future[RequestOutcome[ApprovalProcessReview]] = {

    val selector = Json.obj("ocelotId" -> id, "version" -> version, "reviewType" -> reviewType)
    val projection: Option[JsObject] = Option.empty[JsObject]

    collection
      .find[JsObject, JsObject](selector)
      .one[ApprovalProcessReview]
      .map {
        case Some(review) => Right(review)
        case None => Left(Errors(NotFoundError))
      }
      //$COVERAGE-OFF$
      .recover {
        case error =>
          logger.error(s"Attempt to retrieve review $id and version $version from collection $collectionName failed with error : ${error.getMessage}")
          Left(Errors(DatabaseError))
      }
    //$COVERAGE-ON$
  }

}
