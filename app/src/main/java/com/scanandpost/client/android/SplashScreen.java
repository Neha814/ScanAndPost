package com.scanandpost.client.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

/**
 * Created by Neha on 12/28/2015.
 */
public class SplashScreen extends Activity {
    private Animation anim;
    ImageView welcome_img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);


        welcome_img = (ImageView) findViewById(R.id.welcome_img);
        anim = AnimationUtils.loadAnimation(SplashScreen.this, R.anim.zoom_in);
        welcome_img.startAnimation(anim);

        Thread t = new Thread(){
            public void run(){
                try {
                    sleep(2 * 1000);
                    Intent i = new Intent(SplashScreen.this, LoginScreen.class);
                    startActivity(i);
                    finish();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        };t.start();
    }
}
