package cloud.picking.app.pickingcloud.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.error.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.helper.Ajax;
import cloud.picking.app.pickingcloud.helper.Api;
import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.Utils;
import cloud.picking.app.pickingcloud.helper.VolleyController;
import cloud.picking.app.pickingcloud.model.Collo;
import cloud.picking.app.pickingcloud.model.DaRiassortire;
import cloud.picking.app.pickingcloud.model.Lista;
import cloud.picking.app.pickingcloud.model.Ordine;
import cloud.picking.app.pickingcloud.model.Prodotto;
import cloud.picking.app.pickingcloud.model.Ubicazione;
import cloud.picking.app.pickingcloud.model.UbicazioneTemporanea;

public class SyncActivity extends AppCompatActivity {
    private static final String JSON_PARAM = "json";
    private static final String TAG = "Sync";
    private static final int ordiniStep = 10;
    private Context c;
    private Collo colli;
    private DaRiassortire da_riassortire;
    private Snackbar errorSnackbar = null;
    private int indiceInizialeOrdiniDaInviare = 0;
    private Lista liste;
    private int numTuttiGliOrdiniDaInviare = 0;
    private Ordine ordine;
    private ProgressBar progress;
    private SharedPref sharedPref;
    private Snackbar successSnackbar = null;
    private boolean syncCompleta = false;
    private Button sync_button;
    private RelativeLayout sync_layout;
    private Button sync_orders_button;
    private TextView sync_time;
    private JSONArray tuttiGliOrdiniDaInviare = null;
    private UbicazioneTemporanea ubicazioneTemporanea;

    private void download() {
        Log.d(TAG, "Inizio Download");
        if (!Utils.isConnected(this.c)) {
            Utils.showToast(this.c, getString(R.string.non_sei_connesso));
            return;
        }
        showProgress(true);
        JSONObject payload = new JSONObject();
        JSONArray anagrafiche = new JSONArray();
        if (isSyncCompleta()) {
            anagrafiche.put("prodotti");
            anagrafiche.put("ubicazioni");
        }
        anagrafiche.put("ordini");
        Log.d(TAG, "Anagrafiche Richieste: " + anagrafiche.toString());
        try {
            payload.put("anagrafiche", anagrafiche);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Ajax req = new Ajax(this.c, Api.download(), new Ajax.Callback() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                if (response.optBoolean("response", false)) {
                    SyncActivity.this.sharedPref.setLicense(response.optJSONObject("license"));
                    SyncActivity.this.sharedPref.isFirstSyncPerformed(true);
                    try {
                        if (SyncActivity.this.isSyncCompleta()) {
                            JSONArray prodotti = response.optJSONArray("prodotti");
                            if (prodotti == null) {
                                prodotti = new JSONArray();
                            }
                            Prodotto prodottoModel = new Prodotto(SyncActivity.this.sharedPref);
                            Utils.writeToFile(prodottoModel.getPath(), prodotti.toString());
                            prodottoModel.reload();

                            JSONArray daRiassortire = response.optJSONArray("da_riassortire");
                            if (daRiassortire == null) {
                                daRiassortire = new JSONArray();
                            }
                            DaRiassortire daRiassortireModel = new DaRiassortire(SyncActivity.this.sharedPref);
                            Utils.createFile(daRiassortireModel.getPath());
                            Utils.writeToFile(daRiassortireModel.getPath(), daRiassortire.toString());
                            daRiassortireModel.reload();

                            JSONArray ubicazioni = response.optJSONArray("ubicazioni");
                            if (ubicazioni == null) {
                                ubicazioni = new JSONArray();
                            }
                            Utils.writeToFile(new Ubicazione(SyncActivity.this.sharedPref).getPath(), ubicazioni.toString());
                            SyncActivity.this.ubicazioneTemporanea.clear();
                        }

                        JSONArray ordini = response.optJSONArray("ordini");
                        if (ordini == null) {
                            ordini = new JSONArray();
                        }
                        Ordine ordineModel = new Ordine(SyncActivity.this.sharedPref);
                        Utils.writeToFile(ordineModel.getPath(), ordini.toString());
                        ordineModel.reload();

                        JSONObject colliObj = response.optJSONObject("colli");
                        if (colliObj == null) {
                            colliObj = new JSONObject();
                        }
                        Collo colloModel = new Collo(SyncActivity.this.sharedPref);
                        Utils.createOrWriteFile(colloModel.getPath(), colliObj.toString());
                        colloModel.load();
                        SyncActivity.this.sharedPref.setColliUpdated(false);

                        SyncActivity.this.showProgress(false);
                        SyncActivity.this.showSuccess();
                        Log.d(TAG, "Fine Download");
                    } catch (Exception e) {
                        e.printStackTrace();
                        SyncActivity.this.showProgress(false);
                        SyncActivity.this.showErrorDownload();
                    }
                } else {
                    SyncActivity.this.showProgress(false);
                    SyncActivity.this.showErrorDownload();
                }
            }
        }, new Ajax.ErrorCallback() {
            @Override
            public void onError(VolleyError error) {
                Log.d(TAG, "Fine Download con Errore");
                SyncActivity.this.showProgress(false);
                SyncActivity.this.showErrorDownload();
            }
        });

        req.addStringParam(JSON_PARAM, payload.toString());
        VolleyController.getInstance(this.c).addToRequestQueue(req);
    }

    private boolean isSyncCompleta() {
        return this.syncCompleta;
    }

    private boolean isSyncOrdini() {
        return this.syncCompleta;
    }

    private void setSyncCompleta() {
        this.syncCompleta = true;
    }

    private void setSyncOrdini() {
        this.syncCompleta = false;
    }

    private void showError() {
        this.errorSnackbar = Snackbar.make(findViewById(R.id.syncCoordinatorLayout), R.string.errore_riprova, Snackbar.LENGTH_LONG);
        this.errorSnackbar.getView().setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        this.errorSnackbar.show();
    }

    private void showErrorDownload() {
        this.errorSnackbar = Snackbar.make(findViewById(R.id.syncCoordinatorLayout), R.string.errore_download_riprova, Snackbar.LENGTH_LONG);
        this.errorSnackbar.getView().setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        this.errorSnackbar.show();
    }

    private void showProgress(boolean show) {
        if (show) {
            if (this.progress != null) {
                this.progress.setVisibility(View.VISIBLE);
            }
            if (this.sync_layout != null) {
                this.sync_layout.setVisibility(View.GONE);
            }
            if (this.successSnackbar != null) {
                this.successSnackbar.dismiss();
            }
            if (this.errorSnackbar != null) {
                this.errorSnackbar.dismiss();
            }
        } else {
            if (this.progress != null) {
                this.progress.setVisibility(View.GONE);
            }
            if (this.sync_layout != null) {
                this.sync_layout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showSuccess() {
        this.successSnackbar = Snackbar.make(findViewById(R.id.syncCoordinatorLayout), R.string.sincronizzazione_completata, Snackbar.LENGTH_INDEFINITE);
        this.successSnackbar.getView().setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        this.successSnackbar.setAction(android.R.string.ok, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncActivity.this.successSnackbar.dismiss();
            }
        });
        this.successSnackbar.setActionTextColor(getResources().getColor(android.R.color.white));
        this.successSnackbar.show();
    }

    private void sync() {
        Log.d(TAG, "Inizio Upload");
        if (!Utils.isConnected(this.c)) {
            Utils.showToast(this.c, getString(R.string.non_sei_connesso));
            return;
        }
        showProgress(true);
        this.ordine.load();
        this.tuttiGliOrdiniDaInviare = this.ordine.getDaSincronizzare();
        this.numTuttiGliOrdiniDaInviare = this.tuttiGliOrdiniDaInviare.length();
        this.indiceInizialeOrdiniDaInviare = 0;
        sync(true);
    }

    private void sync(final boolean isFirstCall) {
        Ajax req = new Ajax(this.c, Api.sync(), new Ajax.Callback() {
            @Override
            public void onResponse(JSONObject response) throws JSONException {
                if (response.optBoolean("response", false)) {
                    SyncActivity.this.sharedPref.setLicense(response.optJSONObject("license"));
                    if (isFirstCall && SyncActivity.this.isSyncCompleta()) {
                        Log.d(TAG, "Liste Sincronizzate");
                        try {
                            JSONObject listeObj = response.optJSONObject("liste");
                            if (listeObj != null) {
                                Lista listaModel = new Lista(SyncActivity.this.sharedPref);
                                listaModel.saveAll(listeObj);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    SyncActivity.this.indiceInizialeOrdiniDaInviare += ordiniStep;
                    if (SyncActivity.this.indiceInizialeOrdiniDaInviare < SyncActivity.this.numTuttiGliOrdiniDaInviare) {
                        SyncActivity.this.sync(false);
                    } else {
                        Log.d(TAG, "Fine Upload");
                        String lastSync = response.optString("ultima_sincronizzazione", "");
                        if (!lastSync.isEmpty()) {
                            SyncActivity.this.sharedPref.lastSync(lastSync);
                        }
                        if (SyncActivity.this.sync_time != null) {
                            SyncActivity.this.sync_time.setText(SyncActivity.this.sharedPref.lastSync());
                        }
                        SyncActivity.this.download();
                    }
                } else {
                    SyncActivity.this.showProgress(false);
                    SyncActivity.this.showError();
                }
            }
        }, new Ajax.ErrorCallback() {
            @Override
            public void onError(VolleyError error) {
                SyncActivity.this.showProgress(false);
                SyncActivity.this.showError();
            }
        });

        JSONObject payload = new JSONObject();
        try {
            JSONArray anagrafiche = new JSONArray();
            if (isFirstCall && isSyncCompleta()) {
                this.ubicazioneTemporanea.load();
                payload.put("ubicazioni_temporanee", this.ubicazioneTemporanea.getForSync());
                anagrafiche.put("ubicazioni");

                this.liste.load();
                payload.put("liste", this.liste.getForSync());
                anagrafiche.put("liste");

                this.da_riassortire.load();
                payload.put("da_riassortire", this.da_riassortire.get());
                anagrafiche.put("da_riassortire");
            }

            if (this.numTuttiGliOrdiniDaInviare > 0) {
                int end = this.indiceInizialeOrdiniDaInviare + ordiniStep;
                if (end > this.numTuttiGliOrdiniDaInviare) {
                    end = this.numTuttiGliOrdiniDaInviare;
                }
                JSONArray batchOrdini = new JSONArray();
                JSONObject batchColli = new JSONObject();
                this.colli.load();
                JSONObject allColli = this.colli.get();
                for (int i = this.indiceInizialeOrdiniDaInviare; i < end; i++) {
                    JSONObject ordObj = this.tuttiGliOrdiniDaInviare.getJSONObject(i);
                    batchOrdini.put(ordObj);
                    String ido = ordObj.optString("ido", "");
                    if (!ido.isEmpty() && allColli.has(ido)) {
                        batchColli.put(ido, allColli.get(ido));
                    }
                }
                payload.put("ordini", batchOrdini);
                payload.put("colli", batchColli);
                anagrafiche.put("ordini");
            } else if (isFirstCall && this.sharedPref.hasColliUpdated()) {
                this.colli.load();
                payload.put("colli", this.colli.get());
                anagrafiche.put("ordini");
            }

            payload.put("anagrafiche", anagrafiche);
            Log.d(TAG, "Anagrafiche Inviate: " + anagrafiche.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        req.addStringParam(JSON_PARAM, payload.toString());
        req.setRetryPolicy(new DefaultRetryPolicy(10000, 0, 1.0f));
        VolleyController.getInstance(this.c).addToRequestQueue(req);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.c = this;
        this.sharedPref = new SharedPref(this.c);
        this.ordine = new Ordine(this.sharedPref);
        this.ubicazioneTemporanea = new UbicazioneTemporanea(this.sharedPref);
        this.colli = new Collo(this.sharedPref);
        this.liste = new Lista(this.sharedPref);
        this.da_riassortire = new DaRiassortire(this.sharedPref);

        this.progress = (ProgressBar) findViewById(R.id.progressBar);
        this.sync_layout = (RelativeLayout) findViewById(R.id.sync_layout);
        this.sync_button = (Button) findViewById(R.id.sync_button);
        this.sync_orders_button = (Button) findViewById(R.id.sync_orders_button);
        this.sync_time = (TextView) findViewById(R.id.sync_time);

        this.sync_time.setText(this.sharedPref.lastSync());

        this.sync_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncActivity.this.sync_button.setEnabled(false);
                new AlertDialog.Builder(SyncActivity.this.c)
                        .setMessage(R.string.avviare_sincronizzazione_completa)
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SyncActivity.this.sync_button.setEnabled(true);
                            }
                        })
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SyncActivity.this.sync_button.setEnabled(true);
                                SyncActivity.this.setSyncCompleta();
                                SyncActivity.this.sync();
                            }
                        })
                        .create()
                        .show();
            }
        });

        this.sync_orders_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SyncActivity.this.sync_orders_button.setEnabled(false);
                new AlertDialog.Builder(SyncActivity.this.c)
                        .setMessage(R.string.avviare_sincronizzazione_ordini)
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SyncActivity.this.sync_orders_button.setEnabled(true);
                            }
                        })
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SyncActivity.this.sync_orders_button.setEnabled(true);
                                SyncActivity.this.setSyncOrdini();
                                SyncActivity.this.sync();
                            }
                        })
                        .create()
                        .show();
            }
        });

        JSONObject auth = this.sharedPref.getAuth();
        boolean canSyncOrders = auth == null
                || "1".equals(auth.optString("app_ordini", "0"))
                || "true".equalsIgnoreCase(auth.optString("app_ordini", "false"));
        this.sync_orders_button.setVisibility(canSyncOrders ? View.VISIBLE : View.GONE);
    }
}
