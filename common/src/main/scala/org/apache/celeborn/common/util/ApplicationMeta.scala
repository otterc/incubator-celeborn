/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.common.util;

/**
 * Application meta
 */
class ApplicationMeta(private val appId: String, private val secret: String) {
  def getAppId: String = appId

  def getSecret: String = secret

  override def equals(other: Any): Boolean = other match {
    case that: ApplicationMeta =>
      appId == that.getAppId &&
        secret == that.getSecret
    case _ => false
  }

  override def hashCode(): Int = {
    var result = appId.hashCode()
    result = 31 * result + secret.hashCode()
    result
  }}
