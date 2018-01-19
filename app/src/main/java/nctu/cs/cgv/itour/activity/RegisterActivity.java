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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.maplist.DownloadFileAsyncTask;

import static nctu.cs.cgv.itour.MyApplication.dirPath;
import static nctu.cs.cgv.itour.MyApplication.logFlag;
import static nctu.cs.cgv.itour.MyApplication.mapTag;

public class RegisterActivity extends AppCompatActivity {

    private final int PERMISSIONS_MULTIPLE_REQUEST = 123;
    // UI references
    private EditText emailView;
    private EditText nameView;
    private EditText passwordView;
    private EditText confirmPasswordView;
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        emailView = findViewById(R.id.email);
        nameView = findViewById(R.id.name);
        passwordView = findViewById(R.id.password);
        confirmPasswordView = findViewById(R.id.confirm_password);
        confirmPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    register();
                    return true;
                }
                return false;
            }
        });
        Button emailSignInButton = findViewById(R.id.btn_email_sign_in);
        emailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                register();
            }
        });
    }

    private void register() {

        // Reset errors.
        emailView.setError(null);
        nameView.setError(null);
        passwordView.setError(null);

        // Store values at the time of the login attempt.
        String email = emailView.getText().toString().trim();
        final String name = nameView.getText().toString().trim();
        String password = passwordView.getText().toString().trim();
        String passwordConfirmed = confirmPasswordView.getText().toString().trim();

        boolean cancel = false;
        View focusView = null;

        // Confirm password again
        if (!confirmPassword(password, passwordConfirmed)) {
            confirmPasswordView.setError(getString(R.string.error_confirm_password));
            focusView = confirmPasswordView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            passwordView.setError(getString(R.string.error_invalid_password));
            focusView = passwordView;
            cancel = true;
        }

        // Check name field not empty
        if (TextUtils.isEmpty(name)) {
            nameView.setError(getString(R.string.error_field_required));
            focusView = nameView;
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

        progressDialog.setMessage(getString(R.string.dialog_register));
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                            if (logFlag) {
                                String[] color = {
                                        "ff4444",
                                        "f47d43",
                                        "ffbb33",
                                        "00c851",
                                        "2bbbad",
                                        "33b5e5",
                                        "4285f4",
                                        "aa66cc",
                                        "795548",
                                        "607d8b"
                                };

                                HashMap<String, Object> subjectUser = new HashMap<>();
                                subjectUser.put("color", color[new Random().nextInt(10)]);
                                subjectUser.put("subject", true);
                                subjectUser.put("username", name);

                                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                                databaseReference.child("users").child(user.getUid()).setValue(subjectUser);
                            }

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (!task.isSuccessful()) {
                                                Toast.makeText(RegisterActivity.this, getString(R.string.error_store_profile_failed), Toast.LENGTH_LONG).show();
                                            }
                                            checkPermission();

                                        }
                                    });
                        } else {
                            Toast.makeText(RegisterActivity.this, getString(R.string.error_registration_failed) + "\n" + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                        progressDialog.dismiss();
                    }
                });
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }

    private boolean confirmPassword(String password, String passwordConfirmed) {
        return Objects.equals(password, passwordConfirmed);
    }

    private void startMainActivity() {
        File mapFile = new File(dirPath + "/" + mapTag + "_distorted_map.png");
        File meshFile = new File(dirPath + "/" + mapTag + "_mesh.txt");
        File warpMeshFile = new File(dirPath + "/" + mapTag + "_warpMesh.txt");
        File boundBoxFile = new File(dirPath + "/" + mapTag + "_bound_box.txt");
        File edgeLengthFile = new File(dirPath + "/" + mapTag + "_edge_length.txt");
        File spotListFile = new File(dirPath + "/" + mapTag + "_spot_list.txt");
        if (mapFile.exists() && meshFile.exists() && warpMeshFile.exists() && boundBoxFile.exists() && edgeLengthFile.exists() && spotListFile.exists()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            new DownloadFileAsyncTask(this).execute(mapTag);
        }
    }

    private void checkPermission() {
        int storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int gpsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int micPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if (gpsPermission + storagePermission + micPermission != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showExplanation();
            } else {
                requestPermissions(
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.RECORD_AUDIO},
                        PERMISSIONS_MULTIPLE_REQUEST);
            }
        } else {
            startMainActivity();
        }
    }

    private void showExplanation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.permission_title)
                .setMessage(R.string.permission_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermissions(
                                new String[]{
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.RECORD_AUDIO},
                                PERMISSIONS_MULTIPLE_REQUEST);
                    }
                });
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean storagePermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean gpsPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean micPermission = grantResults[2] == PackageManager.PERMISSION_GRANTED;

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