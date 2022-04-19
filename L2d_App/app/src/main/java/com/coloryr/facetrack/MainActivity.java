package com.coloryr.facetrack;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.coloryr.facetrack.socket.ServiceBroadcastReceiver;
import com.coloryr.facetrack.track.IAR;
import com.coloryr.facetrack.track.ar.SnackbarHelper;
import com.coloryr.facetrack.track.arengine.ArEngineTest;
import com.coloryr.facetrack.track.eye.EyeTrack;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.coloryr.facetrack.track.arcore.ArCoreTest;
import com.coloryr.facetrack.live2d.GLView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    public static MainActivity app;

    @SuppressLint("StaticFieldLeak")
    public static GLView glView;

    @SuppressLint("StaticFieldLeak")
    public static IAR ar;

    @SuppressLint("StaticFieldLeak")
    public static EyeTrack eye;

    @SuppressLint("StaticFieldLeak")
    public static ImageView imageView;

    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();

    private final String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ServiceBroadcastReceiver receiver = new ServiceBroadcastReceiver();

    private NotificationManager mNManager;

    public static void makeNotification(String title, String text, String ticker) {
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getActivity(app.getApplicationContext(),
                1, app.getIntent(), PendingIntent.FLAG_CANCEL_CURRENT);
        Notification.Builder mBuilder = new Notification.Builder(app.getApplicationContext(),
                "Live2DFaceTrack");
        mBuilder.setContentTitle(title)
                .setContentText(text)
                .setTicker(ticker)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        Notification b = mBuilder.build();
        app.mNManager.notify(1, b);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isPermission();

        mNManager = (NotificationManager) this.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("Live2DFaceTrack", "Live2DFaceTrack", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Live2DFaceTrack");
        mNManager.createNotificationChannel(channel);

        app = this;
        glView = new GLView(getBaseContext());
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.image);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_dashboard)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        eye = new EyeTrack(this);
        ar = new ArCoreTest(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ServiceBroadcastReceiver.START_ACTION);
        registerReceiver(receiver, filter);
    }

    private void isPermission() {
        List<String> mPermissionList = new ArrayList<>();
        for (String item : permissions) {
            if (ContextCompat.checkSelfPermission(this, item) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(item);
            }
        }
        if (mPermissionList.size() > 0) {
            ActivityCompat.requestPermissions(this, permissions, 100);
        }
    }

    public static void run(Runnable runnable) {
        app.mainHandler.post(runnable);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!ar.onResume()) {
            ar = new ArEngineTest(this);
            if (!ar.onResume()) {
                messageSnackbarHelper.showError(this, "手机不支持AR功能");
            }
        }
        eye.init();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ar.onPause();
    }
}