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

package models.ocelot.errors

import models.errors.ErrorDetails
import models.ocelot.stanzas.Stanza

trait FlowError {
  //val msgKey: String
  val details: ErrorDetails
}

case class UnknownStanzaType(unknown: Stanza) extends FlowError {
  val msgKey: String = "error.unsupportedStanza"
  val details: ErrorDetails = ErrorDetails(s"Unsupported stanza $unknown found at id = ??", "")
}
case class StanzaNotFound(id: String) extends FlowError {
  val msgKey: String = "error.stanzaMissing"
  val details: ErrorDetails = ErrorDetails(s"Missing stanza at id = $id", id)
}
case class PageStanzaMissing(id: String) extends FlowError {
  val msgKey: String = "error.pageStanzaMissing"
  val details: ErrorDetails = ErrorDetails(s"PageSanza expected but missing at id = $id", id)
}
case class PageUrlEmptyOrInvalid(id: String) extends FlowError {
  val msgKey: String = "error.invalidUrl"
  val details: ErrorDetails = ErrorDetails(s"PageStanza URL empty or invalid at id = $id", id)
}
case class PhraseNotFound(index: Int) extends FlowError {
  val msgKey: String = "error.phraseNotFound"
  val details: ErrorDetails = ErrorDetails(s"Referenced phrase at index $index on stanza id = ?? is missing", "")
}
case class LinkNotFound(index: Int) extends FlowError {
  val msgKey: String = "error.linkNotFound"
  val details: ErrorDetails = ErrorDetails(s"Referenced link at index $index on stanza id = ?? is missing" , "")
}
case class DuplicatePageUrl(id: String, url: String) extends FlowError {
  val msgKey: String = "error.duplicateUrl"
  val details: ErrorDetails = ErrorDetails(s"Duplicate page url $url found on stanza id = $id", id)
}
case class MissingWelshText(index: String, english: String) extends FlowError {
  val msgKey: String = "error.welshMissing"
  val details: ErrorDetails = ErrorDetails(s"Welsh text at index $index on stanza id = ?? is empty", "")
}
