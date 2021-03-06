/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.authinfo.components

import java.net.{ URI, URL }
import java.nio.file.Paths
import java.util.UUID

import nl.knaw.dans.easy.authinfo.{ BagInfo, HttpStatusException, escapePath }

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Elem, XML }

trait BagStoreComponent {
  this: HttpContext =>

  val bagStore: BagStore

  trait BagStore {
    val baseUri: URI

    def loadDDM(bagId: UUID): Try[Elem] = {
      toURL(bagId, "metadata/dataset.xml").flatMap(loadXml)
    }

    def loadFilesXML(bagId: UUID): Try[Elem] = {
      toURL(bagId, "metadata/files.xml").flatMap(loadXml)
    }

    def loadBagInfo(bagId: UUID): Try[BagInfo] = {
      toURL(bagId, "bag-info.txt").flatMap(loadBagInfo)
    }

    private def loadXml(url: URL): Try[Elem] = {
      for {
        response <- Try { Http(url.toString).asString }
        _ <- if (response.isSuccess) Success(())
             else Failure(HttpStatusException(url.toString, response))
      } yield XML.loadString(response.body)
    }

    private def loadBagInfo(url: URL): Try[BagInfo] = {
      for {
        response <- Try { Http(url.toString).asString }
        _ <- if (response.isSuccess) Success(())
             else Failure(HttpStatusException(url.toString, response))
      } yield response
        .body
        .split("\n")
        .map { line =>
          val Array(k, v) = line.split(":", 2)
          (k.trim, v.trim)
        }.toMap
    }

    private def toURL(bagId: UUID, path: String): Try[URL] = Try {
      val f = escapePath(Paths.get(path))
      baseUri.resolve(s"bags/$bagId/$f").toURL
    }
  }
}
