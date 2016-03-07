package com.csce.tamu.eyebell;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.BackendlessCallback;

public class RegisterActivity extends AppCompatActivity {
    private UserRegisterTask mRegisterTask = null;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private EditText mPhoneView;
    private EditText mNameView;
    private EditText mSerialView;
    private View mRegisterFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Register Account");
        setSupportActionBar(toolbar);
        mEmailView = (EditText) findViewById(R.id.rEmail);
        mPasswordView = (EditText) findViewById(R.id.rPassword);
        mPhoneView = (EditText) findViewById(R.id.rPhone);
        mNameView = (EditText) findViewById(R.id.rName);
        mSerialView = (EditText) findViewById(R.id.rEyeSerial);
        Button mEmailRegisterButton = (Button) findViewById(R.id.email_register_button);
        mEmailRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        mRegisterFormView = findViewById(R.id.register_form);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptRegister() {
        if (mRegisterTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String phone = mPhoneView.getText().toString();
        String name = mNameView.getText().toString();
        String serial = mSerialView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mRegisterTask = new UserRegisterTask(email, password, phone, name, serial);
            mRegisterTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    public class UserRegisterTask extends AsyncTask<Void, Void, Boolean> {
        private BackendlessUser backUser;

        UserRegisterTask(String email, String password, String phone, String name, String serial) {
            backUser = new BackendlessUser();
            backUser.setEmail(email);
            backUser.setPassword(password);
            backUser.setProperty("phone", phone);
            backUser.setProperty("name", name);
            backUser.setProperty("serial", serial);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Backendless.UserService.register(backUser, new BackendlessCallback<BackendlessUser>()
                {
                    @Override
                    public void handleResponse( BackendlessUser backendlessUser )
                    {
                        Log.i("Registration", backendlessUser.getEmail() + " successfully registered" );
                    }
                } );
            } catch (Exception e) {
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mRegisterTask = null;

            if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mRegisterTask = null;
        }
    }
}
