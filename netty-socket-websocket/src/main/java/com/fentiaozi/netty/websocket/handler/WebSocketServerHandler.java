package com.fentiaozi.netty.websocket.handler;


import com.alibaba.fastjson.JSONObject;
import com.fentiaozi.netty.websocket.model.vo.WebSocketResult;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * @author visi
 */
@Component("webSocketServerHandler")
@Sharable
@Slf4j
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private WebSocketServerHandshaker handshaker;
/*
    // 心跳丢失次数计数
    private int counter = 0;

    // 连续 counterTime次 没有收到心跳则服务端主动与客户端断开连接
    private int counterTime = 3;
*/


    /**
     * 描述：读取完连接的消息后，对消息进行处理。
     * 这里主要是处理WebSocket请求
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        handlerWebSocketFrame(ctx, msg);
        //重置心跳丢失次数
        //counter = 0;
    }

    /**
     * 描述：处理WebSocketFrame
     *
     * @param ctx
     * @param frame
     * @throws Exception
     */
    private void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // 关闭请求
        if (frame instanceof CloseWebSocketFrame) {
            handshaker =
                    ChannelHandlerPool.webSocketHandshakerMap.get(ctx.channel().id().asLongText());
            if (handshaker == null) {
                sendErrorMessage(ctx, "不存在的客户端连接！");
            } else {
                handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            }
            return;
        }
        // ping请求
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        // 只支持文本格式，不支持二进制消息
        if (!(frame instanceof TextWebSocketFrame)) {
            sendErrorMessage(ctx, "仅支持文本(Text)格式，不支持二进制消息");
        }

        // 客服端发送过来的消息
        String request = ((TextWebSocketFrame) frame).text();
        log.debug("[WebSocket]服务端收到新信息：" + request);
        JSONObject param = null;
        try {
            param = JSONObject.parseObject(request);
        } catch (Exception e) {
            sendErrorMessage(ctx, "JSON字符串转换出错！");
            e.printStackTrace();
        }
        if (param == null) {
            sendErrorMessage(ctx, "参数为空！");
            return;
        }

        String msg = (String) param.get("msg");

    }

    /**
     * 描述：客户端断开连接
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("[WebSocket]与客户端断开连接，通道关闭 {}", ctx.channel());
        ChannelHandlerPool.removeChannel(ctx.channel());
    }

    /**
     * 异常处理：关闭channel
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("[WebSocket]连接异常: {}", cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }

/*
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.READER_IDLE)) {
                // 空闲3s之后触发 (心跳包丢失)
                if (counter >= counterTime) {
                    channelInactive(ctx);
                    // 连续丢失3个心跳包 (断开连接)
                    ctx.channel().close().sync();
                    log.info("已与" + ctx.channel().remoteAddress() + "断开连接");
                    counter = 0;
                } else {
                    counter++;
                    log.info(ctx.channel().remoteAddress() + "丢失了第 " + counter + " 个心跳包");
                }
            }
        }
    }
*/


    private void sendErrorMessage(ChannelHandlerContext ctx, String errorMsg) {
        String responseJson = new WebSocketResult<>().error(errorMsg).toString();

        ctx.channel().writeAndFlush(new TextWebSocketFrame(responseJson));
    }

}
