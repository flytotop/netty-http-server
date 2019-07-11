
package com.farsunset.httpserver.netty.iohandler;


import com.farsunset.httpserver.dto.Response;
import com.farsunset.httpserver.netty.annotation.NettyHttpHandler;
import com.farsunset.httpserver.netty.exception.IllegalMethodNotAllowedException;
import com.farsunset.httpserver.netty.exception.IllegalPathDuplicatedException;
import com.farsunset.httpserver.netty.exception.IllegalPathNotFoundException;
import com.farsunset.httpserver.netty.handler.IFunctionHandler;
import com.farsunset.httpserver.netty.http.NettyHttpRequest;
import com.farsunset.httpserver.netty.http.NettyHttpResponse;
import com.farsunset.httpserver.netty.path.Path;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Stream;

@ChannelHandler.Sharable
@Component
    /**
     * 通过实现ApplicationContextAware将setApplicationContext（）函数引入spring初始化内容中去
     */
    public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerHandler.class);

    private HashMap<Path, IFunctionHandler> functionHandlerMap = new HashMap<>();

    /**
     * 线程工厂
     */
    private ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setName("NettyHttpHandler-" + thread.getName());
        return thread;
    });

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        FullHttpRequest copyRequest = request.copy();
        /**
         * 通过内部类的形式传入一个runnable的实现类并重写了run方法 线程池在执行的时候会调用这个方法
         */
        executor.execute(() -> onReceivedRequest(ctx,new NettyHttpRequest(copyRequest)));
    }

    /**
     *
      * @param context
     * @param request
     */
    private void onReceivedRequest(ChannelHandlerContext context, NettyHttpRequest request){
        /**
         * 处理request请求
         */
        FullHttpResponse response = handleHttpRequest(request);
        /**
         * 通过channel将结果输出 并通过添加监听器的方式关闭channel通道
         */
        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        /**
         * 释放bytebuf缓存
         */
        ReferenceCountUtil.release(request);
    }

    /**
     *
     * @param request NettyHttpRequest extends Fullhttpreuqest
     * @return 请求处理结果
     */
    private FullHttpResponse handleHttpRequest(NettyHttpRequest request) {

        IFunctionHandler functionHandler = null;
        /**
         * 请求处理并根据不同的结果或者捕获的异常进行状态码转换并返回
         */
        try {
            functionHandler = matchFunctionHandler(request);
            Response response =  functionHandler.execute(request);
            return NettyHttpResponse.ok(response.toJSONString());
        }
        catch (IllegalMethodNotAllowedException error){
            return NettyHttpResponse.make(HttpResponseStatus.METHOD_NOT_ALLOWED);
        }
        catch (IllegalPathNotFoundException error){
            return NettyHttpResponse.make(HttpResponseStatus.NOT_FOUND);
        }
        catch (Exception error){
            LOGGER.error(functionHandler.getClass().getSimpleName() + " Error",error);
            return NettyHttpResponse.makeError(error);
        }
    }

    /**
     * spring初始化加载此函数
     * @param applicationContext
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        /**
         * 获得所有NettyHttpHandler的注解类
         */
        Map<String, Object> handlers =  applicationContext.getBeansWithAnnotation(NettyHttpHandler.class);
        for (Map.Entry<String, Object> entry : handlers.entrySet()) {
            Object handler = entry.getValue();
            Path path = Path.make(handler.getClass().getAnnotation(NettyHttpHandler.class));
            /**
             * 查询是否当前处理器的注解是否已经存在（类似于SSM中controler的注解不能重复）
             * 1.存在则抛出异常
             * 2. 不存在则存入Map集合中 在SSM中是通过对类方法注解的扫描 存入内部类mapperRegistry中
             */
            if (functionHandlerMap.containsKey(path)){
                LOGGER.error("IFunctionHandler has duplicated :" + path.toString(),new IllegalPathDuplicatedException());
                System.exit(0);
            }
            functionHandlerMap.put(path, (IFunctionHandler) handler);
        }
    }

    private IFunctionHandler matchFunctionHandler(NettyHttpRequest request) throws IllegalPathNotFoundException, IllegalMethodNotAllowedException {

        AtomicBoolean matched = new AtomicBoolean(false);

        Stream<Path> stream = functionHandlerMap.keySet().stream()
                .filter(((Predicate<Path>) path -> {
                    /**
                     *过滤 Path URI 不匹配的
                     */
                    if (request.matched(path.getUri(), path.isEqual())) {
                        matched.set(true);
                        return matched.get();
                    }
                    return false;

                }).and(path -> {
                    /**
                     * 过滤 Method 匹配的
                     */
                    return request.isAllowed(path.getMethod());
                }));

        Optional<Path> optional = stream.findFirst();

        stream.close();

        if (!optional.isPresent() && !matched.get()){
            throw  new IllegalPathNotFoundException();
        }

        if (!optional.isPresent() && matched.get()){
            throw  new IllegalMethodNotAllowedException();
        }

        return functionHandlerMap.get(optional.get());
    }

}
