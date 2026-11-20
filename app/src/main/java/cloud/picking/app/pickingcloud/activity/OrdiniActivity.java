package cloud.picking.app.pickingcloud.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.adapter.OrdiniAdapter;
import cloud.picking.app.pickingcloud.helper.SeparatorDecoration;
import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.UpdatesTracker;
import cloud.picking.app.pickingcloud.helper.Utils;
import cloud.picking.app.pickingcloud.model.Ordine;

public class OrdiniActivity extends AppCompatActivity {
    public static final String STATO_CODA = "coda";
    public static final String STATO_COMPLETATI = "completati";
    public static final String STATO_KEY = "stato";
    public static final String TAG = "OrdiniActivity";

    private Context c;
    private String filter = "";
    private OrdiniAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private ProgressBar progress;
    private EditText searchEditText;
    private TextView btnFilterPriorita;
    private MenuItem actionViewOrder;
    private boolean isModalitaPrelievoActive = false;
    private String activePriorityFilterLabel = "";
    private SharedPref sharedPref;
    private Toolbar toolbar;

    private class CaricaOrdini extends AsyncTask<Void, Void, JSONArray> {
        private CaricaOrdini() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            OrdiniActivity.this.progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected JSONArray doInBackground(Void... voids) {
            Ordine ordine = new Ordine(new SharedPref(OrdiniActivity.this.c)).load();
            JSONArray ordini = new JSONArray();
            if (OrdiniActivity.this.filter != null && OrdiniActivity.this.filter.equals(STATO_CODA)) {
                ordini = ordine.getCoda();
            } else if (OrdiniActivity.this.filter != null && OrdiniActivity.this.filter.equals(STATO_COMPLETATI)) {
                ordini = ordine.getCompletati();
            }
            if (ordini == null) {
                ordini = new JSONArray();
            }
            int length = ordini.length();
            for (int i = 0; i < length; i++) {
                JSONObject obj = ordini.optJSONObject(i);
                if (obj == null) {
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                sb.append(obj.optString("IdOrdine", obj.optString("ido", ""))).append(" ");
                sb.append(obj.optString("Numero", "")).append(" ");
                sb.append(obj.optString("DeliveryName", obj.optString("dnm", ""))).append(" ");
                sb.append(obj.optString("DataConsegna", obj.optString("end", ""))).append(" ");
                sb.append(obj.optString("InternalComment", "")).append(" ");
                sb.append(obj.optString("Commento", "")).append(" ");
                sb.append(obj.optString("PrioritaTag", "")).append(" ");
                sb.append(Utils.getOrderPriorityDisplayLabel(obj)).append(" ");
                sb.append(obj.optString("Note", ""));
                try {
                    obj.put("search_field", sb.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return ordini;
        }

        @Override
        protected void onPostExecute(JSONArray result) {
            super.onPostExecute(result);
            OrdiniActivity.this.progress.setVisibility(View.GONE);
            OrdiniActivity.this.mAdapter.setData(result);
            OrdiniActivity.this.updatePriorityFilterButtonState();
            OrdiniActivity.this.mAdapter.getFilter().filter(OrdiniActivity.this.searchEditText.getText().toString());
            OrdiniActivity.this.searchEditText.requestFocus();
        }
    }

    private void updatePriorityFilterButtonState() {
        if (this.btnFilterPriorita == null || this.mAdapter == null) {
            return;
        }
        String mode = this.mAdapter.getPriorityFilterMode();
        boolean isActive = !OrdiniAdapter.FILTER_PRIORITY_ALL.equals(mode);
        if (isActive) {
            this.btnFilterPriorita.setBackground(getDrawable(R.drawable.rectangle_primary_dark));
            int activeColor = ContextCompat.getColor(this.c, R.color.primaryDarkColor);
            this.btnFilterPriorita.setTextColor(activeColor);
            this.btnFilterPriorita.setText(this.activePriorityFilterLabel);
            for (Drawable d : this.btnFilterPriorita.getCompoundDrawablesRelative()) {
                if (d != null) {
                    d.mutate().setColorFilter(activeColor, PorterDuff.Mode.SRC_IN);
                }
            }
            for (Drawable d : this.btnFilterPriorita.getCompoundDrawables()) {
                if (d != null) {
                    d.mutate().setColorFilter(activeColor, PorterDuff.Mode.SRC_IN);
                }
            }
        } else {
            this.btnFilterPriorita.setBackground(getDrawable(R.drawable.rectangle));
            int defaultColor = Color.parseColor("#333333");
            this.btnFilterPriorita.setTextColor(defaultColor);
            this.btnFilterPriorita.setText("");
            for (Drawable d : this.btnFilterPriorita.getCompoundDrawablesRelative()) {
                if (d != null) {
                    d.mutate().setColorFilter(defaultColor, PorterDuff.Mode.SRC_IN);
                }
            }
            for (Drawable d : this.btnFilterPriorita.getCompoundDrawables()) {
                if (d != null) {
                    d.mutate().setColorFilter(defaultColor, PorterDuff.Mode.SRC_IN);
                }
            }
        }
    }

    private void showPriorityFilterDialog() {
        if (this.mAdapter == null) {
            return;
        }
        JSONArray allOrdini = this.mAdapter.getData();
        int totalCount = allOrdini != null ? allOrdini.length() : 0;
        int withPriorityCount = 0;
        int withoutPriorityCount = 0;
        int[] countsByLevel = new int[11];
        String[] labelsByLevel = new String[11];

        for (int i = 0; i < totalCount; i++) {
            JSONObject item = allOrdini.optJSONObject(i);
            if (item == null) {
                continue;
            }
            if (Utils.hasOrderPriority(item)) {
                withPriorityCount++;
                int pVal = Utils.getOrderPriorityValue(item);
                if (pVal >= 1 && pVal <= 10) {
                    countsByLevel[pVal]++;
                    if (labelsByLevel[pVal] == null || labelsByLevel[pVal].isEmpty()) {
                        labelsByLevel[pVal] = Utils.getOrderPriorityDisplayLabel(item);
                    }
                }
            } else {
                withoutPriorityCount++;
            }
        }

        final ArrayList<String> optionTitles = new ArrayList<>();
        final ArrayList<String> optionModes = new ArrayList<>();
        final ArrayList<String> optionShortLabels = new ArrayList<>();

        optionTitles.add(getString(R.string.tutti_gli_ordini) + " (" + totalCount + ")");
        optionModes.add(OrdiniAdapter.FILTER_PRIORITY_ALL);
        optionShortLabels.add("");

        if (withPriorityCount > 0) {
            optionTitles.add(getString(R.string.tutti_con_priorita) + " (" + withPriorityCount + ")");
            optionModes.add(OrdiniAdapter.FILTER_PRIORITY_ANY);
            optionShortLabels.add("PRIO");
        }

        for (int p = 10; p >= 1; p--) {
            if (countsByLevel[p] > 0) {
                String lbl = labelsByLevel[p] != null && !labelsByLevel[p].isEmpty() ? labelsByLevel[p] : ("P" + p);
                String title;
                if (lbl.equalsIgnoreCase("P" + p) || lbl.equals(String.valueOf(p))) {
                    title = "Priorità " + p + " [" + lbl + "] (" + countsByLevel[p] + ")";
                } else {
                    title = lbl + " - Priorità " + p + " (" + countsByLevel[p] + ")";
                }
                optionTitles.add(title);
                optionModes.add(String.valueOf(p));
                optionShortLabels.add(lbl);
            }
        }

        if (withPriorityCount > 0 && withoutPriorityCount > 0) {
            optionTitles.add(getString(R.string.senza_priorita) + " (" + withoutPriorityCount + ")");
            optionModes.add(OrdiniAdapter.FILTER_PRIORITY_NONE);
            optionShortLabels.add("NO PRIO");
        }

        String currentMode = this.mAdapter.getPriorityFilterMode();
        int checkedIndex = 0;
        for (int i = 0; i < optionModes.size(); i++) {
            if (optionModes.get(i).equalsIgnoreCase(currentMode)) {
                checkedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this.c)
                .setTitle(R.string.filtra_per_priorita)
                .setSingleChoiceItems(optionTitles.toArray(new String[0]), checkedIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        OrdiniActivity.this.activePriorityFilterLabel = optionShortLabels.get(which);
                        OrdiniActivity.this.mAdapter.setPriorityFilterMode(optionModes.get(which));
                        OrdiniActivity.this.updatePriorityFilterButtonState();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
    }

    private void apriPickingActivity(ArrayList<String> idOrdini, String numeri, String commento, String title, int modalitaPrelievo) {
        Intent i = new Intent(this.c, PickingActivity.class);
        i.putStringArrayListExtra("IdOrdini", idOrdini);
        i.putExtra("numeri", numeri);
        i.putExtra("commento", commento);
        i.putExtra("title", title);
        i.putExtra("modalita_prelievo", modalitaPrelievo);
        this.c.startActivity(i);
    }

    private void applyOrderFilter(String rawQuery) {
        if (this.mAdapter == null) {
            return;
        }
        String query = Utils.sanitizeBarcode(rawQuery).trim();
        this.mAdapter.getFilter().filter(query);
    }

    private void showOrderModal(final ArrayList<String> idOrdini, final String numeri, final String commento, final String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.c);
        builder.setTitle(R.string.modalita_prelievo)
                .setItems(new String[]{"Raggruppa i prodotti", "Suddividi i prodotti per ordine"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        OrdiniActivity.this.apriPickingActivity(idOrdini, numeri, commento, title, which);
                    }
                });
        builder.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ordini);
        this.c = this;

        this.toolbar = (Toolbar) findViewById(R.id.toolbar);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String stato = extras.getString(STATO_KEY);
            this.filter = stato;
            if (stato != null && stato.equals(STATO_CODA)) {
                this.toolbar.setTitle("Ordini in coda");
            } else if (this.filter != null && this.filter.equals(STATO_COMPLETATI)) {
                this.toolbar.setTitle("Ordini completati");
            }
        }
        setSupportActionBar(this.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SharedPref sp = new SharedPref(this.c);
        this.sharedPref = sp;
        boolean keyboardVisible = sp.isKeyboardVisible();

        this.progress = (ProgressBar) findViewById(R.id.progressBar);
        RecyclerView rv = (RecyclerView) findViewById(R.id.ordini_recycler_view);
        this.mRecyclerView = rv;
        rv.setHasFixedSize(true);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        this.mLayoutManager = lm;
        this.mRecyclerView.setLayoutManager(lm);
        this.mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        this.mRecyclerView.addItemDecoration(new SeparatorDecoration(this, Color.parseColor("#EEEEEE"), 1.0f));

        OrdiniAdapter adapter = new OrdiniAdapter(new JSONArray());
        this.mAdapter = adapter;
        adapter.setOnModalitaPrelievoChange(new OrdiniAdapter.OnModalitaPrelievoChange() {
            @Override
            public void onModalitaPrelievoChange(boolean enabled) {
                OrdiniActivity.this.isModalitaPrelievoActive = enabled;
                if (OrdiniActivity.this.actionViewOrder != null) {
                    OrdiniActivity.this.actionViewOrder.setVisible(enabled);
                }
            }
        });
        adapter.setClickListener(new OrdiniAdapter.ItemClickListener() {
            @Override
            public void onClick(View view, int position) {
                try {
                    if (OrdiniActivity.this.mAdapter.getModalitaPrelievo()) {
                        JSONObject item = OrdiniActivity.this.mAdapter.getItem(position);
                        if (item != null) {
                            boolean nowChecked = !item.optBoolean("checked", false);
                            OrdiniActivity.this.mAdapter.setChecked(position, nowChecked);
                            OrdiniActivity.this.mAdapter.notifyItemChanged(position);
                        }
                        return;
                    }
                    JSONObject item = OrdiniActivity.this.mAdapter.getItem(position);
                    if (item == null) {
                        return;
                    }
                    ArrayList<String> idOrdini = new ArrayList<>();
                    idOrdini.add(item.optString("ido", ""));
                    String numero = item.optString("IdOrdine", item.optString("ido", ""));
                    String commento = item.optString("InternalComment", item.optString("Note", "")).trim();
                    String title = "Ordine " + numero + " - " + item.optString("DeliveryName", item.optString("dnm", ""));
                    OrdiniActivity.this.apriPickingActivity(idOrdini, numero, commento, title, -1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                OrdiniActivity.this.mAdapter.setModalitaPrelievo(true);
                OrdiniActivity.this.mAdapter.setChecked(position, true);
                OrdiniActivity.this.mAdapter.notifyItemChanged(position);
            }
        });

        new CaricaOrdini().execute();
        this.mRecyclerView.setAdapter(this.mAdapter);

        this.btnFilterPriorita = (TextView) findViewById(R.id.btn_filter_priorita);
        if (this.btnFilterPriorita != null) {
            this.btnFilterPriorita.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OrdiniActivity.this.showPriorityFilterDialog();
                }
            });
            updatePriorityFilterButtonState();
        }

        EditText search = (EditText) findViewById(R.id.searchEditText);
        this.searchEditText = search;
        search.setBackground(getDrawable(R.drawable.rectangle));
        this.searchEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    OrdiniActivity.this.searchEditText.setBackground(OrdiniActivity.this.getDrawable(R.drawable.rectangle_primary_dark));
                } else {
                    OrdiniActivity.this.searchEditText.setBackground(OrdiniActivity.this.getDrawable(R.drawable.rectangle));
                }
            }
        });

        if (!keyboardVisible) {
            Utils.disableSoftKeyboardForScanner(this.searchEditText);
        }

        this.searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_NULL) {
                    OrdiniActivity.this.applyOrderFilter(OrdiniActivity.this.searchEditText.getText().toString());
                    return true;
                }
                return false;
            }
        });

        this.searchEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event != null && Utils.isScanKeyCode(keyCode)) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        OrdiniActivity.this.applyOrderFilter(OrdiniActivity.this.searchEditText.getText().toString());
                    }
                    return true;
                }
                return false;
            }
        });

        this.searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String raw = s.toString();
                String cleaned = Utils.sanitizeBarcode(raw);
                if (!raw.equals(cleaned)) {
                    s.replace(0, s.length(), cleaned);
                    return;
                }
                OrdiniActivity.this.applyOrderFilter(cleaned);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ordini, menu);
        this.actionViewOrder = menu.findItem(R.id.action_view_order);
        if (this.actionViewOrder != null) {
            this.actionViewOrder.setVisible(this.isModalitaPrelievoActive);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (this.mAdapter != null && this.mAdapter.getModalitaPrelievo()) {
                this.mAdapter.setModalitaPrelievo(false);
                return true;
            }
            finish();
            return true;
        }
        if (item.getItemId() != R.id.action_view_order) {
            return super.onOptionsItemSelected(item);
        }
        SparseBooleanArray checked = this.mAdapter.getItemStateArray();
        int size = checked.size();
        if (size <= 0) {
            Utils.showToast(this.c, R.string.seleziona_un_ordine);
            return false;
        }
        StringBuilder numeriBuilder = new StringBuilder();
        StringBuilder commentoBuilder = new StringBuilder();
        String singleCustomerName = "";
        String singleRawComment = "";
        ArrayList<String> idOrdini = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int pos = checked.keyAt(i);
            if (checked.get(pos)) {
                JSONObject ord = this.mAdapter.getItem(pos);
                if (ord == null) {
                    continue;
                }
                String ido = ord.optString("ido", ord.optString("IdOrdine", ""));
                String numOrdine = ord.optString("IdOrdine", ord.optString("Numero", ido));
                String deliveryName = ord.optString("DeliveryName", ord.optString("dnm", ""));
                String prioLabel = Utils.getOrderPriorityDisplayLabel(ord);
                idOrdini.add(ido);
                singleCustomerName = deliveryName;
                if (numeriBuilder.length() > 0) {
                    numeriBuilder.append(", ");
                }
                numeriBuilder.append(numOrdine);
                String comm = ord.optString("InternalComment", ord.optString("Commento", ord.optString("Note", ""))).trim();
                singleRawComment = comm;
                commentoBuilder.append("<b>Ordine ").append(numOrdine);
                if (!prioLabel.isEmpty()) {
                    commentoBuilder.append(" [").append(prioLabel).append("]");
                }
                commentoBuilder.append("</b><br>");
                if (!comm.isEmpty()) {
                    commentoBuilder.append(comm).append("<br><br>");
                } else {
                    commentoBuilder.append("Nessun commento<br><br>");
                }
            }
        }
        if (idOrdini.isEmpty()) {
            Utils.showToast(this.c, R.string.seleziona_un_ordine);
            return false;
        }
        String numeri = numeriBuilder.toString();
        String commento = idOrdini.size() == 1 ? singleRawComment : commentoBuilder.toString();
        String title = idOrdini.size() == 1
                ? ("Ordine " + numeri + (!singleCustomerName.isEmpty() ? " - " + singleCustomerName : ""))
                : ("Ordini " + numeri);
        if (idOrdini.size() > 1) {
            try {
                int modalita = -2;
                int idx = 0;
                while (true) {
                    if (idx >= idOrdini.size()) {
                        break;
                    }
                    JSONObject savedOrd = new Ordine(this.sharedPref).load().getByIdOrdine(idOrdini.get(idx));
                    int mp = savedOrd != null ? savedOrd.getInt("modalita_prelievo") : -1;
                    if (modalita != -2 && modalita != mp) {
                        modalita = -1;
                        break;
                    }
                    modalita = mp;
                    idx++;
                }
                if (modalita != -1 && modalita != -2) {
                    apriPickingActivity(idOrdini, numeri, commento, title, modalita);
                    return false;
                }
                showOrderModal(idOrdini, numeri, commento, title);
                return false;
            } catch (JSONException e) {
                Log.d(TAG, "Nessuna modalita prelievo settata per gli ordini selezionati");
                showOrderModal(idOrdini, numeri, commento, title);
                return false;
            }
        }
        apriPickingActivity(idOrdini, numeri, commento, title, -1);
        return false;
    }

    @Override
    public void onBackPressed() {
        if (this.mAdapter != null && this.mAdapter.getModalitaPrelievo()) {
            this.mAdapter.setModalitaPrelievo(false);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (UpdatesTracker.isToRefreshOrdiniActivity()) {
            this.mAdapter.resetItemStateArray();
            new CaricaOrdini().execute();
            UpdatesTracker.cancelRefreshOrdiniActivity();
        }
    }
}
