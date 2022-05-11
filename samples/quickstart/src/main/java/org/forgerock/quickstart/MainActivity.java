package org.forgerock.quickstart;

import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.forgerock.android.auth.FRAuth;
import org.forgerock.android.auth.FRUser;
import org.forgerock.android.auth.Logger;
import org.forgerock.android.auth.Node;
import org.forgerock.android.auth.NodeListener;

import android.widget.Toast;
import android.hardware.biometrics.BiometricPrompt;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.KeyguardManager;
import android.os.CancellationSignal;
import android.view.View;
import android.content.DialogInterface;


public class MainActivity extends AppCompatActivity implements NodeListener<FRUser> {

    private static String TAG = MainActivity.class.getName();

    private TextView status;
    private Button loginButton;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Logger.set(Logger.Level.DEBUG);
        FRAuth.start(this);

        checkBiometricSupport();

        status = findViewById(R.id.status);
        loginButton = findViewById(R.id.login);
        logoutButton = findViewById(R.id.logout);
        updateStatus();

        loginButton.setOnClickListener(view -> FRUser.login(getApplicationContext(), this));
        logoutButton.setOnClickListener(view -> {
            FRUser.getCurrentUser().logout();
            updateStatus();
        });
    }

    private Boolean checkBiometricSupport() {

        KeyguardManager keyguardManager =
                (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

        PackageManager packageManager = this.getPackageManager();

        if (!keyguardManager.isKeyguardSecure()) {
            notifyUser("Lock screen security not enabled in Settings");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.USE_BIOMETRIC) !=
                PackageManager.PERMISSION_GRANTED) {

            notifyUser("Fingerprint authentication permission not enabled");
            return false;
        }

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
        {
            return true;
        }

        return true;
    }

    private void updateStatus() {
        runOnUiThread(() -> {
            if (FRUser.getCurrentUser() == null) {
                status.setText("User is not authenticated");
                loginButton.setEnabled(true);
                logoutButton.setEnabled(false);
            } else {
                status.setText("User is authenticated");
                loginButton.setEnabled(false);
                logoutButton.setEnabled(true);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onSuccess(FRUser result) {
            BiometricPrompt biometricPrompt = new BiometricPrompt.Builder(this)
                    .setTitle("Biometric Demo")
                    .setSubtitle("Authentication is required to continue")
                    .setDescription("This app uses biometric authentication to protect your data.")
                    .setNegativeButton("Cancel", this.getMainExecutor(),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    notifyUser("Authentication cancelled");
                                    FRUser.getCurrentUser().logout();
                                    updateStatus();
                                    System.out.println(FRUser.getCurrentUser());
                                }
                            })
                    .build();

            biometricPrompt.authenticate(getCancellationSignal(), getMainExecutor(),
                    getAuthenticationCallback());
        //updateStatus();
        System.out.println(FRUser.getCurrentUser());
    }

    @Override
    public void onException(Exception e) {
        Logger.error(TAG, e.getMessage(), e);
    }

    @Override
    public void onCallbackReceived(Node node) {
        NodeDialogFragment fragment = NodeDialogFragment.newInstance(node);
        fragment.show(getSupportFragmentManager(), NodeDialogFragment.class.getName());
    }

    private void notifyUser(String message) {
        Toast.makeText(this,
                message,
                Toast.LENGTH_LONG).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private BiometricPrompt.AuthenticationCallback getAuthenticationCallback() {

        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              CharSequence errString) {
                notifyUser("Authentication error: " + errString);
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationHelp(int helpCode,
                                             CharSequence helpString) {
                super.onAuthenticationHelp(helpCode, helpString);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                FRUser.getCurrentUser().logout();
                updateStatus();
                System.out.println(FRUser.getCurrentUser());
            }

            @Override
            public void onAuthenticationSucceeded(
                    BiometricPrompt.AuthenticationResult result) {
                notifyUser("Authentication Succeeded");
                super.onAuthenticationSucceeded(result);

                updateStatus();
                System.out.println(FRUser.getCurrentUser());
            }
        };
    }

    private CancellationSignal cancellationSignal;
    private CancellationSignal getCancellationSignal() {

        cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(new
                                                       CancellationSignal.OnCancelListener() {
                                                           @Override
                                                           public void onCancel() {
                                                               notifyUser("Cancelled via signal");
                                                           }
                                                       });
        return cancellationSignal;
    }



}