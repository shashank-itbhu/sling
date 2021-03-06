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
package org.apache.sling.hc.healthchecks.impl;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyUnbounded;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.healthchecks.util.FormattingResultLog;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that checks a scriptable expression */
@Component(
        name="org.apache.sling.hc.ScriptableHealthCheck",
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE,
        metatype=true)
@Properties({
    @Property(name=HealthCheck.NAME),
    @Property(name=HealthCheck.TAGS, unbounded=PropertyUnbounded.ARRAY),
    @Property(name=HealthCheck.MBEAN_NAME)
})
@Service(value=HealthCheck.class)
public class ScriptableHealthCheck implements HealthCheck {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private String expression;
    private String languageExtension;
    private BundleContext bundleContext;

    private static final String DEFAULT_LANGUAGE_EXTENSION = "ecma";

    @Property
    public static final String PROP_EXPRESSION = "expression";

    @Property(value=DEFAULT_LANGUAGE_EXTENSION)
    public static final String PROP_LANGUAGE_EXTENSION = "language.extension";

    @Reference
    private ScriptEngineManager scriptEngineManager;

    @Activate
    public void activate(ComponentContext ctx) {
        bundleContext = ctx.getBundleContext();
        expression = PropertiesUtil.toString(ctx.getProperties().get(PROP_EXPRESSION), "");
        languageExtension = PropertiesUtil.toString(ctx.getProperties().get(PROP_LANGUAGE_EXTENSION), DEFAULT_LANGUAGE_EXTENSION);

        log.debug("Activated scriptable health check name={}, languageExtension={}, expression={}",
                new Object[] {ctx.getProperties().get(HealthCheck.NAME),
                languageExtension, expression});
    }

    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();
        resultLog.debug("Checking expression [{}], language extension=[{}]",  expression, languageExtension);
        try {
            final ScriptEngine engine = scriptEngineManager.getEngineByExtension(languageExtension);
            if (engine == null) {
                resultLog.healthCheckError("No ScriptEngine available for extension {}", languageExtension);
            } else {
                // TODO pluggable Bindings? Reuse the Sling bindings providers?
                final Bindings b = engine.createBindings();
                b.put("jmx", new JmxScriptBinding(resultLog));
                b.put("osgi", new OsgiScriptBinding(bundleContext, resultLog));
                final Object value = engine.eval(expression, b);
                if(value!=null && "true".equals(value.toString().toLowerCase())) {
                    resultLog.debug("Expression [{}] evaluates to true as expected", expression);
                } else {
                    resultLog.warn("Expression [{}] does not evaluate to true as expected, value=[{}]", expression, value);
                }
            }
        } catch (final Exception e) {
            resultLog.healthCheckError(
                    "Exception while evaluating expression [{}] with language extension [{}]: {}",
                    expression, languageExtension, e);
        }
        return new Result(resultLog);
    }
}