package com.coloryr.facetrack.socket

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldPrepender

class ConnectService : Service() {
    private var channelFuture: ChannelFuture? = null

    //创建两个线程组 boosGroup、workerGroup
    private val bossGroup: EventLoopGroup = NioEventLoopGroup()
    private val workerGroup: EventLoopGroup = NioEventLoopGroup()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "androidService--->onCreate()")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (isStart) return START_STICKY
        try {
            //创建服务端的启动对象，设置参数
            val bootstrap = ServerBootstrap()
            //设置两个线程组boosGroup和workerGroup
            bootstrap.group(bossGroup, workerGroup) //设置服务端通道实现类型
                .channel(NioServerSocketChannel::class.java) //快速链接
                .option(ChannelOption.TCP_FASTOPEN_CONNECT, true) //设置线程队列得到连接个数
                .option(ChannelOption.SO_BACKLOG, 128) //设置保持活动连接状态
                .childOption(ChannelOption.SO_KEEPALIVE, true) //使用匿名内部类的形式初始化通道对象
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(socketChannel: SocketChannel) {
                        //给pipeline管道设置处理器
                        socketChannel.pipeline().addLast(LengthFieldPrepender(4)).addLast(MyServerHandler())
                    }
                }) //给workerGroup的EventLoop对应的管道设置处理器
            //绑定端口号，启动服务端
            channelFuture = bootstrap.bind(SERVER_PORT).sync()
            isStart = true
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        //对关闭通道进行监听
        try {
            channelFuture!!.channel().close()
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val TAG = "Live2dServer"
        const val SERVER_PORT = 23456
        var isStart = false
    }
}