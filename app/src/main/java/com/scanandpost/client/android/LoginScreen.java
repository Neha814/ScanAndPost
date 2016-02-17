package com.scanandpost.client.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.scanandpost.client.functions.StringUtils;


/**
 * Created by Neha on 12/28/2015.
 */
public class LoginScreen extends Activity implements View.OnClickListener {

  //  TextInputLayout client_id_layout;
    EditText client_id_edt;
    Button login_btn;
    SharedPreferences sp;

    /*@Override
    public boolean onTouchEvent(MotionEvent event) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        return true;
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login);

        View header = LayoutInflater.from(this).inflate(R.layout.toolbar, null);

        TextView title_tv = (TextView) findViewById(R.id.title_tv);
        LinearLayout home_layout = (LinearLayout) findViewById(R.id.home_layout);
        ImageView home = (ImageView) findViewById(R.id.home);
        title_tv.setText("Sign In");

        home.setVisibility(View.GONE);
        home_layout.setVisibility(View.GONE);

        sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        inIt();
    }

    private void inIt() {
        client_id_edt = (EditText) findViewById(R.id.client_id_edt);
      //  client_id_layout = (TextInputLayout) findViewById(R.id.client_id_layout);
        login_btn = (Button) findViewById(R.id.login_btn);

        login_btn.setOnClickListener(this);

        client_id_edt.addTextChangedListener(generalTextWatcher);
    }

    private TextWatcher generalTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {

            if (client_id_edt.getText().hashCode() == s.hashCode()) {

            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count,
                                      int after) {

            if (client_id_edt.getText().hashCode() == s.hashCode()) {

            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (client_id_edt.getText().hashCode() == s.hashCode()) {
               // client_id_layout.setError(null);
                client_id_edt.setError(null);
            }

        }

    };

    @Override
    public void onClick(View v) {
        if (v == login_btn) {
            CheckValidations();
        }
    }

    private void CheckValidations() {
        if (StringUtils.getLength(client_id_edt) > 0) {
            SharedPreferences.Editor e = sp.edit();
            e.putString("client_id",client_id_edt.getText().toString());
            e.commit();
            Intent i = new Intent(LoginScreen.this, MainActivity.class);
            startActivity(i);
            finish();
        } else {
            client_id_edt.setError("Please enter client id.");
        }
    }
}
