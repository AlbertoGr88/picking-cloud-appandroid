package cloud.picking.app.pickingcloud.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.helper.Utils;

public class ProdottiListaAdapter extends RecyclerView.Adapter<ProdottiListaAdapter.ViewHolder> {
    private Context c;
    private ListListener mListener;
    private JSONArray mValues;

    public interface ListListener {
        void onClick(JSONObject item, int position);
        void onLongClick(JSONObject item, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mDescrizione;
        public final TextView mIdView;
        public JSONObject mItem;
        public final TextView mQuantita;
        public final View mView;

        public ViewHolder(View view) {
            super(view);
            this.mView = view;
            this.mIdView = (TextView) view.findViewById(R.id.prodotto_id);
            this.mDescrizione = (TextView) view.findViewById(R.id.prodotto_descrizione);
            this.mQuantita = (TextView) view.findViewById(R.id.prodotto_quantita);
        }
    }

    public ProdottiListaAdapter(JSONArray items, ListListener listener) {
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
            String id = item.optString("id", item.optString("barcode", ""));
            String descrizione = item.optString("descrizione", "");
            String quantitaRaw = item.optString("quantita", "0").replace(',', '.');
            String quantita = Utils.isFloat(quantitaRaw)
                    ? Utils.formatQuantity(Float.parseFloat(quantitaRaw))
                    : quantitaRaw;

            holder.mItem = item;
            holder.mIdView.setText(id);
            holder.mDescrizione.setText(descrizione);
            holder.mQuantita.setText(quantita);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getAdapterPosition();
                    if (ProdottiListaAdapter.this.mListener != null && pos != RecyclerView.NO_POSITION) {
                        ProdottiListaAdapter.this.mListener.onClick(holder.mItem, pos);
                    }
                }
            });
            holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int pos = holder.getAdapterPosition();
                    if (ProdottiListaAdapter.this.mListener != null && pos != RecyclerView.NO_POSITION) {
                        ProdottiListaAdapter.this.mListener.onLongClick(holder.mItem, pos);
                    }
                    return true;
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        this.c = parent.getContext();
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_prodotti_lista_list_item, parent, false));
    }

    public void setData(JSONArray items) {
        this.mValues = items != null ? items : new JSONArray();
        notifyDataSetChanged();
    }

    public void setListener(ListListener listener) {
        this.mListener = listener;
    }
}
