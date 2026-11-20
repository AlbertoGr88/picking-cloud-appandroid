package cloud.picking.app.pickingcloud.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.error.TimeoutError;
import com.android.volley.error.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.fragments.CompletaOrdineColliFragment;
import cloud.picking.app.pickingcloud.fragments.CompletaOrdineFragment;
import cloud.picking.app.pickingcloud.fragments.PickProdottoFragment;
import cloud.picking.app.pickingcloud.fragments.ProdottiOrdineFragment;
import cloud.picking.app.pickingcloud.helper.Ajax;
import cloud.picking.app.pickingcloud.helper.Api;
import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.UpdatesTracker;
import cloud.picking.app.pickingcloud.helper.Utils;
import cloud.picking.app.pickingcloud.helper.VolleyController;
import cloud.picking.app.pickingcloud.model.Collo;
import cloud.picking.app.pickingcloud.model.DaRiassortire;
import cloud.picking.app.pickingcloud.model.Lista;
import cloud.picking.app.pickingcloud.model.Ordine;
import cloud.picking.app.pickingcloud.model.Prodotto;
import cloud.picking.app.pickingcloud.model.UbicazioneTemporanea;

public class PickingActivity extends AppCompatActivity implements ProdottiOrdineFragment.OnListFragmentInteractionListener {
    public static final String ID_ORDINI_KEY = "IdOrdini";
    public static final int RAGGRUPPA_PRODOTTI = 0;
    public static final int SUDDIVIDI_PRODOTTI = 1;
    public static final String TAG = "PickingActivity";

    private MenuItem actionCompletaOrdine;
    private MenuItem actionOrdiniSelezionati;
    private boolean autoAddQty;
    private boolean autoNext;
    private boolean autoSync;
    private Context c;
    private Collo colloModel;
    private String commentoOrdine;
    private DaRiassortire daRiassortire;
    private ArrayList<String> idOrdini;
    private boolean isCompletaOrdineVisible = true;
    private boolean keyboardVisible;
    private JSONObject license;
    private int modalitaPrelievo = -1;
    private String numeriOrdini;
    private Ordine ordineModel;
    private ArrayList<JSONObject> ordini = new ArrayList<>();
    private JSONArray prodotti = new JSONArray();
    private ProgressBar progressBar;
    private SharedPref sharedPref;
    private Toolbar toolbar;
    private TextView toolbarPrioritaTag;
    private JSONObject highestPriorityOrdine = null;
    private String prioritaSubtitlePrefix = "";
    private String lastBaseSubtitle = "";

    private class AggiornaModalitaPrelievo extends AsyncTask<String, Void, Boolean> {
        private AggiornaModalitaPrelievo() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            PickingActivity.this.showProgress(true);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            return PickingActivity.this.ordineModel.setModalitaPrelievo(
                    PickingActivity.this.idOrdini, Integer.parseInt(params[0]));
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            PickingActivity.this.showProgress(false);
        }
    }

    private class AggiornaOrdine extends AsyncTask<String, Void, Boolean> {
        private AggiornaOrdine() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            PickingActivity.this.showProgress(true);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            boolean saved = PickingActivity.this.ordineModel.setProdottoScannerizzato(
                    params[0], params[1], params[2], params[3]);
            if (saved) {
                UpdatesTracker.needToRefreshOrdiniActivity();
                if (PickingActivity.this.modalitaPrelievo == RAGGRUPPA_PRODOTTI) {
                    PickingActivity.this.prodotti = PickingActivity.this.ordineModel.getProdottiRaggruppatiByIdOrdini(PickingActivity.this.idOrdini);
                } else {
                    PickingActivity.this.prodotti = PickingActivity.this.ordineModel.getProdottiByIdOrdini(PickingActivity.this.idOrdini);
                }
                for (int i = 0; i < PickingActivity.this.prodotti.length(); i++) {
                    JSONObject prod = PickingActivity.this.prodotti.optJSONObject(i);
                    if (prod == null) {
                        continue;
                    }
                    String pid = prod.has("pid") ? prod.optString("pid", "") : prod.optString("IdProdotto", "");
                    JSONObject prodAnagrafica = new Prodotto(PickingActivity.this.sharedPref).getByIdProdotto(pid);
                    JSONArray codes = prodAnagrafica != null ? prodAnagrafica.optJSONArray("codes") : prod.optJSONArray("codes");
                    StringBuilder sb = new StringBuilder();
                    sb.append(pid).append(" ");
                    sb.append(prod.optString("BarCode", prod.optString("Codice", ""))).append(" ");
                    if (codes != null) {
                        for (int j = 0; j < codes.length(); j++) {
                            sb.append(codes.optString(j)).append(" ");
                        }
                    }
                    try {
                        prod.put("search_field", sb.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Log.e(TAG, "ERRORE: AggiornaOrdine");
            }
            return saved;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            PickingActivity.this.showProgress(false);
            if (!Boolean.TRUE.equals(result)) {
                Utils.showToast(PickingActivity.this.c, "Errore durante il salvataggio delle modifiche");
                return;
            }
            UpdatesTracker.refreshOrdiniActivity();
            if (PickingActivity.this.isOrdineCompletato()) {
                new AlertDialog.Builder(PickingActivity.this.c)
                        .setTitle("Ordine Completato")
                        .setMessage("Tutti gli articoli sono stati prelevati. Vuoi procedere alla Chiusura Ordine?")
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PickingActivity.this.caricaCompletaOrdineFragment();
                            }
                        })
                        .create()
                        .show();
                return;
            }
            Utils.showToast(PickingActivity.this.c, "Quantità salvata");
            if (PickingActivity.this.autoNext && PickingActivity.this.prodotti.length() > 1) {
                android.support.v4.app.Fragment current = PickingActivity.this.getSupportFragmentManager()
                        .findFragmentByTag(PickProdottoFragment.TAG);
                if (current instanceof PickProdottoFragment) {
                    int nextPos = PickingActivity.this.findNextIncompleteIndex(((PickProdottoFragment) current).getIndex());
                    PickingActivity.this.reloadPickProdottoFragment(nextPos);
                }
            }
        }
    }

    private class CaricaOrdini extends AsyncTask<ArrayList<String>, Void, Boolean> {
        private CaricaOrdini() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            PickingActivity.this.showProgress(true);
        }

        @SafeVarargs
        @Override
        protected final Boolean doInBackground(ArrayList<String>... params) {
            PickingActivity.this.ordineModel = new Ordine(PickingActivity.this.sharedPref).load();
            PickingActivity.this.colloModel = new Collo(PickingActivity.this.sharedPref).load();
            PickingActivity.this.ordini.clear();
            JSONObject bestPrioOrdine = null;
            int bestPrioVal = -1;
            for (int i = 0; i < params[0].size(); i++) {
                JSONObject ordObj = PickingActivity.this.ordineModel.getByIdOrdine(params[0].get(i));
                if (ordObj != null) {
                    PickingActivity.this.ordini.add(ordObj);
                    if (Utils.hasOrderPriority(ordObj)) {
                        int pVal = Utils.getOrderPriorityValue(ordObj);
                        if (bestPrioOrdine == null || pVal > bestPrioVal) {
                            bestPrioVal = pVal;
                            bestPrioOrdine = ordObj;
                        }
                    }
                }
            }
            PickingActivity.this.highestPriorityOrdine = bestPrioOrdine;
            String prioLabel = Utils.getOrderPriorityDisplayLabel(bestPrioOrdine);
            PickingActivity.this.prioritaSubtitlePrefix = !prioLabel.isEmpty() ? ("Priorità: " + prioLabel) : "";
            if (PickingActivity.this.modalitaPrelievo == RAGGRUPPA_PRODOTTI) {
                PickingActivity.this.prodotti = PickingActivity.this.ordineModel.getProdottiRaggruppatiByIdOrdini(params[0]);
            } else {
                PickingActivity.this.prodotti = PickingActivity.this.ordineModel.getProdottiByIdOrdini(params[0]);
            }

            Prodotto prodottoHelper = new Prodotto(PickingActivity.this.sharedPref);
            for (int i = 0; i < PickingActivity.this.prodotti.length(); i++) {
                JSONObject prod = PickingActivity.this.prodotti.optJSONObject(i);
                if (prod == null) {
                    continue;
                }
                String pid = prod.has("pid") ? prod.optString("pid", "") : prod.optString("IdProdotto", "");
                JSONObject prodAnagrafica = prodottoHelper.getByIdProdotto(pid);
                JSONArray codes = prodAnagrafica != null ? prodAnagrafica.optJSONArray("codes") : prod.optJSONArray("codes");
                StringBuilder sb = new StringBuilder();
                sb.append(pid).append(" ");
                sb.append(prod.optString("BarCode", prod.optString("Codice", ""))).append(" ");
                if (codes != null) {
                    for (int j = 0; j < codes.length(); j++) {
                        sb.append(codes.optString(j)).append(" ");
                    }
                }
                try {
                    prod.put("search_field", sb.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            PickingActivity.this.daRiassortire = new DaRiassortire(PickingActivity.this.sharedPref).load();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            Utils.bindPrioritaBadge(PickingActivity.this.toolbarPrioritaTag, PickingActivity.this.highestPriorityOrdine);
            if (!PickingActivity.this.lastBaseSubtitle.isEmpty()) {
                PickingActivity.this.updateHeaderSubtitle(PickingActivity.this.lastBaseSubtitle);
            }
            PickingActivity.this.caricaProdottiOrdineFragment();
            PickingActivity.this.showProgress(false);
        }
    }

    private class CompletaOrdine extends AsyncTask<String, Void, Boolean> {
        private CompletaOrdine() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            PickingActivity.this.showProgress(true);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            return PickingActivity.this.ordineModel.completa(PickingActivity.this.idOrdini, params[0], params[1]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (Boolean.TRUE.equals(result)) {
                UpdatesTracker.needToRefreshOrdiniActivity();
                if (!PickingActivity.this.autoSync) {
                    PickingActivity.this.showProgress(false);
                    PickingActivity.this.finish();
                } else {
                    PickingActivity.this.sync();
                }
            } else {
                PickingActivity.this.showProgress(false);
                Utils.showToast(PickingActivity.this.c, "Errore durante il salvataggio dei dati dell'ordine");
            }
        }
    }

    private class CompletaOrdineColli extends AsyncTask<String, Void, Boolean> {
        private CompletaOrdineColli() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            PickingActivity.this.showProgress(true);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                boolean savedColli = PickingActivity.this.colloModel.setColliOrdine(
                        PickingActivity.this.getIdOrdine(), new JSONArray(params[0]));
                boolean savedOrdine = PickingActivity.this.ordineModel.completa(
                        PickingActivity.this.idOrdini, "0", "");
                return savedColli && savedOrdine;
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (Boolean.TRUE.equals(result)) {
                UpdatesTracker.needToRefreshOrdiniActivity();
                if (!PickingActivity.this.autoSync) {
                    PickingActivity.this.showProgress(false);
                    PickingActivity.this.finish();
                } else {
                    PickingActivity.this.sync();
                }
            } else {
                PickingActivity.this.showProgress(false);
                Utils.showToast(PickingActivity.this.c, "Errore durante il salvataggio dei colli dell'ordine");
            }
        }
    }

    private void caricaProdottiOrdineFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentsContainer, new ProdottiOrdineFragment())
                .commit();
    }

    private void sync() {
        File ordiniFile = new File(this.sharedPref.getFolderDir() + File.separator + "ordini.json");
        File daRiassortireFile = new File(this.sharedPref.getFolderDir() + File.separator + "da_riassortire.json");
        File ubicazioniTempFile = new File(this.sharedPref.getFolderDir() + File.separator + "ubicazioni_temporanee.json");
        File listeFile = new File(this.sharedPref.getFolderDir() + File.separator + "liste.json");
        File colliFile = new File(this.sharedPref.getFolderDir() + File.separator + "colli.json");

        Ajax req = new Ajax(this.c, Api.sync(), new Ajax.Callback() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getBoolean("response")) {
                        PickingActivity.this.sharedPref.lastSync(
                                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date()));
                        new UbicazioneTemporanea(PickingActivity.this.sharedPref).svuota();
                        new Lista(PickingActivity.this.sharedPref).svuotaComplete();
                    }
                    Utils.showToast(PickingActivity.this.c, R.string.ordine_sincronizzato);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                PickingActivity.this.showProgress(false);
                PickingActivity.this.finish();
            }
        }, new Ajax.ErrorCallback() {
            @Override
            public void onError(VolleyError error) {
                if (error != null && error.getClass().equals(TimeoutError.class)) {
                    Utils.showToast(PickingActivity.this.c, R.string.errore_timeout, 1);
                } else {
                    Utils.showToast(PickingActivity.this.c, R.string.errore_sincronizzazione_ordine, 1);
                }
                PickingActivity.this.showProgress(false);
                PickingActivity.this.finish();
            }
        }, new HashMap<String, String>(), 1);

        req.setRetryPolicy(new DefaultRetryPolicy(0, -1, 1.0f));
        req.addFile("ordini", ordiniFile.getPath());
        req.addFile("da_riassortire", daRiassortireFile.getPath());
        req.addFile("ubicazioni_temporanee", ubicazioniTempFile.getPath());
        req.addFile("liste", listeFile.getPath());
        req.addFile("colli", colliFile.getPath());
        req.setShouldCache(false);
        VolleyController.getInstance(this.c).addToRequestQueue(req);
    }

    public void aggiornaModalitaPrelievo(int modalita, String unused) {
        new AggiornaModalitaPrelievo().execute(String.valueOf(modalita));
    }

    public void aggiornaOrdine(String idOrdine, String idRiga, String scannerizzato, String forzaScan) {
        new AggiornaOrdine().execute(idOrdine, idRiga, scannerizzato, forzaScan);
    }

    private void apriCompletaOrdineFragment() {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        if (this.sharedPref.isModuloColliEnabled() && this.idOrdini.size() == 1) {
            tx.replace(R.id.fragmentsContainer, new CompletaOrdineColliFragment());
        } else {
            tx.replace(R.id.fragmentsContainer, new CompletaOrdineFragment());
        }
        tx.addToBackStack(null);
        tx.commit();
    }

    public void caricaCompletaOrdineFragment() {
        StringBuilder mancanoBuilder = new StringBuilder();
        int length = this.prodotti != null ? this.prodotti.length() : 0;
        for (int i = 0; i < length; i++) {
            JSONObject prod = this.prodotti.optJSONObject(i);
            if (prod == null) {
                continue;
            }
            float scannerizzato = Utils.floatRound(
                    Float.parseFloat(prod.optString("Scannerizzato", "0").replace(',', '.')), 3);
            float qty = Utils.floatRound(
                    Float.parseFloat(prod.optString("Qty", "0").replace(',', '.')), 3);
            if (scannerizzato < qty) {
                float mancante = Utils.floatRound(qty - scannerizzato, 3);
                String code = prod.has("pid") ? prod.optString("pid", "") : prod.optString("IdProdotto", "");
                String descr = prod.optString("pde", "").trim();
                String label = !code.isEmpty() && !descr.isEmpty()
                        ? code + " - " + descr
                        : (!code.isEmpty() ? code : descr);
                if (mancanoBuilder.length() > 0) {
                    mancanoBuilder.append('\n');
                }
                mancanoBuilder.append("- ")
                        .append(label)
                        .append(": ")
                        .append(Utils.formatQuantity(mancante, qty))
                        .append(" (evasi ")
                        .append(Utils.formatQuantity(scannerizzato, qty))
                        .append("/")
                        .append(Utils.formatQuantity(qty))
                        .append(")");
            }
        }

        if (mancanoBuilder.length() > 0) {
            String message = getString(R.string.non_hai_prelevato_tutti_i_prodotti, mancanoBuilder.toString());
            new AlertDialog.Builder(this.c)
                    .setTitle(R.string.ordine_incompleto)
                    .setMessage(message)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PickingActivity.this.apriCompletaOrdineFragment();
                        }
                    })
                    .create()
                    .show();
            return;
        }

        apriCompletaOrdineFragment();
    }

    public DaRiassortire getDaRiassortire() {
        return this.daRiassortire;
    }

    public String getIdOrdine() {
        return this.idOrdini.get(0);
    }

    public ArrayList<String> getIdOrdini() {
        return this.idOrdini;
    }

    public JSONArray getProdotti() {
        return this.prodotti;
    }

    public SharedPref getSharedPref() {
        return this.sharedPref;
    }

    public boolean isAutoAddQtyEnabled() {
        return this.autoAddQty;
    }

    public boolean isAutoNextEnabled() {
        return this.autoNext;
    }

    public boolean isKeyboardVisible() {
        return this.keyboardVisible;
    }

    public boolean isModalitaPrelievo() {
        return this.modalitaPrelievo == RAGGRUPPA_PRODOTTI;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            new AlertDialog.Builder(this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.sicuro_di_voler_chiudere_ordine)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PickingActivity.super.onBackPressed();
                        }
                    })
                    .create()
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.c = this;
        SharedPref sp = new SharedPref(this.c);
        this.sharedPref = sp;
        this.keyboardVisible = sp.isKeyboardVisible();
        this.autoNext = this.sharedPref.isAutoNextEnabled();
        this.autoAddQty = this.sharedPref.isAutoAddQtyEnabled();
        this.autoSync = this.sharedPref.isAutoSyncEnabled();
        this.license = this.sharedPref.getLicense();

        setContentView(R.layout.activity_picking);
        this.toolbar = (Toolbar) findViewById(R.id.toolbar);
        this.toolbarPrioritaTag = (TextView) findViewById(R.id.toolbar_priorita_tag);
        this.progressBar = (ProgressBar) findViewById(R.id.progressBar);

        Bundle extras = getIntent().getExtras();
        String activityTitle = null;
        if (extras != null) {
            this.idOrdini = extras.getStringArrayList(ID_ORDINI_KEY);
            this.numeriOrdini = extras.getString("numeri");
            this.commentoOrdine = extras.getString("commento");
            this.modalitaPrelievo = extras.getInt("modalita_prelievo");
            activityTitle = extras.getString("title");
            new CaricaOrdini().execute(this.idOrdini);
            int mp = this.modalitaPrelievo;
            if (mp != -1) {
                aggiornaModalitaPrelievo(mp, null);
            }
        }
        setSupportActionBar(this.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (activityTitle != null) {
                getSupportActionBar().setTitle(activityTitle);
            }
        }
    }

    public void updateHeaderSubtitle(String subtitle) {
        this.lastBaseSubtitle = subtitle != null ? subtitle : "";
        String fullSubtitle = this.lastBaseSubtitle;
        if (this.prioritaSubtitlePrefix != null && !this.prioritaSubtitlePrefix.isEmpty()) {
            if (!fullSubtitle.isEmpty()) {
                fullSubtitle = this.prioritaSubtitlePrefix + " • " + fullSubtitle;
            } else {
                fullSubtitle = this.prioritaSubtitlePrefix;
            }
        }
        if (this.toolbar != null && this.toolbar.getParent() instanceof View) {
            ((View) this.toolbar.getParent()).setVisibility(View.VISIBLE);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(fullSubtitle);
        } else if (this.toolbar != null) {
            this.toolbar.setSubtitle(fullSubtitle);
        }
    }

    public void updateRowsHeader() {
        int total = this.prodotti != null ? this.prodotti.length() : 0;
        int completed = countCompletedRows();
        String rowsLabel = total == 1 ? "1 riga" : total + " righe";
        if (total > 0) {
            updateHeaderSubtitle(rowsLabel + " (" + completed + "/" + total + " completate)");
        } else {
            updateHeaderSubtitle(rowsLabel);
        }
    }

    private int countCompletedRows() {
        if (this.prodotti == null) {
            return 0;
        }
        int completed = 0;
        for (int i = 0; i < this.prodotti.length(); i++) {
            JSONObject prod = this.prodotti.optJSONObject(i);
            if (prod == null) {
                continue;
            }
            float scan = Utils.floatRound(Float.parseFloat(prod.optString("Scannerizzato", "0").replace(',', '.')), 2);
            float qty = Utils.floatRound(Float.parseFloat(prod.optString("Qty", "0").replace(',', '.')), 2);
            if (scan >= qty) {
                completed++;
            }
        }
        return completed;
    }

    public boolean isOrdineCompletato() {
        int total = this.prodotti != null ? this.prodotti.length() : 0;
        return total > 0 && countCompletedRows() == total;
    }

    private int findNextIncompleteIndex(int currentIndex) {
        int total = this.prodotti != null ? this.prodotti.length() : 0;
        if (total <= 1) {
            return 0;
        }
        for (int offset = 1; offset <= total; offset++) {
            int candidate = (currentIndex + offset) % total;
            JSONObject prod = this.prodotti.optJSONObject(candidate);
            if (prod != null) {
                float scan = Utils.floatRound(Float.parseFloat(prod.optString("Scannerizzato", "0").replace(',', '.')), 2);
                float qty = Utils.floatRound(Float.parseFloat(prod.optString("Qty", "0").replace(',', '.')), 2);
                if (scan < qty) {
                    return candidate;
                }
            }
        }
        return (currentIndex + 1) % total;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.picking, menu);
        this.actionCompletaOrdine = menu.findItem(R.id.action_completa_ordine);
        MenuItem actionCommento = menu.findItem(R.id.action_commento_ordine);
        if (actionCommento != null) {
            boolean hasComment = this.commentoOrdine != null
                    && !this.commentoOrdine.trim().isEmpty()
                    && !"null".equalsIgnoreCase(this.commentoOrdine.trim());
            actionCommento.setVisible(hasComment);
        }
        MenuItem ordiniSel = menu.findItem(R.id.action_ordini_selezionati);
        this.actionOrdiniSelezionati = ordiniSel;
        if (ordiniSel != null) {
            if (this.idOrdini != null && this.idOrdini.size() > 1) {
                ordiniSel.setVisible(true);
                if (this.numeriOrdini != null) {
                    ordiniSel.setTitle("Ordini selezionati: " + this.numeriOrdini);
                }
            } else {
                ordiniSel.setVisible(false);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onListFragmentInteraction(JSONObject item, int position) {
        onListFragmentInteraction(position);
    }

    public void onListFragmentInteraction(int position) {
        PickProdottoFragment fragment = new PickProdottoFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        bundle.putInt(PickProdottoFragment.INDEX_KEY, position);
        fragment.setArguments(bundle);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragmentsContainer, fragment, PickProdottoFragment.TAG);
        tx.addToBackStack(PickProdottoFragment.TAG);
        tx.commit();
    }

    public void reloadPickProdottoFragment(int position) {
        reloadPickProdottoFragment(position, null);
    }

    public void reloadPickProdottoFragment(int position, String barcode) {
        android.support.v4.app.Fragment current = getSupportFragmentManager().findFragmentByTag(PickProdottoFragment.TAG);
        if (current instanceof PickProdottoFragment && current.isAdded() && current.getView() != null) {
            ((PickProdottoFragment) current).loadProductAtIndex(position, barcode);
            return;
        }
        getSupportFragmentManager().popBackStackImmediate();
        PickProdottoFragment fragment = new PickProdottoFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("position", position);
        bundle.putInt(PickProdottoFragment.INDEX_KEY, position);
        if (barcode != null) {
            bundle.putString("code", barcode);
        }
        fragment.setArguments(bundle);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragmentsContainer, fragment, PickProdottoFragment.TAG);
        tx.addToBackStack(PickProdottoFragment.TAG);
        tx.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                onBackPressed();
                return true;
            }
            new AlertDialog.Builder(this)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(R.string.sicuro_di_voler_chiudere_ordine)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PickingActivity.this.finish();
                        }
                    })
                    .create()
                    .show();
            return true;
        } else if (id == R.id.action_completa_ordine) {
            caricaCompletaOrdineFragment();
            return true;
        } else if (id == R.id.action_commento_ordine) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.commento_interno);
            if (this.commentoOrdine != null && !"".equals(this.commentoOrdine)) {
                if (this.idOrdini != null && this.idOrdini.size() > 1) {
                    builder.setMessage(Html.fromHtml(this.commentoOrdine));
                } else {
                    builder.setMessage(this.commentoOrdine);
                }
            } else {
                builder.setMessage("Nessun commento");
            }
            builder.setPositiveButton(android.R.string.ok, null).create().show();
            return true;
        } else if (id == R.id.action_ordini_selezionati) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.ordini.size(); i++) {
                JSONObject ordObj = this.ordini.get(i);
                if (ordObj == null) {
                    continue;
                }
                String num = ordObj.optString("IdOrdine", ordObj.optString("ido", ""));
                String cliente = ordObj.optString("DeliveryName", ordObj.optString("dnm", ""));
                String prio = Utils.getOrderPriorityDisplayLabel(ordObj);
                sb.append("<b>Ordine ").append(num).append("</b>");
                if (!prio.isEmpty()) {
                    sb.append(" [").append(prio).append("]");
                }
                if (!cliente.isEmpty()) {
                    sb.append(" - ").append(cliente);
                }
                sb.append("<br>");
            }
            new AlertDialog.Builder(this)
                    .setTitle("Ordini selezionati")
                    .setMessage(Html.fromHtml(sb.toString()))
                    .setPositiveButton(android.R.string.ok, null)
                    .create()
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (this.actionCompletaOrdine != null) {
            this.actionCompletaOrdine.setVisible(this.isCompletaOrdineVisible);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    public void salvaColliOrdine(String colliJson) {
        new CompletaOrdineColli().execute(colliJson);
    }

    public void salvaNumColliNote(String numColli, String note) {
        new CompletaOrdine().execute(numColli, note);
    }

    public void setActionCompletaOrdineVisibility(boolean visible) {
        this.isCompletaOrdineVisible = visible;
        invalidateOptionsMenu();
    }

    public void showProgress(boolean show) {
        this.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void successivo(int currentPosition) {
        getSupportFragmentManager().popBackStack();
        onListFragmentInteraction(currentPosition + 1);
    }
}
