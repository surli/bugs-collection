package com.firefly.client.http2;

import com.firefly.codec.http2.stream.Session.Listener;
import com.firefly.utils.concurrent.Promise;

public class HTTP2ClientContext {
    public Promise<HTTPClientConnection> promise;
    public Listener listener;
}
