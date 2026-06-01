package com.example.tradget;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tradget.model.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Authentication screen — handles:
 *  - Email / password sign-in and sign-up
 *  - Google Sign-In (primary flow)
 *  - University email domain validation (set university_email_domain in strings.xml)
 *  - Firestore user document creation on first sign-up
 */
public class AuthActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────────
    private TextView     tabSignIn, tabSignUp, authButtonLabel, authError;
    private EditText     inputEmail, inputPassword, inputName, inputConfirmPassword;
    private LinearLayout fieldName, fieldConfirmPassword, authButton, googleSignInButton;
    private ProgressBar  authProgress;

    // ── State ──────────────────────────────────────────────────────────────────
    private boolean isSignInMode = true;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseAuth       auth;
    private GoogleSignInClient googleSignInClient;
    private SessionManager     session;
    private FirestoreRepository repo;

    // ── Google Sign-In result launcher ─────────────────────────────────────────
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        try {
                            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(
                                    result.getData()).getResult(ApiException.class);
                            firebaseAuthWithGoogle(account);
                        } catch (ApiException e) {
                            showError("Google sign-in failed: " + e.getMessage());
                            setLoading(false);
                        }
                    });

    // ══════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        auth    = FirebaseAuth.getInstance();
        session = new SessionManager(this);
        repo    = FirestoreRepository.getInstance();

        bindViews();
        setupGoogleSignIn();
        setupClickListeners();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // View binding & click listeners
    // ══════════════════════════════════════════════════════════════════════════

    private void bindViews() {
        tabSignIn              = findViewById(R.id.tabSignIn);
        tabSignUp              = findViewById(R.id.tabSignUp);
        authButtonLabel        = findViewById(R.id.authButtonLabel);
        authError              = findViewById(R.id.authError);
        authProgress           = findViewById(R.id.authProgress);
        inputEmail             = findViewById(R.id.inputEmail);
        inputPassword          = findViewById(R.id.inputPassword);
        inputName              = findViewById(R.id.inputName);
        inputConfirmPassword   = findViewById(R.id.inputConfirmPassword);
        fieldName              = findViewById(R.id.fieldName);
        fieldConfirmPassword   = findViewById(R.id.fieldConfirmPassword);
        authButton             = findViewById(R.id.authButton);
        googleSignInButton     = findViewById(R.id.googleSignInButton);
    }

    private void setupClickListeners() {
        tabSignIn.setOnClickListener(v -> switchMode(true));
        tabSignUp.setOnClickListener(v -> switchMode(false));
        authButton.setOnClickListener(v -> onAuthButtonClicked());
        googleSignInButton.setOnClickListener(v -> startGoogleSignIn());
    }

    private void switchMode(boolean signIn) {
        isSignInMode = signIn;
        clearError();

        // Tab highlight
        tabSignIn.setBackgroundResource(signIn ? R.drawable.bg_toggle_selected : android.R.color.transparent);
        tabSignIn.setTextColor(signIn ? 0xFFFFFFFF : 0xFF757575);
        tabSignUp.setBackgroundResource(signIn ? android.R.color.transparent : R.drawable.bg_toggle_selected);
        tabSignUp.setTextColor(signIn ? 0xFF757575 : 0xFFFFFFFF);

        // Show / hide sign-up-only fields
        int visibility = signIn ? View.GONE : View.VISIBLE;
        fieldName.setVisibility(visibility);
        fieldConfirmPassword.setVisibility(visibility);

        authButtonLabel.setText(signIn ? "Sign In" : "Create Account");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Auth actions
    // ══════════════════════════════════════════════════════════════════════════

    private void onAuthButtonClicked() {
        clearError();
        String email    = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields"); return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters"); return;
        }

        // University email domain check (set "" in strings.xml to allow all)
        String requiredDomain = getString(R.string.university_email_domain);
        if (!requiredDomain.isEmpty() && !email.endsWith(requiredDomain)) {
            showError("Please use your university email (" + requiredDomain + ")"); return;
        }

        if (isSignInMode) {
            signInWithEmail(email, password);
        } else {
            String name    = inputName.getText().toString().trim();
            String confirm = inputConfirmPassword.getText().toString().trim();
            if (name.isEmpty()) { showError("Please enter your name"); return; }
            if (!password.equals(confirm)) { showError("Passwords do not match"); return; }
            signUpWithEmail(name, email, password);
        }
    }

    private void signInWithEmail(String email, String password) {
        setLoading(true);
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> onAuthSuccess(result.getUser(), null))
            .addOnFailureListener(e -> {
                setLoading(false);
                showError("Sign in failed: " + friendlyError(e.getMessage()));
            });
    }

    private void signUpWithEmail(String name, String email, String password) {
        setLoading(true);
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                FirebaseUser fbUser = result.getUser();
                if (fbUser == null) { setLoading(false); return; }
                // Create the Firestore user document
                User user = new User(fbUser.getUid(), name, email);
                repo.createUser(user, new FirestoreRepository.SimpleCallback() {
                    @Override public void onSuccess() {
                        onAuthSuccess(fbUser, name);
                    }
                    @Override public void onFailure(String error) {
                        setLoading(false);
                        showError("Profile creation failed: " + error);
                    }
                });
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                showError("Sign up failed: " + friendlyError(e.getMessage()));
            });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Google Sign-In
    // ══════════════════════════════════════════════════════════════════════════

    private void setupGoogleSignIn() {
        // default_web_client_id is auto-generated from google-services.json
        // by the google-services Gradle plugin
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void startGoogleSignIn() {
        setLoading(true);
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
            .addOnSuccessListener(result -> {
                FirebaseUser fbUser = result.getUser();
                if (fbUser == null) { setLoading(false); return; }

                boolean isNewUser = result.getAdditionalUserInfo() != null
                        && result.getAdditionalUserInfo().isNewUser();

                if (isNewUser) {
                    // Create Firestore doc for first-time Google sign-in
                    User user = new User(fbUser.getUid(),
                            fbUser.getDisplayName() != null ? fbUser.getDisplayName() : "User",
                            fbUser.getEmail() != null ? fbUser.getEmail() : "");
                    repo.createUser(user, new FirestoreRepository.SimpleCallback() {
                        @Override public void onSuccess() {
                            onAuthSuccess(fbUser, fbUser.getDisplayName());
                        }
                        @Override public void onFailure(String error) {
                            setLoading(false);
                            showError("Profile creation failed");
                        }
                    });
                } else {
                    onAuthSuccess(fbUser, null);
                }
            })
            .addOnFailureListener(e -> {
                setLoading(false);
                showError("Google auth failed: " + e.getMessage());
            });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Post-auth
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Called after any successful authentication path.
     * Loads the user profile from Firestore, caches it, then starts MainAppActivity.
     *
     * @param fbUser  the authenticated Firebase user
     * @param nameHint name provided during sign-up (null → load from Firestore)
     */
    private void onAuthSuccess(FirebaseUser fbUser, String nameHint) {
        repo.getUserProfile(fbUser.getUid(), new FirestoreRepository.Callback<com.example.tradget.model.User>() {
            @Override
            public void onSuccess(com.example.tradget.model.User user) {
                session.saveSession(user.getUid(), user.getName(), user.getEmail());
                session.saveProfile(user.getPhone(), user.getRating(),
                        user.getRidesCompleted(), user.getTotalSaved());
                launchMain();
            }
            @Override
            public void onFailure(String error) {
                // Profile might not exist yet (race condition) — use hint data
                String name = nameHint != null ? nameHint : fbUser.getDisplayName();
                if (name == null) name = "User";
                session.saveSession(fbUser.getUid(), name,
                        fbUser.getEmail() != null ? fbUser.getEmail() : "");
                launchMain();
            }
        });
    }

    private void launchMain() {
        startActivity(new Intent(this, MainAppActivity.class));
        finish();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UI helpers
    // ══════════════════════════════════════════════════════════════════════════

    private void setLoading(boolean loading) {
        authProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        authButtonLabel.setText(loading ? "" : (isSignInMode ? "Sign In" : "Create Account"));
        authButton.setClickable(!loading);
        googleSignInButton.setClickable(!loading);
    }

    private void showError(String msg) {
        authError.setText(msg);
        authError.setVisibility(View.VISIBLE);
    }

    private void clearError() {
        authError.setText("");
        authError.setVisibility(View.GONE);
    }

    /** Converts verbose Firebase error messages to friendly short ones. */
    private String friendlyError(String msg) {
        if (msg == null) return "Unknown error";
        if (msg.contains("no user record")) return "No account found with this email";
        if (msg.contains("password is invalid") || msg.contains("INVALID_LOGIN_CREDENTIALS"))
            return "Incorrect password";
        if (msg.contains("email address is already in use")) return "Email already registered";
        if (msg.contains("badly formatted")) return "Invalid email address";
        return msg;
    }
}
