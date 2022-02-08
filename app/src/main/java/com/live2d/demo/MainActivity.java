package com.live2d.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    public static MainActivity app;

    @SuppressLint("StaticFieldLeak")
    public static GLView glView;

    @SuppressLint("StaticFieldLeak")
    public static ArTest ar;

    public static CameraConnection cameraConnection;

    private final String[] permissions = new String[]{
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Constants.SetContext(this);
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

        if (!new File(Constants.getFaceShapeModelPath()).exists()) {
            FileUtils.copyFileFromRawToOthers(this, R.raw.shape_predictor_68_face_landmarks, Constants.getFaceShapeModelPath());
        }

        //cameraConnection = CameraConnection.newInstance();

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

    @Override
    public void onResume() {
        super.onResume();
        //cameraConnection.startBackgroundThread();

        //cameraConnection.openCamera(1280, 1280);
        ar.onResume();
    }

    @Override
    protected void onPause() {
        //cameraConnection.onPause();
        super.onPause();
        ar.onPause();
    }
}