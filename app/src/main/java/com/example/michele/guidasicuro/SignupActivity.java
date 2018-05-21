package com.example.michele.guidasicuro;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";

    private EditText mUsernameText;
    private EditText mEmailText;
    private EditText mPasswordText;
    private Button mSignupButton;
    private TextView mLoginLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mUsernameText = findViewById(R.id.input_username);
        mEmailText = findViewById(R.id.input_email);
        mPasswordText = findViewById(R.id.input_password);
        mSignupButton = findViewById(R.id.btn_signup);
        mLoginLink = findViewById(R.id.link_login);

        mSignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        mLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                finish();
            }
        });
    }

    public void signup() {
        Log.d(TAG, "Signup");

        if (!validate()) {
            onSignupFailed();
            return;
        }

        mSignupButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(SignupActivity.this,
                ProgressDialog.STYLE_SPINNER);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        final String username = mUsernameText.getText().toString();
        String email = mEmailText.getText().toString();
        String password = mPasswordText.getText().toString();

        String url = "http://maestronim.altervista.org/Guida-Sicuro/api/user-info/create.php";
        Map<String, String> parameters = new HashMap();
        parameters.put("username", username);
        parameters.put("email", email);
        parameters.put("password", password);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (url, new JSONObject(parameters), new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "onResponse");
                        progressDialog.dismiss();
                        try {
                            if (response.getString("success").equals("yes")) {
                                onSignupSuccess(username);
                            } else {
                                onSignupFailed();
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
                        Log.i(TAG, error.getMessage());
                        progressDialog.dismiss();
                        onSignupFailed();
                    }
                });

        // Add a request to RequestQueue.
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }


    public void onSignupSuccess(String username) {
        mSignupButton.setEnabled(true);
        Intent returnIntent = new Intent();
        returnIntent.putExtra("username", username);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    public void onSignupFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        mSignupButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String username = mUsernameText.getText().toString();
        String email = mEmailText.getText().toString();
        String password = mPasswordText.getText().toString();

        if (username.isEmpty() || username.length() < 3 || username.length() > 256) {
            mUsernameText.setError("between 3 and 256 alphanumeric characters");
            valid = false;
        } else {
            mUsernameText.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEmailText.setError("enter a valid email address");
            valid = false;
        } else {
            mEmailText.setError(null);
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
