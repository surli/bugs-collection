package com.firefly.server.http2.router;

import com.firefly.codec.http2.model.*;
import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.SimpleResponse;
import com.firefly.server.http2.router.spi.HTTPBodyHandlerSPI;
import com.firefly.server.http2.router.spi.HTTPSessionHandlerSPI;
import com.firefly.server.http2.router.spi.TemplateHandlerSPI;
import com.firefly.utils.function.Action1;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;

import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A new RoutingContext(ctx) instance is created for each HTTP request.
 * <p>
 * You can visit the RoutingContext instance in the whole router chain. It provides HTTP request/response API and allows you to maintain arbitrary data that lives for the lifetime of the context. Contexts are discarded once they have been routed to the handler for the request.
 * <p>
 * The context also provides access to the Session, cookies and body for the request, given the correct handlers in the application.
 *
 * @author Pengtao Qiu
 */
public interface RoutingContext extends Closeable {

    Object getAttribute(String key);

    Object setAttribute(String key, Object value);

    Object removeAttribute(String key);

    ConcurrentHashMap<String, Object> getAttributes();

    SimpleResponse getResponse();

    SimpleResponse getAsyncResponse();

    SimpleRequest getRequest();

    String getRouterParameter(String name);

    RoutingContext content(Action1<ByteBuffer> content);

    RoutingContext contentComplete(Action1<SimpleRequest> contentComplete);

    RoutingContext messageComplete(Action1<SimpleRequest> messageComplete);

    boolean isAsynchronousRead();

    boolean next();

    boolean hasNext();


    // request wrap
    default String getMethod() {
        return getRequest().getMethod();
    }

    default HttpURI getURI() {
        return getRequest().getURI();
    }

    default HttpVersion getHttpVersion() {
        return getRequest().getHttpVersion();
    }

    default HttpFields getFields() {
        return getRequest().getFields();
    }

    default long getContentLength() {
        return getRequest().getContentLength();
    }

    default List<Cookie> getCookies() {
        return getRequest().getCookies();
    }


    // response wrap
    default RoutingContext setStatus(int status) {
        getResponse().setStatus(status);
        return this;
    }

    default RoutingContext setReason(String reason) {
        getResponse().setReason(reason);
        return this;
    }

    default RoutingContext setHttpVersion(HttpVersion httpVersion) {
        getResponse().setHttpVersion(httpVersion);
        return this;
    }

    default RoutingContext put(HttpHeader header, String value) {
        getResponse().put(header, value);
        return this;
    }

    default RoutingContext put(String header, String value) {
        getResponse().put(header, value);
        return this;
    }

    default RoutingContext add(HttpHeader header, String value) {
        getResponse().add(header, value);
        return this;
    }

    default RoutingContext add(String name, String value) {
        getResponse().add(name, value);
        return this;
    }

    default RoutingContext addCookie(Cookie cookie) {
        getResponse().addCookie(cookie);
        return this;
    }

    default RoutingContext write(String value) {
        getResponse().write(value);
        return this;
    }

    default RoutingContext end(String value) {
        return write(value).end();
    }

    default RoutingContext end() {
        getResponse().end();
        return this;
    }

    default RoutingContext write(byte[] b, int off, int len) {
        getResponse().write(b, off, len);
        return this;
    }

    default RoutingContext write(byte[] b) {
        return write(b, 0, b.length);
    }

    default RoutingContext end(byte[] b) {
        return write(b).end();
    }


    // HTTP body API
    String getParameter(String name);

    List<String> getParameterValues(String name);

    Map<String, List<String>> getParameterMap();

    Collection<Part> getParts();

    Part getPart(String name);

    InputStream getInputStream();

    BufferedReader getBufferedReader();

    String getStringBody(String charset);

    String getStringBody();

    <T> T getJsonBody(Class<T> clazz);

    JsonObject getJsonObjectBody();

    JsonArray getJsonArrayBody();


    // HTTP session API
    HttpSession getSession();

    HttpSession getSession(boolean create);

    boolean isRequestedSessionIdFromURL();

    boolean isRequestedSessionIdFromCookie();

    boolean isRequestedSessionIdValid();

    String getRequestedSessionId();

    // Template API
    void renderTemplate(String resourceName, Object scope);

    void renderTemplate(String resourceName, Object[] scopes);

    void renderTemplate(String resourceName, List<Object> scopes);

}
