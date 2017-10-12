package nctu.cs.cgv.itour.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import java.io.File;
import java.util.Objects;

import nctu.cs.cgv.itour.R;
import nctu.cs.cgv.itour.maplist.DownloadFileAsyncTask;

import static nctu.cs.cgv.itour.MyApplication.dirPath;
import static nctu.cs.cgv.itour.MyApplication.mapTag;

public class RegisterActivity extends AppCompatActivity {

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
        emailView = (EditText) findViewById(R.id.email);
        nameView = (EditText) findViewById(R.id.name);
        passwordView = (EditText) findViewById(R.id.password);
        confirmPasswordView = (EditText) findViewById(R.id.confirm_password);
        confirmPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.register || id == EditorInfo.IME_NULL) {
                    register();
                    return true;
                }
                return false;
            }
        });
        Button emailSignInButton = (Button) findViewById(R.id.btn_email_sign_in);
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

        progressDialog.setMessage("Registering User...");
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

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
                                            startMainActivity();
                                        }
                                    });
                        } else {
                            Toast.makeText(RegisterActivity.this, getString(R.string.error_registration_failed), Toast.LENGTH_LONG).show();
                        }
                        progressDialog.dismiss();
                    }
                });
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        return true;
    }

    private boolean confirmPassword(String password, String passwordConfirmed) {
        return Objects.equals(password, passwordConfirmed);
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
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            new DownloadFileAsyncTask(this).execute(mapTag);
        }
    }
}