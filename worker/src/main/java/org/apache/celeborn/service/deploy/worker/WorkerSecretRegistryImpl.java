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

package org.apache.celeborn.service.deploy.worker;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.client.MasterClient;
import org.apache.celeborn.common.network.sasl.SecretRegistry;
import org.apache.celeborn.common.protocol.PbApplicationMeta;
import org.apache.celeborn.common.protocol.PbApplicationMetaRequest;

/** A secret registry that fetches the secret from the master if it is not found locally. */
public class WorkerSecretRegistryImpl implements SecretRegistry {
  private static final Logger LOG = LoggerFactory.getLogger(WorkerSecretRegistryImpl.class);

  // MasterClient is created in Worker after the secret registry is created and this order currently
  // cannot be changed.
  // So, we need to set the masterClient after the secret registry is created.
  private MasterClient masterClient;
  private final Cache<String, String> secretCache;

  public WorkerSecretRegistryImpl(long maxCacheSize) {
    secretCache = CacheBuilder.newBuilder().maximumSize(maxCacheSize).build();
  }

  /** Gets an appropriate SASL secret key for the given appId. */
  @Override
  public String getSecretKey(String appId) {
    String secret = secretCache.getIfPresent(appId);
    if (secret == null) {
      LOG.debug("Missing the secret for {}; fetching it from the master", appId);
      PbApplicationMetaRequest pbApplicationMetaRequest =
          PbApplicationMetaRequest.newBuilder().setAppId(appId).build();
      try {
        PbApplicationMeta pbApplicationMeta =
            masterClient.askSync(pbApplicationMetaRequest, PbApplicationMeta.class);
        LOG.debug(
            "Successfully fetched the application meta info for " + appId + " from the master");
        register(pbApplicationMeta.getAppId(), pbApplicationMeta.getSecret());
        secret = pbApplicationMeta.getSecret();
      } catch (Throwable e) {
        // We catch Throwable here because masterClient.askSync declares it in its definition.
        // If the secret is null, the authentication will fail so just logging the exception here.
        LOG.error("Failed to fetch the application meta info for {} from the master", appId, e);
      }
    }
    return secret;
  }

  @Override
  public boolean isRegistered(String appId) {
    return secretCache.getIfPresent(appId) != null;
  }

  @Override
  public void register(String appId, String secret) {
    String existingSecret = secretCache.getIfPresent(appId);
    if (existingSecret != null && !existingSecret.equals(secret)) {
      throw new IllegalArgumentException(
          "AppId " + appId + " is already registered. Cannot re-register.");
    }
    secretCache.put(appId, secret);
  }

  @Override
  public void unregister(String appId) {
    secretCache.invalidate(appId);
  }

  public void setMasterClient(MasterClient masterClient) {
    this.masterClient = Preconditions.checkNotNull(masterClient);
  }
}
