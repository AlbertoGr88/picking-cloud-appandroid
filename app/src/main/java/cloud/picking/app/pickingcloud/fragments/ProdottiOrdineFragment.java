package cloud.picking.app.pickingcloud.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.activity.PickingActivity;
import cloud.picking.app.pickingcloud.adapter.ListaProdottiAdapter;

public class ProdottiOrdineFragment extends Fragment {
    public static final String TAG = "ProdottiOrdine";
    private ListaProdottiAdapter adapter;
    private OnListFragmentInteractionListener mListener;
    private PickingActivity pickingActivity;
    private RecyclerView recyclerView;

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(JSONObject item, int position);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            this.mListener = (OnListFragmentInteractionListener) context;
            return;
        }
        throw new RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.pickingActivity = (PickingActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_prodotti_ordine, container, false);
        Context context = view.getContext();
        this.recyclerView = (RecyclerView) view.findViewById(R.id.lista_prodotti_ordini);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        this.adapter = new ListaProdottiAdapter(this.pickingActivity.getProdotti(), this.mListener);
        this.recyclerView.setAdapter(this.adapter);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.pickingActivity != null) {
            if (this.recyclerView != null) {
                this.adapter = new ListaProdottiAdapter(this.pickingActivity.getProdotti(), this.mListener);
                this.recyclerView.setAdapter(this.adapter);
            }
            this.pickingActivity.updateRowsHeader();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
    }
}
