package cloud.picking.app.pickingcloud.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.adapter.ProdottiAdapter;
import cloud.picking.app.pickingcloud.helper.SeparatorDecoration;
import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.Utils;
import cloud.picking.app.pickingcloud.model.DaRiassortire;
import cloud.picking.app.pickingcloud.model.Prodotto;

public class ProdottiActivity extends AppCompatActivity {
    public static final String DA_RIASSORTIRE = "da_riassortire";
    public static final String TAG = "ProdottiActivity";

    private Context c;
    public DaRiassortire daRiassortire;
    private boolean isTextPasted = false;
    private ProdottiAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private ProgressBar progress;
    private EditText searchEditText;
    private SharedPref sharedPref;

    public interface SegnalaListener {
        void onPositiveButtonClick();
    }

    private class CaricaProdotti extends AsyncTask<Void, Void, JSONArray> {
        private CaricaProdotti() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProdottiActivity.this.progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected JSONArray doInBackground(Void... voids) {
            JSONArray prodotti = new Prodotto(new SharedPref(ProdottiActivity.this.c)).getProdotti();
            if (prodotti == null) {
                prodotti = new JSONArray();
            }
            int length = prodotti.length();
            for (int i = 0; i < length; i++) {
                JSONObject prod = prodotti.optJSONObject(i);
                if (prod == null) {
                    continue;
                }
                String pid = prod.optString("pid", "");
                StringBuilder searchField = new StringBuilder(pid);
                JSONArray codes = prod.optJSONArray("codes");
                if (codes != null) {
                    for (int j = 0; j < codes.length(); j++) {
                        searchField.append(" ").append(codes.optString(j)).append(" ");
                    }
                }
                try {
                    prod.put("search_field", searchField.toString());
                    prod.put(DA_RIASSORTIRE, ProdottiActivity.this.daRiassortire.isProdottoDaRiassortire(pid));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return prodotti;
        }

        @Override
        protected void onPostExecute(JSONArray result) {
            super.onPostExecute(result);
            ProdottiActivity.this.progress.setVisibility(View.GONE);
            ProdottiActivity.this.mAdapter.setData(result);
            ProdottiActivity.this.mAdapter.getFilter().filter(ProdottiActivity.this.searchEditText.getText().toString());
            ProdottiActivity.this.searchEditText.requestFocus();
        }
    }

    private void focusSearchBox() {
        if (this.searchEditText == null) {
            return;
        }
        this.searchEditText.requestFocus();
        this.searchEditText.post(new Runnable() {
            @Override
            public void run() {
                if (ProdottiActivity.this.searchEditText != null) {
                    ProdottiActivity.this.searchEditText.requestFocus();
                }
            }
        });
    }

    private void performSearch() {
        if (this.searchEditText == null) {
            return;
        }
        String query = Utils.sanitizeBarcode(this.searchEditText.getText().toString());
        this.mAdapter.getFilter().filter(query);
        this.searchEditText.getText().clear();
        focusSearchBox();
    }

    private boolean isTextPasted() {
        return this.isTextPasted;
    }

    private void isTextPasted(boolean value) {
        this.isTextPasted = value;
    }

    private void segnalaComeRiassortito(final JSONObject prodotto, final SegnalaListener segnalaListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.c);
        builder.setTitle(R.string.segnala_come_riassortito);
        builder.setMessage(R.string.segnala_come_riassortito_messaggio);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ProdottiActivity.this.daRiassortire.setRiassortimento(prodotto.optString("pid"), 0);
                segnalaListener.onPositiveButtonClick();
                Log.d(TAG, ProdottiActivity.this.daRiassortire.get().toString());
            }
        });
        builder.show();
    }

    private void segnalaDaRiassortire(final JSONObject prodotto, final SegnalaListener segnalaListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.c);
        builder.setTitle(R.string.segnala_riassortimento);
        builder.setMessage(R.string.segnala_riassortimento_messaggio);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ProdottiActivity.this.daRiassortire.setRiassortimento(prodotto.optString("pid"), 1);
                segnalaListener.onPositiveButtonClick();
                Log.d(TAG, ProdottiActivity.this.daRiassortire.get().toString());
            }
        });
        builder.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prodotti);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.c = this;
        SharedPref sp = new SharedPref(this.c);
        this.sharedPref = sp;
        boolean keyboardVisible = sp.isKeyboardVisible();

        this.progress = (ProgressBar) findViewById(R.id.progressBar);
        RecyclerView rv = (RecyclerView) findViewById(R.id.prodotti_recycler_view);
        this.mRecyclerView = rv;
        rv.setHasFixedSize(true);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        this.mLayoutManager = lm;
        this.mRecyclerView.setLayoutManager(lm);
        this.mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        this.mRecyclerView.addItemDecoration(new SeparatorDecoration(this, Color.parseColor("#EEEEEE"), 1.0f));

        this.daRiassortire = new DaRiassortire(this.sharedPref).load();
        ProdottiAdapter adapter = new ProdottiAdapter(new JSONArray());
        this.mAdapter = adapter;
        adapter.setClickListener(new ProdottiAdapter.ItemClickListener() {
            @Override
            public void onClick(View view, final int position) {
                final JSONObject item = ProdottiActivity.this.mAdapter.getItem(position);
                if (item == null) {
                    return;
                }
                if (item.optBoolean(DA_RIASSORTIRE)) {
                    ProdottiActivity.this.segnalaComeRiassortito(item, new SegnalaListener() {
                        @Override
                        public void onPositiveButtonClick() {
                            try {
                                item.put(DA_RIASSORTIRE, false);
                                ProdottiActivity.this.mAdapter.updateItem(position, item);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    ProdottiActivity.this.segnalaDaRiassortire(item, new SegnalaListener() {
                        @Override
                        public void onPositiveButtonClick() {
                            try {
                                item.put(DA_RIASSORTIRE, true);
                                ProdottiActivity.this.mAdapter.updateItem(position, item);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }

            @Override
            public void onPostFiltering() {
                ProdottiActivity.this.focusSearchBox();
            }
        });

        new CaricaProdotti().execute();
        this.mRecyclerView.setAdapter(this.mAdapter);

        EditText search = (EditText) findViewById(R.id.searchEditText);
        this.searchEditText = search;
        search.setBackground(getDrawable(R.drawable.rectangle));
        this.searchEditText.setNextFocusDownId(R.id.searchEditText);
        this.searchEditText.setNextFocusForwardId(R.id.searchEditText);
        this.searchEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ProdottiActivity.this.searchEditText.setBackground(ProdottiActivity.this.getDrawable(R.drawable.rectangle_primary_dark));
                } else {
                    ProdottiActivity.this.searchEditText.setBackground(ProdottiActivity.this.getDrawable(R.drawable.rectangle));
                }
            }
        });

        if (!keyboardVisible) {
            Utils.disableSoftKeyboardForScanner(this.searchEditText);
            this.searchEditText.addTextChangedListener(new TextWatcher() {
                String beforeText = "";

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    this.beforeText = s.toString();
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() - this.beforeText.length() > 1) {
                        ProdottiActivity.this.isTextPasted(true);
                    }
                    if (ProdottiActivity.this.isTextPasted()) {
                        ProdottiActivity.this.isTextPasted(false);
                        ProdottiActivity.this.performSearch();
                    }
                }
            });
        }

        this.searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_GO
                        || actionId == EditorInfo.IME_NULL) {
                    if (event == null || event.getAction() == KeyEvent.ACTION_DOWN) {
                        ProdottiActivity.this.performSearch();
                    }
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
                        ProdottiActivity.this.performSearch();
                    }
                    return true;
                }
                return false;
            }
        });
    }
}
