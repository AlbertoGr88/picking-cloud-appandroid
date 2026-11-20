package cloud.picking.app.pickingcloud.fragments;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.method.DigitsKeyListener;

import cloud.picking.app.pickingcloud.R;
import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.Utils;

public class ImpostazioniFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(
                new SharedPref(getActivity().getApplicationContext()).getAuthSharedPreferencesName());
        addPreferencesFromResource(R.xml.impostazioni);

        Preference maxWeightPref = findPreference(SharedPref.MAX_PESO_COLLO_WARNING);
        if (maxWeightPref instanceof EditTextPreference) {
            final EditTextPreference weightPref = (EditTextPreference) maxWeightPref;
            if (weightPref.getEditText() != null) {
                weightPref.getEditText().setKeyListener(DigitsKeyListener.getInstance("0123456789.,"));
            }
            updateMaxWeightSummary(weightPref, weightPref.getText());
            weightPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String raw = newValue != null ? newValue.toString().trim().replace(',', '.') : "";
                    if (raw.isEmpty()) {
                        updateMaxWeightSummary(weightPref, "");
                        return true;
                    }
                    if (!Utils.isFloat(raw)) {
                        Utils.showToast(getActivity(), "Inserisci un peso numerico valido in kg");
                        return false;
                    }
                    float val = Utils.floatRound(Float.parseFloat(raw), 3);
                    if (val < 0.0f) {
                        Utils.showToast(getActivity(), "Il peso non può essere negativo");
                        return false;
                    }
                    String formatted = val == 0.0f ? "0" : Utils.formatQuantity(val);
                    weightPref.setText(formatted);
                    updateMaxWeightSummary(weightPref, formatted);
                    return false;
                }
            });
        }
    }

    private void updateMaxWeightSummary(EditTextPreference pref, String rawValue) {
        String clean = rawValue != null ? rawValue.trim().replace(',', '.') : "";
        if (!clean.isEmpty() && Utils.isFloat(clean)) {
            float val = Utils.floatRound(Float.parseFloat(clean), 3);
            if (val > 0.0f) {
                pref.setSummary("Chiedi conferma se il peso supera "
                        + Utils.formatQuantity(val) + " kg (0 o vuoto per disattivare)");
                return;
            }
        }
        pref.setSummary("Controllo disattivato (inserisci una soglia in kg per attivarlo)");
    }
}
