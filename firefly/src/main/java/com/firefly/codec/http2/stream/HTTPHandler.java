package com.firefly.codec.http2.stream;

import com.firefly.codec.http2.model.MetaData;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.utils.function.Action4;
import com.firefly.utils.function.Action6;
import com.firefly.utils.function.Func4;
import com.firefly.utils.function.Func5;

import java.nio.ByteBuffer;

public interface HTTPHandler {

    boolean content(ByteBuffer item, MetaData.Request request, MetaData.Response response,
                    HTTPOutputStream output,
                    HTTPConnection connection);

    boolean contentComplete(MetaData.Request request, MetaData.Response response,
                            HTTPOutputStream output,
                            HTTPConnection connection);

    boolean headerComplete(MetaData.Request request, MetaData.Response response,
                           HTTPOutputStream output,
                           HTTPConnection connection);

    boolean messageComplete(MetaData.Request request, MetaData.Response response,
                            HTTPOutputStream output,
                            HTTPConnection connection);

    void badMessage(int status, String reason, MetaData.Request request, MetaData.Response response,
                    HTTPOutputStream output, HTTPConnection connection);

    void earlyEOF(MetaData.Request request, MetaData.Response response,
                  HTTPOutputStream output,
                  HTTPConnection connection);

    class Adapter implements HTTPHandler {

        protected Func4<Request, Response, HTTPOutputStream, HTTPConnection, Boolean> headerComplete;
        protected Func5<ByteBuffer, Request, Response, HTTPOutputStream, HTTPConnection, Boolean> content;
        protected Func4<Request, Response, HTTPOutputStream, HTTPConnection, Boolean> contentComplete;
        protected Func4<Request, Response, HTTPOutputStream, HTTPConnection, Boolean> messageComplete;
        protected Action6<Integer, String, Request, Response, HTTPOutputStream, HTTPConnection> badMessage;
        protected Action4<Request, Response, HTTPOutputStream, HTTPConnection> earlyEOF;

        @Override
        public boolean headerComplete(Request request, Response response,
                                      HTTPOutputStream output,
                                      HTTPConnection connection) {
            if (headerComplete != null) {
                return headerComplete.call(request, response, output, connection);
            } else {
                return false;
            }
        }

        @Override
        public boolean content(ByteBuffer item, Request request, Response response,
                               HTTPOutputStream output,
                               HTTPConnection connection) {
            if (content != null) {
                return content.call(item, request, response, output, connection);
            } else {
                return false;
            }
        }

        @Override
        public boolean contentComplete(MetaData.Request request, MetaData.Response response,
                                       HTTPOutputStream output,
                                       HTTPConnection connection) {
            if (contentComplete != null) {
                return contentComplete.call(request, response, output, connection);
            } else {
                return false;
            }
        }


        @Override
        public boolean messageComplete(Request request, Response response,
                                       HTTPOutputStream output,
                                       HTTPConnection connection) {
            if (messageComplete != null) {
                return messageComplete.call(request, response, output, connection);
            } else {
                return true;
            }
        }

        @Override
        public void badMessage(int status, String reason, Request request, Response response,
                               HTTPOutputStream output,
                               HTTPConnection connection) {
            if (badMessage != null) {
                badMessage.call(status, reason, request, response, output, connection);
            }
        }

        @Override
        public void earlyEOF(Request request, Response response, HTTPOutputStream output, HTTPConnection connection) {
            if (earlyEOF != null) {
                earlyEOF.call(request, response, output, connection);
            }
        }

    }

}
