package com.coloryr.facetrack.socket

import com.coloryr.facetrack.MainActivity
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

class MyServerHandler : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val byteBuf = msg as ByteBuf
        SocketUtils.startPack(byteBuf)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.close()
        SocketUtils.channel = null
        cause.printStackTrace()
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        SocketUtils.checkOk = false
        SocketUtils.channel = ctx.channel()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        SocketUtils.channel = null
        if (SocketUtils.channel == ctx.channel()) {
            MainActivity.run(Runnable { MainActivity.makeNotification("电脑连接", "与电脑连接断开", "连接断开") })
        }
    }
}