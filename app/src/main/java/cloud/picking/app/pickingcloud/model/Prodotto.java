package cloud.picking.app.pickingcloud.model;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;

import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.Utils;

public class Prodotto {
    private static final String FILE_NAME = "prodotti.json";
    private static final String TAG = "Prodotto";
    private static JSONArray prodotti = null;
    private static HashMap<String, JSONObject> indexByPid = null;
    private static HashMap<String, JSONObject> indexByEan = null;
    private SharedPref sharedPref;

    public Prodotto(Context context) {
        this(new SharedPref(context));
    }

    public Prodotto(SharedPref sharedPref) {
        this.sharedPref = sharedPref;
    }

    private static synchronized void rebuildIndexes() {
        indexByPid = new HashMap<>();
        indexByEan = new HashMap<>();
        if (prodotti == null) {
            return;
        }
        int len = prodotti.length();
        for (int i = 0; i < len; i++) {
            JSONObject item = prodotti.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String pid = Utils.sanitizeBarcode(item.optString("pid", ""));
            if (!pid.isEmpty()) {
                indexByPid.put(pid, item);
                indexByEan.put(pid.toLowerCase(), item);
            }
            JSONArray codes = item.optJSONArray("codes");
            if (codes != null) {
                for (int j = 0; j < codes.length(); j++) {
                    String code = Utils.sanitizeBarcode(codes.optString(j, ""));
                    if (!code.isEmpty()) {
                        indexByEan.put(code.toLowerCase(), item);
                    }
                }
            }
        }
    }

    public JSONObject getByIdProdotto(String pid) {
        if (pid == null) {
            return null;
        }
        if (prodotti == null || indexByPid == null) {
            load();
        }
        String cleanPid = Utils.sanitizeBarcode(pid);
        if (indexByPid != null) {
            JSONObject found = indexByPid.get(cleanPid);
            if (found != null) {
                return found;
            }
        }
        for (int i = 0; i < prodotti.length(); i++) {
            try {
                JSONObject item = prodotti.getJSONObject(i);
                if (item.getString("pid").equals(cleanPid)) {
                    return item;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getPath() {
        return this.sharedPref.getFolderDir() + FILE_NAME;
    }

    public JSONArray getProdotti() {
        if (prodotti == null) {
            load();
        }
        return prodotti;
    }

    public JSONObject getProdottoByEAN(String ean) {
        if (ean == null) {
            return null;
        }
        String cleanEan = Utils.sanitizeBarcode(ean);
        if (cleanEan.isEmpty()) {
            return null;
        }
        if (prodotti == null || indexByEan == null) {
            load();
        }
        if (indexByEan != null) {
            JSONObject found = indexByEan.get(cleanEan.toLowerCase());
            if (found != null) {
                return found;
            }
        }
        for (int i = 0; i < prodotti.length(); i++) {
            try {
                JSONObject item = prodotti.getJSONObject(i);
                if (item.getString("pid").equalsIgnoreCase(cleanEan)) {
                    return item;
                }
                JSONArray codes = item.optJSONArray("codes");
                if (codes != null) {
                    for (int j = 0; j < codes.length(); j++) {
                        if (codes.getString(j).equalsIgnoreCase(cleanEan)) {
                            return item;
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void load() {
        try {
            String content = Utils.readFromFile(getPath());
            if (content == null || content.trim().isEmpty()) {
                prodotti = new JSONArray();
            } else {
                prodotti = new JSONArray(content);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            prodotti = new JSONArray();
        }
        rebuildIndexes();
    }

    public void reload() {
        prodotti = null;
        indexByPid = null;
        indexByEan = null;
        load();
    }

    public void updateUbicazione(String pid, String ubi) {
        if (prodotti == null) {
            load();
        }
        String cleanPid = Utils.sanitizeBarcode(pid);
        JSONObject indexed = indexByPid != null ? indexByPid.get(cleanPid) : null;
        if (indexed != null) {
            try {
                indexed.put("pub", ubi);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            int i = 0;
            while (i < prodotti.length()) {
                try {
                    JSONObject item = prodotti.getJSONObject(i);
                    if (item.getString("pid").equals(cleanPid)) {
                        item.put("pub", ubi);
                        prodotti.put(i, item);
                        break;
                    }
                    i++;
                } catch (JSONException e) {
                    e.printStackTrace();
                    i++;
                }
            }
        }
        try {
            Utils.writeToFile(getPath(), prodotti.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
