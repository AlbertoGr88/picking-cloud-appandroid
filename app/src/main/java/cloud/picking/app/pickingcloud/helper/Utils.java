package cloud.picking.app.pickingcloud.helper;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import cloud.picking.app.pickingcloud.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Utils {
    private static final int BUFFER = 8192;
    public static final int KEYCODE_SCAN_HSCAN = 229;
    public static final int KEYCODE_SCAN_TRIGGER = 287;

    public static boolean createFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        return file.createNewFile();
    }

    public static boolean createOrWriteFile(String filePath, String data) throws IOException {
        if (!new File(filePath).exists()) {
            createFile(filePath);
        }
        return writeToFile(filePath, data);
    }

    public static Double doubleRound(Double d, int decimalPlace) {
        if (d == null || Double.isNaN(d) || Double.isInfinite(d)) {
            return 0.0d;
        }
        return new BigDecimal(Double.toString(d)).setScale(decimalPlace, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * Formatta una quantità omettendo ".0" se il valore è intero.
     */
    public static String formatQuantity(Float value) {
        return formatQuantity(value, null);
    }

    /**
     * Formatta una quantità mostrando i decimali solo se il valore stesso o la quantità di riferimento
     * contiene una parte decimale non nulla.
     */
    public static String formatQuantity(Float value, Float referenceOrderedQty) {
        if (value == null || Float.isNaN(value) || Float.isInfinite(value)) {
            return hasDecimalPart(referenceOrderedQty) ? "0.0" : "0";
        }
        float rounded = floatRound(value, 3);
        if (Math.abs(rounded) < 0.0005f) {
            return hasDecimalPart(referenceOrderedQty) ? "0.0" : "0";
        }
        BigDecimal bd = new BigDecimal(Float.toString(rounded)).stripTrailingZeros();
        if (bd.scale() <= 0) {
            if (hasDecimalPart(referenceOrderedQty)) {
                return String.valueOf(bd.longValue()) + ".0";
            }
            return String.valueOf(bd.longValue());
        }
        return bd.toPlainString();
    }

    /**
     * Verifica se una quantità possiede una parte decimale significativa.
     */
    public static boolean hasDecimalPart(Float value) {
        if (value == null || Float.isNaN(value) || Float.isInfinite(value)) {
            return false;
        }
        float rounded = floatRound(value, 3);
        return Math.abs(rounded - Math.round(rounded)) >= 0.0005f;
    }

    public static Float floatRound(Float d, int decimalPlace) {
        if (d == null || Float.isNaN(d) || Float.isInfinite(d)) {
            return 0.0f;
        }
        return new BigDecimal(Float.toString(d)).setScale(decimalPlace, BigDecimal.ROUND_HALF_UP).floatValue();
    }

    public static String getDateFromStr(String str) {
        return getDateFromStr(str, "yyyy-MM-dd HH:mm:ss", "dd/MM/yyyy");
    }

    public static String getDateFromStr(String str, String formatFrom, String formatTo) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        try {
            return new SimpleDateFormat(formatTo).format(new SimpleDateFormat(formatFrom).parse(str));
        } catch (ParseException e) {
            return "";
        }
    }

    public static String getLastLocationPath(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public static void hideSoftKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static boolean isFloat(String str) {
        if (str == null) {
            return false;
        }
        try {
            Float.parseFloat(str.trim().replace(',', '.'));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isInteger(String s) {
        return isInteger(s, 10);
    }

    public static boolean isInteger(String s, int radix) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) {
                    return false;
                }
            } else if (Character.digit(s.charAt(i), radix) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pulisce il codice a barre letto dai terminali PDA rimuovendo eventuali caratteri CR/LF o spazi.
     */
    public static String sanitizeBarcode(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\r", "").replace("\n", "").trim();
    }

    /**
     * Verifica se il keyCode corrisponde a un tasto di conferma/scansione barcode PDA.
     */
    public static boolean isScanKeyCode(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                || keyCode == KEYCODE_SCAN_HSCAN
                || keyCode == KEYCODE_SCAN_TRIGGER;
    }

    /**
     * Verifica se l'evento proviene da un lettore barcode hardware integrato PDA
     * (Enter, Numpad Enter, o tasti dedicati scan 229 / 287).
     */
    public static boolean isScanActionKey(int keyCode, KeyEvent event) {
        if (event == null || event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }
        return isScanKeyCode(keyCode);
    }

    /**
     * Disabilita la tastiera a schermo mantenendo il focus per il lettore barcode integrato PDA.
     */
    public static void disableSoftKeyboardForScanner(EditText editText) {
        if (editText == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            editText.setShowSoftInputOnFocus(false);
        }
        editText.setInputType(InputType.TYPE_NULL);
    }

    /**
     * Riproduce un tono di avviso/errore rilasciando sempre la risorsa ToneGenerator.
     */
    public static void playErrorTone() {
        try {
            final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        tg.release();
                    } catch (Exception ignored) {
                    }
                }
            }, 250);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Riproduce un file audio raw rilasciando il MediaPlayer al termine della riproduzione.
     */
    public static void playErrorSound(Context context, int rawResId) {
        try {
            MediaPlayer mp = MediaPlayer.create(context, rawResId);
            if (mp != null) {
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        try {
                            mediaPlayer.release();
                        } catch (Exception ignored) {
                        }
                    }
                });
                mp.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return "";
        }
        StringBuilder sb = new StringBuilder((int) Math.min(file.length() + 64, 1024 * 1024));
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)), BUFFER);
            char[] buf = new char[BUFFER];
            int numRead;
            while ((numRead = br.read(buf)) != -1) {
                sb.append(buf, 0, numRead);
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        return sb.toString();
    }

    public static void showSoftKeyboard(View view) {
        if (view != null && view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    public static void showToast(Context c, int res) {
        Toast.makeText(c, res, Toast.LENGTH_SHORT).show();
    }

    public static void showToast(Context c, int res, int duration) {
        Toast.makeText(c, res, duration).show();
    }

    public static void showToast(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
    }

    public static void showToastLong(Context c, int res) {
        Toast.makeText(c, res, Toast.LENGTH_LONG).show();
    }

    public static void showToastLong(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
    }

    public static void toast(Context c, String msg, int duration) {
        Toast.makeText(c, msg, duration).show();
    }

    public static boolean unzip(String zipFile, String location) throws IOException {
        byte[] buffer = new byte[BUFFER];
        try {
            if (!location.endsWith(File.separator)) {
                location = location + File.separator;
            }
            File f = new File(location);
            if (!f.isDirectory()) {
                f.mkdirs();
            }
            ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), BUFFER));
            try {
                while (true) {
                    ZipEntry ze = zin.getNextEntry();
                    if (ze != null) {
                        File unzipFile = new File(location + ze.getName());
                        if (ze.isDirectory()) {
                            if (!unzipFile.isDirectory()) {
                                unzipFile.mkdirs();
                            }
                        } else {
                            File parentDir = unzipFile.getParentFile();
                            if (parentDir != null && !parentDir.isDirectory()) {
                                parentDir.mkdirs();
                            }
                            BufferedOutputStream fout = new BufferedOutputStream(new FileOutputStream(unzipFile, false), BUFFER);
                            try {
                                int size;
                                while ((size = zin.read(buffer, 0, BUFFER)) != -1) {
                                    fout.write(buffer, 0, size);
                                }
                                zin.closeEntry();
                                fout.flush();
                            } finally {
                                fout.close();
                            }
                        }
                    } else {
                        return true;
                    }
                }
            } finally {
                zin.close();
            }
        } catch (Exception e) {
            Log.e("", "Unzip exception", e);
            return false;
        }
    }

    public static boolean writeToFile(String filePath, String data) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        FileWriter writer = new FileWriter(file);
        try {
            writer.write(data != null ? data : "");
            writer.flush();
            return true;
        } finally {
            writer.close();
        }
    }

    public static boolean zip(String[] files, String zipFile) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
        try {
            byte[] data = new byte[BUFFER];
            for (int i = 0; i < files.length; i++) {
                FileInputStream fi = new FileInputStream(files[i]);
                BufferedInputStream origin = new BufferedInputStream(fi, BUFFER);
                try {
                    ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, count);
                    }
                } finally {
                    origin.close();
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            out.close();
        }
    }

    /**
     * Aggiorna barra di avanzamento e tinta del progresso (senza colorare lo sfondo del widget).
     */
    public static void updateOrderProgressBar(ProgressBar progressBar, float done, float total) {
        if (progressBar == null) {
            return;
        }
        int percent = total > 0.0f ? (int) Math.floor((done * 100.0d) / total) : 0;
        if (percent < 0) {
            percent = 0;
        }
        progressBar.setMax(100);
        progressBar.setProgress(Math.min(percent, 100));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int colorRes;
            if (done > total) {
                colorRes = R.color.primaryLightColor;
            } else if (percent >= 100) {
                colorRes = R.color.lightGreen;
            } else if (percent > 25) {
                colorRes = R.color.lightYellow;
            } else {
                colorRes = R.color.lightRed;
            }
            progressBar.setProgressTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(progressBar.getContext(), colorRes)));
        }
    }

    /**
     * Restituisce il livello numerico della priorità di un ordine (1..10), oppure 0 se assente.
     */
    public static int getOrderPriorityValue(JSONObject ordine) {
        if (ordine == null) {
            return 0;
        }
        String raw = ordine.optString("Priorita", "").trim();
        if (raw.isEmpty()) {
            String tag = ordine.optString("PrioritaTag", "").trim();
            if (isInteger(tag)) {
                raw = tag;
            }
        }
        if (isInteger(raw)) {
            int val = Integer.parseInt(raw);
            if (val >= 1 && val <= 10) {
                return val;
            }
        }
        return 0;
    }

    /**
     * Verifica se un ordine ha una priorità impostata.
     */
    public static boolean hasOrderPriority(JSONObject ordine) {
        if (ordine == null) {
            return false;
        }
        return !ordine.optString("Priorita", "").trim().isEmpty()
                || !ordine.optString("PrioritaTag", "").trim().isEmpty();
    }

    /**
     * Restituisce l'etichetta leggibile della priorità per un ordine:
     * - se ha un tag testuale/colore configurato: il nome del tag in maiuscolo (es. "URG")
     * - se è solo un valore numerico senza tag: "P" + numero (es. "P3")
     * - vuoto se non ha priorità.
     */
    public static String getOrderPriorityDisplayLabel(JSONObject ordine) {
        if (ordine == null) {
            return "";
        }
        String tag = ordine.optString("PrioritaTag", "").trim();
        String prio = ordine.optString("Priorita", "").trim();
        String rawColor = ordine.optString("PrioritaColore", "").trim();
        if (tag.isEmpty() && prio.isEmpty()) {
            return "";
        }
        String display = !tag.isEmpty() ? tag : prio;
        boolean hasCustomTag = !rawColor.isEmpty() || (!prio.isEmpty() && !display.equalsIgnoreCase(prio));
        if (!hasCustomTag && isInteger(display)) {
            return "P" + display;
        }
        return display.toUpperCase();
    }

    /**
     * Popola e colora un badge TextView per la priorità dell'ordine:
     * - con tag e colore della licenza: sfondo colorato e testo ad alto contrasto (bianco o scuro se sfondo chiaro)
     * - solo numero senza tag configurato (Opzione B): sfondo slate-200 (#E2E8F0), testo slate-800 (#1E293B) e label "P<num>"
     * - senza priorità: View.GONE.
     */
    public static void bindPrioritaBadge(TextView view, JSONObject ordine) {
        if (view == null) {
            return;
        }
        String label = getOrderPriorityDisplayLabel(ordine);
        if (label.isEmpty()) {
            view.setVisibility(View.GONE);
            return;
        }
        String rawColor = ordine != null ? ordine.optString("PrioritaColore", "").trim() : "";
        String tag = ordine != null ? ordine.optString("PrioritaTag", "").trim() : "";
        String prio = ordine != null ? ordine.optString("Priorita", "").trim() : "";
        boolean hasCustomTag = !rawColor.isEmpty() || (!tag.isEmpty() && !prio.isEmpty() && !tag.equalsIgnoreCase(prio));

        int bgColor;
        int textColor;
        float density = view.getResources().getDisplayMetrics().density;

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(density * 10);

        if (hasCustomTag) {
            bgColor = Color.parseColor("#64748B");
            if (!rawColor.isEmpty()) {
                try {
                    bgColor = Color.parseColor(rawColor);
                } catch (IllegalArgumentException ignored) {
                }
            }
            // Calcolo luminanza percepita per garantire contrasto anche su colori chiari
            double luminance = (0.299 * Color.red(bgColor)
                    + 0.587 * Color.green(bgColor)
                    + 0.114 * Color.blue(bgColor)) / 255.0;
            textColor = luminance > 0.68 ? Color.parseColor("#1E293B") : Color.WHITE;
            background.setColor(bgColor);
        } else {
            // Opzione B: priorità numerica senza tag definito (allineata a PriorityBadge.vue: bg-slate-200 text-slate-800)
            bgColor = Color.parseColor("#E2E8F0");
            textColor = Color.parseColor("#1E293B");
            background.setColor(bgColor);
            background.setStroke(Math.max(1, (int) (density * 1)), Color.parseColor("#CBD5E1"));
        }

        view.setBackground(background);
        view.setTextColor(textColor);
        view.setText(label);
        view.setVisibility(View.VISIBLE);
    }
}
