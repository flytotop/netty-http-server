
package com.farsunset.httpserver.netty.iohandler;

import com.farsunset.httpserver.netty.http.NettyHttpResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.ReferenceCountUtil;
import org.springframework.stereotype.Component;


@ChannelHandler.Sharable
@Component
/**
 * 在这里可以做拦截器，验证一些请求的合法性
 */
public class InterceptorHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext context, Object msg)   {
        if (isPassed((FullHttpRequest) msg)){
            /**
             * 提交给下一个ChannelHandler去处理
             * 并且不需要调用ReferenceCountUtil.release(msg);来释放引用计数
             */
            context.fireChannelRead(msg);
            return;
        }
        /**
         * 非异常引起的错误需要手动关闭channel通道
         */
        ReferenceCountUtil.release(msg);
        context.writeAndFlush(NettyHttpResponse.make(HttpResponseStatus.UNAUTHORIZED)).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 修改实现来验证合法性
     * @param request
     * @return
     */
    private boolean isPassed(FullHttpRequest request){
        return true;
    }
}
