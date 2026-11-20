package cloud.picking.app.pickingcloud.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.Utils;

public class UbicazioneTemporanea {
    private static final String FILE_NAME = "ubicazioni_temporanee.json";
    private static final String TAG = "UbicazioneTemporanea";
    private static JSONArray ubicazioniTemporanee = null;
    private SharedPref sharedPref;

    public UbicazioneTemporanea(SharedPref sharedPref) {
        this.sharedPref = sharedPref;
    }

    public boolean clear() {
        try {
            ubicazioniTemporanee = new JSONArray();
            return Utils.createOrWriteFile(getPath(), ubicazioniTemporanee.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public JSONArray get() {
        if (ubicazioniTemporanee == null) {
            load();
        }
        return ubicazioniTemporanee;
    }

    /**
     * Restituisce la mappa `{IdProdotto: Ubicazione}` attesa dal server in `ubicazioni_temporanee`.
     */
    public JSONObject getForSync() {
        if (ubicazioniTemporanee == null) {
            load();
        }
        JSONObject map = new JSONObject();
        if (ubicazioniTemporanee == null) {
            return map;
        }
        for (int i = 0; i < ubicazioniTemporanee.length(); i++) {
            JSONObject item = ubicazioniTemporanee.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String pid = item.optString("id", "").trim();
            String ubicazione = item.optString("ubicazione", "").trim();
            if (!pid.isEmpty() && !ubicazione.isEmpty()) {
                try {
                    map.put(pid, ubicazione);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }

    public String getPath() {
        return this.sharedPref.getFolderDir() + FILE_NAME;
    }

    public UbicazioneTemporanea load() {
        try {
            String content = Utils.readFromFile(getPath());
            if (content == null || content.trim().isEmpty()) {
                ubicazioniTemporanee = new JSONArray();
            } else {
                ubicazioniTemporanee = new JSONArray(content);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            ubicazioniTemporanee = new JSONArray();
        }
        return this;
    }

    public boolean svuota() {
        return clear();
    }

    public void reload() {
        ubicazioniTemporanee = null;
        load();
    }

    public void save(String eanProdotto, String ubicazione) {
        if (ubicazioniTemporanee == null) {
            load();
        }
        try {
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            int existingIdx = -1;
            for (int i = 0; i < ubicazioniTemporanee.length(); i++) {
                JSONObject existing = ubicazioniTemporanee.optJSONObject(i);
                if (existing != null && eanProdotto.equals(existing.optString("id", ""))) {
                    existingIdx = i;
                    break;
                }
            }
            JSONObject item = new JSONObject();
            item.put("id", eanProdotto);
            item.put("ubicazione", ubicazione);
            item.put("data_inserimento", now);
            if (existingIdx >= 0) {
                ubicazioniTemporanee.put(existingIdx, item);
            } else {
                ubicazioniTemporanee.put(item);
            }
            Utils.createOrWriteFile(getPath(), ubicazioniTemporanee.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
