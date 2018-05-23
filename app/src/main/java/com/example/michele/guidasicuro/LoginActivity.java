package com.example.michele.guidasicuro;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.content.Intent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Michele on 11/04/2018.
 */

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;

    private EditText mUsernameText;
    private EditText mPasswordText;
    private Button mLoginButton;
    private TextView mSignupLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        else {
            // Hide the action bar
            android.support.v7.app.ActionBar actionBar = getSupportActionBar();
            actionBar.hide();
        }

        mUsernameText = findViewById(R.id.input_username);
        mPasswordText = findViewById(R.id.input_password);
        mLoginButton = findViewById(R.id.btn_login);
        mSignupLink = findViewById(R.id.link_signup);

        mLoginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        mSignupLink.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
            }
        });
    }

    public void login() {
        Log.d(TAG, "Login");

        if (!validate()) {
            onLoginFailed();
            return;
        }

        mLoginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        final String username = mUsernameText.getText().toString();
        String password = mPasswordText.getText().toString();

        String url = "http://maestronim.altervista.org/Guida-Sicuro/api/user-info/check_credentials.php";
        Map<String, String> parameters = new HashMap();
        parameters.put("username", username);
        parameters.put("password", password);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (url, new JSONObject(parameters), new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse");
                        progressDialog.dismiss();
                        try {
                            if (response.getString("valid").equals("yes")) {
                                onLoginSuccess(username);
                            } else {
                                onLoginFailed();
                            }
                        } catch(JSONException e) {
                            e.printStackTrace();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i(TAG, "onErrorResponse");
                        progressDialog.dismiss();
                        onLoginFailed();
                    }
                });

        // Add a request to RequestQueue.
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {
                // By default we just finish the Activity and log them in automatically
                Intent returnIntent = new Intent();
                returnIntent.putExtra("username", data.getStringExtra("username"));
                setResult(Activity.RESULT_OK, returnIntent);
                this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess(String username) {
        mLoginButton.setEnabled(true);
        Intent returnIntent = new Intent();
        returnIntent.putExtra("username", username);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        mLoginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String username = mUsernameText.getText().toString();
        String password = mPasswordText.getText().toString();

        if (username.isEmpty() || username.length() < 3 || username.length() > 256) {
            mUsernameText.setError("between 3 and 256 alphanumeric characters");
            valid = false;
        } else {
            mUsernameText.setError(null);
        }

        if (password.isEmpty() || password.length() < 8 || password.length() > 256) {
            mPasswordText.setError("between 8 and 256 alphanumeric characters");
            valid = false;
        } else {
            mPasswordText.setError(null);
        }

        return valid;
    }
}
