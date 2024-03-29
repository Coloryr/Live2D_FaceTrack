package com.coloryr.facetrack

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.coloryr.facetrack.live2d.GLRenderer
import com.coloryr.facetrack.live2d.GLView
import com.coloryr.facetrack.live2d.JniBridgeJava
import com.coloryr.facetrack.socket.ConnectService
import com.coloryr.facetrack.track.IAR
import com.coloryr.facetrack.track.ar.SnackbarHelper
import com.coloryr.facetrack.track.arcore.ArCoreTest
import com.coloryr.facetrack.track.arengine.ArEngineTest
import com.coloryr.facetrack.track.eye.EyeTrack
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

class MainActivity : AppCompatActivity() {
    private val messageSnackbarHelper = SnackbarHelper()
    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.INTERNET
    )
    private val mainHandler = Handler(Looper.getMainLooper())
    private var mNManager: NotificationManager? = null
    private var constraintLayout: RelativeLayout? = null

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        Handler(mainLooper).post {
            while (true) {
                try {
                    Looper.loop()
                } catch (e1: Exception) {
                    val alertDialog1 = AlertDialog.Builder(this)
                        .setTitle("这是标题")//标题
                        .setMessage(getErrorInfoFromException(e1))//内容
                        .setIcon(R.mipmap.ic_launcher)//图标
                        .create()
                    alertDialog1.show()
                    e1.printStackTrace()
                }
            }
        }

        Thread.setDefaultUncaughtExceptionHandler { t, e1 ->
            val alertDialog1 = AlertDialog.Builder(this)
                .setTitle("这是标题")//标题
                .setMessage(getErrorInfoFromException(e1))//内容
                .setIcon(R.mipmap.ic_launcher)//图标
                .create()
            alertDialog1.show()
            e1.printStackTrace()
        }
    }

    fun getErrorInfoFromException(e: Throwable): String? {
        return try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            """
     
     $sw
     
     """.trimIndent()
        } catch (e2: Exception) {
            "bad getErrorInfoFromException"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isPermission
        mNManager = this.applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("Live2DFaceTrack", "Live2DFaceTrack", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Live2DFaceTrack"
        mNManager!!.createNotificationChannel(channel)
        app = this
        glView = GLView(baseContext)
        setContentView(R.layout.activity_main)

        constraintLayout = findViewById(R.id.gl_root)
        fps = findViewById(R.id.fps)
        timer.schedule(object : TimerTask() {
            override fun run() {
                val fps: Int = GLRenderer.getFps()
                run(Runnable { MainActivity.fps.text = fps.toString() })
            }
        }, 0, 1000)
        JniBridgeJava.ChangeModel()
        glView.callAdd(constraintLayout)
        JniBridgeJava.LoadModel("sizuku", "shizuku")

        eye = EyeTrack(this)
        ar = ArCoreTest(this)
        val intent1 = Intent(this, ConnectService::class.java)
        this.startService(intent1)
    }

    private val isPermission: Unit
        get() {
            val mPermissionList: MutableList<String> = ArrayList()
            for (item in permissions) {
                if (ContextCompat.checkSelfPermission(this, item) != PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(item)
                }
            }
            if (mPermissionList.size > 0) {
                ActivityCompat.requestPermissions(this, permissions, 100)
            }
        }

    public override fun onResume() {
        super.onResume()
        if (!ar.onResume()) {
            ar = ArEngineTest(this)
            if (!ar.onResume()) {
                messageSnackbarHelper.showError(this, "手机不支持AR功能")
            }
        }
        eye.init()
        val intent1 = Intent(this, ConnectService::class.java)
        this.startService(intent1)
    }

    override fun onPause() {
        super.onPause()
        ar.onPause()
        val intent1 = Intent(this, ConnectService::class.java)
        this.stopService(intent1)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var app: MainActivity

        @SuppressLint("StaticFieldLeak")
        lateinit var glView: GLView

        @SuppressLint("StaticFieldLeak")
        lateinit var ar: IAR

        @SuppressLint("StaticFieldLeak")
        lateinit var eye: EyeTrack

        @SuppressLint("StaticFieldLeak")
        private lateinit var fps: TextView
        private val timer = Timer()

        fun makeNotification(title: String?, text: String?, ticker: String?) {
            @SuppressLint("UnspecifiedImmutableFlag") val pendingIntent = PendingIntent.getActivity(
                app.applicationContext,
                1, app.intent, PendingIntent.FLAG_CANCEL_CURRENT
            )
            val mBuilder = Notification.Builder(
                app.applicationContext,
                "Live2DFaceTrack"
            )
            mBuilder.setContentTitle(title)
                .setContentText(text)
                .setTicker(ticker)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            val b = mBuilder.build()
            app.mNManager!!.notify(1, b)
        }

        fun run(runnable: Runnable?) {
            app.mainHandler.post(runnable!!)
        }
    }
}