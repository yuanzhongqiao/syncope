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
package org.apache.syncope.core.spring.security;

import javax.annotation.Resource;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.HmacJwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsVerificationSignature;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation for internal JWT validation.
 */
public class SyncopeJWTSSOProvider implements JWTSSOProvider, InitializingBean {

    @Resource(name = "jwtIssuer")
    private String jwtIssuer;

    @Resource(name = "jwsKey")
    private String jwsKey;

    @Autowired
    private JwsSignatureProvider signatureProvider;

    @Autowired
    private UserDAO userDAO;

    private JwsSignatureVerifier delegate;

    @Override
    public void afterPropertiesSet() throws Exception {
        delegate = new HmacJwsSignatureVerifier(jwsKey.getBytes(), signatureProvider.getAlgorithm());
    }

    @Override
    public String getIssuer() {
        return jwtIssuer;
    }

    @Override
    public SignatureAlgorithm getAlgorithm() {
        return delegate.getAlgorithm();
    }

    @Override
    public boolean verify(final JwsHeaders headers, final String unsignedText, final byte[] signature) {
        return delegate.verify(headers, unsignedText, signature);
    }

    @Override
    public JwsVerificationSignature createJwsVerificationSignature(final JwsHeaders headers) {
        return delegate.createJwsVerificationSignature(headers);
    }

    @Transactional(readOnly = true)
    @Override
    public User resolve(final String jwtSubject) {
        return userDAO.findByUsername(jwtSubject);
    }

}
