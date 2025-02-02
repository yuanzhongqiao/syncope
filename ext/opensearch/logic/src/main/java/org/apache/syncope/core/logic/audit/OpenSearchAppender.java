/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.logic.audit;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.ext.opensearch.client.OpenSearchIndexManager;

public class OpenSearchAppender extends AbstractAppender {

    public static class Builder extends AbstractAppender.Builder<Builder>
            implements org.apache.logging.log4j.core.util.Builder<OpenSearchAppender> {

        private OpenSearchIndexManager openSearchIndexManager;

        private String domain;

        public OpenSearchAppender.Builder setDomain(final String domain) {
            this.domain = domain;
            return this;
        }

        public OpenSearchAppender.Builder setIndexManager(
                final OpenSearchIndexManager openSearchIndexManager) {

            this.openSearchIndexManager = openSearchIndexManager;
            return this;
        }

        @Override
        public OpenSearchAppender build() {
            if (domain == null || openSearchIndexManager == null) {
                LOGGER.error("Cannot create OpenSearchAppender without Domain or IndexManager.");
                return null;
            }
            return new OpenSearchAppender(
                    getName(), getFilter(), getLayout(), isIgnoreExceptions(), domain, openSearchIndexManager);
        }
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private final String domain;

    protected final OpenSearchIndexManager openSearchIndexManager;

    protected OpenSearchAppender(
            final String name,
            final Filter filter,
            final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions,
            final String domain,
            final OpenSearchIndexManager openSearchIndexManager) {

        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
        this.domain = domain;
        this.openSearchIndexManager = openSearchIndexManager;
    }

    @Override
    public void append(final LogEvent event) {
        try {
            openSearchIndexManager.audit(
                    domain,
                    event.getTimeMillis(),
                    POJOHelper.deserialize(event.getMessage().getFormattedMessage(), JsonNode.class));
        } catch (Exception e) {
            LOGGER.error("While requesting to index event for appender [{}]", getName(), e);
        }
    }
}
