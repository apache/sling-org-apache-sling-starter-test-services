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
package org.apache.sling.launchpad.testservices.servlets;

import javax.servlet.Servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Predicate;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Servlet.class)
@SlingServletResourceTypes(
        resourceTypes = {"testing/hideservlets"},
        methods = {"GET", "POST"},
        extensions = "txt")
public class HidingTestServlet extends SlingAllMethodsServlet implements Predicate<String> {

    private static final long serialVersionUID = 1L;
    private final List<String> hiddenPaths = new ArrayList<>();
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    public void activate(BundleContext ctx) {
        // Register this a Predicate to test SLING-12739
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("name", "sling.servlet.resolver.resource.hiding");
        ctx.registerService(Predicate.class.getName(), this, properties);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("text/plain");
        response.getWriter().write(getClass().getName());
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        final boolean hide = request.getParameter("hide").equals("true");
        String status = null;
        if (hide) {
            // Hide both this servlet and the corresponding test script
            hiddenPaths.add("/apps/testing/hideservlets/txt.GET.servlet");
            hiddenPaths.add("/apps/testing/hidescripts/hidescripts.txt.esp");
            status = String.format("hiding the following servlets/scripts: %s", hiddenPaths);
        } else {
            hiddenPaths.clear();
            status = String.format("not hiding any servlets/scripts");
        }
        final String msg = String.format("POST with hide=%s received, %s", hide, status);
        log.info(msg);
        response.setContentType("text/plain");
        response.getWriter().write(msg);
    }

    @Override
    public boolean test(String path) {
        final boolean result = hiddenPaths.contains(path);
        if (result) {
            log.info("Hiding servlet/script at path {}", path);
        }
        return result;
    }
}
