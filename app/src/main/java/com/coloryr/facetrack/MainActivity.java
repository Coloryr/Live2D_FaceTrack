package com.coloryr.facetrack;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.coloryr.facetrack.track.eye.EyeTrack;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.coloryr.facetrack.track.arcore.ArTest;
import com.coloryr.facetrack.live2d.GLView;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    public static MainActivity app;

    @SuppressLint("StaticFieldLeak")
    public static GLView glView;

    @SuppressLint("StaticFieldLeak")
    public static ArTest ar;

    @SuppressLint("StaticFieldLeak")
    public static EyeTrack eye;

    private final String[] permissions = new String[]{
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isPermission();
        app = this;
        glView = new GLView(getBaseContext());
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_dashboard)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        eye = new EyeTrack(this);
        ar = new ArTest(this);
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

    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                try {
                    eye.init();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("opencv", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d("opencv", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        ar.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ar.onPause();
    }
}