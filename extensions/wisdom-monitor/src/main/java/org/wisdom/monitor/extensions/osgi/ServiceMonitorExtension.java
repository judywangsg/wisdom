/*
 * #%L
 * Wisdom-Framework
 * %%
 * Copyright (C) 2013 - 2014 Wisdom Framework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wisdom.monitor.extensions.osgi;

import com.google.common.collect.ImmutableMap;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.Controller;
import org.wisdom.api.annotations.Path;
import org.wisdom.api.annotations.Route;
import org.wisdom.api.annotations.View;
import org.wisdom.api.http.HttpMethod;
import org.wisdom.api.http.Result;
import org.wisdom.api.security.Authenticated;
import org.wisdom.api.templates.Template;
import org.wisdom.monitor.extensions.security.MonitorAuthenticator;
import org.wisdom.monitor.service.MonitorExtension;

import java.util.HashSet;
import java.util.List;


@Controller
@Path("/monitor/osgi/service")
@Authenticated(MonitorAuthenticator.class)
public class ServiceMonitorExtension extends DefaultController implements MonitorExtension {

    @View("monitor/services")
    Template services;

    @Context
    BundleContext context;

    /**
     * Just a simple service event counter.
     */
    private ServiceEventCounter counter = new ServiceEventCounter();

    @Validate
    public void start() {
        counter.start();
    }

    @Invalidate
    public void stop() {
        counter.stop();
    }

    @Route(method = HttpMethod.GET, uri = "")
    public Result svc() {
        return ok(render(services));
    }

    private int getProviderBundleCount(List<ServiceModel> svc) {
        HashSet<String> set = new HashSet<String>();
        for (ServiceModel service : svc) {
            set.add(service.getProvidingBundle());
        }
        return set.size();
    }

    private int getProviderCount(List<ServiceModel> svc) {
        HashSet<String> set = new HashSet<String>();
        for (ServiceModel service : svc) {
            String name = service.getProperties().get("instance.name");
            if (name != null) {
                set.add(name);
            }
        }
        return set.size();
    }


    @Route(method = HttpMethod.GET, uri = "/services")
    public Result services() {
        final List<ServiceModel> svc = ServiceModel.services(context);
        return ok(ImmutableMap.of(
                "services", svc,
                "events", counter.get(),
                "providers", Integer.toString(getProviderCount(svc)),
                "bundles", Integer.toString(getProviderBundleCount(svc)))).json();
    }

    @Override
    public String label() {
        return "Services";
    }

    @Override
    public String url() {
        return "/monitor/osgi/service";
    }

    @Override
    public String category() {
        return "osgi";
    }

    /**
     * A simple class counting events.
     * No synchronization involved, as we can be off without be in troubles.
     */
    private class ServiceEventCounter implements ServiceListener {

        int counter = 0;

        public void start() {
            context.addServiceListener(this);
        }

        public void reset() {
            counter = 0;
        }

        public void stop() {
            context.removeServiceListener(this);
        }

        public int get() {
            return counter;
        }

        @Override
        public void serviceChanged(ServiceEvent serviceEvent) {
            counter++;
        }
    }
}
