package com.coloryr.facetrack.socket;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES30;
import com.alibaba.fastjson.JSON;
import com.coloryr.facetrack.MainActivity;
import com.coloryr.facetrack.R;
import com.coloryr.facetrack.RgbaUtils;
import com.coloryr.facetrack.live2d.JniBridgeJava;
import com.coloryr.facetrack.objs.ModelObj;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class SocketUtils {
    public static final String PACK_INIT = "Live2D server init";
    public static final String PACK_OK = "Live2D server ok";
    public static boolean checkOk;

    public static Channel channel;

    public static void startPack(ByteBuf pack) {
        if (!checkOk) {
            String data = pack.toString(StandardCharsets.UTF_8);
            if (data.equals(PACK_INIT)) {
                sendPack(PACK_OK);
                checkOk = true;
                MainActivity.run(() -> {
                    MainActivity.makeNotification("电脑连接", "成功与电脑连接", "连接成功");
                });
            }
        } else {
            short type = pack.readShort();
            switch (type) {
                case 0:
                    ModelObj obj = new ModelObj();
                    obj.isLoad = JniBridgeJava.isLoad();
                    obj.name = JniBridgeJava.getName();
                    ByteBuf buff = Unpooled.buffer();
                    buff.writeShort(0);
                    String data = JSON.toJSONString(obj);
                    buff.writeInt(data.length());
                    buff.writeCharSequence(data, StandardCharsets.UTF_8);
                    sendPack(buff);
                    break;
            }
        }
    }

    private static byte[] b;

    public static void sendImage(ByteBuffer data, int width, int height) {
        if (channel != null) {
            GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, data);

            ByteBuf buf = Unpooled.buffer();
            buf.writeShort(256);
            buf.writeInt(width);
            buf.writeInt(height);

            if (b == null || b.length != data.remaining())
                b = new byte[data.remaining()];
            data.get(b, 0, b.length);
            buf.writeInt(b.length);
            buf.writeBytes(b, 0, b.length);

            if (channel != null) {
                try {
                    channel.writeAndFlush(buf).syncUninterruptibly();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            data.clear();
        }
    }

    public static void sendPack(String data) {
        if (channel != null) {
            channel.writeAndFlush(Unpooled.copiedBuffer(data, CharsetUtil.UTF_8));
        }
    }

    public static void sendPack(ByteBuf data) {
        if (channel != null) {
            channel.writeAndFlush(data);
        }
    }
}
