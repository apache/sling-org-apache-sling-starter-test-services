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
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonWriter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPathsStrict;
import org.osgi.service.component.annotations.Component;

/**
 * The <code>JmxServlet</code> lists JMX MBeans belonging to a Sling in JSON format
 *
 */
@Component(service = Servlet.class)
@SlingServletPathsStrict(paths = "/bin/jmx", extensions = "json")
public class JmxServlet extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName queryName = new ObjectName("org.apache.sling:*");

            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);

            // list MBean names under the domain "org.apache.sling"
            JsonArrayBuilder names = Json.createArrayBuilder();
            for (ObjectName name : server.queryNames(queryName, null)) {
                names.add(name.toString());
            }
            try (JsonWriter writer = Json.createWriter(response.getWriter())) {
                writer.writeArray(names.build());
            }
        } catch (MalformedObjectNameException e) {
            throw new ServletException(e);
        }
    }
}
