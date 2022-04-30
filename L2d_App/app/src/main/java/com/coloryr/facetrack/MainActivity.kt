package com.coloryr.facetrack

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.coloryr.facetrack.live2d.GLView
import com.coloryr.facetrack.socket.ServiceBroadcastReceiver
import com.coloryr.facetrack.track.IAR
import com.coloryr.facetrack.track.ar.SnackbarHelper
import com.coloryr.facetrack.track.arcore.ArCoreTest
import com.coloryr.facetrack.track.arengine.ArEngineTest
import com.coloryr.facetrack.track.eye.EyeTrack
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private val messageSnackbarHelper = SnackbarHelper()
    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.INTERNET
    )
    private val mainHandler = Handler(Looper.getMainLooper())
    private val receiver = ServiceBroadcastReceiver()
    private var mNManager: NotificationManager? = null
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
        imageView = findViewById(R.id.image)
        //val navView = findViewById<BottomNavigationView>(R.id.nav_view)
        val appBarConfiguration: AppBarConfiguration = AppBarConfiguration.Builder(
            R.id.navigation_dashboard
        ).build()
        val navController = findNavController(this, R.id.nav_host_fragment_activity_main)
        setupActionBarWithNavController(this, navController, appBarConfiguration)
        eye = EyeTrack(this)
        ar = ArCoreTest(this)
        val filter = IntentFilter()
        filter.addAction(ServiceBroadcastReceiver.Companion.START_ACTION)
        registerReceiver(receiver, filter)
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
        if (!ar!!.onResume()) {
            ar = ArEngineTest(this)
            if (!ar!!.onResume()) {
                messageSnackbarHelper.showError(this, "手机不支持AR功能")
            }
        }
        eye!!.init()
    }

    override fun onPause() {
        super.onPause()
        ar!!.onPause()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        var app: MainActivity? = null

        @SuppressLint("StaticFieldLeak")
        var glView: GLView? = null

        @SuppressLint("StaticFieldLeak")
        var ar: IAR? = null

        @SuppressLint("StaticFieldLeak")
        var eye: EyeTrack? = null

        @SuppressLint("StaticFieldLeak")
        var imageView: ImageView? = null
        fun makeNotification(title: String?, text: String?, ticker: String?) {
            @SuppressLint("UnspecifiedImmutableFlag") val pendingIntent = PendingIntent.getActivity(
                app!!.applicationContext,
                1, app!!.intent, PendingIntent.FLAG_CANCEL_CURRENT
            )
            val mBuilder = Notification.Builder(
                app!!.applicationContext,
                "Live2DFaceTrack"
            )
            mBuilder.setContentTitle(title)
                .setContentText(text)
                .setTicker(ticker)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            val b = mBuilder.build()
            app!!.mNManager!!.notify(1, b)
        }

        fun run(runnable: Runnable?) {
            app!!.mainHandler.post(runnable!!)
        }
    }
}