package cloud.picking.app.pickingcloud.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;

public class ColliAdapter extends RecyclerView.Adapter<ColliAdapter.ViewHolder> {
    private static ItemClickListener clickListener;
    private Context c;
    private JSONArray colli;

    public interface ItemClickListener {
        void onClick(View view, int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView altezza;
        public TextView larghezza;
        public TextView peso;
        public TextView profondita;
        public TextView progressivo;

        public ViewHolder(View view) {
            super(view);
            this.progressivo = (TextView) view.findViewById(R.id.collo_progressivo);
            this.larghezza = (TextView) view.findViewById(R.id.collo_larghezza);
            this.altezza = (TextView) view.findViewById(R.id.collo_altezza);
            this.profondita = (TextView) view.findViewById(R.id.collo_profondita);
            this.peso = (TextView) view.findViewById(R.id.collo_peso);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (ColliAdapter.clickListener != null) {
                ColliAdapter.clickListener.onClick(view, getAdapterPosition());
            }
        }
    }

    public ColliAdapter(Context context, JSONArray colli) {
        this.c = context;
        this.colli = colli != null ? colli : new JSONArray();
    }

    public JSONArray getData() {
        return this.colli;
    }

    @Override
    public int getItemCount() {
        return this.colli != null ? this.colli.length() : 0;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        JSONObject item = this.colli.optJSONObject(position);
        if (item == null) {
            return;
        }
        String prog = String.valueOf(position + 1);
        String larg = item.optString("larghezza", "0");
        String alt = item.optString("altezza", "0");
        String prof = item.optString("profondita", "0");
        String pesoVal = item.optString("peso", "0");

        holder.progressivo.setText("Collo n." + prog);
        holder.larghezza.setText(larg);
        holder.altezza.setText(alt);
        holder.profondita.setText(prof);
        holder.peso.setText(pesoVal);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.colli_list_item, parent, false));
    }

    public static void setClickListener(ItemClickListener listener) {
        clickListener = listener;
    }
}
