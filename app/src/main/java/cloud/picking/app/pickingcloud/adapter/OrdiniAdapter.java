package cloud.picking.app.pickingcloud.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.helper.Utils;

public class OrdiniAdapter extends RecyclerView.Adapter<OrdiniAdapter.ViewHolder> implements Filterable {
    public static final String FILTER_PRIORITY_ALL = "ALL";
    public static final String FILTER_PRIORITY_ANY = "ANY";
    public static final String FILTER_PRIORITY_NONE = "NONE";

    private static ItemClickListener clickListener;
    private static OnModalitaPrelievoChange onModalitaPrelievoChange;
    private Context c;
    private boolean modalitaPrelievo = false;
    private JSONArray ordini;
    private JSONArray ordiniFiltered;
    private String priorityFilterMode = FILTER_PRIORITY_ALL;
    private String lastSearchQuery = "";

    public interface ItemClickListener {
        void onClick(View view, int position);
        void onLongClick(View view, int position);
    }

    public interface OnModalitaPrelievoChange {
        void onModalitaPrelievoChange(boolean enabled);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public CheckBox checkBox;
        public TextView dataConsegna;
        public TextView deliveryName;
        public TextView evasi;
        public TextView idOrdine;
        public TextView prioritaTag;
        public ProgressBar progress;

        public ViewHolder(View view) {
            super(view);
            this.idOrdine = (TextView) view.findViewById(R.id.IdOrdine);
            this.prioritaTag = (TextView) view.findViewById(R.id.PrioritaTag);
            this.deliveryName = (TextView) view.findViewById(R.id.DeliveryName);
            this.dataConsegna = (TextView) view.findViewById(R.id.DataConsegna);
            this.evasi = (TextView) view.findViewById(R.id.evasi);
            this.checkBox = (CheckBox) view.findViewById(R.id.checkBox);
            this.progress = (ProgressBar) view.findViewById(R.id.orderItemProgress);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (OrdiniAdapter.clickListener != null) {
                OrdiniAdapter.clickListener.onClick(view, getAdapterPosition());
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if (OrdiniAdapter.clickListener != null) {
                OrdiniAdapter.clickListener.onLongClick(view, getAdapterPosition());
            }
            return true;
        }
    }

    public OrdiniAdapter(JSONArray ordini) {
        this.ordini = ordini != null ? ordini : new JSONArray();
        this.ordiniFiltered = this.ordini;
    }

    public JSONArray getData() {
        return this.ordini;
    }

    public JSONArray getDataFiltered() {
        return this.ordiniFiltered;
    }

    public JSONObject getItem(int position) {
        if (this.ordiniFiltered != null && position >= 0 && position < this.ordiniFiltered.length()) {
            return this.ordiniFiltered.optJSONObject(position);
        }
        return null;
    }

    public SparseBooleanArray getItemStateArray() {
        SparseBooleanArray result = new SparseBooleanArray();
        if (this.ordiniFiltered != null) {
            for (int i = 0; i < this.ordiniFiltered.length(); i++) {
                JSONObject item = this.ordiniFiltered.optJSONObject(i);
                if (item != null && item.optBoolean("checked", false)) {
                    result.put(i, true);
                }
            }
        }
        return result;
    }

    public void resetItemStateArray() {
        setModalitaPrelievo(false);
    }

    public String getPriorityFilterMode() {
        return this.priorityFilterMode;
    }

    public void setPriorityFilterMode(String mode) {
        this.priorityFilterMode = (mode == null || mode.trim().isEmpty()) ? FILTER_PRIORITY_ALL : mode.trim();
        getFilter().filter(this.lastSearchQuery);
    }

    private boolean matchesPriorityFilter(JSONObject item) {
        if (item == null || FILTER_PRIORITY_ALL.equals(this.priorityFilterMode)) {
            return true;
        }
        boolean hasPrio = Utils.hasOrderPriority(item);
        if (FILTER_PRIORITY_ANY.equals(this.priorityFilterMode)) {
            return hasPrio;
        }
        if (FILTER_PRIORITY_NONE.equals(this.priorityFilterMode)) {
            return !hasPrio;
        }
        if (!hasPrio) {
            return false;
        }
        String prioVal = String.valueOf(Utils.getOrderPriorityValue(item));
        String tagVal = Utils.getOrderPriorityDisplayLabel(item);
        return this.priorityFilterMode.equalsIgnoreCase(prioVal)
                || this.priorityFilterMode.equalsIgnoreCase(tagVal)
                || this.priorityFilterMode.equalsIgnoreCase(item.optString("PrioritaTag", "").trim());
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String query = charSequence != null ? charSequence.toString().trim().toLowerCase() : "";
                OrdiniAdapter.this.lastSearchQuery = query;
                boolean hasTextFilter = !query.isEmpty();
                boolean hasPrioFilter = !FILTER_PRIORITY_ALL.equals(OrdiniAdapter.this.priorityFilterMode);
                if (!hasTextFilter && !hasPrioFilter) {
                    OrdiniAdapter.this.ordiniFiltered = OrdiniAdapter.this.ordini;
                } else {
                    String[] tokens = hasTextFilter ? query.split("\\s+") : new String[0];
                    JSONArray filtered = new JSONArray();
                    for (int i = 0; i < OrdiniAdapter.this.ordini.length(); i++) {
                        try {
                            JSONObject item = OrdiniAdapter.this.ordini.getJSONObject(i);
                            if (matchesPriorityFilter(item) && (!hasTextFilter || matchesOrderQuery(item, tokens))) {
                                filtered.put(item);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    OrdiniAdapter.this.ordiniFiltered = filtered;
                }
                FilterResults results = new FilterResults();
                results.values = OrdiniAdapter.this.ordiniFiltered;
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                OrdiniAdapter.this.ordiniFiltered = (JSONArray) filterResults.values;
                OrdiniAdapter.this.notifyDataSetChanged();
            }
        };
    }

    /**
     * Verifica la corrispondenza %like% (parziale e case-insensitive) su numero ordine,
     * cliente, data consegna e note.
     */
    private boolean matchesOrderQuery(JSONObject item, String[] tokens) {
        if (item == null || tokens == null || tokens.length == 0) {
            return true;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(item.optString("IdOrdine", "")).append(' ');
        sb.append(item.optString("ido", "")).append(' ');
        sb.append(item.optString("Numero", "")).append(' ');
        sb.append(item.optString("DeliveryName", "")).append(' ');
        sb.append(item.optString("dnm", "")).append(' ');
        sb.append(item.optString("DataConsegna", "")).append(' ');
        sb.append(item.optString("end", "")).append(' ');
        sb.append(item.optString("InternalComment", "")).append(' ');
        sb.append(item.optString("Note", "")).append(' ');
        sb.append(item.optString("Commento", "")).append(' ');
        sb.append(item.optString("PrioritaTag", "")).append(' ');
        sb.append(Utils.getOrderPriorityDisplayLabel(item)).append(' ');
        sb.append(item.optString("search_field", ""));
        String haystack = sb.toString().toLowerCase();
        for (String token : tokens) {
            if (!token.isEmpty() && !haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getItemCount() {
        return this.ordiniFiltered != null ? this.ordiniFiltered.length() : 0;
    }

    public boolean getModalitaPrelievo() {
        return this.modalitaPrelievo;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        try {
            JSONObject ordine = this.ordiniFiltered.getJSONObject(position);
            String idOrdine = ordine.has("IdOrdine") ? ordine.optString("IdOrdine", "") : ordine.optString("ido", "");
            String deliveryName = ordine.has("DeliveryName") ? ordine.optString("DeliveryName", "") : ordine.optString("dnm", "");
            String dataConsegna = ordine.has("DataConsegna") ? ordine.optString("DataConsegna", "") : ordine.optString("end", "");
            JSONArray prd = ordine.has("prodotti") ? ordine.optJSONArray("prodotti") : ordine.optJSONArray("prd");
            if (prd == null) {
                prd = new JSONArray();
            }
            boolean isChecked = ordine.optBoolean("checked", false);

            Float totalQty = 0.0f;
            Float totalScan = 0.0f;
            for (int i = 0; i < prd.length(); i++) {
                JSONObject riga = prd.getJSONObject(i);
                totalQty = Utils.floatRound(totalQty + Float.parseFloat(riga.optString("Qty", "0").replace(',', '.')), 3);
                totalScan = Utils.floatRound(totalScan + Float.parseFloat(riga.optString("Scannerizzato", "0").replace(',', '.')), 3);
            }

            Utils.updateOrderProgressBar(holder.progress, totalScan, totalQty);

            holder.idOrdine.setText("N. ORDINE " + idOrdine);
            Utils.bindPrioritaBadge(holder.prioritaTag, ordine);
            holder.deliveryName.setText(deliveryName);
            holder.dataConsegna.setText("Data consegna: " + dataConsegna);
            holder.evasi.setText(Utils.formatQuantity(totalScan, totalQty) + " su " + Utils.formatQuantity(totalQty));

            holder.checkBox.setOnCheckedChangeListener(null);
            if (getModalitaPrelievo()) {
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setChecked(isChecked);
                holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                        OrdiniAdapter.this.setChecked(position, checked);
                    }
                });
            } else {
                holder.checkBox.setVisibility(View.GONE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        this.c = parent.getContext();
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.ordini_list_item, parent, false));
    }

    public void setChecked(int position, boolean checked) {
        try {
            JSONObject item = this.ordiniFiltered.getJSONObject(position);
            item.put("checked", checked);
            this.ordiniFiltered.put(position, item);
            boolean anyChecked = false;
            int i = 0;
            while (true) {
                if (i >= this.ordiniFiltered.length()) {
                    break;
                }
                if (this.ordiniFiltered.getJSONObject(i).optBoolean("checked", false)) {
                    anyChecked = true;
                    break;
                }
                i++;
            }
            if (!anyChecked) {
                setModalitaPrelievo(false);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setClickListener(ItemClickListener listener) {
        clickListener = listener;
    }

    public void setData(JSONArray data) {
        this.ordini = data != null ? data : new JSONArray();
        this.ordiniFiltered = this.ordini;
        notifyDataSetChanged();
    }

    public void setModalitaPrelievo(boolean enabled) {
        if (!enabled) {
            for (int i = 0; i < this.ordiniFiltered.length(); i++) {
                try {
                    JSONObject item = this.ordiniFiltered.getJSONObject(i);
                    item.put("checked", false);
                    this.ordiniFiltered.put(i, item);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        this.modalitaPrelievo = enabled;
        notifyDataSetChanged();
        if (onModalitaPrelievoChange != null) {
            onModalitaPrelievoChange.onModalitaPrelievoChange(enabled);
        }
    }

    public void setOnModalitaPrelievoChange(OnModalitaPrelievoChange listener) {
        onModalitaPrelievoChange = listener;
    }
}
