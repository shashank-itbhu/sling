/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
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
package org.apache.sling.hc.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.sling.hc.api.HealthCheck;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Select from available {@link HealthCheck} services */
public class HealthCheckFilter {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final BundleContext bundleContext;
    public static final String OMIT_PREFIX = "-";

    public HealthCheckFilter(final BundleContext bc) {
        bundleContext = bc;
    }

    /**
     * Get all health check services with one of the supplied tags.
     * @return A list of services - might be the empty list if none matches
     */
    @SuppressWarnings("unchecked")
    public List<HealthCheck> getTaggedHealthChecks(final String... tags) {
        final ServiceReference [] refs = this.getTaggedHealthCheckServiceReferences(tags);
        final List<HealthCheck> result = new ArrayList<HealthCheck>();

        if ( refs != null ) {
            final List<ServiceReference> sortedRefs = Arrays.asList(refs);
            Collections.sort(sortedRefs);

            for(final ServiceReference ref : sortedRefs) {
                final HealthCheck hc = (HealthCheck)bundleContext.getService(ref);
                log.debug("Selected HealthCheck service {}", hc);
                if ( hc != null ) {
                    result.add(hc);
                }
            }
        }

        return result;
    }

    /**
     * Get all service references for health check services with one of the supplied tags.
     * @return An array of service references - might be an empty error if none matches
     */
    public ServiceReference[] getTaggedHealthCheckServiceReferences(final String... tags) {
        // Build service filter
        final StringBuilder filterBuilder = new StringBuilder();
        filterBuilder.append("(&(objectClass=").append(HealthCheck.class.getName()).append(")");
        final int prefixLen = OMIT_PREFIX.length();
        for(String tag : tags) {
            tag = tag.trim();
            if(tag.length() == 0) {
                continue;
            }
            if(tag.startsWith(OMIT_PREFIX)) {
                filterBuilder.append("(!(").append(HealthCheck.TAGS).append("=").append(tag.substring(prefixLen)).append("))");
            } else {
                filterBuilder.append("(").append(HealthCheck.TAGS).append("=").append(tag).append(")");
            }
        }
        filterBuilder.append(")");

        try {
            final String filterString = filterBuilder.length() == 0 ? null : filterBuilder.toString();
            bundleContext.createFilter(filterString); // check syntax early
            final ServiceReference[] refs = bundleContext.getServiceReferences(HealthCheck.class.getName(), filterString);
            if (refs == null) {
                log.debug("Found no HealthCheck services with filter [{}]", filterString);
                return new ServiceReference[0];
            } else {
                log.debug("Found {} HealthCheck services with filter [{}]", refs.length, filterString);
            }
            return refs;
        } catch (final InvalidSyntaxException ise) {
            // this should not happen, but we fail gracefully
            log.error("Invalid OSGi filter syntax in '" + filterBuilder + "'", ise);
            return new ServiceReference[0];
        }
    }
}
