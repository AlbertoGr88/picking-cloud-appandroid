package cloud.picking.app.pickingcloud.model;

import org.json.JSONArray;

import cloud.picking.app.pickingcloud.helper.SharedPref;

public class Ubicazione {
    private static final String FILE_NAME = "ubicazioni.json";
    private static final String TAG = "Ubicazione";
    private JSONArray ubicazioni = new JSONArray();
    private SharedPref sharedPref;

    public Ubicazione(SharedPref sharedPref) {
        this.sharedPref = sharedPref;
    }

    public String getPath() {
        return this.sharedPref.getFolderDir() + FILE_NAME;
    }
}
