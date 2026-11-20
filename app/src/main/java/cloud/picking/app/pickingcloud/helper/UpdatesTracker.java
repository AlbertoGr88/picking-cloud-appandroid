package cloud.picking.app.pickingcloud.helper;

public class UpdatesTracker {
    private static boolean listeModificate = false;
    private static boolean ordiniActiviy = false;
    private static Integer ordiniInCoda = null;

    public static void cancelRefreshListe() {
        listeModificate = false;
    }

    public static void cancelRefreshOrdiniActivity() {
        ordiniActiviy = false;
    }

    public static Integer getOrdiniInCoda() {
        return ordiniInCoda;
    }

    public static boolean isToRefreshListe() {
        return listeModificate;
    }

    public static boolean isToRefreshOrdiniActivity() {
        return ordiniActiviy;
    }

    public static void refreshListe() {
        listeModificate = true;
    }

    public static void refreshOrdiniActivity() {
        ordiniActiviy = true;
    }

    public static void needToRefreshOrdiniActivity() {
        ordiniActiviy = true;
    }

    public static void setOrdiniInCoda(Integer count) {
        ordiniInCoda = count;
    }
}
