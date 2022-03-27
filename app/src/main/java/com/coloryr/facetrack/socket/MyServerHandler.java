package com.coloryr.facetrack.socket;

import com.coloryr.facetrack.MainActivity;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class MyServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        SocketUtils.startPack(byteBuf);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        SocketUtils.channel = null;
        cause.printStackTrace();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        SocketUtils.checkOk = false;
        SocketUtils.channel = ctx.channel();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SocketUtils.channel = null;
        if (SocketUtils.channel.equals(ctx.channel())) {
            MainActivity.run(() -> {
                MainActivity.makeNotification("电脑连接", "与电脑连接断开", "连接断开");
            });
        }
    }
}
