package cloud.picking.app.pickingcloud.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import cloud.picking.app.pickingcloud.helper.SharedPref;
import cloud.picking.app.pickingcloud.helper.UpdatesTracker;
import cloud.picking.app.pickingcloud.helper.Utils;

public class Lista {
    private static final String FILE_NAME = "liste.json";
    private static final String TAG = "Lista";
    private static JSONObject liste = null;
    private SharedPref sharedPref;

    public Lista(SharedPref sharedPref) {
        this.sharedPref = sharedPref;
    }

    public boolean aggiungiProdotto(String key, String barcode, String quantita, String descrizione, boolean nuovo, String prodottoId) {
        String cleanBarcode = Utils.sanitizeBarcode(barcode);
        String cleanQtyStr = quantita != null ? quantita.trim().replace(',', '.') : "0";
        if (cleanBarcode.isEmpty() || cleanQtyStr.isEmpty() || !Utils.isFloat(cleanQtyStr)) {
            return false;
        }
        JSONObject lista = getByKey(key);
        if (lista == null) {
            return false;
        }
        try {
            JSONArray prodotti = lista.optJSONArray("prodotti");
            if (prodotti == null) {
                prodotti = new JSONArray();
            }
            int foundIndex = -1;
            for (int i = 0; i < prodotti.length(); i++) {
                JSONObject item = prodotti.optJSONObject(i);
                if (item != null && item.optString("id", item.optString("barcode", "")).equals(cleanBarcode)) {
                    foundIndex = i;
                    break;
                }
            }
            if (foundIndex >= 0) {
                JSONObject existing = prodotti.getJSONObject(foundIndex);
                String oldQtyStr = existing.optString("quantita", "0").replace(',', '.');
                float oldQty = Utils.isFloat(oldQtyStr) ? Float.parseFloat(oldQtyStr) : 0.0f;
                float newQty = Float.parseFloat(cleanQtyStr);
                if (!nuovo) {
                    newQty += oldQty;
                }
                existing.put("id", cleanBarcode);
                existing.put("barcode", cleanBarcode);
                existing.put("quantita", String.valueOf(Utils.floatRound(newQty, 3)));
                prodotti.put(foundIndex, existing);
            } else {
                float parsedQty = Utils.floatRound(Float.parseFloat(cleanQtyStr), 3);
                JSONObject newProd = new JSONObject();
                newProd.put("id", cleanBarcode);
                newProd.put("barcode", cleanBarcode);
                newProd.put("prodotto_id", prodottoId != null ? prodottoId : "0");
                newProd.put("descrizione", descrizione != null ? descrizione : "");
                newProd.put("quantita", String.valueOf(parsedQty));
                newProd.put("data_inserimento", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                prodotti.put(newProd);
            }
            lista.put("prodotti", prodotti);
            return save(lista);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean cambiaStato(String key, boolean completa) {
        JSONObject lista = getByKey(key);
        if (lista == null) {
            return false;
        }
        try {
            String flag = completa ? "1" : "0";
            lista.put("completata", flag);
            lista.put("completa", flag);
            return save(lista);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean cancellaProdottoPerPosizione(String key, String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        JSONObject lista = getByKey(key);
        if (lista == null) {
            return false;
        }
        try {
            JSONArray prodotti = lista.optJSONArray("prodotti");
            JSONArray updated = new JSONArray();
            if (prodotti != null) {
                for (int i = 0; i < prodotti.length(); i++) {
                    JSONObject item = prodotti.optJSONObject(i);
                    if (item != null && !item.optString("id", item.optString("barcode", "")).equals(id)) {
                        updated.put(item);
                    }
                }
            }
            lista.put("prodotti", updated);
            return save(lista);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public JSONObject get() {
        if (liste == null) {
            load();
        }
        return liste;
    }

    public JSONObject getByKey(String key) {
        if (liste == null) {
            load();
        }
        if (liste != null && liste.has(key)) {
            try {
                return liste.getJSONObject(key);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public JSONArray getComplete() {
        if (liste == null) {
            load();
        }
        JSONArray result = new JSONArray();
        if (liste == null) {
            return result;
        }
        List<JSONObject> items = new ArrayList<>();
        Iterator<String> keys = liste.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (liste.get(key) instanceof JSONObject) {
                    JSONObject item = liste.getJSONObject(key);
                    if (!item.has("eliminata") || !item.getString("eliminata").equals("1")) {
                        items.add(item);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(items, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject a, JSONObject b) {
                int cmp = compareDatesDesc(a.optString("data_creazione", ""), b.optString("data_creazione", ""));
                if (cmp != 0) {
                    return cmp;
                }
                return compareDatesDesc(a.optString("data_ultima_modifica", ""), b.optString("data_ultima_modifica", ""));
            }
        });
        for (JSONObject item : items) {
            result.put(item);
        }
        return result;
    }

    private int compareDatesDesc(String d1, String d2) {
        String s1 = d1 != null ? d1.trim() : "";
        String s2 = d2 != null ? d2.trim() : "";
        if (s1.isEmpty() && s2.isEmpty()) {
            return 0;
        }
        if (s1.isEmpty()) {
            return 1;
        }
        if (s2.isEmpty()) {
            return -1;
        }
        long t1 = parseDateMillis(s1);
        long t2 = parseDateMillis(s2);
        if (t1 != Long.MIN_VALUE && t2 != Long.MIN_VALUE) {
            return Long.compare(t2, t1);
        }
        return s2.compareTo(s1);
    }

    private long parseDateMillis(String value) {
        String[] patterns = new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                sdf.setLenient(false);
                Date parsed = sdf.parse(value);
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (ParseException ignored) {
            }
        }
        return Long.MIN_VALUE;
    }

    public String getPath() {
        return this.sharedPref.getFolderDir() + FILE_NAME;
    }

    public void load() {
        try {
            String content = Utils.readFromFile(getPath());
            if (content == null || content.trim().isEmpty()) {
                liste = new JSONObject();
            } else {
                liste = new JSONObject(content);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            liste = new JSONObject();
        }
    }

    public void reload() {
        liste = null;
        load();
    }

    public boolean save(JSONObject lista) {
        if (liste == null) {
            load();
        }
        try {
            UpdatesTracker.refreshListe();
            lista.put("data_ultima_modifica", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            liste.put(lista.getString("key"), lista);
            return Utils.createOrWriteFile(getPath(), liste.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Restituisce una copia delle liste pronte per la sincronizzazione col server:
     * esclude quelle eliminate e normalizza `completa`, `data` e `barcode`.
     */
    public JSONObject getForSync() {
        if (liste == null) {
            load();
        }
        JSONObject payload = new JSONObject();
        if (liste == null) {
            return payload;
        }
        Iterator<String> keys = liste.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object raw = liste.get(key);
                if (!(raw instanceof JSONObject)) {
                    continue;
                }
                JSONObject source = (JSONObject) raw;
                if ("1".equals(source.optString("eliminata", "0"))) {
                    continue;
                }
                JSONObject copy = new JSONObject(source.toString());
                if (!copy.has("key") || copy.optString("key", "").isEmpty()) {
                    copy.put("key", key);
                }
                String completaFlag = copy.optString("completata", copy.optString("completa", "0"));
                copy.put("completata", completaFlag);
                copy.put("completa", completaFlag);

                String dataVal = copy.optString("data_creazione", copy.optString("data", ""));
                if (!dataVal.isEmpty()) {
                    copy.put("data", dataVal);
                    copy.put("data_creazione", dataVal);
                }

                JSONArray prodottiSrc = copy.optJSONArray("prodotti");
                JSONArray prodottiOut = new JSONArray();
                if (prodottiSrc != null) {
                    for (int i = 0; i < prodottiSrc.length(); i++) {
                        JSONObject prodSrc = prodottiSrc.optJSONObject(i);
                        if (prodSrc == null) {
                            continue;
                        }
                        JSONObject prodCopy = new JSONObject(prodSrc.toString());
                        String barcode = prodCopy.optString("barcode", prodCopy.optString("id", ""));
                        prodCopy.put("barcode", barcode);
                        prodCopy.put("id", barcode);
                        if ("0".equals(prodCopy.optString("prodotto_id", ""))) {
                            prodCopy.put("prodotto_id", "");
                        }
                        prodottiOut.put(prodCopy);
                    }
                }
                copy.put("prodotti", prodottiOut);
                payload.put(key, copy);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return payload;
    }

    public boolean saveAll(JSONObject allListe) {
        try {
            UpdatesTracker.refreshListe();
            liste = allListe != null ? allListe : new JSONObject();
            return Utils.createOrWriteFile(getPath(), liste.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean cancellaPerPosizione(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        if (liste == null) {
            load();
        }
        if (liste != null && liste.has(key)) {
            try {
                JSONObject item = liste.getJSONObject(key);
                item.put("eliminata", "1");
                return save(item);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean nuova(String nome) {
        return saveNew(nome) != null;
    }

    public boolean svuotaComplete() {
        return saveAll(new JSONObject());
    }

    public JSONObject saveNew(String nome) {
        if (liste == null) {
            load();
        }
        try {
            String key = UUID.randomUUID().toString();
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            JSONObject newLista = new JSONObject();
            newLista.put("key", key);
            newLista.put("nome", nome);
            newLista.put("completata", "0");
            newLista.put("completa", "0");
            newLista.put("eliminata", "0");
            newLista.put("prodotti", new JSONArray());
            newLista.put("data", now);
            newLista.put("data_creazione", now);
            newLista.put("data_ultima_modifica", now);
            if (save(newLista)) {
                return newLista;
            }
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
