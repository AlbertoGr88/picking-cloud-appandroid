package cloud.picking.app.pickingcloud.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.error.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.helper.Ajax;
import cloud.picking.app.pickingcloud.helper.Api;
import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.VolleyController;

public class LoginActivity extends AppCompatActivity {
    public static final String TAG = "LoginActivity";
    private Context c;
    private String device_id;
    private TextView device_tv;
    private EditText mCodeView;
    // TODO: [TEST V2] Rimuovere mCustomUrlSwitch, mCustomUrlLayout, mCustomUrlView al termine dei test v2
    private SwitchCompat mCustomUrlSwitch;
    private TextInputLayout mCustomUrlLayout;
    private EditText mCustomUrlView;
    private View mLoginFormView;
    private EditText mPasswordView;
    private View mProgressView;
    private EditText mUsernameView;
    private SharedPref sharedPref;
    private TextView versione;

    private void attemptLogin() {
        this.mUsernameView.setError(null);
        this.mPasswordView.setError(null);
        this.mCodeView.setError(null);
        if (this.mCustomUrlView != null) {
            this.mCustomUrlView.setError(null);
        }

        final String codice_licenza = this.mCodeView.getText().toString().trim();
        String username = this.mUsernameView.getText().toString().trim();
        String password = this.mPasswordView.getText().toString();
        boolean useCustomUrl = this.mCustomUrlSwitch != null && this.mCustomUrlSwitch.isChecked();
        String customUrl = (useCustomUrl && this.mCustomUrlView != null)
                ? this.mCustomUrlView.getText().toString().trim()
                : "";

        boolean cancel = false;
        View focusView = null;

        if (useCustomUrl) {
            if (TextUtils.isEmpty(customUrl)) {
                this.mCustomUrlView.setError(getString(R.string.errore_campo_obbligatorio));
                focusView = this.mCustomUrlView;
                cancel = true;
            } else if (!Api.isValidCustomUrl(customUrl)) {
                this.mCustomUrlView.setError(getString(R.string.errore_url_non_valido));
                focusView = this.mCustomUrlView;
                cancel = true;
            }
        }
        if (TextUtils.isEmpty(password)) {
            this.mPasswordView.setError(getString(R.string.errore_campo_obbligatorio));
            focusView = this.mPasswordView;
            cancel = true;
        }
        if (TextUtils.isEmpty(username)) {
            this.mUsernameView.setError(getString(R.string.errore_campo_obbligatorio));
            focusView = this.mUsernameView;
            cancel = true;
        }
        if (TextUtils.isEmpty(codice_licenza)) {
            this.mCodeView.setError(getString(R.string.errore_campo_obbligatorio));
            focusView = this.mCodeView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
            return;
        }

        showProgress(true);
        // Configura il sottodominio API corretto per tutte le licenze ("RAGSTORE" -> "ragstore", "TEST"/"DEV" -> "dev", altre -> "app")
        Api.configureForLicense(codice_licenza);
        // Se l'opzione URL personalizzato (Test v2) è attiva, memorizza e usa l'URL inserito manualmente
        if (useCustomUrl) {
            this.sharedPref.setCustomApiUrl(customUrl);
        } else {
            this.sharedPref.setCustomApiUrl(null);
        }

        Ajax req = new Ajax(this.c, Api.login(), new Ajax.Callback() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getBoolean("isauth") && !response.isNull("auth")) {
                        JSONObject auth = response.getJSONObject("auth");
                        auth.put("codice_licenza", codice_licenza.toLowerCase());
                        LoginActivity.this.sharedPref.setAuth(auth);
                        LoginActivity.this.sharedPref.setLicense(response.optJSONObject("license"));
                        LoginActivity.this.sharedPref.setFolderDir(
                                LoginActivity.this.c.getFilesDir().getAbsolutePath()
                                        + File.separator + codice_licenza.toLowerCase()
                                        + "_" + auth.getString("id"));
                        LoginActivity.this.initAppFileStructure();
                        LoginActivity.this.startActivity(new Intent(LoginActivity.this.c, MainActivity.class));
                        LoginActivity.this.finish();
                    }
                    Log.i(TAG, response.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                LoginActivity.this.showProgress(false);
            }
        }, new Ajax.ErrorCallback() {
            @Override
            public void onError(VolleyError error) {
                LoginActivity.this.showProgress(false);
            }
        });
        req.addStringParam("codice_licenza", codice_licenza);
        req.addStringParam("username", username);
        req.addStringParam("password", password);
        req.addStringParam("device_id", this.device_id);
        VolleyController.getInstance(this.c).addToRequestQueue(req);
    }

    private void initAppFileStructure() throws IOException {
        File dir = new File(this.sharedPref.getFolderDir());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File ordiniFile = new File(this.sharedPref.getFolderDir(), "ordini.json");
        if (!ordiniFile.exists()) {
            ordiniFile.createNewFile();
            PrintWriter pw = new PrintWriter(new FileWriter(ordiniFile));
            pw.write("[]");
            pw.close();
        }
        File daRiassortireFile = new File(this.sharedPref.getFolderDir(), "da_riassortire.json");
        if (!daRiassortireFile.exists()) {
            daRiassortireFile.createNewFile();
            PrintWriter pw = new PrintWriter(new FileWriter(daRiassortireFile));
            pw.write("[]");
            pw.close();
        }
        File ubicazioniTempFile = new File(this.sharedPref.getFolderDir(), "ubicazioni_temporanee.json");
        if (!ubicazioniTempFile.exists()) {
            ubicazioniTempFile.createNewFile();
            PrintWriter pw = new PrintWriter(new FileWriter(ubicazioniTempFile));
            pw.write("[]");
            pw.close();
        }
        File listeFile = new File(this.sharedPref.getFolderDir(), "liste.json");
        if (!listeFile.exists()) {
            listeFile.createNewFile();
            PrintWriter pw = new PrintWriter(new FileWriter(listeFile));
            pw.write("{}");
            pw.close();
        }
        File colliFile = new File(this.sharedPref.getFolderDir() + File.separator + "colli.json");
        if (!colliFile.exists()) {
            colliFile.createNewFile();
            PrintWriter pw = new PrintWriter(new FileWriter(colliFile));
            pw.write("{}");
            pw.close();
        }
        File prodottiDir = new File(this.sharedPref.getFolderDir() + File.separator + "prodotti");
        if (!prodottiDir.exists()) {
            prodottiDir.mkdirs();
        }
        File ubicazioniDir = new File(this.sharedPref.getFolderDir() + File.separator + "ubicazioni");
        if (!ubicazioniDir.exists()) {
            ubicazioniDir.mkdirs();
        }
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        this.mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        this.mLoginFormView.animate()
                .setDuration(shortAnimTime)
                .alpha(show ? 0.0f : 1.0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        LoginActivity.this.mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                    }
                });

        this.mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        this.mProgressView.animate()
                .setDuration(shortAnimTime)
                .alpha(show ? 1.0f : 0.0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        LoginActivity.this.mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        this.c = this;
        this.sharedPref = new SharedPref(this);
        this.device_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        this.mCodeView = (EditText) findViewById(R.id.codice_licenza);
        this.mUsernameView = (EditText) findViewById(R.id.username);
        this.mPasswordView = (EditText) findViewById(R.id.password);

        // TODO: [TEST V2] Rimuovere inizializzazione campo URL personalizzato al termine dei test v2
        this.mCustomUrlSwitch = (SwitchCompat) findViewById(R.id.custom_url_switch);
        this.mCustomUrlLayout = (TextInputLayout) findViewById(R.id.custom_url_layout);
        this.mCustomUrlView = (EditText) findViewById(R.id.custom_url);
        String savedCustomUrl = this.sharedPref.getCustomApiUrl();
        if (savedCustomUrl != null && !savedCustomUrl.isEmpty()) {
            this.mCustomUrlSwitch.setChecked(true);
            this.mCustomUrlLayout.setVisibility(View.VISIBLE);
            this.mCustomUrlView.setText(savedCustomUrl);
        }
        this.mCustomUrlSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LoginActivity.this.mCustomUrlLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                if (!isChecked) {
                    LoginActivity.this.sharedPref.setCustomApiUrl(null);
                    LoginActivity.this.mCustomUrlView.setError(null);
                } else {
                    LoginActivity.this.mCustomUrlView.requestFocus();
                }
            }
        });

        this.mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id != 6 && id != 0) {
                    return false;
                }
                LoginActivity.this.attemptLogin();
                return true;
            }
        });

        ((Button) findViewById(R.id.login_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginActivity.this.attemptLogin();
            }
        });

        this.mLoginFormView = findViewById(R.id.login_form);
        this.mProgressView = findViewById(R.id.login_progress);

        this.versione = (TextView) findViewById(R.id.versioneTextView);
        this.versione.setText("VERSIONE 1.7");

        this.device_tv = (TextView) findViewById(R.id.deviceTextView);
        this.device_tv.setText("Device ID: " + this.device_id);
    }
}
