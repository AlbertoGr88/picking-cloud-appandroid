package cloud.picking.app.pickingcloud.helper;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public class SharedPref {
    public static final String AUTH = "auth";
    public static final String AUTOSYNC = "autosync";
    public static final String AUTO_ADD_QTY = "autoaddqty";
    public static final String AUTO_NEXT = "autonext";
    public static final String CODICE_LICENZA = "codice_licenza";
    public static final String COLLI_UPDATED = "colli_updated";
    // TODO: [TEST V2] Rimuovere CUSTOM_API_URL e relativi getter/setter al termine dei test v2
    public static final String CUSTOM_API_URL = "custom_api_url";
    public static final String DATABASE_NAME = "picking_prefs";
    public static final String FOLDER_DIR = "folder_dir";
    public static final String ID = "id";
    public static final String LICENSE = "license";
    public static final String MAX_PESO_COLLO_WARNING = "max_peso_collo_warning";
    public static final String MODULO_COLLI = "modulo_colli";
    public static final String PREFS_NAME = "picking_prefs";
    public static final String SESSION_NAME = "picking_cloud_web_api";
    public static final String SOFT_KEYBOARD = "keyboard";
    public static final String SOUND_ON_ERROR = "sound_on_error";
    public static final String isFirstSyncPerformed = "isFirstSyncPerformed";

    private SharedPreferences authSharedPreferences = null;
    private Context c;
    private SharedPreferences.Editor editor = null;
    private SharedPreferences sharedPreferences;

    public SharedPref(Context context) {
        this.c = context;
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Api.setCustomBaseUrl(getCustomApiUrl());
        if (isAuth()) {
            applyLicenseRouting();
        }
    }

    public String getCustomApiUrl() {
        return this.sharedPreferences.getString(CUSTOM_API_URL, null);
    }

    public void setCustomApiUrl(String customUrl) {
        String normalized = Api.normalizeCustomUrl(customUrl);
        if (normalized == null) {
            edit().remove(CUSTOM_API_URL).commit();
            Api.setCustomBaseUrl(null);
        } else {
            edit().putString(CUSTOM_API_URL, normalized).commit();
            Api.setCustomBaseUrl(normalized);
        }
    }

    public void applyLicenseRouting() {
        Api.setCustomBaseUrl(getCustomApiUrl());
        JSONObject auth = getAuth();
        if (auth != null) {
            Api.configureForLicense(auth.optString(CODICE_LICENZA, ""));
        } else {
            Api.configureForLicense(null);
        }
    }

    public SharedPreferences.Editor edit() {
        if (this.editor == null) {
            this.editor = this.sharedPreferences.edit();
        }
        return this.editor;
    }

    public JSONObject getAuth() {
        try {
            String raw = this.sharedPreferences.getString(AUTH, null);
            if (raw != null) {
                return new JSONObject(raw);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public SharedPreferences getAuthPrefs() {
        if (this.authSharedPreferences == null) {
            String fileName = getAuthPrefsFileName();
            if (fileName == null) {
                fileName = PREFS_NAME;
            }
            this.authSharedPreferences = this.c.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        }
        return this.authSharedPreferences;
    }

    public SharedPreferences getAuthSharedPreferences() {
        return getAuthPrefs();
    }

    public String getAuthPrefsFileName() {
        try {
            JSONObject auth = getAuth();
            if (auth != null) {
                return auth.getString(CODICE_LICENZA) + "_" + auth.getString(ID) + "_" + PREFS_NAME;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getAuthSharedPreferencesName() {
        return getAuthPrefsFileName();
    }

    public String getDatabaseName() {
        return getAuthPrefsFileName();
    }

    public String getFolderDir() {
        String dir = this.sharedPreferences.getString(FOLDER_DIR, "");
        if (dir != null && !dir.isEmpty() && !dir.endsWith(java.io.File.separator)) {
            dir = dir + java.io.File.separator;
        }
        return dir;
    }

    public JSONObject getLicense() {
        try {
            String raw = this.sharedPreferences.getString(LICENSE, null);
            if (raw != null) {
                return new JSONObject(raw);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isModuloColliEnabled() {
        JSONObject license = getLicense();
        if (license == null || !license.has(MODULO_COLLI)) {
            return false;
        }
        Object value = license.opt(MODULO_COLLI);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            return "1".equals(str) || "true".equalsIgnoreCase(str);
        }
        return false;
    }

    public String getSession() {
        return this.sharedPreferences.getString(SESSION_NAME, "");
    }

    public float getMaxPesoColloWarning() {
        String raw = getAuthPrefs().getString(MAX_PESO_COLLO_WARNING, "30");
        if (raw == null) {
            return 0.0f;
        }
        String clean = raw.trim().replace(',', '.');
        if (clean.isEmpty() || !Utils.isFloat(clean)) {
            return 0.0f;
        }
        float val = Utils.floatRound(Float.parseFloat(clean), 3);
        return val > 0.0f ? val : 0.0f;
    }

    public boolean isAuth() {
        return getAuth() != null;
    }

    public boolean isAutoAddQtyEnabled() {
        return getAuthPrefs().getBoolean(AUTO_ADD_QTY, false);
    }

    public boolean isAutoAddQty() {
        return isAutoAddQtyEnabled();
    }

    public boolean isAutoNextEnabled() {
        return getAuthPrefs().getBoolean(AUTO_NEXT, false);
    }

    public boolean isAutoNext() {
        return isAutoNextEnabled();
    }

    public boolean isAutoSyncEnabled() {
        return getAuthPrefs().getBoolean(AUTOSYNC, false);
    }

    public boolean isAutoSync() {
        return isAutoSyncEnabled();
    }

    public void isFirstSyncPerformed(boolean value) {
        getAuthPrefs().edit().putBoolean(isFirstSyncPerformed, value).commit();
    }

    public boolean isFirstSyncPerformed() {
        return getAuthPrefs().getBoolean(isFirstSyncPerformed, false);
    }

    public boolean isKeyboardVisible() {
        return getAuthPrefs().getBoolean(SOFT_KEYBOARD, false);
    }

    public boolean isSoundOnErrorEnabled() {
        return getAuthPrefs().getBoolean(SOUND_ON_ERROR, false);
    }

    public boolean isSoundOnError() {
        return isSoundOnErrorEnabled();
    }

    public boolean hasColliUpdated() {
        return getAuthPrefs().getBoolean(COLLI_UPDATED, false);
    }

    public void setColliUpdated(boolean updated) {
        getAuthPrefs().edit().putBoolean(COLLI_UPDATED, updated).commit();
    }

    public void keyboard(boolean visible) {
        getAuthPrefs().edit().putBoolean(SOFT_KEYBOARD, visible).commit();
    }

    public String lastSync() {
        return getAuthPrefs().getString("last_sync", "-");
    }

    public void lastSync(String time) {
        getAuthPrefs().edit().putString("last_sync", time).commit();
    }

    public void logout() {
        edit().remove(AUTH).remove(LICENSE).commit();
        Api.configureForLicense(null);
    }

    public void saveSession(String session) {
        edit().putString(SESSION_NAME, session).commit();
    }

    public void setSession(String session) {
        saveSession(session);
    }

    public void setAuth(JSONObject auth) {
        if (auth != null) {
            edit().putString(AUTH, auth.toString()).commit();
            Api.configureForLicense(auth.optString(CODICE_LICENZA, ""));
        }
    }

    public void setFolderDir(String dir) {
        if (dir != null && !dir.isEmpty() && !dir.endsWith(java.io.File.separator)) {
            dir = dir + java.io.File.separator;
        }
        edit().putString(FOLDER_DIR, dir).commit();
    }

    public void setLicense(JSONObject license) {
        if (license != null) {
            edit().putString(LICENSE, license.toString()).commit();
        }
    }
}
