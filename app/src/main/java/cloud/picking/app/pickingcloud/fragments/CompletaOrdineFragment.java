package cloud.picking.app.pickingcloud.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.activity.PickingActivity;
import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.Utils;
import cloud.picking.app.pickingcloud.model.Ordine;

public class CompletaOrdineFragment extends Fragment {
    public static final String TAG = "CompletaOrdine";
    private Context c;
    private Button chiudiOrdineBtn;
    private EditText noteEditText;
    private EditText numColliEditText;
    private JSONObject ordine;
    private PickingActivity pickingActivity;
    private SharedPref sharedPref;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.c = getContext();
        this.sharedPref = new SharedPref(this.c);
        this.pickingActivity = (PickingActivity) getActivity();
        this.ordine = new Ordine(this.sharedPref).load().getById(this.pickingActivity.getIdOrdini().get(0));

        View view = inflater.inflate(R.layout.fragment_completa_ordine, container, false);
        this.numColliEditText = (EditText) view.findViewById(R.id.ordine_numero_colli);
        this.chiudiOrdineBtn = (Button) view.findViewById(R.id.ordine_salva);
        this.noteEditText = (EditText) view.findViewById(R.id.ordine_note);

        try {
            if (this.ordine != null) {
                this.numColliEditText.setText(this.ordine.has("NumeroColli") ? this.ordine.optString("NumeroColli", "") : this.ordine.optString("cll", ""));
                this.noteEditText.setText(this.ordine.has("Note") ? this.ordine.optString("Note", "") : this.ordine.optString("ntc", ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.chiudiOrdineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String numColli = CompletaOrdineFragment.this.numColliEditText.getText().toString();
                if (!Utils.isInteger(numColli) || Integer.parseInt(numColli) < 0) {
                    Utils.showToast(CompletaOrdineFragment.this.c, "Il numero dei colli inserito non è valido.");
                    return;
                }
                String note = CompletaOrdineFragment.this.noteEditText.getText().toString();
                CompletaOrdineFragment.this.pickingActivity.salvaNumColliNote(numColli, note);
            }
        });
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
