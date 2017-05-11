package com.firefly.server.http2;

import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.net.SSLContextFactory;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.Router;
import com.firefly.server.http2.router.RouterManager;
import com.firefly.server.http2.router.handler.body.HTTPBodyConfiguration;

/**
 * @author Pengtao Qiu
 */
public class HTTP2ServerBuilder {

    private SimpleHTTPServer server;
    private RouterManager routerManager;
    private Router currentRouter;

    public HTTP2ServerBuilder httpsServer() {
        SimpleHTTPServerConfiguration configuration = new SimpleHTTPServerConfiguration();
        configuration.setSecureConnectionEnabled(true);
        return httpServer(configuration, new HTTPBodyConfiguration());
    }

    public HTTP2ServerBuilder httpsServer(SSLContextFactory sslContextFactory) {
        SimpleHTTPServerConfiguration configuration = new SimpleHTTPServerConfiguration();
        configuration.setSecureConnectionEnabled(true);
        configuration.setSslContextFactory(sslContextFactory);
        return httpServer(configuration, new HTTPBodyConfiguration());
    }

    public HTTP2ServerBuilder httpServer() {
        return httpServer(new SimpleHTTPServerConfiguration(), new HTTPBodyConfiguration());
    }

    public HTTP2ServerBuilder httpServer(SimpleHTTPServerConfiguration serverConfiguration,
                                         HTTPBodyConfiguration httpBodyConfiguration) {
        server = new SimpleHTTPServer(serverConfiguration);
        routerManager = RouterManager.create(httpBodyConfiguration);
        return this;
    }

    /**
     * register an new router
     *
     * @return HTTP2ServerBuilder
     */
    public HTTP2ServerBuilder router() {
        currentRouter = routerManager.register();
        return this;
    }

    private void check() {
        if (server == null) {
            throw new IllegalStateException("the http server has not been created, please call httpServer() first");
        }
    }

    public HTTP2ServerBuilder listen(String host, int port) {
        check();
        server.headerComplete(routerManager::accept).listen(host, port);
        return this;
    }

    public HTTP2ServerBuilder listen() {
        check();
        server.headerComplete(routerManager::accept).listen();
        return this;
    }

    public HTTP2ServerBuilder stop() {
        check();
        server.stop();
        return this;
    }

    // delegated Router methods

    public HTTP2ServerBuilder path(String url) {
        currentRouter.path(url);
        return this;
    }

    public HTTP2ServerBuilder pathRegex(String regex) {
        currentRouter.pathRegex(regex);
        return this;
    }

    public HTTP2ServerBuilder method(String method) {
        currentRouter.method(method);
        return this;
    }

    public HTTP2ServerBuilder method(HttpMethod httpMethod) {
        currentRouter.method(httpMethod);
        return this;
    }

    public HTTP2ServerBuilder get(String url) {
        currentRouter.get(url);
        return this;
    }

    public HTTP2ServerBuilder post(String url) {
        currentRouter.post(url);
        return this;
    }

    public HTTP2ServerBuilder put(String url) {
        currentRouter.put(url);
        return this;
    }

    public HTTP2ServerBuilder delete(String url) {
        currentRouter.delete(url);
        return this;
    }

    public HTTP2ServerBuilder consumes(String contentType) {
        currentRouter.consumes(contentType);
        return this;
    }

    public HTTP2ServerBuilder produces(String accept) {
        currentRouter.produces(accept);
        return this;
    }

    public HTTP2ServerBuilder handler(Handler context) {
        currentRouter.handler(context);
        return this;
    }
}
