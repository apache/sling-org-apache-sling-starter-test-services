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
package org.apache.sling.starter.testservices.filters;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.wrappers.ResourceResolverWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A javax.servlet.Filter that wraps the Sling request with a custom
 * {@link SlingHttpServletRequestWrapper} which overrides
 * {@link SlingHttpServletRequest#getResourceResolver()} to return a
 * custom {@link ResourceResolverWrapper}. This tests that a custom
 * resource resolver provided by a javax filter's request wrapper is
 * correctly propagated through request dispatch and the Jakarta
 * migration layer into script bindings (e.g. via
 * {@code <sling:defineObjects />}).
 *
 * <p>The filter is activated when the request parameter
 * {@code wrapResourceResolver=true} is present.</p>
 */
@Component(
        service = Filter.class,
        property = {
            "service.description:String=ResourceResolver Wrapping Test Filter",
            "service.vendor:String=The Apache Software Foundation",
            "sling.filter.scope:String=request",
            "service.ranking:Integer=" + Integer.MIN_VALUE
        })
public class ResourceResolverWrappingFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ResourceResolverWrappingFilter.class);

    /**
     * Request parameter that triggers the resource resolver wrapping.
     */
    public static final String PARAM_WRAP = "wrapResourceResolver";

    /**
     * The class name of our custom wrapper, used for verification in tests.
     */
    public static final String WRAPPER_CLASS_NAME = CustomResourceResolverWrapper.class.getName();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            String wrapParam = slingRequest.getParameter(PARAM_WRAP);
            if ("true".equals(wrapParam)) {
                LOG.debug("Wrapping resource resolver for request {}", slingRequest.getPathInfo());
                ResourceResolver customResolver = new CustomResourceResolverWrapper(slingRequest.getResourceResolver());
                // Wrap the resource so that resource.getResourceResolver() returns the custom resolver
                Resource wrappedResource = new CustomResourceWrapper(slingRequest.getResource(), customResolver);
                SlingHttpServletRequest wrappedRequest =
                        new CustomSlingRequestWrapper(slingRequest, customResolver, wrappedResource);
                // Forward with the wrapped resource and request
                slingRequest.getRequestDispatcher(wrappedResource).forward(wrappedRequest, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to do
    }

    /**
     * Custom request wrapper that overrides getResourceResolver() and getResource()
     * to return custom wrappers, testing that these overrides survive request dispatch.
     */
    static class CustomSlingRequestWrapper extends SlingHttpServletRequestWrapper {

        private final ResourceResolver customResolver;
        private final Resource customResource;

        CustomSlingRequestWrapper(
                SlingHttpServletRequest wrappedRequest, ResourceResolver customResolver, Resource customResource) {
            super(wrappedRequest);
            this.customResolver = customResolver;
            this.customResource = customResource;
        }

        @Override
        public ResourceResolver getResourceResolver() {
            return customResolver;
        }

        @Override
        public Resource getResource() {
            return customResource;
        }
    }

    /**
     * A simple resource resolver wrapper that delegates everything.
     * Its distinguishing feature is that it has a unique class name
     * that can be verified in tests.
     */
    public static class CustomResourceResolverWrapper extends ResourceResolverWrapper {

        public CustomResourceResolverWrapper(ResourceResolver resolver) {
            super(resolver);
        }
    }

    /**
     * A resource wrapper that overrides getResourceResolver() to return
     * the custom resolver, ensuring the resource and resolver stay paired.
     */
    static class CustomResourceWrapper extends ResourceWrapper {

        private final ResourceResolver customResolver;

        CustomResourceWrapper(Resource resource, ResourceResolver customResolver) {
            super(resource);
            this.customResolver = customResolver;
        }

        @Override
        public ResourceResolver getResourceResolver() {
            return customResolver;
        }
    }
}
