/*
 * Copyright (c) 2009 Lockheed Martin Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.eurekastreams.server.service.opensocial.config;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.common.crypto.Crypto;
import org.apache.shindig.gadgets.http.HttpFetcher;
import org.apache.shindig.gadgets.oauth.OAuthFetcherConfig;
import org.apache.shindig.gadgets.oauth.OAuthRequest;
import org.apache.shindig.gadgets.oauth.OAuthStore;
import org.eurekastreams.server.service.opensocial.gadgets.oauth.OAuthStoreImpl;
import org.springframework.transaction.PlatformTransactionManager;
import org.eurekastreams.server.persistence.OAuthConsumerMapper;
import org.eurekastreams.server.persistence.OAuthEntryMapper;
import org.eurekastreams.server.persistence.OAuthTokenMapper;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.spring.SpringIntegration;

/**
 * Loads pre-reqs for OAuth.
 */
public class OAuthModule extends AbstractModule
{
    /**
     * Configure the dependant objects.
     */
    @Override
    protected void configure()
    {
        bind(BlobCrypter.class).annotatedWith(Names.named(OAuthFetcherConfig.OAUTH_STATE_CRYPTER)).toProvider(
                OAuthCrypterProvider.class);

        bind(OAuthStore.class).to(OAuthStoreImpl.class);
        bind(OAuthRequest.class).toProvider(OAuthRequestProvider.class);

        bind(OAuthConsumerMapper.class).toProvider(
                SpringIntegration.fromSpring(OAuthConsumerMapper.class, "jpaOAuthConsumerMapper"));
        bind(OAuthTokenMapper.class).toProvider(
                SpringIntegration.fromSpring(OAuthTokenMapper.class, "jpaOAuthTokenMapper"));
        bind(OAuthEntryMapper.class).toProvider(
                SpringIntegration.fromSpring(OAuthEntryMapper.class, "jpaOAuthEntryMapper"));
        bind(PlatformTransactionManager.class).toProvider(
                SpringIntegration.fromSpring(PlatformTransactionManager.class, "transactionManager"));
    }

    /**
     * Inner class for setting up OAuthCryperProvider.
     */
    @Singleton
    public static class OAuthCrypterProvider implements Provider<BlobCrypter>
    {
        /**
         * Crypter.
         */
        private final BlobCrypter crypter;

        /**
         * Constructor.
         * 
         * @param stateCrypterPath
         *              the path to the crypter key file.
         * @throws IOException if the path was not found.
         */
        @Inject
        public OAuthCrypterProvider(@Named("shindig.signing.state-key") final String stateCrypterPath) 
            throws IOException
        {
            if (StringUtils.isBlank(stateCrypterPath))
            {
                crypter = new BasicBlobCrypter(Crypto.getRandomBytes(BasicBlobCrypter.MASTER_KEY_MIN_LEN));
            }
            else
            {
                crypter = new BasicBlobCrypter(new File(stateCrypterPath));
            }
        }

        /**
         * @return the crypter.
         */
        public BlobCrypter get()
        {
            return crypter;
        }
    }

    /**
     * Inner class for setting up OAuthRequestProvider.
     */
    public static class OAuthRequestProvider implements Provider<OAuthRequest>
    {
        /**
         * Fetcher.
         */
        private final HttpFetcher fetcher;
        
        /**
         * Fetcher config.
         */
        private final OAuthFetcherConfig config;

        /**
         * Constructor.
         * 
         * @param inFetcher
         *              the fetcher.
         * @param inConfig
         *              the config.
         */
        @Inject
        public OAuthRequestProvider(final HttpFetcher inFetcher, final OAuthFetcherConfig inConfig)
        {
            fetcher = inFetcher;
            config = inConfig;
        }

        /**
         * @return the oauth request.
         */
        public OAuthRequest get()
        {
            return new OAuthRequest(config, fetcher);
        }
    }
}
