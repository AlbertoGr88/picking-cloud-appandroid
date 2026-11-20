package cloud.picking.app.pickingcloud.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.helper.Utils;

public class ProdottiAdapter extends RecyclerView.Adapter<ProdottiAdapter.ViewHolder> implements Filterable {
    private static ItemClickListener clickListener;
    private Context c;
    private JSONArray prodotti;
    private JSONArray prodottiFiltered;

    public interface ItemClickListener {
        void onClick(View view, int position);
        void onPostFiltering();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView descrizione;
        public TextView extraBarCodes;
        public TextView idProdotto;
        public TextView info;
        public TextView prezzo;
        public TextView riassortimentoLabel;

        public ViewHolder(View view) {
            super(view);
            this.idProdotto = (TextView) view.findViewById(R.id.IdProdotto);
            this.descrizione = (TextView) view.findViewById(R.id.Descrizione);
            this.extraBarCodes = (TextView) view.findViewById(R.id.ExtraBarCodes);
            this.info = (TextView) view.findViewById(R.id.info);
            this.prezzo = (TextView) view.findViewById(R.id.Prezzo);
            this.riassortimentoLabel = (TextView) view.findViewById(R.id.riassortimento_label);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (ProdottiAdapter.clickListener != null) {
                ProdottiAdapter.clickListener.onClick(view, getAdapterPosition());
            }
        }
    }

    public ProdottiAdapter(JSONArray prodotti) {
        this.prodotti = prodotti != null ? prodotti : new JSONArray();
        this.prodottiFiltered = this.prodotti;
    }

    public JSONArray getData() {
        return this.prodotti;
    }

    public JSONArray getDataFiltered() {
        return this.prodottiFiltered;
    }

    public JSONObject getItem(int position) {
        if (this.prodottiFiltered != null && position >= 0 && position < this.prodottiFiltered.length()) {
            return this.prodottiFiltered.optJSONObject(position);
        }
        return null;
    }

    public void updateItem(int position, JSONObject item) {
        if (this.prodottiFiltered != null && position >= 0 && position < this.prodottiFiltered.length() && item != null) {
            try {
                this.prodottiFiltered.put(position, item);
                notifyItemChanged(position);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String query = Utils.sanitizeBarcode(charSequence != null ? charSequence.toString() : "").toLowerCase();
                if (query.isEmpty()) {
                    ProdottiAdapter.this.prodottiFiltered = ProdottiAdapter.this.prodotti;
                } else {
                    JSONArray filtered = new JSONArray();
                    for (int i = 0; i < ProdottiAdapter.this.prodotti.length(); i++) {
                        try {
                            JSONObject item = ProdottiAdapter.this.prodotti.getJSONObject(i);
                            if (item.optString("pid", "").toLowerCase().contains(query)
                                    || item.optString("pde", "").toLowerCase().contains(query)) {
                                filtered.put(item);
                            } else {
                                JSONArray codes = item.optJSONArray("codes");
                                if (codes != null) {
                                    int j = 0;
                                    while (true) {
                                        if (j >= codes.length()) {
                                            break;
                                        }
                                        if (codes.optString(j, "").toLowerCase().contains(query)) {
                                            filtered.put(item);
                                            break;
                                        }
                                        j++;
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    ProdottiAdapter.this.prodottiFiltered = filtered;
                }
                FilterResults results = new FilterResults();
                results.values = ProdottiAdapter.this.prodottiFiltered;
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                ProdottiAdapter.this.prodottiFiltered = (JSONArray) filterResults.values;
                ProdottiAdapter.this.notifyDataSetChanged();
                if (ProdottiAdapter.clickListener != null) {
                    ProdottiAdapter.clickListener.onPostFiltering();
                }
            }
        };
    }

    @Override
    public int getItemCount() {
        return this.prodottiFiltered != null ? this.prodottiFiltered.length() : 0;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try {
            JSONObject item = this.prodottiFiltered.getJSONObject(position);
            String pid = item.optString("pid", "");
            String pde = item.optString("pde", "");
            String pqt = item.optString("pqt", "0");
            String pub = item.optString("pub", "");
            String ppz = item.optString("ppz", "0.00");
            boolean daRiassortire = item.optBoolean("da_riassortire", false);

            JSONArray codes = item.optJSONArray("codes");
            ArrayList<String> extraCodes = new ArrayList<>();
            if (codes != null) {
                for (int i = 0; i < codes.length(); i++) {
                    String code = codes.optString(i, "");
                    if (!code.isEmpty()) {
                        extraCodes.add(code);
                    }
                }
            }

            holder.idProdotto.setText(pid);
            holder.descrizione.setText(pde);
            holder.extraBarCodes.setText(TextUtils.join(" - ", extraCodes));
            holder.info.setText(this.c.getString(R.string.info_prodotto, pqt, pub));
            holder.prezzo.setText("€ " + ppz);
            holder.riassortimentoLabel.setVisibility(daRiassortire ? View.VISIBLE : View.INVISIBLE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        this.c = parent.getContext();
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.prodotti_list_item, parent, false));
    }

    public void setClickListener(ItemClickListener listener) {
        clickListener = listener;
    }

    public void setData(JSONArray data) {
        this.prodotti = data != null ? data : new JSONArray();
        this.prodottiFiltered = this.prodotti;
        notifyDataSetChanged();
    }
}
