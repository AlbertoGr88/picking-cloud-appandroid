package cloud.picking.app.pickingcloud.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.activity.PickingActivity;
import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.Utils;
import cloud.picking.app.pickingcloud.model.DaRiassortire;

public class PickProdottoFragment extends Fragment {
    public static final String INDEX_KEY = "index_key";
    public static final String TAG = "PickProdotto";
    private MenuItem actionProdottoDaRiassortire;
    private MenuItem actionProdottoRiassortito;
    private Button aggiungi;
    private Context c;
    private String code;
    private boolean daRiassortire;
    private boolean compactScreen = false;
    private DaRiassortire daRiassortireModel;
    private TextView descrizione;
    private TextView idprodotto;
    private int index;
    private boolean isSearchTextPasted = false;
    private JSONObject item;
    private TextView notes;
    private PickingActivity pickingActivity;
    private View pickForm;
    private Button precedente;
    private JSONArray prodotti;
    private Float qty;
    private TextView qtyView;
    private EditText quantita;
    private Button salva;
    private TextView scannerizzata;
    private Float scannerizzato;
    private EditText search;
    private SharedPref sharedPref;
    private Button sottrai;
    private Button successivo;
    private TextView ubicazione;
    private String forzaScan = "0";
    private boolean isForzaScanEnabled = true;
    private boolean isUpdatingQuantitaText = false;

    private interface ConfirmationCallback {
        void onPositiveButtonClick();
    }

    private class Riassortimento extends AsyncTask<Integer, Void, Void> {
        private Riassortimento() {
        }

        @Override
        protected Void doInBackground(Integer... params) {
            String pid = PickProdottoFragment.this.item.has("pid")
                    ? PickProdottoFragment.this.item.optString("pid", "")
                    : PickProdottoFragment.this.item.optString("IdProdotto", "");
            PickProdottoFragment.this.daRiassortireModel.setRiassortimento(pid, params[0]);
            return null;
        }
    }

    private Float getMaxAddableQty() {
        float ordered = this.qty != null ? this.qty : 0.0f;
        float picked = this.scannerizzato != null ? this.scannerizzato : 0.0f;
        float remaining = Utils.floatRound(ordered - picked, 3);
        return remaining > 0.0f ? remaining : 0.0f;
    }

    private void notifyMaxQtyExceeded(Float maxAddable) {
        Utils.playErrorTone();
        Utils.showToast(this.c, "Quantità massima indicabile: " + Utils.formatQuantity(maxAddable, this.qty));
    }

    private void addQuantita(Float delta) {
        Float maxAddable = getMaxAddableQty();
        Float current = getFloatQuantita();
        float base = current == null ? 0.0f : current;
        float next = Utils.floatRound(base + delta, 3);
        if (delta > 0.0f && base >= maxAddable) {
            setQuantita(maxAddable);
            notifyMaxQtyExceeded(maxAddable);
            return;
        }
        if (next > maxAddable) {
            next = maxAddable;
            notifyMaxQtyExceeded(maxAddable);
        } else if (next < 0.0f) {
            next = 0.0f;
        }
        setQuantita(next);
    }

    private void aggiornaOrdine(String forzaScan) {
        try {
            Float addQty = getFloatQuantita();
            if (addQty != null) {
                Float maxAddable = getMaxAddableQty();
                if (addQty > maxAddable) {
                    addQty = maxAddable;
                }
                if (addQty < 0.0f) {
                    addQty = 0.0f;
                }
                float baseScanned = this.scannerizzato != null ? this.scannerizzato : 0.0f;
                float newTotal = Utils.floatRound(baseScanned + addQty, 3);
                if (this.qty != null && newTotal > this.qty) {
                    newTotal = this.qty;
                }
                this.scannerizzato = newTotal;
                this.item.put("Scannerizzato", String.valueOf(this.scannerizzato));
                this.scannerizzata.setText(Utils.formatQuantity(this.scannerizzato, this.qty));
                updateScannerizzataStyle();
                setQuantita(0.0f);
            }
            String ido = this.item.has("ido")
                    ? this.item.optString("ido", "")
                    : this.item.optString("IdOrdine", "");
            String rigaId = this.item.has("IdRigaOrdine")
                    ? this.item.optString("IdRigaOrdine", "")
                    : this.item.optString("IdRiga", "");
            this.pickingActivity.aggiornaOrdine(
                    ido,
                    rigaId,
                    String.valueOf(this.scannerizzato != null ? this.scannerizzato : 0.0f),
                    forzaScan);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateScannerizzataStyle() {
        if (this.scannerizzato != null && this.qty != null && this.scannerizzato.equals(this.qty)) {
            this.scannerizzata.setBackgroundResource(this.compactScreen ? R.drawable.rectangle_xs : R.drawable.rectangle);
            this.scannerizzata.setTextColor(Color.parseColor("#000000"));
        } else {
            this.scannerizzata.setBackgroundResource(this.compactScreen ? R.drawable.rectangle_primary_dark_xs : R.drawable.rectangle_primary_dark);
            this.scannerizzata.setTextColor(ContextCompat.getColor(this.c, R.color.lightRed));
        }
    }

    public int getIndex() {
        return this.index;
    }

    private void showPickForm(boolean show, String isForzaScan) {
        this.forzaScan = isForzaScan != null ? isForzaScan : "0";
        if (this.pickForm == null || this.salva == null) {
            return;
        }
        if (show) {
            this.pickForm.setVisibility(View.VISIBLE);
            this.salva.setVisibility(View.VISIBLE);
            this.salva.setEnabled(true);
        } else {
            this.pickForm.setVisibility(View.INVISIBLE);
            this.salva.setVisibility(View.INVISIBLE);
            this.salva.setEnabled(false);
        }
    }

    private Float getFloatQuantita() {
        String text = this.quantita.getText().toString().trim().replace(',', '.');
        if (Utils.isFloat(text)) {
            return Utils.floatRound(Float.parseFloat(text), 3);
        }
        return null;
    }

    private boolean isSearchTextPasted() {
        return this.isSearchTextPasted;
    }

    private void isSearchTextPasted(boolean value) {
        this.isSearchTextPasted = value;
    }

    private void loadMenuButtons() {
        if (this.actionProdottoRiassortito == null || this.actionProdottoDaRiassortire == null) {
            return;
        }
        if (getDaRiassortire()) {
            this.actionProdottoRiassortito.setVisible(true);
            this.actionProdottoDaRiassortire.setVisible(false);
        } else {
            this.actionProdottoDaRiassortire.setVisible(true);
            this.actionProdottoRiassortito.setVisible(false);
        }
    }

    private void focusSearchBox() {
        if (this.search == null) {
            return;
        }
        this.search.setFocusableInTouchMode(true);
        this.search.requestFocus();
        this.search.post(new Runnable() {
            @Override
            public void run() {
                if (PickProdottoFragment.this.search != null) {
                    PickProdottoFragment.this.search.requestFocus();
                    if (!PickProdottoFragment.this.sharedPref.isKeyboardVisible()) {
                        Utils.hideSoftKeyboard(PickProdottoFragment.this.search);
                    }
                }
            }
        });
    }

    /**
     * Aggiorna i campi del prodotto direttamente in questa schermata senza chiudere e riaprire il fragment.
     */
    public void loadProductAtIndex(int newIndex, String scannedCode) {
        this.prodotti = this.pickingActivity.getProdotti();
        int total = this.prodotti != null ? this.prodotti.length() : 0;
        if (total <= 0 || newIndex < 0 || newIndex >= total) {
            return;
        }
        this.index = newIndex;
        Bundle args = getArguments();
        if (args != null) {
            args.putInt(INDEX_KEY, newIndex);
            args.putInt("position", newIndex);
        }
        this.item = this.prodotti.optJSONObject(this.index);
        try {
            if (this.item != null) {
                this.descrizione.setText(this.item.optString("pde", ""));
                String notesStr = this.item.has("notes") ? this.item.optString("notes", "") : this.item.optString("Notes", "");
                if (!notesStr.isEmpty()) {
                    this.notes.setText(notesStr);
                    this.notes.setVisibility(View.VISIBLE);
                } else {
                    this.notes.setVisibility(View.GONE);
                }
                String itemPid = this.item.has("pid") ? this.item.optString("pid", "") : this.item.optString("IdProdotto", "");
                this.idprodotto.setText(itemPid);
                this.ubicazione.setText(this.item.optString("pub", ""));

                this.qty = Utils.floatRound(Float.valueOf(this.item.optString("Qty", "0").replace(',', '.')), 3);
                this.scannerizzato = Utils.floatRound(Float.valueOf(this.item.optString("Scannerizzato", "0").replace(',', '.')), 3);

                this.qtyView.setText(Utils.formatQuantity(this.qty));
                this.scannerizzata.setText(Utils.formatQuantity(this.scannerizzato, this.qty));

                updateScannerizzataStyle();
                setQuantita(0.0f);
                setDaRiassortire(this.daRiassortireModel.isProdottoDaRiassortire(itemPid));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (this.isForzaScanEnabled) {
            showPickForm(false, "0");
        } else {
            showPickForm(true, "0");
        }

        loadMenuButtons();
        this.pickingActivity.invalidateOptionsMenu();
        this.pickingActivity.updateHeaderSubtitle("Prodotto " + (this.index + 1) + " di " + total);

        this.search.getText().clear();
        if (scannedCode != null && !scannedCode.isEmpty()) {
            this.search.setText(scannedCode);
            search(scannedCode);
        } else {
            focusSearchBox();
        }
    }

    private void search(String rawBarcode) {
        String barcode = Utils.sanitizeBarcode(rawBarcode);
        this.search.getText().clear();
        focusSearchBox();
        if (barcode.isEmpty()) {
            return;
        }

        boolean found = false;
        int i = 0;
        while (true) {
            if (i >= this.prodotti.length()) {
                break;
            }
            try {
                JSONObject prod = this.prodotti.getJSONObject(i);
                boolean matched = false;
                String prodPid = prod.has("pid") ? prod.optString("pid", "") : prod.optString("IdProdotto", "");
                String prodBarcode = prod.optString("BarCode", "");
                if (prodPid.equalsIgnoreCase(barcode) || (!prodBarcode.isEmpty() && prodBarcode.equalsIgnoreCase(barcode))) {
                    matched = true;
                } else {
                    JSONArray codes = prod.optJSONArray("codes");
                    if (codes != null) {
                        for (int j = 0; j < codes.length(); j++) {
                            if (Utils.sanitizeBarcode(codes.optString(j, "")).equalsIgnoreCase(barcode)) {
                                matched = true;
                                break;
                            }
                        }
                    }
                }
                if (matched) {
                    found = true;
                    if (i != this.index) {
                        loadProductAtIndex(i, barcode);
                    } else {
                        showPickForm(true, "0");
                        if (this.sharedPref.isAutoAddQty()) {
                            Float maxAddable = getMaxAddableQty();
                            if (maxAddable <= 0.0f) {
                                notifyMaxQtyExceeded(maxAddable);
                            } else {
                                addQuantita(maxAddable < 1.0f ? maxAddable : 1.0f);
                                salvaQuantita("0");
                            }
                        }
                        focusSearchBox();
                    }
                    break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            i++;
        }

        if (!found) {
            Utils.showToast(this.c, "Prodotto con codice " + barcode + " non trovato");
            Utils.playErrorTone();
            if (this.sharedPref.isSoundOnError()) {
                Utils.playErrorSound(this.c, R.raw.error);
            }
            focusSearchBox();
        }
    }

    private void setQuantita(Float value) {
        Float rounded = Utils.floatRound(value, 3);
        Float maxAddable = getMaxAddableQty();
        if (rounded < 0.0f) {
            rounded = 0.0f;
        } else if (rounded > maxAddable) {
            rounded = maxAddable;
        }
        this.isUpdatingQuantitaText = true;
        try {
            String formatted = Utils.formatQuantity(rounded, this.qty);
            this.quantita.setText(formatted);
            this.quantita.setSelection(formatted.length());
        } finally {
            this.isUpdatingQuantitaText = false;
        }
    }

    private void showAlertDialog(int titleRes, String message, final ConfirmationCallback callback) {
        new AlertDialog.Builder(this.c)
                .setTitle(titleRes)
                .setMessage(Html.fromHtml(message))
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onPositiveButtonClick();
                    }
                })
                .create()
                .show();
    }

    public void clear() {
        this.search.getText().clear();
        this.descrizione.setText("");
        this.notes.setText("");
        this.idprodotto.setText("");
        this.ubicazione.setText("");
        this.scannerizzata.setText("");
        this.quantita.getText().clear();
    }

    public boolean getDaRiassortire() {
        return this.daRiassortire;
    }

    public void setDaRiassortire(boolean value) {
        this.daRiassortire = value;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.c = getContext();
        this.sharedPref = new SharedPref(this.c);
        this.pickingActivity = (PickingActivity) getActivity();
        this.daRiassortireModel = this.pickingActivity.getDaRiassortire();

        View view = inflater.inflate(R.layout.fragment_pick_prodotto, container, false);
        this.search = (EditText) view.findViewById(R.id.searchEditText);
        this.descrizione = (TextView) view.findViewById(R.id.pick_descrizione);
        this.notes = (TextView) view.findViewById(R.id.pick_notes);
        this.idprodotto = (TextView) view.findViewById(R.id.pick_idprodotto);
        this.ubicazione = (TextView) view.findViewById(R.id.pick_ubicazione);
        this.qtyView = (TextView) view.findViewById(R.id.pick_qty);
        this.scannerizzata = (TextView) view.findViewById(R.id.pick_scannerizzata);
        this.quantita = (EditText) view.findViewById(R.id.quantita);
        this.aggiungi = (Button) view.findViewById(R.id.aggiungi);
        this.sottrai = (Button) view.findViewById(R.id.sottrai);
        this.salva = (Button) view.findViewById(R.id.salva);
        this.successivo = (Button) view.findViewById(R.id.successivo);
        this.precedente = (Button) view.findViewById(R.id.precedente);
        this.pickForm = view.findViewById(R.id.pick_form);
        this.aggiungi.setClickable(true);

        TextView labelQta = (TextView) view.findViewById(R.id.labelQta);
        if (labelQta != null) {
            labelQta.setText(this.pickingActivity.isModalitaPrelievo() ? "Q.tà" : "Q.tà Ordine");
        }

        WindowManager wm = (WindowManager) this.c.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            if (size.y < 1000) {
                this.compactScreen = true;
                this.qtyView.setBackgroundResource(R.drawable.rectangle_xs);
                this.scannerizzata.setBackgroundResource(R.drawable.rectangle_primary_dark_xs);
                ViewGroup.LayoutParams lp = this.pickForm.getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) {
                    ((ViewGroup.MarginLayoutParams) lp).setMargins(0, 0, 0, 0);
                    this.pickForm.setLayoutParams(lp);
                }
            }
        }

        if (!this.sharedPref.isKeyboardVisible()) {
            Utils.disableSoftKeyboardForScanner(this.search);
        }

        this.search.setNextFocusDownId(R.id.searchEditText);
        this.search.setNextFocusForwardId(R.id.searchEditText);
        focusSearchBox();
        this.search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    PickProdottoFragment.this.search(PickProdottoFragment.this.search.getText().toString());
                    return true;
                }
                return false;
            }
        });
        this.search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (Utils.isScanActionKey(keyCode, event)) {
                    PickProdottoFragment.this.search(PickProdottoFragment.this.search.getText().toString());
                    return true;
                }
                if (event != null && Utils.isScanKeyCode(keyCode)) {
                    PickProdottoFragment.this.focusSearchBox();
                    return true;
                }
                return false;
            }
        });
        this.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (start == 0 && before == 0 && count > 1) {
                    PickProdottoFragment.this.isSearchTextPasted(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (PickProdottoFragment.this.isSearchTextPasted()) {
                    PickProdottoFragment.this.isSearchTextPasted(false);
                    PickProdottoFragment.this.search(s.toString());
                }
            }
        });

        JSONObject auth = this.sharedPref.getAuth();
        this.isForzaScanEnabled = auth == null
                || auth.optInt("app_forza_scan", 1) == 1
                || "true".equalsIgnoreCase(auth.optString("app_forza_scan", ""));
        if (this.isForzaScanEnabled) {
            View.OnClickListener forzaScanClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (PickProdottoFragment.this.pickForm.getVisibility() == View.VISIBLE) {
                        return;
                    }
                    new AlertDialog.Builder(PickProdottoFragment.this.c)
                            .setTitle("Conferma")
                            .setMessage("Vuoi procedere all'inserimento della quantità anche se non hai scannerizzato il prodotto?")
                            .setNegativeButton(android.R.string.no, null)
                            .setPositiveButton("SI (sconsigliato)", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    PickProdottoFragment.this.showPickForm(true, "1");
                                }
                            })
                            .create()
                            .show();
                }
            };
            this.idprodotto.setOnClickListener(forzaScanClickListener);
            this.idprodotto.setFocusable(false);
            this.idprodotto.setFocusableInTouchMode(false);
            if (this.idprodotto.getParent() instanceof View) {
                View codRow = (View) this.idprodotto.getParent();
                codRow.setOnClickListener(forzaScanClickListener);
                codRow.setFocusable(false);
                codRow.setFocusableInTouchMode(false);
            }
        }

        int initialIndex = getArguments() != null ? getArguments().getInt(INDEX_KEY, 0) : 0;
        loadProductAtIndex(initialIndex, null);

        this.quantita.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (PickProdottoFragment.this.isUpdatingQuantitaText) {
                    return;
                }
                String text = s.toString().trim().replace(',', '.');
                if (text.isEmpty() || ".".equals(text)) {
                    return;
                }
                if (Utils.isFloat(text)) {
                    Float entered = Utils.floatRound(Float.parseFloat(text), 3);
                    Float maxAddable = PickProdottoFragment.this.getMaxAddableQty();
                    if (entered > maxAddable) {
                        PickProdottoFragment.this.setQuantita(maxAddable);
                        PickProdottoFragment.this.notifyMaxQtyExceeded(maxAddable);
                    }
                }
            }
        });

        this.aggiungi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickProdottoFragment.this.addQuantita(1.0f);
            }
        });
        this.sottrai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickProdottoFragment.this.addQuantita(-1.0f);
            }
        });
        this.salva.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickProdottoFragment.this.salvaQuantita();
            }
        });

        final int totalProdotti = this.prodotti != null ? this.prodotti.length() : 0;
        if (totalProdotti > 1) {
            this.precedente.setEnabled(true);
            this.precedente.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int prevIndex = (PickProdottoFragment.this.index - 1 + totalProdotti) % totalProdotti;
                    PickProdottoFragment.this.loadProductAtIndex(prevIndex, null);
                }
            });
            this.successivo.setEnabled(true);
            this.successivo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int nextIndex = (PickProdottoFragment.this.index + 1) % totalProdotti;
                    PickProdottoFragment.this.loadProductAtIndex(nextIndex, null);
                }
            });
        } else {
            this.precedente.setEnabled(false);
            this.successivo.setEnabled(false);
        }

        if (getArguments().containsKey("code")) {
            this.code = getArguments().getString("code");
            getArguments().remove("code");
        }
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        try {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.action_prodotto_da_riassortire) {
                showAlertDialog(
                        R.string.segnala_riassortimento,
                        getString(R.string.segnala_riassortimento_messaggio,
                                "<b>" + this.item.optString("pde", "") + "</b>"),
                        new ConfirmationCallback() {
                            @Override
                            public void onPositiveButtonClick() {
                                PickProdottoFragment.this.setDaRiassortire(true);
                                PickProdottoFragment.this.loadMenuButtons();
                                new Riassortimento().execute(DaRiassortire.DA_RIASSORTIRE);
                            }
                        });
            } else if (itemId == R.id.action_prodotto_riassortito) {
                showAlertDialog(
                        R.string.segnala_come_riassortito,
                        getString(R.string.segnala_come_riassortito_messaggio,
                                "<b>" + this.item.optString("pde", "") + "</b>"),
                        new ConfirmationCallback() {
                            @Override
                            public void onPositiveButtonClick() {
                                PickProdottoFragment.this.setDaRiassortire(false);
                                PickProdottoFragment.this.loadMenuButtons();
                                new Riassortimento().execute(DaRiassortire.RIASSORTITO);
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        this.actionProdottoDaRiassortire = menu.findItem(R.id.action_prodotto_da_riassortire);
        this.actionProdottoRiassortito = menu.findItem(R.id.action_prodotto_riassortito);
        loadMenuButtons();
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onStart() {
        super.onStart();
        int total = this.prodotti != null ? this.prodotti.length() : 0;
        if (total > 0) {
            this.pickingActivity.updateHeaderSubtitle("Prodotto " + (this.index + 1) + " di " + total);
        }
        if (this.code != null) {
            this.search.setText(this.code);
            search(this.code);
            this.code = null;
        }
        focusSearchBox();
    }

    @Override
    public void onResume() {
        super.onResume();
        focusSearchBox();
    }

    public void salvaQuantita() {
        salvaQuantita(this.forzaScan);
    }

    public void salvaQuantita(final String forzaScan) {
        Float currentQty = getFloatQuantita();
        if (currentQty == null || currentQty < 0.0f) {
            Utils.showToast(this.c, "La quantità inserita non è valida");
            Utils.playErrorTone();
            return;
        }
        Float maxAddable = getMaxAddableQty();
        if (currentQty > maxAddable) {
            setQuantita(maxAddable);
            notifyMaxQtyExceeded(maxAddable);
            return;
        }
        aggiornaOrdine(forzaScan);
    }
}
