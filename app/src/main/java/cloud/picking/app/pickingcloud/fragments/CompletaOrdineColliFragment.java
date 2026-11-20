package cloud.picking.app.pickingcloud.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.activity.PickingActivity;
import cloud.picking.app.pickingcloud.adapter.ColliAdapter;
import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.Utils;
import cloud.picking.app.pickingcloud.model.Collo;
import cloud.picking.app.pickingcloud.model.Ordine;

public class CompletaOrdineColliFragment extends Fragment {
    public static final String TAG = "CompletaOrdineColli";
    private Button aggiungiColloBtn;
    private EditText altezzaEditText;
    private Context c;
    private Button chiudiOrdineBtn;
    private JSONArray colli = new JSONArray();
    private ColliAdapter colliAdapter;
    private RecyclerView colliRecyclerView;
    private Collo collo;
    private EditText larghezzaEditText;
    private EditText noteEditText;
    private TextInputLayout noteLayout;
    private JSONObject ordine;
    private EditText pesoEditText;
    private PickingActivity pickingActivity;
    private EditText profonditaEditText;
    private SharedPref sharedPref;

    private class CaricaColli extends AsyncTask<Void, Void, Void> {
        private CaricaColli() {
        }

        @Override
        protected Void doInBackground(Void... voids) {
            CompletaOrdineColliFragment.this.colli = new JSONArray();
            try {
                CompletaOrdineColliFragment.this.collo.load();
                JSONObject allColli = CompletaOrdineColliFragment.this.collo.get();
                String orderId = CompletaOrdineColliFragment.this.getOrderId();
                if (!orderId.isEmpty() && allColli.has(orderId)) {
                    CompletaOrdineColliFragment.this.colli = allColli.getJSONArray(orderId);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            CompletaOrdineColliFragment.this.colliAdapter = new ColliAdapter(
                    CompletaOrdineColliFragment.this.c, CompletaOrdineColliFragment.this.colli);
            CompletaOrdineColliFragment.this.colliRecyclerView.setLayoutManager(
                    new LinearLayoutManager(CompletaOrdineColliFragment.this.c));
            CompletaOrdineColliFragment.this.colliRecyclerView.setAdapter(
                    CompletaOrdineColliFragment.this.colliAdapter);

            ColliAdapter.setClickListener(new ColliAdapter.ItemClickListener() {
                @Override
                public void onClick(View view, final int position) {
                    new AlertDialog.Builder(CompletaOrdineColliFragment.this.c)
                            .setTitle(R.string.conferma)
                            .setMessage("Eliminare il Collo n." + (position + 1) + "?")
                            .setNegativeButton(android.R.string.no, null)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    CompletaOrdineColliFragment.this.colliAdapter.getData().remove(position);
                                    CompletaOrdineColliFragment.this.colliAdapter.notifyDataSetChanged();
                                    new SalvaColli().execute(CompletaOrdineColliFragment.this.colliAdapter.getData());
                                }
                            })
                            .create()
                            .show();
                }
            });
        }
    }

    private class SalvaColli extends AsyncTask<JSONArray, Void, Boolean> {
        private SalvaColli() {
        }

        @Override
        protected Boolean doInBackground(JSONArray... params) {
            String orderId = CompletaOrdineColliFragment.this.getOrderId();
            if (!orderId.isEmpty()) {
                boolean savedColli = CompletaOrdineColliFragment.this.collo.save(orderId, params[0]);
                if (savedColli) {
                    new Ordine(CompletaOrdineColliFragment.this.sharedPref)
                            .aggiornaNumeroColli(orderId, String.valueOf(params[0].length()));
                }
                return savedColli;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!Boolean.TRUE.equals(result)) {
                Utils.showToast(CompletaOrdineColliFragment.this.c, "Errore durante il salvataggio dei colli");
            }
        }
    }

    private void aggiungiCollo(String larghezza, String altezza, String profondita, float pesoFloat) {
        JSONObject newCollo = new JSONObject();
        try {
            newCollo.put("larghezza", larghezza);
            newCollo.put("altezza", altezza);
            newCollo.put("profondita", profondita);
            newCollo.put("peso", String.valueOf(pesoFloat));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.colliAdapter.getData().put(newCollo);
        this.colliAdapter.notifyDataSetChanged();
        new SalvaColli().execute(this.colliAdapter.getData());

        this.larghezzaEditText.getText().clear();
        this.altezzaEditText.getText().clear();
        this.profonditaEditText.getText().clear();
        this.pesoEditText.getText().clear();
        this.larghezzaEditText.requestFocus();
        this.larghezzaEditText.post(new Runnable() {
            @Override
            public void run() {
                CompletaOrdineColliFragment.this.larghezzaEditText.requestFocus();
            }
        });
    }

    private String getOrderId() {
        if (this.ordine == null) {
            return "";
        }
        return this.ordine.has("ido")
                ? this.ordine.optString("ido", "")
                : this.ordine.optString("IdOrdine", "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.c = getContext();
        this.sharedPref = new SharedPref(this.c);
        this.collo = new Collo(this.sharedPref);
        this.pickingActivity = (PickingActivity) getActivity();
        this.ordine = new Ordine(this.sharedPref).load().getById(this.pickingActivity.getIdOrdini().get(0));

        View view = inflater.inflate(R.layout.fragment_completa_ordine_colli, container, false);
        this.colliRecyclerView = (RecyclerView) view.findViewById(R.id.colliRecyclerView);
        this.chiudiOrdineBtn = (Button) view.findViewById(R.id.btn_colli_chiudi);
        this.noteLayout = (TextInputLayout) view.findViewById(R.id.ordine_note_layout);
        this.larghezzaEditText = (EditText) view.findViewById(R.id.collo_larghezza);
        this.altezzaEditText = (EditText) view.findViewById(R.id.collo_altezza);
        this.profonditaEditText = (EditText) view.findViewById(R.id.collo_profondita);
        this.pesoEditText = (EditText) view.findViewById(R.id.collo_peso);
        this.aggiungiColloBtn = (Button) view.findViewById(R.id.btn_aggiungi_collo);
        this.noteEditText = (EditText) view.findViewById(R.id.ordine_note);

        try {
            if (this.ordine != null) {
                this.noteEditText.setText(this.ordine.has("Note") ? this.ordine.optString("Note", "") : this.ordine.optString("ntc", ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.aggiungiColloBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String larghezza = CompletaOrdineColliFragment.this.larghezzaEditText.getText().toString().trim();
                final String altezza = CompletaOrdineColliFragment.this.altezzaEditText.getText().toString().trim();
                final String profondita = CompletaOrdineColliFragment.this.profonditaEditText.getText().toString().trim();
                String peso = CompletaOrdineColliFragment.this.pesoEditText.getText().toString().trim();

                if (larghezza.isEmpty() || !Utils.isInteger(larghezza) || Integer.parseInt(larghezza) <= 0) {
                    Utils.showToast(CompletaOrdineColliFragment.this.c, "La Larghezza non è valida");
                } else if (altezza.isEmpty() || !Utils.isInteger(altezza) || Integer.parseInt(altezza) <= 0) {
                    Utils.showToast(CompletaOrdineColliFragment.this.c, "L'Altezza non è valida");
                } else if (profondita.isEmpty() || !Utils.isInteger(profondita) || Integer.parseInt(profondita) <= 0) {
                    Utils.showToast(CompletaOrdineColliFragment.this.c, "La Profondità non è valida");
                } else if (peso.isEmpty() || !Utils.isFloat(peso) || Float.parseFloat(peso.replace(',', '.')) <= 0.0f) {
                    Utils.showToast(CompletaOrdineColliFragment.this.c, "Il Peso non è valido");
                } else {
                    final float pesoFloat = Utils.floatRound(Float.parseFloat(peso.replace(',', '.')), 3);
                    float maxPesoWarning = CompletaOrdineColliFragment.this.sharedPref.getMaxPesoColloWarning();
                    if (maxPesoWarning > 0.0f && pesoFloat > maxPesoWarning) {
                        Utils.playErrorTone();
                        new AlertDialog.Builder(CompletaOrdineColliFragment.this.c)
                                .setTitle("Attenzione peso collo")
                                .setMessage("Hai inserito un peso di "
                                        + Utils.formatQuantity(pesoFloat)
                                        + " kg (soglia configurata: "
                                        + Utils.formatQuantity(maxPesoWarning)
                                        + " kg).\nSei sicuro che il peso è corretto?")
                                .setNegativeButton("NO, MODIFICA", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        CompletaOrdineColliFragment.this.pesoEditText.requestFocus();
                                        CompletaOrdineColliFragment.this.pesoEditText.selectAll();
                                    }
                                })
                                .setPositiveButton("SI, CONFERMA", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        CompletaOrdineColliFragment.this.aggiungiCollo(
                                                larghezza, altezza, profondita, pesoFloat);
                                    }
                                })
                                .create()
                                .show();
                    } else {
                        CompletaOrdineColliFragment.this.aggiungiCollo(
                                larghezza, altezza, profondita, pesoFloat);
                    }
                }
            }
        });

        this.chiudiOrdineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int numColli = CompletaOrdineColliFragment.this.colliAdapter != null
                        ? CompletaOrdineColliFragment.this.colliAdapter.getData().length()
                        : CompletaOrdineColliFragment.this.colli.length();
                CompletaOrdineColliFragment.this.pickingActivity.salvaNumColliNote(
                        String.valueOf(numColli),
                        CompletaOrdineColliFragment.this.noteEditText.getText().toString());
            }
        });

        new CaricaColli().execute();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        this.pickingActivity.setActionCompletaOrdineVisibility(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.pickingActivity.setActionCompletaOrdineVisibility(true);
    }
}
