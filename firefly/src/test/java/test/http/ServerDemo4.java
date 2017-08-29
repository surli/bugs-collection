package test.http;

import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.server.http2.SimpleHTTPServer;
import com.firefly.server.http2.SimpleResponse;
import com.firefly.utils.io.BufferUtils;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ServerDemo4 {

    public static void main(String[] args) {
        SimpleHTTPServer server = new SimpleHTTPServer();
        server.headerComplete(req -> {
            List<ByteBuffer> list = new ArrayList<>();
            req.content(list::add)
               .contentComplete(request -> {
                   String msg = BufferUtils.toString(list, "UTF-8");
                   StringBuilder s = new StringBuilder();
                   s.append("content complete").append("\r\n")
                    .append(req.toString()).append("\r\n")
                    .append(req.getFields().toString()).append("\r\n")
                    .append(msg).append("\r\n");
                   System.out.println(s.toString());
                   request.put("msg", msg);
               })
               .messageComplete(request -> {
                   SimpleResponse response = req.getResponse();
                   String path = req.getRequest().getURI().getPath();

                   response.getResponse().getFields().put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN.asString());

                   switch (path) {
                       case "/":
                           System.out.println(request.getRequest().toString());
                           System.out.println(request.getRequest().getFields());
                           String msg = BufferUtils.toString(list, "UTF-8");
                           System.out.println(msg);
                           try (PrintWriter writer = response.getPrintWriter()) {
                               writer.print("server demo 4");
                           }
                           break;
                       case "/postData":
                           try (PrintWriter writer = response.getPrintWriter()) {
                               writer.print("receive message -> " + request.get("msg"));
                           }
                           break;

                       default:
                           response.getResponse().setStatus(HttpStatus.NOT_FOUND_404);
                           try (PrintWriter writer = response.getPrintWriter()) {
                               writer.print("resource not found");
                           }
                           break;
                   }

               });

        }).listen("localhost", 3333);
    }

}
