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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Clob;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public class AuditConfRepoExtImpl implements AuditConfRepoExt {

    protected static final Logger LOG = LoggerFactory.getLogger(AuditConfRepoExt.class);

    protected static class MessageCriteriaBuilder {

        protected final StringBuilder query = new StringBuilder();

        protected String andIfNeeded() {
            return query.length() == 0 ? " " : " AND ";
        }

        protected int setParameter(final List<Object> parameters, final Object parameter) {
            parameters.add(parameter);
            return parameters.size();
        }

        protected MessageCriteriaBuilder entityKey(final String entityKey) {
            if (entityKey != null) {
                query.append(andIfNeeded()).append(AuditConfDAO.AUDIT_ENTRY_MESSAGE_COLUMN).
                        append(" LIKE '%key%").append(entityKey).append("%'");
            }
            return this;
        }

        public MessageCriteriaBuilder type(final AuditElements.EventCategoryType type) {
            if (type != null) {
                query.append(andIfNeeded()).append(AuditConfDAO.AUDIT_ENTRY_MESSAGE_COLUMN).
                        append(" LIKE '%\"type\":\"").append(type.name()).append("\"%'");
            }
            return this;
        }

        public MessageCriteriaBuilder category(final String category) {
            if (StringUtils.isNotBlank(category)) {
                query.append(andIfNeeded()).append(AuditConfDAO.AUDIT_ENTRY_MESSAGE_COLUMN).
                        append(" LIKE '%\"category\":\"").append(category).append("\"%'");
            }
            return this;
        }

        public MessageCriteriaBuilder subcategory(final String subcategory) {
            if (StringUtils.isNotBlank(subcategory)) {
                query.append(andIfNeeded()).append(AuditConfDAO.AUDIT_ENTRY_MESSAGE_COLUMN).
                        append(" LIKE '%\"subcategory\":\"").append(subcategory).append("\"%'");
            }
            return this;
        }

        public MessageCriteriaBuilder events(final List<String> events) {
            if (!events.isEmpty()) {
                query.append(andIfNeeded()).append("( ").
                        append(events.stream().
                                map(event -> AuditConfDAO.AUDIT_ENTRY_MESSAGE_COLUMN
                                + " LIKE '%\"event\":\"" + event + "\"%'").
                                collect(Collectors.joining(" OR "))).
                        append(" )");
            }
            return this;
        }

        public MessageCriteriaBuilder result(final AuditElements.Result result) {
            if (result != null) {
                query.append(andIfNeeded()).append(AuditConfDAO.AUDIT_ENTRY_MESSAGE_COLUMN).
                        append(" LIKE '%\"result\":\"").append(result.name()).append("\"%' ");
            }
            return this;
        }

        public MessageCriteriaBuilder before(final OffsetDateTime before, final List<Object> parameters) {
            if (before != null) {
                query.append(andIfNeeded()).append(AuditConfDAO.AUDIT_ENTRY_EVENT_DATE_COLUMN).
                        append(" <= ?").append(setParameter(parameters, before));
            }
            return this;
        }

        public MessageCriteriaBuilder after(final OffsetDateTime after, final List<Object> parameters) {
            if (after != null) {
                query.append(andIfNeeded()).append(AuditConfDAO.AUDIT_ENTRY_EVENT_DATE_COLUMN).
                        append(" >= ?").append(setParameter(parameters, after));
            }
            return this;
        }

        public String build() {
            return query.toString();
        }
    }

    protected final EntityManager entityManager;

    public AuditConfRepoExtImpl(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected MessageCriteriaBuilder messageCriteriaBuilder(final String entityKey) {
        return new MessageCriteriaBuilder().entityKey(entityKey);
    }

    protected void fillWithParameters(final Query query, final List<Object> parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i) instanceof Boolean aBoolean) {
                query.setParameter(i + 1, aBoolean ? 1 : 0);
            } else {
                query.setParameter(i + 1, parameters.get(i));
            }
        }
    }

    @Override
    public long countEntries(
            final String entityKey,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final OffsetDateTime before,
            final OffsetDateTime after) {

        List<Object> parameters = new ArrayList<>();
        String queryString = "SELECT COUNT(0)"
                + " FROM " + AuditConfDAO.AUDIT_ENTRY_TABLE
                + " WHERE " + messageCriteriaBuilder(entityKey).
                        type(type).
                        category(category).
                        subcategory(subcategory).
                        result(result).
                        events(events).
                        before(before, parameters).
                        after(after, parameters).
                        build();
        Query query = entityManager.createNativeQuery(queryString);
        fillWithParameters(query, parameters);

        return ((Number) query.getSingleResult()).intValue();
    }

    protected String select() {
        return AuditConfDAO.AUDIT_ENTRY_MESSAGE_COLUMN;
    }

    @Transactional(readOnly = true)
    @Override
    public List<AuditEntry> searchEntries(
            final String entityKey,
            final AuditElements.EventCategoryType type,
            final String category,
            final String subcategory,
            final List<String> events,
            final AuditElements.Result result,
            final OffsetDateTime before,
            final OffsetDateTime after,
            final Pageable pageable) {

        List<Object> parameters = new ArrayList<>();
        String queryString = "SELECT " + select()
                + " FROM " + AuditConfDAO.AUDIT_ENTRY_TABLE
                + " WHERE " + messageCriteriaBuilder(entityKey).
                        type(type).
                        category(category).
                        subcategory(subcategory).
                        result(result).
                        events(events).
                        before(before, parameters).
                        after(after, parameters).
                        build();
        if (!pageable.getSort().isEmpty()) {
            queryString += " ORDER BY " + pageable.getSort().stream().
                    map(clause -> clause.getProperty() + ' ' + clause.getDirection().name()).
                    collect(Collectors.joining(","));
        }

        Query query = entityManager.createNativeQuery(queryString);
        fillWithParameters(query, parameters);

        if (pageable.isPaged()) {
            query.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
            query.setMaxResults(pageable.getPageSize());
        }

        @SuppressWarnings("unchecked")
        List<Object> entries = query.getResultList();
        return entries.stream().map(row -> {
            String value;
            if (row instanceof Clob clob) {
                try {
                    value = clob.getSubString(1, (int) clob.length());
                } catch (SQLException e) {
                    LOG.error("Unexpected error reading Audit Entry for entity key {}", entityKey, e);
                    return null;
                }
            } else {
                value = row.toString();
            }
            return POJOHelper.deserialize(value, AuditEntry.class);
        }).filter(Objects::nonNull).toList();
    }
}
