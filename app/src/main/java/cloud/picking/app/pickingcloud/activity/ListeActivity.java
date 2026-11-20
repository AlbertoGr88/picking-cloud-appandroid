package cloud.picking.app.pickingcloud.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.fragments.ListeFragment;
import cloud.picking.app.pickingcloud.fragments.ProdottiListaFragment;
import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.Utils;
import cloud.picking.app.pickingcloud.model.Lista;

public class ListeActivity extends AppCompatActivity implements ListeFragment.OnListFragmentInteractionListener {
    public static final String TAG = "ListeActivity";
    private FloatingActionButton addLista;
    private Context c;
    private FragmentRefreshListener fragmentRefreshListener;
    private Lista listaModel;
    private JSONArray liste = new JSONArray();
    private ProgressBar progressBar;
    private SharedPref sharedPref;
    private Toolbar toolbar;

    public interface FragmentRefreshListener {
        void onRefresh(JSONArray liste);
    }

    private class CancellaLista extends AsyncTask<String, Void, Boolean> {
        private CancellaLista() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ListeActivity.this.showProgress(true);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            boolean deleted = ListeActivity.this.listaModel.cancellaPerPosizione(params[0]);
            if (deleted) {
                ListeActivity.this.liste = ListeActivity.this.listaModel.getComplete();
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!Boolean.TRUE.equals(result)) {
                Utils.showToast(ListeActivity.this.c, "Errore durante l'eliminazione della lista");
            }
            if (ListeActivity.this.getFragmentRefreshListener() != null) {
                ListeActivity.this.getFragmentRefreshListener().onRefresh(ListeActivity.this.liste);
            }
            ListeActivity.this.showProgress(false);
        }
    }

    private class CaricaListe extends AsyncTask<Void, Void, Void> {
        private CaricaListe() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ListeActivity.this.showProgress(true);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ListeActivity.this.listaModel.load();
            ListeActivity.this.liste = ListeActivity.this.listaModel.getComplete();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            ListeActivity.this.caricaListeFragment();
            ListeActivity.this.showProgress(false);
        }
    }

    private class NuovaLista extends AsyncTask<String, Void, Boolean> {
        private NuovaLista() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ListeActivity.this.showProgress(true);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            boolean created = ListeActivity.this.listaModel.nuova(params[0]);
            if (created) {
                ListeActivity.this.liste = ListeActivity.this.listaModel.getComplete();
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!Boolean.TRUE.equals(result)) {
                Utils.showToast(ListeActivity.this.c, "Errore durante la creazione della lista");
            }
            if (ListeActivity.this.getFragmentRefreshListener() != null) {
                ListeActivity.this.getFragmentRefreshListener().onRefresh(ListeActivity.this.liste);
            }
            ListeActivity.this.showProgress(false);
        }
    }

    private void caricaListeFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentsContainer, new ListeFragment())
                .commit();
    }

    public FloatingActionButton getAddLista() {
        return this.addLista;
    }

    public FragmentRefreshListener getFragmentRefreshListener() {
        return this.fragmentRefreshListener;
    }

    public JSONArray getListe() {
        return this.liste;
    }

    public Toolbar getToolbar() {
        return this.toolbar;
    }

    public void setFragmentRefreshListener(FragmentRefreshListener listener) {
        this.fragmentRefreshListener = listener;
    }

    public void setListe(JSONArray liste) {
        this.liste = liste;
    }

    public void showProgress(boolean show) {
        this.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.c = this;
        this.sharedPref = new SharedPref(this);
        this.listaModel = new Lista(this.sharedPref);

        setContentView(R.layout.activity_liste);
        Toolbar tb = (Toolbar) findViewById(R.id.toolbar);
        this.toolbar = tb;
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        this.addLista = (FloatingActionButton) findViewById(R.id.addListaFAB);
        this.progressBar = (ProgressBar) findViewById(R.id.progressBar);

        new CaricaListe().execute();

        this.addLista.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(ListeActivity.this.c);
                input.setInputType(1);
                final AlertDialog dialog = new AlertDialog.Builder(ListeActivity.this.c)
                        .setView(input)
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                Utils.hideSoftKeyboard(input);
                            }
                        })
                        .setPositiveButton(R.string.salva, null)
                        .setTitle(R.string.nuova_lista)
                        .setMessage(R.string.messaggio_descrittivo_nuova_lista)
                        .create();

                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(final DialogInterface d) {
                        Utils.showSoftKeyboard(input);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String nome = input.getText().toString();
                                if (nome.length() < 3) {
                                    Utils.showToast(ListeActivity.this.c, R.string.lista_nome_lunghezza_minima);
                                } else {
                                    Utils.hideSoftKeyboard(input);
                                    d.dismiss();
                                    new NuovaLista().execute(nome);
                                }
                            }
                        });
                    }
                });
                dialog.show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.liste, menu);
        return true;
    }

    private String extractListKey(JSONObject item) {
        if (item == null) {
            return "";
        }
        String key = item.optString("key", "");
        return !key.isEmpty() ? key : item.optString("id", "");
    }

    @Override
    public void onListFragmentInteraction(JSONObject item) {
        onListFragmentClick(item, extractListKey(item));
    }

    public void onListFragmentClick(JSONObject item, String key) {
        ProdottiListaFragment fragment = new ProdottiListaFragment();
        Bundle bundle = new Bundle();
        bundle.putString("key", key);
        bundle.putString(ProdottiListaFragment.KEY_LISTA, key);
        fragment.setArguments(bundle);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragmentsContainer, fragment);
        tx.addToBackStack(null);
        tx.commit();
    }

    @Override
    public void onListFragmentLongClick(final JSONObject item) {
        onListFragmentLongClick(item, extractListKey(item));
    }

    public void onListFragmentLongClick(JSONObject item, final String key) {
        new AlertDialog.Builder(this.c)
                .setTitle(R.string.conferma)
                .setMessage(Html.fromHtml(getString(
                        R.string.conferma_cancella_lista_messaggio,
                        "<b>" + item.optString("nome", "") + "</b>")))
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new CancellaLista().execute(key);
                    }
                })
                .create()
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home && getSupportFragmentManager().getBackStackEntryCount() > 0) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
