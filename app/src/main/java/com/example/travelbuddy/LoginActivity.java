package com.example.travelbuddy;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

/*
 * Écran de connexion — c'est le premier écran que l'utilisateur voit (LAUNCHER).
 * Deux modes : connexion classique (email + mot de passe) ou via Google.
 * Si l'utilisateur n'a pas de compte, il peut basculer en mode inscription.
 * On utilise SharedPreferences pour la case "se souvenir de moi".
 */
public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001; // code de requête pour Google Sign-In

    // Champs du formulaire
    private EditText email, password, name;
    private Button loginBtn, registerBtn;
    private CheckBox rememberMe;
    private SignInButton googleSignInBtn;

    // Firebase Auth pour gérer la connexion
    private FirebaseAuth auth;
    private GoogleSignInClient mGoogleSignInClient;

    // SharedPreferences pour sauvegarder l'email/mdp si "se souvenir" est coché
    private SharedPreferences loginPreferences;
    private SharedPreferences.Editor loginPrefsEditor;
    private Boolean saveLogin;

    private TextInputLayout nameLayout;
    private boolean isRegistering = false; // on commence en mode connexion, pas inscription


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialisation de Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Récupération des vues depuis le layout
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        name = findViewById(R.id.name);
        nameLayout = findViewById(R.id.nameLayout);
        loginBtn = findViewById(R.id.loginBtn);
        registerBtn = findViewById(R.id.registerBtn);
        rememberMe = findViewById(R.id.rememberMe);
        googleSignInBtn = findViewById(R.id.googleSignInBtn);

        // On charge les préférences sauvegardées (email/mdp)
        loginPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        loginPrefsEditor = loginPreferences.edit();

        // Si l'utilisateur avait coché "se souvenir", on pré-remplit les champs
        saveLogin = loginPreferences.getBoolean("saveLogin", false);
        if (saveLogin) {
            email.setText(loginPreferences.getString("username", ""));
            password.setText(loginPreferences.getString("password", ""));
            rememberMe.setChecked(true);
        }

        // Bouton Connexion : on tente de se connecter avec email + mot de passe
        loginBtn.setOnClickListener(v -> {
            String e = email.getText().toString();
            String p = password.getText().toString();

            // Si "se souvenir" est coché, on sauvegarde dans les préférences
            if (rememberMe.isChecked()) {
                loginPrefsEditor.putBoolean("saveLogin", true);
                loginPrefsEditor.putString("username", e);
                loginPrefsEditor.putString("password", p);
                loginPrefsEditor.commit();
            } else {
                // Sinon on efface tout
                loginPrefsEditor.clear();
                loginPrefsEditor.commit();
            }

            // Connexion via Firebase Auth
            auth.signInWithEmailAndPassword(e, p)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(this, "Login success", Toast.LENGTH_SHORT).show();
                        // On envoie vers le fil d'astuces après connexion réussie
                        startActivity(new Intent(this, TipsFeedActivity.class));
                    })
                    .addOnFailureListener(e1 ->
                            Toast.makeText(this, e1.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // Bouton Inscription : deux clics nécessaires
        // 1er clic : on affiche le champ nom et on cache le bouton login
        // 2ème clic : on crée le compte
        registerBtn.setOnClickListener(v -> {
            if (isRegistering) {
                String e = email.getText().toString();
                String p = password.getText().toString();
                String n = name.getText().toString();

                if (n.isEmpty()) {
                    name.setError("Name is required");
                    return;
                }

                // Création du compte dans Firebase Auth
                auth.createUserWithEmailAndPassword(e, p)
                        .addOnSuccessListener(authResult -> {
                            FirebaseUser user = auth.getCurrentUser();
                            // On sauvegarde aussi le profil dans la base de données
                            saveUserToDatabase(user, n);
                        })
                        .addOnFailureListener(e12 ->
                                Toast.makeText(this, e12.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                // Premier clic : on passe en mode inscription
                isRegistering = true;
                nameLayout.setVisibility(View.VISIBLE);
                loginBtn.setVisibility(View.GONE);
                registerBtn.setText("Register");
            }
        });

        // Configuration de Google Sign-In
        // On demande le token d'identité et l'email
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Clic sur le bouton Google → lance le flow de connexion Google
        googleSignInBtn.setOnClickListener(v -> signIn());
    }

    // Lance l'intent Google Sign-In (ouvre la popup de choix de compte)
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Callback quand l'utilisateur revient de la popup Google
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // On récupère le compte Google sélectionné
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // On utilise le token Google pour s'authentifier côté Firebase
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        // On enregistre le user dans la BD au cas où c'est sa première connexion
                        saveUserToDatabase(user, acct.getDisplayName());
                    } else {
                        Toast.makeText(this, "Google Sign In failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Sauvegarde les infos de l'utilisateur dans Firebase Realtime Database
    // sous le noeud "users/{uid}" avec son nom et son email
    private void saveUserToDatabase(FirebaseUser firebaseUser, String name) {
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            DatabaseReference currentUserDb = FirebaseDatabase.getInstance().getReference().child("users").child(userId);

            // On prépare les données sous forme de HashMap
            HashMap<String, Object> userInfo = new HashMap<>();
            userInfo.put("name", name);
            userInfo.put("email", firebaseUser.getEmail());

            currentUserDb.setValue(userInfo).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, TipsFeedActivity.class));
                }
            });
        }
    }
}
