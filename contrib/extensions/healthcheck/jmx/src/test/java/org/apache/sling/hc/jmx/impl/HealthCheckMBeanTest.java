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
package org.apache.sling.hc.jmx.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.apache.sling.hc.healthchecks.util.SimpleConstraintChecker;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class HealthCheckMBeanTest {
    private final MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
    private boolean resultOk;
    public static final String OBJECT_NAME = "org.apache.sling.testing:type=HealthCheckMBeanTest";

    private HealthCheck testHealthCheck = new HealthCheck() {

        @Override
        public Result execute() {
            if(resultOk) {
                return new Result(Result.Status.OK, "Nothing to report, result ok");
            } else {
                return new Result(Result.Status.WARN, "Result is not ok!");
            }
        }
    };

    private void assertJmxValue(String mbeanName, String attributeName, String constraint, boolean expected) throws Exception {
        final MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = new ObjectName(mbeanName);
        if(jmxServer.queryNames(objectName, null).size() == 0) {
            fail("MBean not found: " + objectName);
        }
        final Object value = jmxServer.getAttribute(objectName, attributeName);
        final ResultLog resultLog = new ResultLog();
        new SimpleConstraintChecker().check(value, constraint, resultLog);
        assertEquals("Expecting result " + expected + "(" + resultLog + ")", expected, resultLog.getAggregateStatus().equals(Result.Status.OK));

    }

    @Test
    public void testBean() throws Exception {
        final ServiceReference ref = new ServiceReference() {

            @Override
            public boolean isAssignableTo(Bundle bundle, String className) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public Bundle[] getUsingBundles() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String[] getPropertyKeys() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Object getProperty(String key) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Bundle getBundle() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public int compareTo(Object reference) {
                // TODO Auto-generated method stub
                return 0;
            }
        };
        final HealthCheckMBean mbean = new HealthCheckMBean(ref, testHealthCheck);
        final ObjectName name = new ObjectName(OBJECT_NAME);
        jmxServer.registerMBean(mbean, name);
        try {
            resultOk = true;
            assertJmxValue(OBJECT_NAME, "ok", "true", true);

            resultOk = false;
            assertJmxValue(OBJECT_NAME, "ok", "true", false);

            assertJmxValue(OBJECT_NAME, "log", "contains message=Result is not ok!", true);
        } finally {
            jmxServer.unregisterMBean(name);
        }
    }

}