package cloud.picking.app.pickingcloud.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.activity.ListeActivity;
import cloud.picking.app.pickingcloud.adapter.ProdottiListaAdapter;
import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.Utils;
import cloud.picking.app.pickingcloud.model.Lista;
import cloud.picking.app.pickingcloud.model.Prodotto;

public class ProdottiListaFragment extends Fragment {
    public static final String KEY_LISTA = "KEY_LISTA";
    public static String TAG = "ProdottiLista";
    private ProdottiListaAdapter adapter;
    private Context c;
    private Switch completaSwitch;
    private boolean completata;
    private String descrizione;
    private ImageView eanClearButton;
    private EditText eanProdotto;
    private View eanProdottoContainer;
    private boolean isSearchTextPasted = false;
    private String key;
    private JSONObject lista;
    private Lista listaModel;
    private ListeActivity listeActivity;
    private String prodottoId = "0";
    private Prodotto prodottoModel;
    private RecyclerView recyclerView;
    private SearchView searchView;
    private SharedPref sharedPref;

    private class AggiungiProdottoLista extends AsyncTask<String, Void, Boolean> {
        private AggiungiProdottoLista() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProdottiListaFragment.this.listeActivity.showProgress(true);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            boolean saved = ProdottiListaFragment.this.listaModel.aggiungiProdotto(
                    ProdottiListaFragment.this.key,
                    params[0],
                    params[1],
                    ProdottiListaFragment.this.descrizione,
                    params[2].equals("1"),
                    ProdottiListaFragment.this.prodottoId);
            if (saved) {
                ProdottiListaFragment.this.lista = ProdottiListaFragment.this.listaModel.getByKey(ProdottiListaFragment.this.key);
                ProdottiListaFragment.this.listeActivity.setListe(ProdottiListaFragment.this.listaModel.getComplete());
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!Boolean.TRUE.equals(result)) {
                Utils.showToast(ProdottiListaFragment.this.c, "Errore durante l'aggiunta del prodotto");
            }
            if (ProdottiListaFragment.this.lista != null && ProdottiListaFragment.this.adapter != null) {
                JSONArray prodotti = ProdottiListaFragment.this.lista.optJSONArray("prodotti");
                ProdottiListaFragment.this.adapter.setData(prodotti != null ? prodotti : new JSONArray());
            }
            ProdottiListaFragment.this.eanProdotto.getText().clear();
            ProdottiListaFragment.this.eanProdotto.requestFocus();
            ProdottiListaFragment.this.listeActivity.showProgress(false);
        }
    }

    private class CambiaStatoLista extends AsyncTask<Boolean, Void, Boolean> {
        private boolean targetCompleta;

        private CambiaStatoLista() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProdottiListaFragment.this.listeActivity.showProgress(true);
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            this.targetCompleta = params[0];
            boolean ok = ProdottiListaFragment.this.listaModel.cambiaStato(ProdottiListaFragment.this.key, this.targetCompleta);
            if (ok) {
                ProdottiListaFragment.this.completata = this.targetCompleta;
                ProdottiListaFragment.this.lista = ProdottiListaFragment.this.listaModel.getByKey(ProdottiListaFragment.this.key);
                ProdottiListaFragment.this.listeActivity.setListe(ProdottiListaFragment.this.listaModel.getComplete());
            }
            return ok;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!Boolean.TRUE.equals(result)) {
                Utils.showToast(ProdottiListaFragment.this.c, "Errore durante il cambio stato della lista");
                ProdottiListaFragment.this.syncCompletaSwitch();
            } else {
                ProdottiListaFragment.this.setListaCompleta(this.targetCompleta);
                ProdottiListaFragment.this.eanProdotto.getText().clear();
                ProdottiListaFragment.this.eanProdotto.requestFocus();
            }
            ProdottiListaFragment.this.listeActivity.showProgress(false);
        }
    }

    private class CancellaProdottoLista extends AsyncTask<String, Void, Boolean> {
        private CancellaProdottoLista() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProdottiListaFragment.this.listeActivity.showProgress(true);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            boolean deleted = ProdottiListaFragment.this.listaModel.cancellaProdottoPerPosizione(
                    ProdottiListaFragment.this.key, params[0]);
            if (deleted) {
                ProdottiListaFragment.this.lista = ProdottiListaFragment.this.listaModel.getByKey(ProdottiListaFragment.this.key);
                ProdottiListaFragment.this.listeActivity.setListe(ProdottiListaFragment.this.listaModel.getComplete());
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!Boolean.TRUE.equals(result)) {
                Utils.showToast(ProdottiListaFragment.this.c, "Errore durante l'eliminazione del prodotto");
            }
            if (ProdottiListaFragment.this.lista != null && ProdottiListaFragment.this.adapter != null) {
                JSONArray prodotti = ProdottiListaFragment.this.lista.optJSONArray("prodotti");
                ProdottiListaFragment.this.adapter.setData(prodotti != null ? prodotti : new JSONArray());
            }
            ProdottiListaFragment.this.eanProdotto.getText().clear();
            ProdottiListaFragment.this.eanProdotto.requestFocus();
            ProdottiListaFragment.this.listeActivity.showProgress(false);
        }
    }

    private class CaricaLista extends AsyncTask<Void, Void, JSONObject> {
        private CaricaLista() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProdottiListaFragment.this.listeActivity.showProgress(true);
        }

        @Override
        protected JSONObject doInBackground(Void... voids) {
            ProdottiListaFragment.this.lista = ProdottiListaFragment.this.listaModel.getByKey(ProdottiListaFragment.this.key);
            return ProdottiListaFragment.this.lista;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            if (ProdottiListaFragment.this.lista == null) {
                Utils.showToast(ProdottiListaFragment.this.c, "Lista non trovata");
                ProdottiListaFragment.this.listeActivity.showProgress(false);
                if (getFragmentManager() != null) {
                    getFragmentManager().popBackStack();
                }
                return;
            }
            ProdottiListaFragment.this.completata = ProdottiListaFragment.this.lista
                    .optString("completata", ProdottiListaFragment.this.lista.optString("completa", "0"))
                    .equals("1");
            ProdottiListaFragment.this.listeActivity.getToolbar().setTitle(
                    ProdottiListaFragment.this.lista.optString("nome", ""));
            JSONArray prodotti = ProdottiListaFragment.this.lista.optJSONArray("prodotti");
            ProdottiListaFragment.this.adapter = new ProdottiListaAdapter(
                    prodotti != null ? prodotti : new JSONArray(), null);
            ProdottiListaFragment.this.recyclerView.setAdapter(ProdottiListaFragment.this.adapter);
            ProdottiListaFragment.this.setListaCompleta(ProdottiListaFragment.this.completata);
            ProdottiListaFragment.this.syncCompletaSwitch();
            ProdottiListaFragment.this.listeActivity.invalidateOptionsMenu();
            ProdottiListaFragment.this.eanProdotto.requestFocus();
            ProdottiListaFragment.this.listeActivity.showProgress(false);
        }
    }

    private class CercaProdotto extends AsyncTask<String, Void, JSONObject> {
        private String barcode;

        private CercaProdotto() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProdottiListaFragment.this.listeActivity.showProgress(true);
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            this.barcode = Utils.sanitizeBarcode(params[0]);
            if (this.barcode.isEmpty()) {
                return null;
            }
            ProdottiListaFragment.this.prodottoModel.load();
            return ProdottiListaFragment.this.prodottoModel.getProdottoByEAN(this.barcode);
        }

        @Override
        protected void onPostExecute(JSONObject prod) {
            super.onPostExecute(prod);
            if (prod != null) {
                ProdottiListaFragment.this.prodottoId = prod.optString("pid", "");
                ProdottiListaFragment.this.descrizione = prod.optString("pde", "");
                ProdottiListaFragment.this.showQuantityModal(this.barcode, null, false);
            } else {
                ProdottiListaFragment.this.prodottoId = "0";
                ProdottiListaFragment.this.descrizione = "";
                ProdottiListaFragment.this.showQuantityModal(this.barcode, null, false);
                Utils.playErrorTone();
            }
            ProdottiListaFragment.this.listeActivity.showProgress(false);
        }
    }

    private boolean isSearchTextPasted() {
        return this.isSearchTextPasted;
    }

    private void isSearchTextPasted(boolean value) {
        this.isSearchTextPasted = value;
    }

    private void search(String query) {
        String sanitized = Utils.sanitizeBarcode(query);
        this.eanProdotto.getText().clear();
        if (!sanitized.isEmpty()) {
            new CercaProdotto().execute(sanitized);
        }
    }

    private void setListaCompleta(boolean isCompleta) {
        if (this.adapter == null) {
            return;
        }
        if (isCompleta) {
            this.eanProdottoContainer.setVisibility(View.GONE);
            this.adapter.setListener(null);
            return;
        }
        this.eanProdottoContainer.setVisibility(View.VISIBLE);
        this.adapter.setListener(new ProdottiListaAdapter.ListListener() {
            @Override
            public void onClick(JSONObject item, int position) {
                if (item == null) {
                    return;
                }
                ProdottiListaFragment.this.prodottoId = item.optString("prodotto_id", "0");
                ProdottiListaFragment.this.descrizione = item.optString("descrizione", "");
                ProdottiListaFragment.this.showQuantityModal(
                        item.optString("id", item.optString("barcode", "")),
                        item.optString("quantita", ""),
                        true);
            }

            @Override
            public void onLongClick(final JSONObject item, int position) {
                if (item == null) {
                    return;
                }
                final String itemId = item.optString("id", item.optString("barcode", ""));
                new AlertDialog.Builder(ProdottiListaFragment.this.c)
                        .setTitle(R.string.conferma)
                        .setMessage(Html.fromHtml(ProdottiListaFragment.this.getString(
                                R.string.conferma_cancella_prodotto_lista_messaggio,
                                "<b>" + itemId + "</b>")))
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new CancellaProdottoLista().execute(itemId);
                            }
                        })
                        .create()
                        .show();
            }
        });
    }

    private void syncCompletaSwitch() {
        if (this.completaSwitch == null) {
            return;
        }
        this.completaSwitch.setOnCheckedChangeListener(null);
        this.completaSwitch.setChecked(this.completata);
        this.completaSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                new CambiaStatoLista().execute(isChecked);
            }
        });
    }

    private void showQuantityModal(final String barcode, final String currentQty, final boolean isEdit) {
        final EditText input = new EditText(this.c);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (currentQty != null && !currentQty.isEmpty()) {
            String cleanQty = currentQty.replace(',', '.');
            String formattedQty = Utils.isFloat(cleanQty)
                    ? Utils.formatQuantity(Float.parseFloat(cleanQty))
                    : currentQty;
            input.setText(formattedQty);
            input.setSelection(input.getText().length());
        }

        final AlertDialog dialog = new AlertDialog.Builder(this.c)
                .setTitle("Prodotto " + barcode)
                .setMessage(this.descrizione)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        ProdottiListaFragment.this.eanProdotto.getText().clear();
                        ProdottiListaFragment.this.eanProdotto.requestFocus();
                        Utils.hideSoftKeyboard(input);
                    }
                })
                .setPositiveButton(R.string.salva, null)
                .create();

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface d) {
                ProdottiListaFragment.this.eanProdotto.getText().clear();
                ProdottiListaFragment.this.eanProdotto.requestFocus();
            }
        });

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface d) {
                Utils.showSoftKeyboard(input);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String val = input.getText().toString().trim().replace(',', '.');
                        if (val.isEmpty()) {
                            Utils.showToast(ProdottiListaFragment.this.c, R.string.inserisci_quantita);
                        } else if (!Utils.isFloat(val) || Float.parseFloat(val) <= 0.0f) {
                            Utils.showToast(ProdottiListaFragment.this.c, R.string.quantita_non_valida);
                        } else {
                            Utils.hideSoftKeyboard(input);
                            d.dismiss();
                            new AggiungiProdottoLista().execute(barcode, val, isEdit ? "1" : "0");
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.c = getContext();
        this.sharedPref = new SharedPref(this.c);
        this.listaModel = new Lista(this.sharedPref);
        this.prodottoModel = new Prodotto(this.sharedPref);
        this.listeActivity = (ListeActivity) getActivity();
        Bundle args = getArguments();
        this.key = args != null ? args.getString(KEY_LISTA, "") : "";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.listeActivity.getAddLista().hide();
        View view = inflater.inflate(R.layout.fragment_prodotti_lista, container, false);
        Context context = view.getContext();

        this.eanProdottoContainer = view.findViewById(R.id.ean_prodotto_container);
        this.eanProdotto = (EditText) view.findViewById(R.id.ean_prodotto);
        this.eanClearButton = (ImageView) view.findViewById(R.id.ean_clear_button);
        this.recyclerView = (RecyclerView) view.findViewById(R.id.prodotti_lista_recycler_view);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(context));

        if (!this.sharedPref.isKeyboardVisible()) {
            this.eanClearButton.setVisibility(View.GONE);
            Utils.disableSoftKeyboardForScanner(this.eanProdotto);
            this.eanProdotto.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (!Utils.isScanActionKey(keyCode, event)) {
                        return false;
                    }
                    ProdottiListaFragment.this.search(ProdottiListaFragment.this.eanProdotto.getText().toString());
                    return true;
                }
            });
            this.eanProdotto.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (start == 0 && before == 0 && count > 1) {
                        ProdottiListaFragment.this.isSearchTextPasted(true);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (ProdottiListaFragment.this.isSearchTextPasted()) {
                        ProdottiListaFragment.this.isSearchTextPasted(false);
                        ProdottiListaFragment.this.search(s.toString());
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
                    ProdottiListaFragment.this.search(ProdottiListaFragment.this.eanProdotto.getText().toString());
                    return true;
                }
            });
            this.eanProdotto.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus || ProdottiListaFragment.this.eanProdotto.getText().toString().length() <= 0) {
                        ProdottiListaFragment.this.eanClearButton.setVisibility(View.GONE);
                    } else {
                        ProdottiListaFragment.this.eanClearButton.setVisibility(View.VISIBLE);
                    }
                }
            });
            this.eanProdotto.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() > 0) {
                        ProdottiListaFragment.this.eanClearButton.setVisibility(View.VISIBLE);
                    } else {
                        ProdottiListaFragment.this.eanClearButton.setVisibility(View.GONE);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
            this.eanClearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ProdottiListaFragment.this.eanProdotto.getText().clear();
                    ProdottiListaFragment.this.eanProdotto.requestFocus();
                }
            });
        }

        new CaricaLista().execute();
        return view;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem switchItem = menu.findItem(R.id.app_bar_switch);
        if (switchItem != null && switchItem.getActionView() != null) {
            switchItem.setVisible(true);
            this.completaSwitch = (Switch) switchItem.getActionView().findViewById(R.id.toggle_button);
            syncCompletaSwitch();
        }
        super.onPrepareOptionsMenu(menu);
    }
}
