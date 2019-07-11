
package com.farsunset.httpserver.netty.handler;


import com.farsunset.httpserver.dto.Response;
import com.farsunset.httpserver.netty.annotation.NettyHttpHandler;
import com.farsunset.httpserver.netty.http.NettyHttpRequest;

@NettyHttpHandler(path = "/hello/world")
public class HelloWorldHandler implements IFunctionHandler<String> {
    @Override
    public Response<String> execute(NettyHttpRequest request) {
         return Response.ok("Hello World");
    }
}
