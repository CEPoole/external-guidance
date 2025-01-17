/*
 * Copyright 2021 HM Revenue & Customs
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

import core.services._
import java.util.UUID
import javax.inject.{Inject, Singleton}
import core.models.RequestOutcome
import core.models.errors.{BadRequestError, InternalServerError, NotFoundError}
import play.api.libs.json.JsObject
import repositories.ScratchRepository
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import config.AppConfig

@Singleton
class ScratchService @Inject() (repository: ScratchRepository,
                                pageBuilder: ValidatingPageBuilder,
                                implicit val c: AppConfig) {
  val logger = Logger(getClass)

  def save(json: JsObject): Future[RequestOutcome[UUID]] =
    guidancePagesAndProcess(pageBuilder, json).fold(
      err => Future.successful(Left(err)),
      result => {
        val (_, _, process) = result
        repository.save(process).map {
          case Left(_) => Left(InternalServerError)
          case result => result
        }
      }
    )

  def getById(id: String): Future[RequestOutcome[JsObject]] = {

    def getProcess(id: UUID): Future[RequestOutcome[JsObject]] = repository.getById(id) map {
      case error @ Left(NotFoundError) => error
      case Left(_) => Left(InternalServerError)
      case result => result
    }

    validateUUID(id) match {
      case Some(id) => getProcess(id)
      case None => Future { Left(BadRequestError) }
    }
  }

}
