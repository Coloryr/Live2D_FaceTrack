package com.coloryr.facetrack.socket;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES30;
import com.alibaba.fastjson.JSON;
import com.coloryr.facetrack.MainActivity;
import com.coloryr.facetrack.R;
import com.coloryr.facetrack.RgbaUtils;
import com.coloryr.facetrack.live2d.JniBridgeJava;
import com.coloryr.facetrack.live2d.TrackSave;
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

                    break;
            }
        }
    }

    public static void send() {
        if (channel != null) {
            ByteBuf buff = Unpooled.buffer();
            buff.writeShort(0);
            buff.writeFloat(TrackSave.AngleY);
            buff.writeFloat(TrackSave.AngleX);
            buff.writeFloat(TrackSave.AngleZ);
            buff.writeFloat(TrackSave.MouthOpenY);
            buff.writeFloat(TrackSave.EyeBallX);
            buff.writeFloat(TrackSave.EyeBallY);
            buff.writeFloat(TrackSave.BodyZ);
            buff.writeFloat(TrackSave.BodyY);
            buff.writeFloat(TrackSave.EyeLOpen);
            buff.writeFloat(TrackSave.EyeROpen);
            sendPack(buff);
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
