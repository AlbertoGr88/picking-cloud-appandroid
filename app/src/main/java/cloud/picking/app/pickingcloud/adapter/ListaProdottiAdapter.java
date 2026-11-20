package cloud.picking.app.pickingcloud.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.fragments.ProdottiOrdineFragment;
import cloud.picking.app.pickingcloud.helper.Utils;

public class ListaProdottiAdapter extends RecyclerView.Adapter<ListaProdottiAdapter.ViewHolder> {
    private Context c;
    private final ProdottiOrdineFragment.OnListFragmentInteractionListener mListener;
    private JSONArray mValues;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mDescrizione;
        public final TextView mEvaso;
        public final TextView mIdView;
        public JSONObject mItem;
        public final ProgressBar mProgresso;
        public final TextView mQuantita;
        public final View mView;

        public ViewHolder(View view) {
            super(view);
            this.mView = view;
            this.mIdView = (TextView) view.findViewById(R.id.prodotto_id);
            this.mDescrizione = (TextView) view.findViewById(R.id.prodotto_descrizione);
            this.mQuantita = (TextView) view.findViewById(R.id.prodotto_quantita);
            this.mEvaso = (TextView) view.findViewById(R.id.prodotto_evaso);
            this.mProgresso = (ProgressBar) view.findViewById(R.id.prodotto_progresso);
        }
    }

    public ListaProdottiAdapter(JSONArray items, ProdottiOrdineFragment.OnListFragmentInteractionListener listener) {
        this.mValues = items != null ? items : new JSONArray();
        this.mListener = listener;
    }

    @Override
    public int getItemCount() {
        return this.mValues != null ? this.mValues.length() : 0;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        try {
            JSONObject item = this.mValues.getJSONObject(position);
            String idProdotto = item.has("pid") ? item.optString("pid", "") : item.optString("IdProdotto", "");
            String descrizione = item.optString("pde", "");
            Float qty = Utils.floatRound(Float.valueOf(item.optString("Qty", "0").replace(',', '.')), 3);
            Float scannerizzato = Utils.floatRound(Float.valueOf(item.optString("Scannerizzato", "0").replace(',', '.')), 3);

            holder.mItem = item;
            holder.mIdView.setText(idProdotto);
            holder.mDescrizione.setText(descrizione);
            holder.mQuantita.setText(Utils.formatQuantity(qty));
            holder.mEvaso.setText(Utils.formatQuantity(scannerizzato, qty));

            Utils.updateOrderProgressBar(holder.mProgresso, scannerizzato, qty);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ListaProdottiAdapter.this.mListener != null) {
                        ListaProdottiAdapter.this.mListener.onListFragmentInteraction(holder.mItem, position);
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        this.c = parent.getContext();
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_prodotti_ordine_list_item, parent, false));
    }
}
