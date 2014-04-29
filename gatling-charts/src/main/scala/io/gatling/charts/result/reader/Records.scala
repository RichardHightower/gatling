/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.charts.result.reader

import scala.collection.mutable

import io.gatling.core.result.Group
import io.gatling.core.result.message.{ KO, MessageEvent, Status }
import io.gatling.core.result.writer.FileDataWriter.GroupMessageSerializer

object RecordParser {

  val groupCache = mutable.Map.empty[String, Group]

  def parseGroup(string: String) = groupCache.getOrElseUpdate(string, GroupMessageSerializer.deserializeGroups(string))

  def parseRequestRecord(strings: Array[String], bucketFunction: Int => Int, runStart: Long): RequestRecord = {

    val group = {
      val groupString = strings(3)
      if (groupString.isEmpty) None else Some(parseGroup(groupString))
    }
    val request = strings(4)
    val executionStart = (strings(5).toLong - runStart).toInt
    val requestEnd = (strings(6).toLong - runStart).toInt
    val responseStart = (strings(7).toLong - runStart).toInt
    val executionEnd = (strings(8).toLong - runStart).toInt
    val status = Status.valueOf(strings(9))
    val errorMessage = if (status == KO) Some(strings(10)) else None
    val executionStartBucket = bucketFunction(executionStart)
    val executionEndBucket = bucketFunction(executionEnd)
    val responseTime = executionEnd - executionStart
    val latency = responseStart - requestEnd
    RequestRecord(group, request, executionStart.toInt, executionEnd, status, executionStartBucket, executionEndBucket, responseTime, latency, errorMessage)
  }

  def parseUserRecord(strings: Array[String], bucketFunction: Int => Int, runStart: Long): UserRecord = {

    val scenario = strings(0)
    val userId = strings(1)
    val event = MessageEvent(strings(3))
    val startDate = reduceAccuracy((strings(4).toLong - runStart).toInt)
    val endDate = reduceAccuracy((strings(5).toLong - runStart).toInt)
    UserRecord(scenario, userId, startDate, event, bucketFunction(startDate), bucketFunction(endDate))
  }

  def parseGroupRecord(strings: Array[String], bucketFunction: Int => Int, runStart: Long): GroupRecord = {

    val group = parseGroup(strings(3))
    val entryDate = (strings(4).toLong - runStart).toInt
    val exitDate = (strings(5).toLong - runStart).toInt
    val cumulatedResponseTime = strings(6).toInt
    val oks = strings(7).toInt
    val kos = strings(8).toInt
    val status = Status.valueOf(strings(9))
    val duration = exitDate - entryDate
    val executionDateBucket = bucketFunction(entryDate)
    GroupRecord(group, entryDate, duration, cumulatedResponseTime, oks, kos, status, executionDateBucket)
  }
}

case class RequestRecord(group: Option[Group], name: String, requestStart: Int, responseEnd: Int, status: Status, requestStartBucket: Int, responseEndBucket: Int, responseTime: Int, latency: Int, errorMessage: Option[String])
case class UserRecord(scenario: String, userId: String, startDate: Int, event: MessageEvent, startDateBucket: Int, endDateBucket: Int)
case class GroupRecord(group: Group, startDate: Int, duration: Int, cumulatedResponseTime: Int, oks: Int, kos: Int, status: Status, startDateBucket: Int)
