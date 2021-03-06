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
package org.apache.sling.jcr.jackrabbit.client;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.jcr.Repository;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.AbstractSlingRepository;
import org.osgi.service.log.LogService;

/**
 * The <code>SlingClientRepository</code> TODO
 *
 */
@Component(name="org.apache.sling.jcr.jackrabbit.client.SlingClientRepository", metatype=true,
    description="%repository.description", label="%repository.name", configurationFactory=true,
    policy=ConfigurationPolicy.REQUIRE)
@Properties({
    @Property(name="service.description", value="Factory for non-embedded JCR Repository Instances"),
    @Property(name="java.naming.factory.initial", value="org.apache.jackrabbit.core.jndi.provider.DummyInitialContextFactory"),
    @Property(name="java.naming.provider.url", value="http://sling.apache.org")
})
public class SlingClientRepository extends AbstractSlingRepository
        implements Repository, SlingRepository {

    @Property(value="jackrabbit")
    public static final String REPOSITORY_NAME = "name";

    //---------- Repository Publication ---------------------------------------

    @Override
    protected Repository acquireRepository() {
        Repository repository = super.acquireRepository();
        if (repository != null) {
            return repository;
        }

        @SuppressWarnings("unchecked")
        Dictionary<String, Object> environment = this.getComponentContext().getProperties();
        Repository repo = null;

        String repoName = (String) environment.get(REPOSITORY_NAME);
        if (repoName == null) {
            log(LogService.LOG_ERROR,
                "acquireRepository: Missing property 'name'");
            return null;
        }

        final Hashtable<String, Object> jndiContext = this.fromDictionary(environment);
        repo = getRepositoryAccessor().getRepository(repoName, jndiContext);
        if (repo == null) {
            log(LogService.LOG_ERROR,
                "acquireRepository: Cannot acquire repository '" + repoName
                    + "'");
        }

        return repo;
    }


    //---------- internal -----------------------------------------------------

    private Hashtable<String, Object> fromDictionary(Dictionary<String, Object> source) {
        Hashtable<String, Object> table = new Hashtable<String, Object>();
        for (Enumeration<String> ke=source.keys(); ke.hasMoreElements(); ) {
            String key = ke.nextElement();
            table.put(key, source.get(key));
        }
        return table;
    }
}
