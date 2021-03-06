/**
 * Copyright (C) 2013-2017 Expedia Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotels.styx.proxy;

import com.hotels.styx.api.HttpClient;
import com.hotels.styx.api.HttpHandler2;
import com.hotels.styx.api.HttpInterceptor;
import com.hotels.styx.api.HttpRequest;
import com.hotels.styx.api.HttpResponse;
import com.hotels.styx.client.applications.BackendService;
import com.hotels.styx.infrastructure.Registry;
import com.hotels.styx.server.HttpRouter;
import org.slf4j.Logger;
import rx.Observable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.concat;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.naturalOrder;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A {@link HttpHandler2} implementation.
 */
public class BackendServicesRouter implements HttpRouter, Registry.ChangeListener<BackendService> {
    private static final Logger LOG = getLogger(BackendServicesRouter.class);

    private final BackendServiceClientFactory clientFactory;
    private final ConcurrentMap<String, ProxyToClientPipeline> routes;

    public BackendServicesRouter(BackendServiceClientFactory clientFactory) {
        this.clientFactory = checkNotNull(clientFactory);
        this.routes = new ConcurrentSkipListMap<>(
                comparingInt(String::length).reversed()
                        .thenComparing(naturalOrder()));
    }

    ConcurrentMap<String, ProxyToClientPipeline> routes() {
        return routes;
    }

    @Override
    public Optional<HttpHandler2> route(HttpRequest request) {
        String path = request.path();

        return routes.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .findFirst()
                .map(Map.Entry::getValue);
    }

    @Override
    public void onChange(Registry.Changes<BackendService> changes) {
        concat(changes.added(), changes.updated()).forEach(backendService -> {
            ProxyToClientPipeline pipeline = new ProxyToClientPipeline(newClientHandler(backendService));

            ProxyToClientPipeline updated = routes.put(backendService.path(), pipeline);
            LOG.info("added path={} current routes={}", backendService.path(), routes.keySet());
            if (updated != null) {
                updated.close();
            }

            pipeline.registerStatusGauges();
        });

        changes.removed().forEach(backendService ->
                routes.remove(backendService.path())
                        .close());
    }

    private HttpClient newClientHandler(BackendService backendService) {
        return clientFactory.createClient(backendService);
    }

    @Override
    public void onError(Throwable ex) {
        LOG.warn("Error from registry", ex);
    }

    private static class ProxyToClientPipeline implements HttpHandler2 {
        private final HttpClient client;

        ProxyToClientPipeline(HttpClient httpClient) {
            this.client = checkNotNull(httpClient);
        }

        @Override
        public Observable<HttpResponse> handle(HttpRequest request, HttpInterceptor.Context context) {
            return client.sendRequest(request)
                    .doOnError(throwable -> handleError(request, throwable));
        }

        public void close() {
            client.close();
        }

        public void registerStatusGauges() {
            client.registerStatusGauges();
        }

        private static void handleError(HttpRequest request, Throwable throwable) {
            LOG.error("Error proxying request={} exceptionClass={} exceptionMessage=\"{}\"", new Object[]{request, throwable.getClass().getName(), throwable.getMessage()});
        }

    }
}
