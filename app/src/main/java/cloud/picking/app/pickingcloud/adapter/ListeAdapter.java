package cloud.picking.app.pickingcloud.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.fragments.ListeFragment;
import cloud.picking.app.pickingcloud.helper.Utils;

public class ListeAdapter extends RecyclerView.Adapter<ListeAdapter.ViewHolder> {
    private Context c;
    private final ListeFragment.OnListFragmentInteractionListener mListener;
    private JSONArray mValues;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView mCompletataView;
        public final TextView mInfoView;
        public JSONObject mItem;
        public final TextView mNomeView;
        public final View mView;

        public ViewHolder(View view) {
            super(view);
            this.mView = view;
            this.mNomeView = (TextView) view.findViewById(R.id.nome);
            this.mInfoView = (TextView) view.findViewById(R.id.info);
            this.mCompletataView = (ImageView) view.findViewById(R.id.completata);
        }
    }

    public ListeAdapter(JSONArray items, ListeFragment.OnListFragmentInteractionListener listener) {
        this.mValues = items != null ? items : new JSONArray();
        this.mListener = listener;
    }

    @Override
    public int getItemCount() {
        return this.mValues != null ? this.mValues.length() : 0;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        try {
            holder.mItem = this.mValues.getJSONObject(position);
            String nome = holder.mItem.optString("nome", "");
            JSONArray prodottiArr = holder.mItem.optJSONArray("prodotti");
            int numProdotti = prodottiArr != null ? prodottiArr.length() : 0;
            String dataRaw = holder.mItem.optString("data_creazione", holder.mItem.optString("data", ""));
            String data = Utils.getDateFromStr(dataRaw);
            boolean completata = holder.mItem.optString("completata", holder.mItem.optString("completa", "0")).equals("1");

            holder.mNomeView.setText(nome);
            holder.mInfoView.setText(this.c.getString(R.string.info_lista, data, numProdotti));
            holder.mCompletataView.setVisibility(completata ? View.VISIBLE : View.GONE);

            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ListeAdapter.this.mListener != null) {
                        ListeAdapter.this.mListener.onListFragmentInteraction(holder.mItem);
                    }
                }
            });
            holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (ListeAdapter.this.mListener != null) {
                        ListeAdapter.this.mListener.onListFragmentLongClick(holder.mItem);
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
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_liste_item, parent, false));
    }

    public void setData(JSONArray items) {
        this.mValues = items != null ? items : new JSONArray();
        notifyDataSetChanged();
    }
}
