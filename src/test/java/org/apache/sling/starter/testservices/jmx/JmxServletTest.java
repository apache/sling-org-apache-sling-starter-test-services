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
package org.apache.sling.starter.testservices.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Proxy;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JmxServletTest {

    @Test
    public void plainNameRoundTrips() throws Exception {
        assertNameRoundTrips("plain");
    }

    @Test
    public void quotedNameRoundTrips() throws Exception {
        assertNameRoundTrips(ObjectName.quote("quotes\" and backslash\\"));
    }

    @Test
    public void unquotedBackslashRoundTrips() throws Exception {
        assertNameRoundTrips("backslash\\name");
    }

    private void assertNameRoundTrips(String nameProperty) throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName(
                "org.apache.sling:type=JsonSerializationTest,id=" + UUID.randomUUID() + ",name=" + nameProperty);
        server.registerMBean(new TestBean(), name);
        try {
            assertTrue("JMX must accept the test MBean name", server.isRegistered(name));
            assertEquals("registered", server.getAttribute(name, "Value"));

            String content = render();
            try (JsonReader reader = Json.createReader(new StringReader(content))) {
                assertTrue(
                        "The servlet must preserve the registered name in its JSON response: " + content,
                        reader.readArray().getValuesAs(JsonString.class).stream()
                                .map(JsonString::getString)
                                .anyMatch(name.toString()::equals));
            }
        } finally {
            server.unregisterMBean(name);
        }
    }

    public interface TestBeanMBean {
        String getValue();
    }

    public static class TestBean implements TestBeanMBean {
        @Override
        public String getValue() {
            return "registered";
        }
    }

    private String render() throws Exception {
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);
        SlingHttpServletResponse response = (SlingHttpServletResponse) Proxy.newProxyInstance(
                SlingHttpServletResponse.class.getClassLoader(),
                new Class<?>[] {SlingHttpServletResponse.class},
                (proxy, method, arguments) -> {
                    switch (method.getName()) {
                        case "getWriter":
                            return writer;
                        case "setContentType":
                            assertEquals("application/json", arguments[0]);
                            return null;
                        case "setStatus":
                            assertEquals(HttpServletResponse.SC_OK, arguments[0]);
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
        new JmxServlet().doGet(null, response);
        writer.flush();
        return output.toString();
    }
}
