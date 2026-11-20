package cloud.picking.app.pickingcloud.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.activity.ListeActivity;
import cloud.picking.app.pickingcloud.adapter.ListeAdapter;

public class ListeFragment extends Fragment {
    public static final String TAG = "Liste";
    private ListeAdapter adapter;
    private ListeActivity listeActivity;
    private OnListFragmentInteractionListener mListener;
    private RecyclerView recyclerView;

    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(JSONObject item);
        void onListFragmentLongClick(JSONObject item);
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
        this.listeActivity = (ListeActivity) getActivity();
        this.listeActivity.setFragmentRefreshListener(new ListeActivity.FragmentRefreshListener() {
            @Override
            public void onRefresh(JSONArray liste) {
                if (ListeFragment.this.adapter != null) {
                    ListeFragment.this.adapter.setData(liste);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.listeActivity.getToolbar().setTitle(R.string.liste_prodotti);
        this.listeActivity.getAddLista().show();
        View view = inflater.inflate(R.layout.fragment_liste, container, false);
        Context context = view.getContext();
        this.recyclerView = (RecyclerView) view.findViewById(R.id.lista_prodotti_ordini);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(context));
        this.adapter = new ListeAdapter(this.listeActivity.getListe(), this.mListener);
        this.recyclerView.setAdapter(this.adapter);
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
    }
}
