package com.scanandpost.client.android;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.scanandpost.client.android.Fragments.HomeFragment;


public class MainActivity extends FragmentActivity {

   static TextView title_tv;

    private static final int ZBAR_SCANNER_REQUEST = 0;
    /*private static final int ZBAR_QR_SCANNER_REQUEST = 1;

    private static final int GALLERY_IAMGE = 2;
    private static final int CAMERA_IMAGE = 3;
*/



   /* @Override
    public boolean onTouchEvent(MotionEvent event) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        return true;
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View header = LayoutInflater.from(this).inflate(R.layout.toolbar, null);

         title_tv = (TextView) findViewById(R.id.title_tv);
        LinearLayout home_layout = (LinearLayout) findViewById(R.id.home_layout);
        ImageView home = (ImageView) findViewById(R.id.home);
        title_tv.setText("Home");

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddInitialFragment();
            }
        });
        home_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddInitialFragment();
            }
        });


        AddInitialFragment();
    }

    private void AddInitialFragment() {
        Fragment fragment = null;
        fragment = new HomeFragment();
        String title = "Home";

        if (fragment != null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_frame, fragment);
            ft.commit();
        }


    }


    @Override
    public void onBackPressed() {
            super.onBackPressed();
    }



/*    private void ScanItems() {
        if (isCameraAvailable()) {
            Intent intent = new Intent(getApplicationContext(), ZBarScannerActivity.class);
            //intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE});
           *//* public static final int NONE = 0;
            public static final int PARTIAL = 1;
            public static final int EAN8 = 8;
            public static final int UPCE = 9;
            public static final int ISBN10 = 10;
            public static final int UPCA = 12;
            public static final int EAN13 = 13;
            public static final int ISBN13 = 14;
            public static final int I25 = 25;
            public static final int DATABAR = 34;
            public static final int DATABAR_EXP = 35;
            public static final int CODABAR = 38;
            public static final int CODE39 = 39;
            public static final int PDF417 = 57;
            public static final int QRCODE = 64;
            public static final int CODE93 = 93;
            public static final int CODE128 = 128;*//*
            intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{Symbol.QRCODE,Symbol.NONE,
                    Symbol.PARTIAL,Symbol.EAN8,Symbol.UPCE,Symbol.ISBN10,Symbol.UPCA,Symbol.EAN13,
                    Symbol.ISBN13,Symbol.I25,Symbol.DATABAR,Symbol.DATABAR_EXP,Symbol.CODABAR,
                    Symbol.CODE39,Symbol.PDF417,Symbol.CODE128,Symbol.CODE93});
            startActivityForResult(intent, ZBAR_SCANNER_REQUEST);
        } else {
            Toast.makeText(getApplicationContext(), "Rear Facing Camera Unavailable", Toast.LENGTH_SHORT).show();
        }
    }*/
    public boolean isCameraAvailable() {
        PackageManager pm = getApplicationContext().getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static void changeTitle(String title) {
       title_tv.setText(title);
    }

   /* public static void showFragment(int f) {
    navigationView.getMenu().getItem(f).setChecked(true);
    }
    public static void showFragment1(int f) {
        navigationView.getMenu().getItem(f).setChecked(false);
    }*/

}
