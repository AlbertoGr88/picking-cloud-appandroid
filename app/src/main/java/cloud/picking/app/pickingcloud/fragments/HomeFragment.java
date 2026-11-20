package cloud.picking.app.pickingcloud.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.activity.ListeActivity;
import cloud.picking.app.pickingcloud.activity.MainActivity;
import cloud.picking.app.pickingcloud.activity.OrdiniActivity;
import cloud.picking.app.pickingcloud.activity.SyncActivity;
import cloud.picking.app.pickingcloud.activity.UbicazioneActivity;
import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.UpdatesTracker;
import cloud.picking.app.pickingcloud.helper.Utils;
import cloud.picking.app.pickingcloud.model.Lista;
import cloud.picking.app.pickingcloud.model.Ordine;
import cloud.picking.app.pickingcloud.model.UbicazioneTemporanea;

public class HomeFragment extends Fragment {
    private RelativeLayout boxListe;
    private RelativeLayout boxOrdiniCoda;
    private RelativeLayout boxOrdiniCompletati;
    private RelativeLayout boxUbicazioni;
    private Context c;
    private MainActivity mainActivity;
    private JSONArray ordiniCompletati;
    private JSONArray ordiniInCoda;
    private ProgressBar progressListe;
    private ProgressBar progressOrdiniCoda;
    private ProgressBar progressOrdiniCompletati;
    private ProgressBar progressUbicazioni;
    private SharedPref sharedPref;
    private TextView testoListe;
    private TextView testoOrdiniCoda;
    private TextView sottotestoOrdiniCoda;
    private TextView testoOrdiniCompletati;
    private TextView testoUbicazioni;
    private TextView valoreListe;
    private TextView valoreOrdiniCoda;
    private TextView valoreOrdiniCompletati;
    private TextView valoreUbicazioni;

    private class Carica extends AsyncTask<Void, Void, Integer> {
        private int countListe;
        private int countOrdiniCoda;
        private int countOrdiniCodaPrioritari;
        private int countOrdiniCompletati;
        private int countUbicazioni;

        private Carica() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            HomeFragment.this.progressOrdiniCoda.setVisibility(View.VISIBLE);
            HomeFragment.this.valoreOrdiniCoda.setVisibility(View.GONE);
            if (HomeFragment.this.sottotestoOrdiniCoda != null) {
                HomeFragment.this.sottotestoOrdiniCoda.setVisibility(View.GONE);
            }
            HomeFragment.this.progressOrdiniCompletati.setVisibility(View.VISIBLE);
            HomeFragment.this.valoreOrdiniCompletati.setVisibility(View.GONE);
            HomeFragment.this.progressUbicazioni.setVisibility(View.VISIBLE);
            HomeFragment.this.valoreUbicazioni.setVisibility(View.GONE);
            HomeFragment.this.progressListe.setVisibility(View.VISIBLE);
            HomeFragment.this.valoreListe.setVisibility(View.GONE);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            Ordine ordine = new Ordine(HomeFragment.this.sharedPref);
            ordine.load();
            HomeFragment.this.ordiniInCoda = ordine.getCoda();
            HomeFragment.this.ordiniCompletati = ordine.getCompletati();
            this.countOrdiniCoda = HomeFragment.this.ordiniInCoda.length();
            this.countOrdiniCodaPrioritari = 0;
            for (int i = 0; i < this.countOrdiniCoda; i++) {
                JSONObject item = HomeFragment.this.ordiniInCoda.optJSONObject(i);
                if (Utils.hasOrderPriority(item)) {
                    this.countOrdiniCodaPrioritari++;
                }
            }
            this.countOrdiniCompletati = HomeFragment.this.ordiniCompletati.length();

            UbicazioneTemporanea ubicazioneTemporanea = new UbicazioneTemporanea(HomeFragment.this.sharedPref);
            ubicazioneTemporanea.load();
            this.countUbicazioni = ubicazioneTemporanea.get().length();

            Lista lista = new Lista(HomeFragment.this.sharedPref);
            lista.load();
            this.countListe = lista.getComplete().length();

            UpdatesTracker.setOrdiniInCoda(this.countOrdiniCoda);
            return null;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            HomeFragment.this.progressOrdiniCoda.setVisibility(View.GONE);
            HomeFragment.this.valoreOrdiniCoda.setText(String.valueOf(this.countOrdiniCoda));
            HomeFragment.this.valoreOrdiniCoda.setVisibility(View.VISIBLE);
            if (HomeFragment.this.sottotestoOrdiniCoda != null) {
                if (this.countOrdiniCodaPrioritari > 0 && HomeFragment.this.isAdded()) {
                    HomeFragment.this.sottotestoOrdiniCoda.setText(
                            HomeFragment.this.getString(R.string.ordini_con_priorita_count, this.countOrdiniCodaPrioritari));
                    HomeFragment.this.sottotestoOrdiniCoda.setVisibility(View.VISIBLE);
                } else {
                    HomeFragment.this.sottotestoOrdiniCoda.setVisibility(View.GONE);
                }
            }

            HomeFragment.this.progressOrdiniCompletati.setVisibility(View.GONE);
            HomeFragment.this.valoreOrdiniCompletati.setText(String.valueOf(this.countOrdiniCompletati));
            HomeFragment.this.valoreOrdiniCompletati.setVisibility(View.VISIBLE);

            HomeFragment.this.progressUbicazioni.setVisibility(View.GONE);
            HomeFragment.this.valoreUbicazioni.setText(String.valueOf(this.countUbicazioni));
            HomeFragment.this.valoreUbicazioni.setVisibility(View.VISIBLE);

            HomeFragment.this.progressListe.setVisibility(View.GONE);
            HomeFragment.this.valoreListe.setText(String.valueOf(this.countListe));
            HomeFragment.this.valoreListe.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.c = getContext();
        this.mainActivity = (MainActivity) getActivity();
        this.sharedPref = new SharedPref(this.c);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        this.boxOrdiniCoda = (RelativeLayout) view.findViewById(R.id.home_box_ordini_coda);
        this.boxOrdiniCompletati = (RelativeLayout) view.findViewById(R.id.home_box_ordini_completati);
        this.boxUbicazioni = (RelativeLayout) view.findViewById(R.id.home_box_ubicazioni);
        this.boxListe = (RelativeLayout) view.findViewById(R.id.home_box_liste);

        this.testoOrdiniCoda = (TextView) this.boxOrdiniCoda.findViewById(R.id.testo);
        this.sottotestoOrdiniCoda = (TextView) this.boxOrdiniCoda.findViewById(R.id.sottotesto);
        this.progressOrdiniCoda = (ProgressBar) this.boxOrdiniCoda.findViewById(R.id.progress);
        this.valoreOrdiniCoda = (TextView) this.boxOrdiniCoda.findViewById(R.id.valore);

        this.testoOrdiniCompletati = (TextView) this.boxOrdiniCompletati.findViewById(R.id.testo);
        this.progressOrdiniCompletati = (ProgressBar) this.boxOrdiniCompletati.findViewById(R.id.progress);
        this.valoreOrdiniCompletati = (TextView) this.boxOrdiniCompletati.findViewById(R.id.valore);

        this.testoUbicazioni = (TextView) this.boxUbicazioni.findViewById(R.id.testo);
        this.progressUbicazioni = (ProgressBar) this.boxUbicazioni.findViewById(R.id.progress);
        this.valoreUbicazioni = (TextView) this.boxUbicazioni.findViewById(R.id.valore);

        this.testoListe = (TextView) this.boxListe.findViewById(R.id.testo);
        this.progressListe = (ProgressBar) this.boxListe.findViewById(R.id.progress);
        this.valoreListe = (TextView) this.boxListe.findViewById(R.id.valore);

        this.boxOrdiniCoda.setBackgroundColor(ContextCompat.getColor(this.c, R.color.lightRed));
        this.testoOrdiniCoda.setText(getString(R.string.ordini_in_coda));

        this.boxOrdiniCompletati.setBackgroundColor(ContextCompat.getColor(this.c, R.color.lightGreen));
        this.testoOrdiniCompletati.setText(getString(R.string.ordini_completati));

        this.boxUbicazioni.setBackgroundColor(ContextCompat.getColor(this.c, R.color.primaryLightColor));
        this.testoUbicazioni.setText(getString(R.string.nuove_ubicazioni));

        this.boxListe.setBackgroundColor(ContextCompat.getColor(this.c, R.color.lightYellow));
        this.testoListe.setText(getString(R.string.liste_prodotti));

        org.json.JSONObject auth = this.sharedPref.getAuth();
        boolean showOrdini = auth == null
                || "1".equals(auth.optString("app_ordini", "0"))
                || "true".equalsIgnoreCase(auth.optString("app_ordini", "false"));
        boolean showUbicazioni = auth != null
                && ("1".equals(auth.optString("app_ubicazioni", "0"))
                        || "true".equalsIgnoreCase(auth.optString("app_ubicazioni", "false")));
        boolean showListe = auth != null
                && ("1".equals(auth.optString("app_liste", "0"))
                        || "true".equalsIgnoreCase(auth.optString("app_liste", "false")));

        this.boxOrdiniCoda.setVisibility(showOrdini ? View.VISIBLE : View.GONE);
        this.boxOrdiniCompletati.setVisibility(showOrdini ? View.VISIBLE : View.GONE);
        this.boxUbicazioni.setVisibility(showUbicazioni ? View.VISIBLE : View.GONE);
        this.boxListe.setVisibility(showListe ? View.VISIBLE : View.GONE);

        this.boxOrdiniCoda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeFragment.this.c, OrdiniActivity.class);
                i.putExtra(OrdiniActivity.STATO_KEY, OrdiniActivity.STATO_CODA);
                HomeFragment.this.c.startActivity(i);
            }
        });
        this.boxOrdiniCompletati.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomeFragment.this.c.startActivity(new Intent(HomeFragment.this.c, SyncActivity.class));
            }
        });
        this.boxUbicazioni.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomeFragment.this.c.startActivity(new Intent(HomeFragment.this.c, UbicazioneActivity.class));
            }
        });
        this.boxListe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomeFragment.this.c.startActivity(new Intent(HomeFragment.this.c, ListeActivity.class));
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        new Carica().execute();
    }
}
