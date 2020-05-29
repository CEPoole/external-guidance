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

import java.util.UUID

import base.UnitSpec
import mocks.{MockApprovalProcessReviewRepository, MockApprovalRepository}
import models._
import models.errors._
import models.ocelot.ProcessJson
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{JsArray, JsObject, Json}

import scala.concurrent.Future

class ApprovalServiceSpec extends UnitSpec with MockFactory {

  private trait Test extends MockApprovalRepository with MockApprovalProcessReviewRepository with ApprovalProcessJson with ProcessJson {

    val invalidId: String = "ext9005"

    val invalidProcess: JsObject = Json.obj("idx" -> invalidId)

    lazy val service: ApprovalService = new ApprovalService(mockApprovalRepository, mockApprovalProcessReviewRepository)

    val processReview = ApprovalProcessReview(
      UUID.randomUUID(),
      "oct90001",
      1,
      "2i-review",
      "title",
      List()
    )
  }

  "Calling the getById method" when {
    "the ID identifies a valid process" should {
      "return a JSON representing the submitted ocelot process" in new Test {

        val returnFromRepo: RequestOutcome[ApprovalProcess] = Right(approvalProcess)
        val expected: RequestOutcome[JsObject] = Right(approvalProcess.process)

        MockApprovalRepository
          .getById(validId)
          .returns(Future.successful(returnFromRepo))

        whenReady(service.getById(validId)) { result =>
          result shouldBe expected
        }
      }
    }

    "the ID cannot be matched to a submitted process" should {
      "return a not found response" in new Test {

        val expected: RequestOutcome[ApprovalProcess] = Left(Errors(NotFoundError))

        MockApprovalRepository
          .getById(validId)
          .returns(Future.successful(expected))

        whenReady(service.getById(validId)) { result =>
          result shouldBe expected
        }
      }
    }

    "the repository reports a database error" should {
      "return an internal server error" in new Test {

        val repositoryError: RequestOutcome[ApprovalProcess] = Left(Errors(DatabaseError))
        val expected: RequestOutcome[JsObject] = Left(Errors(InternalServiceError))

        MockApprovalRepository
          .getById(validId)
          .returns(Future.successful(repositoryError))

        whenReady(service.getById(validId)) { result =>
          result shouldBe expected
        }
      }
    }
  }

  "Calling the save method" when {

    "the id and JSON are valid" should {
      "return valid Id" in new Test {

        val expected: RequestOutcome[String] = Right(validId)

        MockApprovalRepository
          .update(approvalProcess)
          .returns(Future.successful(expected))

        MockApprovalRepository
          .getById("oct90001")
          .returns(Future.successful(Right(approvalProcess)))

        MockApprovalProcessReviewRepository
          .save(processReview)
          .returns(Future.successful(Right(processReview.id)))

        whenReady(service.save(validOnePageJson.as[JsObject], "2i-review", "SubmittedFor2iReview")) {
          case Right(id) => id shouldBe validId
          case _ => fail
        }
      }
    }

    "the process is saved" when {
      "the save of review data fails" should {
        "return an internal error" in new Test {

          val expected: RequestOutcome[String] = Left(Errors(InternalServiceError))

          MockApprovalRepository
            .update(approvalProcess)
            .returns(Future.successful(Right(validId)))

          MockApprovalRepository
            .getById("oct90001")
            .returns(Future.successful(Right(approvalProcess)))

          MockApprovalProcessReviewRepository
            .save(processReview)
            .returns(Future.successful(Left(Errors(DatabaseError))))

          whenReady(service.save(validOnePageJson.as[JsObject], "2i-review", "SubmittedFor2iReview")) {
            case result @ Left(_) => result shouldBe expected
            case _ => fail
          }
        }
      }
    }

    "the process is saved" when {
      "the subsequent get of the process fails with a NotFoundError" should {
        "return an internal error" in new Test {

          val expected: RequestOutcome[String] = Left(Errors(NotFoundError))

          MockApprovalRepository
            .update(approvalProcess)
            .returns(Future.successful(Right(validId)))

          MockApprovalRepository
            .getById("oct90001")
            .returns(Future.successful(Left(Errors(NotFoundError))))

          whenReady(service.save(validOnePageJson.as[JsObject], "2i-review", "SubmittedFor2iReview")) {
            case result @ Left(_) => result shouldBe expected
            case _ => fail
          }
        }
      }

      "the subsequent get of the process fails with a DatabaseError" should {
        "return an internal error" in new Test {

          val expected: RequestOutcome[String] = Left(Errors(InternalServiceError))

          MockApprovalRepository
            .update(approvalProcess)
            .returns(Future.successful(Right(validId)))

          MockApprovalRepository
            .getById("oct90001")
            .returns(Future.successful(Left(Errors(DatabaseError))))

          whenReady(service.save(validOnePageJson.as[JsObject], "2iReview", "SubmittedFor2iReview")) {
            case result @ Left(_) => result shouldBe expected
            case _ => fail
          }
        }
      }
    }

    "the JSON is invalid" should {
      "not call the repository" in new Test {
        MockApprovalRepository
          .update(approvalProcess)
          .never()

        service.save(invalidProcess, "2i-review", "SubmittedFor2iReview")
      }

      "return a bad request error" in new Test {
        val expected: RequestOutcome[String] = Left(Errors(BadRequestError))

        whenReady(service.save(invalidProcess, "2i-review", "SubmittedFor2iReview")) {
          case result @ Left(_) => result shouldBe expected
          case _ => fail
        }
      }
    }

    "a database error occurs" should {
      "return an internal error" in new Test {
        val repositoryResponse: RequestOutcome[String] = Left(Errors(DatabaseError))
        val expected: RequestOutcome[String] = Left(Errors(InternalServiceError))

        MockApprovalRepository
          .update(approvalProcess)
          .returns(Future.successful(repositoryResponse))

        whenReady(service.save(validOnePageJson.as[JsObject], "2i-review", "SubmittedFor2iReview")) {
          case result @ Left(_) => result shouldBe expected
          case _ => fail
        }
      }
    }
  }

  "Calling the approvalSummaryList method" when {
    "there are entries to return" should {
      "return a List of approval processes" in new Test {

        val expected: RequestOutcome[List[ApprovalProcessSummary]] = Right(List(approvalProcessSummary))

        MockApprovalRepository
          .approvalSummaryList()
          .returns(Future.successful(expected))

        whenReady(service.approvalSummaryList()) {
          case Right(jsonList) =>
            val list: List[ApprovalProcessSummary] = jsonList.as[List[ApprovalProcessSummary]]
            list.size shouldBe 1
            val entry = list.head
            entry.id shouldBe approvalProcessSummary.id
            entry.title shouldBe approvalProcessSummary.title
            entry.status shouldBe approvalProcessSummary.status
          case _ => fail
        }
      }
    }

    "there are no processes in the database" should {
      "return an empty list" in new Test {

        val expected: RequestOutcome[JsArray] = Right(JsArray())
        val returnedList: RequestOutcome[List[ApprovalProcessSummary]] = Right(List())

        MockApprovalRepository
          .approvalSummaryList()
          .returns(Future.successful(returnedList))

        whenReady(service.approvalSummaryList()) { result =>
          result shouldBe expected
        }
      }
    }

    "the repository reports a database error" should {
      "return an internal server error" in new Test {

        val repositoryError: RequestOutcome[List[ApprovalProcessSummary]] = Left(Errors(DatabaseError))
        val expected: RequestOutcome[List[ApprovalProcessSummary]] = Left(Errors(InternalServiceError))

        MockApprovalRepository
          .approvalSummaryList()
          .returns(Future.successful(repositoryError))

        whenReady(service.approvalSummaryList()) { result =>
          result shouldBe expected
        }
      }
    }
  }

}
