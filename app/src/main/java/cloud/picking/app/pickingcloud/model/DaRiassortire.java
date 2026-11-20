package cloud.picking.app.pickingcloud.model;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;

import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.Utils;

public class DaRiassortire {
    public static final int DA_RIASSORTIRE = 1;
    private static final String FILE_NAME = "da_riassortire.json";
    public static final int RIASSORTITO = 0;
    private static final String TAG = "DaRiassortire";
    public static final String UPDATE = "upd";
    public static final int UPDATED = 1;
    public static final String VAL = "val";
    private static JSONArray daRiassortire = null;
    private static HashSet<String> daRiassortireSet = null;
    private SharedPref sharedPref;

    public DaRiassortire(Context context) {
        this(new SharedPref(context));
    }

    public DaRiassortire(SharedPref sharedPref) {
        this.sharedPref = sharedPref;
    }

    private static synchronized void rebuildIndex() {
        daRiassortireSet = new HashSet<>();
        if (daRiassortire == null) {
            return;
        }
        int len = daRiassortire.length();
        for (int i = 0; i < len; i++) {
            JSONObject item = daRiassortire.optJSONObject(i);
            if (item != null && item.optInt(VAL, RIASSORTITO) == DA_RIASSORTIRE) {
                String pid = item.optString("pid", "");
                if (!pid.isEmpty()) {
                    daRiassortireSet.add(pid);
                }
            }
        }
    }

    public JSONArray get() {
        return get(true);
    }

    public JSONArray get(boolean onlyUpdated) {
        if (daRiassortire == null) {
            load();
        }
        if (!onlyUpdated) {
            return daRiassortire;
        }
        JSONArray filtered = new JSONArray();
        for (int i = 0; i < daRiassortire.length(); i++) {
            try {
                JSONObject item = daRiassortire.getJSONObject(i);
                if (item.has(UPDATE) && item.getInt(UPDATE) == UPDATED) {
                    filtered.put(item);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return filtered;
    }

    public String getPath() {
        return this.sharedPref.getFolderDir() + FILE_NAME;
    }

    public boolean isProdottoDaRiassortire(String idProdotto) {
        if (idProdotto == null || idProdotto.isEmpty()) {
            return false;
        }
        if (daRiassortire == null || daRiassortireSet == null) {
            load();
        }
        return daRiassortireSet != null && daRiassortireSet.contains(idProdotto);
    }

    public DaRiassortire load() {
        try {
            String content = Utils.readFromFile(getPath());
            if (content == null || content.trim().isEmpty()) {
                daRiassortire = new JSONArray();
            } else {
                daRiassortire = new JSONArray(content);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            daRiassortire = new JSONArray();
        }
        rebuildIndex();
        return this;
    }

    public void reload() {
        daRiassortire = null;
        daRiassortireSet = null;
        load();
    }

    public void setRiassortimento(String idProdotto, int stato) {
        if (daRiassortire == null) {
            load();
        }
        boolean found = false;
        int i = 0;
        while (i < daRiassortire.length()) {
            try {
                JSONObject item = daRiassortire.getJSONObject(i);
                if (item.getString("pid").equals(idProdotto)) {
                    found = true;
                    item.put(VAL, stato);
                    item.put(UPDATE, UPDATED);
                    daRiassortire.put(i, item);
                    break;
                }
                i++;
            } catch (JSONException e) {
                e.printStackTrace();
                i++;
            }
        }
        if (!found) {
            try {
                JSONObject newItem = new JSONObject();
                newItem.put("pid", idProdotto);
                newItem.put(VAL, stato);
                newItem.put(UPDATE, UPDATED);
                daRiassortire.put(newItem);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (daRiassortireSet == null) {
            rebuildIndex();
        } else if (stato == DA_RIASSORTIRE) {
            daRiassortireSet.add(idProdotto);
        } else {
            daRiassortireSet.remove(idProdotto);
        }
        updateFile();
    }

    public void updateFile() {
        try {
            Utils.writeToFile(getPath(), daRiassortire.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
