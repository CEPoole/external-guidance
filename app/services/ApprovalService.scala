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
import models.ocelot.Process
import models.{ApprovalProcess, ApprovalProcessMeta, RequestOutcome}
import play.api.Logger
import play.api.libs.json._
import repositories.ApprovalRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ApprovalService @Inject() (repository: ApprovalRepository) {

  val logger: Logger = Logger(this.getClass)

  def save(jsonProcess: JsObject): Future[RequestOutcome[String]] = {

    def saveProcess(approvalProcess: ApprovalProcess): Future[RequestOutcome[String]] = {
      repository.update(approvalProcess) map {
        case Left(_) => Left(Errors(InternalServiceError))
        case result => result
      }
    }

    def createApprovalProcess(process: Process): ApprovalProcess = {
      ApprovalProcess(process.meta.id, ApprovalProcessMeta(process.meta.id, process.meta.title), jsonProcess)
    }

    jsonProcess.validate[Process] match {
      case JsSuccess(process, _) =>
        saveProcess(createApprovalProcess(process))
      case JsError(errors) =>
        logger.error(s"Parsing process failed with the following error(s): $errors")
        Future { Left(Errors(BadRequestError)) }
    }

  }

  def getById(id: String): Future[RequestOutcome[JsObject]] = {

    repository.getById(id) map {
      case error @ Left(Errors(NotFoundError :: Nil)) => error
      case Left(_) => Left(Errors(InternalServiceError))
      case result => result
    }
  }

  def approvalSummaryList(): Future[RequestOutcome[JsArray]] = {
    repository.approvalSummaryList().map {
      case Left(_) => Left(Errors(InternalServiceError))
      case Right(success) => Right(Json.toJson(success).as[JsArray])
    }
  }

}
