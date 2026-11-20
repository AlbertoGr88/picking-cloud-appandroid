package cloud.picking.app.pickingcloud.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.Utils;
import cloud.picking.app.pickingcloud.model.Prodotto;
import cloud.picking.app.pickingcloud.model.UbicazioneTemporanea;

public class UbicazioneActivity extends AppCompatActivity {
    private static final String TAG = "Ubicazione";
    private Button btnAssegna;
    private TextView descrizioneView;
    private ImageView eanClearButton;
    private LinearLayout eanInfo;
    private EditText eanProdotto;
    private EditText eanUbicazione;
    private boolean isTextPasted = false;
    private Prodotto prodotti;
    private ProgressBar progress;
    private SharedPref sharedPref;
    private UbicazioneTemporanea ubicazioniTemporanee;
    private TextView ultimaUbicazioneView;

    private class CercaProdotto extends AsyncTask<String, Void, JSONObject> {
        private CercaProdotto() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            UbicazioneActivity.this.showProgress(true);
            UbicazioneActivity.this.showInfo(null);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            if (params.length <= 0) {
                return null;
            }
            String ean = Utils.sanitizeBarcode(params[0]);
            if (ean.isEmpty()) {
                return null;
            }
            return UbicazioneActivity.this.prodotti.getProdottoByEAN(ean);
        }

        @Override
        protected void onPostExecute(JSONObject prod) {
            super.onPostExecute(prod);
            UbicazioneActivity.this.showProgress(false);
            UbicazioneActivity.this.showInfo(prod);
            if (prod == null) {
                UbicazioneActivity ubicazioneActivity = UbicazioneActivity.this;
                Utils.showToast(ubicazioneActivity, ubicazioneActivity.getString(R.string.errore_prodotto_non_trovato));
                UbicazioneActivity.this.eanProdotto.requestFocus();
                Utils.playErrorTone();
            }
        }
    }

    private class SalvaUbicazione extends AsyncTask<String, Void, Boolean> {
        private SalvaUbicazione() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            UbicazioneActivity.this.showProgress(true);
            UbicazioneActivity.this.showInfo(null);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            if (params.length == 2) {
                String ean = Utils.sanitizeBarcode(params[0]);
                String ubi = Utils.sanitizeBarcode(params[1]);
                JSONObject prod = UbicazioneActivity.this.prodotti.getProdottoByEAN(ean);
                if (prod != null) {
                    String pid = prod.optString("pid", "");
                    if (!pid.isEmpty() && !ubi.isEmpty()) {
                        UbicazioneActivity.this.ubicazioniTemporanee.save(pid, ubi);
                        UbicazioneActivity.this.prodotti.updateUbicazione(pid, ubi);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean saved) {
            super.onPostExecute(saved);
            UbicazioneActivity.this.showProgress(false);
            UbicazioneActivity.this.eanProdotto.getText().clear();
            UbicazioneActivity.this.eanProdotto.requestFocus();
            UbicazioneActivity.this.eanUbicazione.getText().clear();
            UbicazioneActivity.this.showInfo(null);
            UbicazioneActivity ubicazioneActivity = UbicazioneActivity.this;
            if (Boolean.TRUE.equals(saved)) {
                Utils.showToast(ubicazioneActivity, ubicazioneActivity.getString(R.string.nuova_ubicazione_salvata));
            } else {
                Utils.showToast(ubicazioneActivity, ubicazioneActivity.getString(R.string.errore_prodotto_non_trovato));
                Utils.playErrorTone();
            }
        }
    }

    private boolean isTextPasted() {
        return this.isTextPasted;
    }

    private void isTextPasted(boolean value) {
        this.isTextPasted = value;
    }

    private void showInfo(JSONObject prod) {
        if (prod != null) {
            this.eanInfo.setVisibility(View.VISIBLE);
            this.btnAssegna.setVisibility(View.VISIBLE);
            this.eanUbicazione.requestFocus();
            this.descrizioneView.setText(prod.optString("pde", ""));
            this.ultimaUbicazioneView.setText(prod.optString("pub", ""));
        } else {
            this.eanInfo.setVisibility(View.GONE);
            this.descrizioneView.setText("");
            this.ultimaUbicazioneView.setText("");
            this.btnAssegna.setVisibility(View.GONE);
        }
    }

    private void showProgress(boolean show) {
        if (show) {
            this.progress.setVisibility(View.VISIBLE);
        } else {
            this.progress.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ubicazione);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.sharedPref = new SharedPref(this);
        this.prodotti = new Prodotto(this.sharedPref);
        this.ubicazioniTemporanee = new UbicazioneTemporanea(this.sharedPref);

        this.eanProdotto = (EditText) findViewById(R.id.ean_prodotto);
        this.eanUbicazione = (EditText) findViewById(R.id.ean_ubicazione);
        this.eanInfo = (LinearLayout) findViewById(R.id.ean_info);
        this.btnAssegna = (Button) findViewById(R.id.btn_assegna);
        this.progress = (ProgressBar) findViewById(R.id.progress);
        this.descrizioneView = (TextView) this.eanInfo.findViewById(R.id.descrizione);
        this.ultimaUbicazioneView = (TextView) this.eanInfo.findViewById(R.id.ultima_ubicazione);
        this.eanClearButton = (ImageView) findViewById(R.id.ean_clear_button);

        View.OnKeyListener assignKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (!Utils.isScanActionKey(keyCode, event)) {
                    return false;
                }
                String ean = Utils.sanitizeBarcode(UbicazioneActivity.this.eanProdotto.getText().toString());
                String ubi = Utils.sanitizeBarcode(UbicazioneActivity.this.eanUbicazione.getText().toString());
                if (!ean.isEmpty() && !ubi.isEmpty()) {
                    new SalvaUbicazione().execute(ean, ubi);
                }
                return true;
            }
        };
        this.eanUbicazione.setOnKeyListener(assignKeyListener);

        if (!this.sharedPref.isKeyboardVisible()) {
            this.eanClearButton.setVisibility(View.GONE);
            Utils.disableSoftKeyboardForScanner(this.eanProdotto);
            Utils.disableSoftKeyboardForScanner(this.eanUbicazione);

            this.eanProdotto.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (!Utils.isScanActionKey(keyCode, event)) {
                        return false;
                    }
                    String ean = Utils.sanitizeBarcode(UbicazioneActivity.this.eanProdotto.getText().toString());
                    if (!ean.isEmpty()) {
                        new CercaProdotto().execute(ean);
                    }
                    return true;
                }
            });
            this.eanProdotto.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    UbicazioneActivity.this.showInfo(null);
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (start > 0 && count > 1) {
                        UbicazioneActivity.this.eanProdotto.getText().replace(0, start, "");
                    }
                    if (before == 0 && count > 1) {
                        UbicazioneActivity.this.isTextPasted(true);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (UbicazioneActivity.this.isTextPasted()) {
                        UbicazioneActivity.this.isTextPasted(false);
                        String ean = Utils.sanitizeBarcode(UbicazioneActivity.this.eanProdotto.getText().toString());
                        if (!ean.isEmpty()) {
                            new CercaProdotto().execute(ean);
                        }
                    }
                }
            });
        } else {
            this.eanProdotto.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (!Utils.isScanActionKey(keyCode, event)) {
                        return false;
                    }
                    String ean = Utils.sanitizeBarcode(UbicazioneActivity.this.eanProdotto.getText().toString());
                    if (!ean.isEmpty()) {
                        new CercaProdotto().execute(ean);
                    }
                    return true;
                }
            });
            this.eanProdotto.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus || UbicazioneActivity.this.eanProdotto.getText().toString().length() <= 0) {
                        UbicazioneActivity.this.eanClearButton.setVisibility(View.GONE);
                    } else {
                        UbicazioneActivity.this.eanClearButton.setVisibility(View.VISIBLE);
                    }
                }
            });
            this.eanProdotto.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    UbicazioneActivity.this.showInfo(null);
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() > 0) {
                        UbicazioneActivity.this.eanClearButton.setVisibility(View.VISIBLE);
                    } else {
                        UbicazioneActivity.this.eanClearButton.setVisibility(View.GONE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            this.eanClearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UbicazioneActivity.this.eanProdotto.getText().clear();
                    UbicazioneActivity.this.eanProdotto.requestFocus();
                }
            });
        }

        this.btnAssegna.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ean = Utils.sanitizeBarcode(UbicazioneActivity.this.eanProdotto.getText().toString());
                String ubi = Utils.sanitizeBarcode(UbicazioneActivity.this.eanUbicazione.getText().toString());
                if (!ean.isEmpty() && !ubi.isEmpty()) {
                    new SalvaUbicazione().execute(ean, ubi);
                }
            }
        });
    }
}
