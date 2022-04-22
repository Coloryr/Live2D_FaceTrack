package com.coloryr.facetrack.socket

import com.coloryr.facetrack.MainActivity
import com.coloryr.facetrack.live2d.TrackSave
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.util.CharsetUtil
import java.nio.charset.StandardCharsets

object SocketUtils {
    const val PACK_INIT = "Live2D server init"
    const val PACK_OK = "Live2D server ok"
    var checkOk = false
    var channel: Channel? = null
    fun startPack(pack: ByteBuf) {
        if (!checkOk) {
            val data = pack.toString(StandardCharsets.UTF_8)
            if (data == PACK_INIT) {
                sendPack(PACK_OK)
                checkOk = true
                MainActivity.run(Runnable {
                    MainActivity.makeNotification(
                        "电脑连接",
                        "成功与电脑连接",
                        "连接成功"
                    )
                })
            }
        } else {
            val type = pack.readShort()
            when (type) {

            }
        }
    }

    fun send() {
        if (channel != null) {
            val buff = Unpooled.buffer()
            buff.writeShort(0)
            buff.writeFloat(TrackSave.AngleY)
            buff.writeFloat(TrackSave.AngleX)
            buff.writeFloat(TrackSave.AngleZ)
            buff.writeFloat(TrackSave.MouthOpenY)
            buff.writeFloat(TrackSave.EyeBallX)
            buff.writeFloat(TrackSave.EyeBallY)
            buff.writeFloat(TrackSave.BodyZ)
            buff.writeFloat(TrackSave.BodyY)
            buff.writeFloat(TrackSave.EyeLOpen)
            buff.writeFloat(TrackSave.EyeROpen)
            sendPack(buff)
        }
    }

    fun sendPack(data: String?) {
        if (channel != null) {
            channel!!.writeAndFlush(Unpooled.copiedBuffer(data, CharsetUtil.UTF_8))
        }
    }

    fun sendPack(data: ByteBuf?) {
        if (channel != null) {
            channel!!.writeAndFlush(data)
        }
    }
}