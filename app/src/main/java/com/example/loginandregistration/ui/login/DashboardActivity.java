package com.example.loginandregistration.ui.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.loginandregistration.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class DashboardActivity extends AppCompatActivity {
    private SessionHandler session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        session = new SessionHandler(getApplicationContext());
        User user = session.getUserDetails();
        TextView welcomeText = findViewById(R.id.welcomeText);

        welcomeText.setText("Welcome "+user.getFullName()+", your session will expire on "+user.getSessionExpiryDate());

        Button logoutBtn = findViewById(R.id.btnLogout);

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                session.logoutUser();
                Intent i = new Intent(DashboardActivity.this, LoginActivity.class);
                startActivity(i);
                finish();

            }
        });
    }

    /**
     * Created by Abhi on 20 Jan 2018 020.
     */

    public static class SessionHandler {
        private static final String PREF_NAME = "UserSession";
        private static final String KEY_USERNAME = "username";
        private static final String KEY_EXPIRES = "expires";
        private static final String KEY_FULL_NAME = "full_name";
        private static final String KEY_EMPTY = "";
        private Context mContext;
        private SharedPreferences.Editor mEditor;
        private SharedPreferences mPreferences;

        public SessionHandler(Context mContext) {
            this.mContext = mContext;
            mPreferences = mContext.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
            this.mEditor = mPreferences.edit();
        }

        /**
         * Logs in the user by saving user details and setting session
         *
         * @param username
         * @param fullName
         */
        public void loginUser(String username, String fullName) {
            mEditor.putString(KEY_USERNAME, username);
            mEditor.putString(KEY_FULL_NAME, fullName);
            Date date = new Date();

            //Set user session for next 7 days
            long millis = date.getTime() + (7 * 24 * 60 * 60 * 1000);
            mEditor.putLong(KEY_EXPIRES, millis);
            mEditor.commit();
        }

        /**
         * Checks whether user is logged in
         *
         * @return
         */
        public boolean isLoggedIn() {
            Date currentDate = new Date();

            long millis = mPreferences.getLong(KEY_EXPIRES, 0);

            /* If shared preferences does not have a value
             then user is not logged in
             */
            if (millis == 0) {
                return false;
            }
            Date expiryDate = new Date(millis);

            /* Check if session is expired by comparing
            current date and Session expiry date
            */
            return currentDate.before(expiryDate);
        }

        /**
         * Fetches and returns user details
         *
         * @return user details
         */
        public User getUserDetails() {
            //Check if user is logged in first
            if (!isLoggedIn()) {
                return null;
            }
            User user = new User();
            user.setUsername(mPreferences.getString(KEY_USERNAME, KEY_EMPTY));
            user.setFullName(mPreferences.getString(KEY_FULL_NAME, KEY_EMPTY));
            user.setSessionExpiryDate(new Date(mPreferences.getLong(KEY_EXPIRES, 0)));

            return user;
        }

        /**
         * Logs out user by clearing the session
         */
        public void logoutUser(){
            mEditor.clear();
            mEditor.commit();
        }

    }

    /**
     * Created by Abhi on 20 Jan 2018 020.
     */

    public static class User {
        String username;
        String fullName;
        Date sessionExpiryDate;

        public void setUsername(String username) {
            this.username = username;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public void setSessionExpiryDate(Date sessionExpiryDate) {
            this.sessionExpiryDate = sessionExpiryDate;
        }

        public String getUsername() {
            return username;
        }

        public String getFullName() {
            return fullName;
        }

        public Date getSessionExpiryDate() {
            return sessionExpiryDate;
        }
    }

    public static class LoginActivity extends AppCompatActivity {
        private static final String KEY_STATUS = "status";
        private static final String KEY_MESSAGE = "message";
        private static final String KEY_FULL_NAME = "full_name";
        private static final String KEY_USERNAME = "username";
        private static final String KEY_PASSWORD = "password";
        private static final String KEY_EMPTY = "";
        private EditText etUsername;
        private EditText etPassword;
        private String username;
        private String password;
        private ProgressDialog pDialog;
        private String login_url = "http://192.168.43.72:8888/api/member/login.php";
        private SessionHandler session;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            session = new SessionHandler(getApplicationContext());

            if(session.isLoggedIn()){
                loadDashboard();
            }
            setContentView(R.layout.activity_login);

            etUsername = findViewById(R.id.etLoginUsername);
            etPassword = findViewById(R.id.etLoginPassword);

            Button register = findViewById(R.id.btnLoginRegister);
            Button login = findViewById(R.id.btnLogin);

            //Launch Registration screen when Register Button is clicked
            register.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                    startActivity(i);
                    finish();
                }
            });

            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Retrieve the data entered in the edit texts
                    username = etUsername.getText().toString().toLowerCase().trim();
                    password = etPassword.getText().toString().trim();
                    if (validateInputs()) {
                        login();
                    }
                }
            });
        }

        /**
         * Launch Dashboard Activity on Successful Login
         */
        private void loadDashboard() {
            Intent i = new Intent(getApplicationContext(), DashboardActivity.class);
            startActivity(i);
            finish();

        }

        /**
         * Display Progress bar while Logging in
         */

        private void displayLoader() {
            pDialog = new ProgressDialog(LoginActivity.this);
            pDialog.setMessage("Logging In.. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        private void login() {
            displayLoader();
            JSONObject request = new JSONObject();
            try {
                //Populate the request parameters
                request.put(KEY_USERNAME, username);
                request.put(KEY_PASSWORD, password);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            JsonObjectRequest jsArrayRequest = new JsonObjectRequest
                    (Request.Method.POST, login_url, request, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            pDialog.dismiss();
                            try {
                                //Check if user got logged in successfully

                                if (response.getInt(KEY_STATUS) == 0) {
                                    session.loginUser(username,response.getString(KEY_FULL_NAME));
                                    loadDashboard();

                                }else{
                                    Toast.makeText(getApplicationContext(),
                                            response.getString(KEY_MESSAGE), Toast.LENGTH_SHORT).show();

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            pDialog.dismiss();

                            //Display error message whenever an error occurs
                            Toast.makeText(getApplicationContext(),
                                    error.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });

            // Access the RequestQueue through your singleton class.
            MySingleton.getInstance(this).addToRequestQueue(jsArrayRequest);
        }

        /**
         * Validates inputs and shows error if any
         * @return
         */
        private boolean validateInputs() {
            if(KEY_EMPTY.equals(username)){
                etUsername.setError("Username cannot be empty");
                etUsername.requestFocus();
                return false;
            }
            if(KEY_EMPTY.equals(password)){
                etPassword.setError("Password cannot be empty");
                etPassword.requestFocus();
                return false;
            }
            return true;
        }
    }
}