package com.example.travelbuddy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private EditText email, password;
    private Button loginBtn, registerBtn;
    private CheckBox rememberMe;
    private SignInButton googleSignInBtn;
    private FirebaseAuth auth;
    private GoogleSignInClient mGoogleSignInClient;
    private SharedPreferences loginPreferences;
    private SharedPreferences.Editor loginPrefsEditor;
    private Boolean saveLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        registerBtn = findViewById(R.id.registerBtn);
        rememberMe = findViewById(R.id.rememberMe);
        googleSignInBtn = findViewById(R.id.googleSignInBtn);

        loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        loginPrefsEditor = loginPreferences.edit();

        saveLogin = loginPreferences.getBoolean("saveLogin", false);
        if (saveLogin) {
            email.setText(loginPreferences.getString("username", ""));
            password.setText(loginPreferences.getString("password", ""));
            rememberMe.setChecked(true);
        }

        loginBtn.setOnClickListener(v -> {
            String e = email.getText().toString();
            String p = password.getText().toString();

            if (rememberMe.isChecked()) {
                loginPrefsEditor.putBoolean("saveLogin", true);
                loginPrefsEditor.putString("username", e);
                loginPrefsEditor.putString("password", p);
                loginPrefsEditor.commit();
            } else {
                loginPrefsEditor.clear();
                loginPrefsEditor.commit();
            }

            auth.signInWithEmailAndPassword(e, p)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(this, "Login success", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, TipsFeedActivity.class));
                    })
                    .addOnFailureListener(e1 ->
                            Toast.makeText(this, e1.getMessage(), Toast.LENGTH_SHORT).show());
        });

        registerBtn.setOnClickListener(v -> {
            String e = email.getText().toString();
            String p = password.getText().toString();

            auth.createUserWithEmailAndPassword(e, p)
                    .addOnSuccessListener(authResult ->
                            Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e12 ->
                            Toast.makeText(this, e12.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInBtn.setOnClickListener(v -> signIn());
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Google Sign In success", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, TipsFeedActivity.class));
                    } else {
                        Toast.makeText(this, "Google Sign In failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}