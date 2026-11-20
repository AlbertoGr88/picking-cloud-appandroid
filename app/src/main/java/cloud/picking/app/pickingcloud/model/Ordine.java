package cloud.picking.app.pickingcloud.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.UpdatesTracker;
import cloud.picking.app.pickingcloud.helper.Utils;

public class Ordine {
    private static final String FILE_NAME = "ordini.json";
    private static final String TAG = "Ordine";
    private static JSONArray ordini = null;
    private SharedPref sharedPref;

    public Ordine(SharedPref sharedPref) {
        this.sharedPref = sharedPref;
    }

    public boolean aggiornaOrdine(String ido, String idRiga, String scannerizzato) {
        return aggiornaOrdine(ido, idRiga, scannerizzato, "0");
    }

    public boolean aggiornaOrdine(String ido, String idRiga, String scannerizzato, String forzaScan) {
        boolean updated = false;
        if (ordini == null) {
            load();
        }
        int i = 0;
        while (true) {
            if (i >= ordini.length()) {
                break;
            }
            try {
                JSONObject ordine = ordini.getJSONObject(i);
                if (ordine.getString("ido").equals(ido)) {
                    JSONArray prd = ordine.optJSONArray("prodotti");
                    if (prd == null) {
                        prd = ordine.optJSONArray("prd");
                    }
                    if (prd == null) {
                        prd = new JSONArray();
                    }
                    boolean allLinesCompleted = true;
                    for (int j = 0; j < prd.length(); j++) {
                        JSONObject riga = prd.getJSONObject(j);
                        String rigaId = riga.has("IdRigaOrdine") ? riga.optString("IdRigaOrdine") : riga.optString("IdRiga");
                        if (rigaId.equals(idRiga)) {
                            String cleanScan = scannerizzato != null ? scannerizzato.replace(',', '.') : "0";
                            float scanVal = Utils.isFloat(cleanScan) ? Utils.floatRound(Float.parseFloat(cleanScan), 3) : 0.0f;
                            riga.put("Scannerizzato", String.valueOf(scanVal));
                            riga.put("updated", "1");
                            if (forzaScan != null && !forzaScan.isEmpty()) {
                                riga.put("forza_scan", forzaScan);
                            }
                            prd.put(j, riga);
                        }
                        float qty = Float.parseFloat(riga.optString("Qty", "0").replace(',', '.'));
                        float scan = Float.parseFloat(riga.optString("Scannerizzato", "0").replace(',', '.'));
                        if (Utils.floatRound(qty, 3) > Utils.floatRound(scan, 3)) {
                            allLinesCompleted = false;
                        }
                    }
                    ordine.put("prodotti", prd);
                    ordine.put("updated", "1");
                    if (allLinesCompleted) {
                        ordine.put("StatoOrdine", "1");
                    } else {
                        ordine.put("StatoOrdine", "0");
                    }
                    ordini.put(i, ordine);
                    updated = true;
                    break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            i++;
        }
        if (updated) {
            try {
                UpdatesTracker.refreshOrdiniActivity();
                Utils.writeToFile(getPath(), ordini.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return updated;
    }

    public boolean aggiornaNumeroColli(String ido, String numeroColli) {
        boolean updated = false;
        if (ordini == null) {
            load();
        }
        for (int i = 0; i < ordini.length(); i++) {
            try {
                JSONObject ordine = ordini.getJSONObject(i);
                if (ordine.getString("ido").equals(ido)) {
                    ordine.put("NumeroColli", numeroColli);
                    ordine.put("updated", "1");
                    ordini.put(i, ordine);
                    updated = true;
                    break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (updated) {
            try {
                UpdatesTracker.needToRefreshOrdiniActivity();
                UpdatesTracker.refreshOrdiniActivity();
                Utils.writeToFile(getPath(), ordini.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return updated;
    }

    public boolean chiudiOrdine(String ido, String numeroColli, String note) {
        boolean updated = false;
        if (ordini == null) {
            load();
        }
        int i = 0;
        while (true) {
            if (i >= ordini.length()) {
                break;
            }
            try {
                JSONObject ordine = ordini.getJSONObject(i);
                if (ordine.getString("ido").equals(ido)) {
                    ordine.put("NumeroColli", numeroColli);
                    ordine.put("Note", note);
                    ordine.put("StatoOrdine", "1");
                    ordine.put("updated", "1");
                    java.util.Date now = new java.util.Date();
                    ordine.put("DataChiusura", new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ITALY).format(now));
                    ordine.put("OraChiusura", new java.text.SimpleDateFormat("HH:mm", java.util.Locale.ITALY).format(now));
                    ordini.put(i, ordine);
                    updated = true;
                    break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            i++;
        }
        if (updated) {
            try {
                Utils.writeToFile(getPath(), ordini.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return updated;
    }

    public JSONArray get() {
        if (ordini == null) {
            load();
        }
        return ordini;
    }

    public JSONObject getById(String idOrdine) {
        if (ordini == null) {
            load();
        }
        for (int i = 0; i < ordini.length(); i++) {
            try {
                JSONObject item = ordini.getJSONObject(i);
                if (item.getString("ido").equals(idOrdine)) {
                    return item;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public JSONArray getByNumero(ArrayList<String> numeri) {
        if (ordini == null) {
            load();
        }
        JSONArray result = new JSONArray();
        for (int i = 0; i < ordini.length(); i++) {
            try {
                JSONObject item = ordini.getJSONObject(i);
                if (numeri.contains(item.getString("ido"))) {
                    result.put(item);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public JSONArray getCoda() {
        if (ordini == null) {
            load();
        }
        JSONArray result = new JSONArray();
        for (int i = 0; i < ordini.length(); i++) {
            try {
                JSONObject item = ordini.getJSONObject(i);
                String stato = item.has("StatoOrdine") ? item.optString("StatoOrdine", "0") : item.optString("cmp", "0");
                if ("0".equals(stato)) {
                    result.put(item);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public JSONArray getCompletati() {
        if (ordini == null) {
            load();
        }
        JSONArray result = new JSONArray();
        for (int i = 0; i < ordini.length(); i++) {
            try {
                JSONObject item = ordini.getJSONObject(i);
                String stato = item.has("StatoOrdine") ? item.optString("StatoOrdine", "0") : item.optString("cmp", "0");
                if ("1".equals(stato)) {
                    result.put(item);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public JSONArray getDaSincronizzare() {
        if (ordini == null) {
            load();
        }
        JSONArray result = new JSONArray();
        for (int i = 0; i < ordini.length(); i++) {
            try {
                JSONObject item = ordini.getJSONObject(i);
                String upd = item.has("updated") ? item.optString("updated", "0") : item.optString("mod", "0");
                if ("1".equals(upd)) {
                    result.put(item);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public String getPath() {
        return this.sharedPref.getFolderDir() + FILE_NAME;
    }

    public JSONArray getProdottiByIdOrdini(ArrayList<String> idOrdini) {
        if (ordini == null) {
            load();
        }
        JSONArray ordiniSelezionati = getByNumero(idOrdini);
        Prodotto prodottoModel = new Prodotto(this.sharedPref);
        JSONArray result = new JSONArray();
        try {
            if (ordiniSelezionati.length() > 1) {
                for (int i = 0; i < ordiniSelezionati.length(); i++) {
                    JSONObject ordObj = ordiniSelezionati.getJSONObject(i);
                    JSONArray prd = ordObj.has("prodotti") ? ordObj.optJSONArray("prodotti") : ordObj.optJSONArray("prd");
                    if (prd == null) {
                        continue;
                    }
                    for (int j = 0; j < prd.length(); j++) {
                        JSONObject sourceLine = prd.getJSONObject(j);
                        JSONObject lineCopy = new JSONObject(sourceLine.toString());
                        int existingIdx = indexProdottoInArray(lineCopy, result);
                        if (existingIdx >= 0) {
                            JSONObject existing = result.getJSONObject(existingIdx);
                            float q1 = Float.parseFloat(existing.optString("Qty", "0").replace(',', '.'));
                            float q2 = Float.parseFloat(lineCopy.optString("Qty", "0").replace(',', '.'));
                            existing.put("Qty", String.valueOf(Utils.floatRound(q1 + q2, 3)));

                            float s1 = Float.parseFloat(existing.optString("Scannerizzato", "0").replace(',', '.'));
                            float s2 = Float.parseFloat(lineCopy.optString("Scannerizzato", "0").replace(',', '.'));
                            existing.put("Scannerizzato", String.valueOf(Utils.floatRound(s1 + s2, 3)));
                            result.put(existingIdx, existing);
                        } else {
                            float q = Float.parseFloat(lineCopy.optString("Qty", "0").replace(',', '.'));
                            float s = Float.parseFloat(lineCopy.optString("Scannerizzato", "0").replace(',', '.'));
                            lineCopy.put("Qty", String.valueOf(Utils.floatRound(q, 3)));
                            lineCopy.put("Scannerizzato", String.valueOf(Utils.floatRound(s, 3)));
                            result.put(lineCopy);
                        }
                    }
                }
            } else if (ordiniSelezionati.length() > 0) {
                JSONObject ordObj = ordiniSelezionati.getJSONObject(0);
                JSONArray prd = ordObj.has("prodotti") ? ordObj.optJSONArray("prodotti") : ordObj.optJSONArray("prd");
                if (prd == null) {
                    prd = new JSONArray();
                }
                for (int j = 0; j < prd.length(); j++) {
                    JSONObject lineCopy = new JSONObject(prd.getJSONObject(j).toString());
                    float q = Float.parseFloat(lineCopy.optString("Qty", "0").replace(',', '.'));
                    float s = Float.parseFloat(lineCopy.optString("Scannerizzato", "0").replace(',', '.'));
                    lineCopy.put("Qty", String.valueOf(Utils.floatRound(q, 3)));
                    lineCopy.put("Scannerizzato", String.valueOf(Utils.floatRound(s, 3)));
                    result.put(lineCopy);
                }
            }

            for (int i = 0; i < result.length(); i++) {
                JSONObject item = result.getJSONObject(i);
                JSONObject anagrafica = prodottoModel.getByIdProdotto(item.has("pid") ? item.optString("pid", "") : item.optString("IdProdotto", ""));
                if (anagrafica != null) {
                    item.put("pde", anagrafica.optString("pde", ""));
                    item.put("pub", anagrafica.optString("pub", ""));
                    JSONArray codes = anagrafica.optJSONArray("codes");
                    item.put("codes", codes != null ? codes : new JSONArray());
                } else {
                    if (!item.has("pde")) {
                        item.put("pde", "");
                    }
                    if (!item.has("pub")) {
                        item.put("pub", "");
                    }
                    if (!item.has("codes")) {
                        item.put("codes", new JSONArray());
                    }
                }
                result.put(i, item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    public int indexProdottoInArray(JSONObject prod, JSONArray array) {
        String prodId = prod.has("pid") ? prod.optString("pid", "") : prod.optString("IdProdotto", "");
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject item = array.getJSONObject(i);
                String itemId = item.has("pid") ? item.optString("pid", "") : item.optString("IdProdotto", "");
                if (!itemId.isEmpty() && itemId.equals(prodId)) {
                    return i;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    public JSONObject getByIdOrdine(String idOrdine) {
        return getById(idOrdine);
    }

    public JSONArray getProdottiRaggruppatiByIdOrdini(ArrayList<String> idOrdini) {
        return getProdottiByIdOrdini(idOrdini);
    }

    public boolean setModalitaPrelievo(ArrayList<String> idOrdini, int modalita) {
        if (ordini == null) {
            load();
        }
        boolean updated = false;
        for (int i = 0; i < ordini.length(); i++) {
            try {
                JSONObject ordine = ordini.getJSONObject(i);
                if (idOrdini != null && idOrdini.contains(ordine.getString("ido"))) {
                    ordine.put("modalita_prelievo", modalita);
                    ordini.put(i, ordine);
                    updated = true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if (updated) {
            try {
                Utils.writeToFile(getPath(), ordini.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return updated;
    }

    public boolean setProdottoScannerizzato(String idOrdine, String idRiga, String scannerizzato, String forzaScan) {
        return aggiornaOrdine(idOrdine, idRiga, scannerizzato, forzaScan);
    }

    public boolean completa(ArrayList<String> idOrdini, String numeroColli, String note) {
        if (idOrdini == null || idOrdini.isEmpty()) {
            return false;
        }
        boolean allOk = true;
        for (int i = 0; i < idOrdini.size(); i++) {
            if (!chiudiOrdine(idOrdini.get(i), numeroColli, note)) {
                allOk = false;
            }
        }
        return allOk;
    }

    public Ordine load() {
        try {
            String content = Utils.readFromFile(getPath());
            if (content == null || content.trim().isEmpty()) {
                ordini = new JSONArray();
            } else {
                ordini = new JSONArray(content);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            ordini = new JSONArray();
        }
        return this;
    }

    public void reload() {
        ordini = null;
        load();
    }
}
