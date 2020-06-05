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

package services

import javax.inject.{Inject, Singleton}
import models.errors.{BadRequestError, Errors, InternalServiceError, NotFoundError}
import models.{ApprovalProcess, ApprovalProcessReview, RequestOutcome}
import play.api.Logger
import play.api.libs.json._
import repositories.{ApprovalProcessReviewRepository, ApprovalRepository}
import utils.ProcessUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ApprovalService @Inject() (repository: ApprovalRepository, reviewRepository: ApprovalProcessReviewRepository) {

  val logger: Logger = Logger(this.getClass)

  def save(jsonProcess: JsObject, reviewType: String, initialStatus: String): Future[RequestOutcome[String]] = {

    def saveProcess(approvalProcess: ApprovalProcess): Future[RequestOutcome[String]] = {
      repository.update(approvalProcess) map {
        case Left(_) => Left(Errors(InternalServiceError))
        case result => result
      }
    }

    def saveReview(approvalProcessReview: ApprovalProcessReview): Future[RequestOutcome[String]] = {
      reviewRepository.save(approvalProcessReview) map {
        case Right(_) => Right(approvalProcessReview.ocelotId)
        case _ => Left(Errors(InternalServiceError))
      }
    }

    validateProcess(jsonProcess) match {
      case Right(process) =>
        saveProcess(createApprovalProcess(process.meta.id, process.meta.title, initialStatus, jsonProcess)) flatMap {
          case Right(savedId) =>
            repository.getById(savedId) flatMap {
              case Right(approvalProcess) => saveReview(createApprovalProcessReview(process, reviewType, approvalProcess.version))
              case Left(Errors(NotFoundError :: Nil)) => Future.successful(Left(Errors(NotFoundError)))
              case Left(_) => Future.successful(Left(Errors(InternalServiceError)))
            }
          case errors => Future.successful(errors)
        }
      case Left(_) =>
        Future.successful(Left(Errors(BadRequestError)))
    }

  }

  def getById(id: String): Future[RequestOutcome[JsObject]] = {

    repository.getById(id) map {
      case Left(Errors(NotFoundError :: Nil)) => Left(Errors(NotFoundError))
      case Left(_) => Left(Errors(InternalServiceError))
      case Right(result) => Right(result.process)
    }
  }

  def approvalSummaryList(): Future[RequestOutcome[JsArray]] = {
    repository.approvalSummaryList().map {
      case Left(_) => Left(Errors(InternalServiceError))
      case Right(success) => Right(Json.toJson(success).as[JsArray])
    }
  }

}