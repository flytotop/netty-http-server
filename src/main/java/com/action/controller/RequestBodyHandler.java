package com.action.controller;

import com.farsunset.httpserver.dto.Response;
import com.farsunset.httpserver.netty.annotation.NettyHttpHandler;
import com.farsunset.httpserver.netty.handler.IFunctionHandler;
import com.farsunset.httpserver.netty.http.NettyHttpRequest;

@NettyHttpHandler(path = "/request/body",method = "POST")
public class RequestBodyHandler implements IFunctionHandler<String> {
    @Override
    public Response<String> execute(NettyHttpRequest request) {
        /**
         * 可以在此拿到json转成业务需要的对象
         */
        String json = request.contentText();
        return Response.ok(json);
    }
}