package cloud.picking.app.pickingcloud.helper;

import android.net.Uri;

public class Api {
    public static final String ANAGRAFICHE = "anagrafiche";
    public static final String APP_FORZA_SCAN = "app_forza_scan";
    public static final String APP_LISTE = "app_liste";
    public static final String APP_ORDINI = "app_ordini";
    public static final String APP_UBICAZIONI = "app_ubicazioni";
    public static final String AUTH = "auth";
    public static final String COLLI = "colli";
    public static final String DA_RIASSORTIRE = "da_riassortire";
    public static final String FORCE_LOGOUT = "forcelogout";
    public static final String ID = "id";
    public static final String ISAUTH = "isauth";
    public static final String LICENSE = "license";
    public static final String LISTE = "liste";
    public static final String LOG = "log";
    public static final String OP = "op";
    public static final String ORDINI = "ordini";
    public static final String PRODOTTI = "prodotti";
    public static final String RESPONSE = "response";
    public static final String SUBOP = "subop";
    public static final String UBICAZIONI = "ubicazioni";
    public static final String UBICAZIONI_TEMPORANEE = "ubicazioni_temporanee";
    public static final String UI = "ui";
    public static final String ULTIMA_SINCRONIZZAZIONE = "ultima_sincronizzazione";

    public static final String APP_SUBDOMAIN = "app";
    public static final String DEV_SUBDOMAIN = "dev";
    public static final String RAGSTORE_SUBDOMAIN = "ragstore";

    public static String SUBDOMAIN = APP_SUBDOMAIN;
    public static boolean TEST = true;
    // TODO: [TEST V2] Rimuovere CUSTOM_BASE_URL e i relativi helper al termine dei test v2
    private static String CUSTOM_BASE_URL = null;

    public static String normalizeCustomUrl(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        return trimmed;
    }

    public static boolean isValidCustomUrl(String rawUrl) {
        String normalized = normalizeCustomUrl(rawUrl);
        if (normalized == null) {
            return false;
        }
        Uri parsed = Uri.parse(normalized);
        String scheme = parsed.getScheme();
        String host = parsed.getHost();
        return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && host != null && !host.trim().isEmpty();
    }

    public static void setCustomBaseUrl(String customUrl) {
        String normalized = normalizeCustomUrl(customUrl);
        CUSTOM_BASE_URL = (normalized != null && isValidCustomUrl(normalized)) ? normalized : null;
    }

    public static String getCustomBaseUrl() {
        return CUSTOM_BASE_URL;
    }

    private static Uri.Builder builder() {
        if (CUSTOM_BASE_URL != null) {
            Uri parsed = Uri.parse(CUSTOM_BASE_URL);
            Uri.Builder customBuilder = parsed.buildUpon();
            String path = parsed.getPath();
            if (path == null || path.isEmpty() || "/".equals(path)) {
                customBuilder.path("api/");
            }
            return customBuilder
                    .appendQueryParameter("log", "0")
                    .appendQueryParameter("version_name", "1.7");
        }
        return new Uri.Builder()
                .scheme("https")
                .authority(SUBDOMAIN + ".picking.cloud")
                .path("api/")
                .appendQueryParameter("log", "0")
                .appendQueryParameter("version_name", "1.7");
    }

    public static String getSubdomain() {
        return SUBDOMAIN;
    }

    public static void setSubdomain(String subdomain) {
        if (subdomain == null || subdomain.trim().isEmpty()) {
            SUBDOMAIN = APP_SUBDOMAIN;
        } else {
            SUBDOMAIN = subdomain.trim().toLowerCase();
        }
    }

    public static void setTest(boolean test) {
        TEST = test;
    }

    /**
     * Configura il sottodominio API per qualsiasi codice licenza:
     * - "RAGSTORE" -> "ragstore"
     * - "TEST" o "DEV" -> "dev"
     * - tutte le altre licenze -> "app"
     */
    public static void configureForLicense(String codiceLicenza) {
        if (codiceLicenza == null) {
            SUBDOMAIN = APP_SUBDOMAIN;
            return;
        }
        String normalized = codiceLicenza.trim().toUpperCase();
        if ("RAGSTORE".equals(normalized)) {
            SUBDOMAIN = RAGSTORE_SUBDOMAIN;
        } else if ("TEST".equals(normalized) || "DEV".equals(normalized)) {
            SUBDOMAIN = DEV_SUBDOMAIN;
        } else {
            SUBDOMAIN = APP_SUBDOMAIN;
        }
    }

    public static String login() {
        return builder()
                .appendQueryParameter("op", "login")
                .build()
                .toString();
    }

    public static String auth() {
        return login();
    }

    public static String sync() {
        return builder()
                .appendQueryParameter("op", "sync_fast")
                .appendQueryParameter("subop", "upload")
                .build()
                .toString();
    }

    public static String download() {
        return builder()
                .appendQueryParameter("op", "sync_fast")
                .appendQueryParameter("subop", "download")
                .build()
                .toString();
    }
}
