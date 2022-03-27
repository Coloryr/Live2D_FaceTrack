package com.coloryr.facetrack.socket;


import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;

public class ConnectService extends Service {

    private static final int ID = 10;
    public static final String TAG = "Live2dServer";
    private static ChannelFuture channelFuture;
    //创建两个线程组 boosGroup、workerGroup
    private static final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private static final EventLoopGroup workerGroup = new NioEventLoopGroup();
    public static boolean isStart;
    private static final int SERVER_PORT = 23456;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "androidService--->onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isStart)
            return START_NOT_STICKY;

        try {
            //创建服务端的启动对象，设置参数
            ServerBootstrap bootstrap = new ServerBootstrap();
            //设置两个线程组boosGroup和workerGroup
            bootstrap.group(bossGroup, workerGroup)
                    //设置服务端通道实现类型
                    .channel(NioServerSocketChannel.class)
                    //快速链接
                    .option(ChannelOption.TCP_FASTOPEN_CONNECT, true)
                    //设置线程队列得到连接个数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    //设置保持活动连接状态
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    //使用匿名内部类的形式初始化通道对象
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            //给pipeline管道设置处理器
                            socketChannel.pipeline().addLast(new LengthFieldPrepender(4)).addLast(new MyServerHandler());
                        }
                    });//给workerGroup的EventLoop对应的管道设置处理器
            //绑定端口号，启动服务端
            channelFuture = bootstrap.bind(SERVER_PORT).sync();
            isStart = true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        //对关闭通道进行监听
        try {
            channelFuture.channel().close();
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}