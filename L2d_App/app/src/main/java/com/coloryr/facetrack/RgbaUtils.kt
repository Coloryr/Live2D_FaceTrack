package com.coloryr.facetrack

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat

object RgbaUtils {
    fun getRBitmap(id: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(MainActivity.app, id)
        val bd = drawable as BitmapDrawable?
        return bd!!.bitmap
    }

    /**
     * @方法描述 将RGB字节数组转换成Bitmap，
     */
    fun rgb2Bitmap(data: ByteArray, width: Int, height: Int): Bitmap? {
        val colors = convertByteToColor(data) ?: return null //取RGB值转换为int数组
        return Bitmap.createBitmap(
            colors, 0, width, width, height,
            Bitmap.Config.ARGB_8888
        )
    }

    // 将一个byte数转成int
    // 实现这个函数的目的是为了将byte数当成无符号的变量去转化成int
    fun convertByteToInt(data: Byte): Int {
        val heightBit = data.toInt() shr 4 and 0x0F
        val lowBit = 0x0F and data.toInt()
        return heightBit * 16 + lowBit
    }

    // 将纯RGB数据数组转化成int像素数组
    fun convertByteToColor(data: ByteArray): IntArray? {
        val size = data.size
        if (size == 0) {
            return null
        }
        var arg = 0
        if (size % 3 != 0) {
            arg = 1
        }

        // 一般RGB字节数组的长度应该是3的倍数，
        // 不排除有特殊情况，多余的RGB数据用黑色0XFF000000填充
        val color = IntArray(size / 3 + arg)
        var red: Int
        var green: Int
        var blue: Int
        val colorLen = color.size
        if (arg == 0) {
            for (i in 0 until colorLen) {
                red = convertByteToInt(data[i * 3])
                green = convertByteToInt(data[i * 3 + 1])
                blue = convertByteToInt(data[i * 3 + 2])

                // 获取RGB分量值通过按位或生成int的像素值
                color[i] = red shl 16 or (green shl 8) or blue or -0x1000000
            }
        } else {
            for (i in 0 until colorLen - 1) {
                red = convertByteToInt(data[i * 3])
                green = convertByteToInt(data[i * 3 + 1])
                blue = convertByteToInt(data[i * 3 + 2])
                color[i] = red shl 16 or (green shl 8) or blue or -0x1000000
            }
            color[colorLen - 1] = -0x1000000
        }
        return color
    }
}