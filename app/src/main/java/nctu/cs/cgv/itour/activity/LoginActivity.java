package nctu.cs.cgv.itour.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.maplist.DownloadFileAsyncTask;

import static nctu.cs.cgv.itour.MyApplication.dirPath;
import static nctu.cs.cgv.itour.MyApplication.logFlag;
import static nctu.cs.cgv.itour.MyApplication.mapTag;

public class LoginActivity extends AppCompatActivity {

    // UI references
    private EditText emailView;
    private EditText passwordView;
    private ProgressDialog progressDialog;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkPermission();
            } else {
                startMainActivity();
            }
        }

        progressDialog = new ProgressDialog(this);
        emailView = (EditText) findViewById(R.id.email);
        passwordView = (EditText) findViewById(R.id.password);
        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    signin();
                    return true;
                }
                return false;
            }
        });
        Button emailSignInButton = (Button) findViewById(R.id.btn_email_sign_in);
        emailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signin();
            }
        });

        Button registerButton = (Button) findViewById(R.id.btn_register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
        Button guestBtn = (Button) findViewById(R.id.btn_guest);
        guestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logFlag = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkPermission();
                } else {
                    startMainActivity();
                }
            }
        });
    }

    private void signin() {

        // Reset errors.
        emailView.setError(null);
        passwordView.setError(null);

        // Store values at the time of the login attempt.
        String email = emailView.getText().toString().trim();
        String password = passwordView.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            passwordView.setError(getString(R.string.error_invalid_password));
            focusView = passwordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            emailView.setError(getString(R.string.error_field_required));
            focusView = emailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            emailView.setError(getString(R.string.error_invalid_email));
            focusView = emailView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
            return;
        }

        progressDialog.setMessage("Sign In...");
        progressDialog.show();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                checkPermission();
                            } else {
                                startMainActivity();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Sign in failed.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return true;
    }

    private void startMainActivity() {
        File mapFile = new File(dirPath+ "/"  + mapTag + "_distorted_map.png");
        File meshFile = new File(dirPath+ "/"  + mapTag + "_mesh.txt");
        File warpMeshFile = new File(dirPath+ "/"  + mapTag + "_warpMesh.txt");
        File boundBoxFile = new File(dirPath+ "/"  + mapTag + "_bound_box.txt");
        File edgeLengthFile = new File(dirPath+ "/"  + mapTag + "_edge_length.txt");
        File spotListFile = new File(dirPath+ "/"  + mapTag + "_spot_list.txt");
        if (mapFile.exists() && meshFile.exists() && warpMeshFile.exists() && boundBoxFile.exists() && edgeLengthFile.exists() && spotListFile.exists()) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            new DownloadFileAsyncTask(this).execute(mapTag);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermission() {
        final int PERMISSIONS_MULTIPLE_REQUEST = 123;
        int storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int gpsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (gpsPermission + storagePermission != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showExplanation();
            } else {
                requestPermissions(
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_MULTIPLE_REQUEST);
            }
        } else {
            startMainActivity();
        }
    }

    private void showExplanation() {
        final int PERMISSIONS_MULTIPLE_REQUEST = 123;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_title)
                .setMessage(R.string.permission_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermissions(
                                new String[]{
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSIONS_MULTIPLE_REQUEST);
                    }
                });
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        final int PERMISSIONS_MULTIPLE_REQUEST = 123;

        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean storagePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean gpsPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (storagePermission && gpsPermission) {
                        startMainActivity();
                    } else {
                        showExplanation();
                    }
                }
                break;
        }
    }
}