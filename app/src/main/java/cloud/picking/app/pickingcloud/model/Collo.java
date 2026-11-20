package cloud.picking.app.pickingcloud.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.Utils;

public class Collo {
    private static final String FILE_NAME = "colli.json";
    private static final String TAG = "Collo";
    private static JSONObject colli = null;
    private SharedPref sharedPref;

    public Collo(SharedPref sharedPref) {
        this.sharedPref = sharedPref;
    }

    public boolean clear() {
        colli = new JSONObject();
        try {
            return Utils.createOrWriteFile(getPath(), colli.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public JSONObject get() {
        if (colli == null) {
            load();
        }
        return colli;
    }

    public String getPath() {
        return this.sharedPref.getFolderDir() + FILE_NAME;
    }

    public Collo load() {
        try {
            String content = Utils.readFromFile(getPath());
            if (content == null || content.trim().isEmpty()) {
                colli = new JSONObject();
            } else {
                colli = new JSONObject(content);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            colli = new JSONObject();
        }
        return this;
    }

    public boolean setColliOrdine(String idOrdine, JSONArray colliArray) {
        return save(idOrdine, colliArray);
    }

    public boolean save(String idOrdine, JSONArray colliArray) {
        if (colli == null) {
            load();
        }
        try {
            colli.put(idOrdine, colliArray);
            boolean written = Utils.createOrWriteFile(getPath(), colli.toString());
            if (written) {
                this.sharedPref.setColliUpdated(true);
            }
            return written;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
