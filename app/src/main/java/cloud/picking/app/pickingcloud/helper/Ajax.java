package cloud.picking.app.pickingcloud.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.VolleyError;
import com.android.volley.request.SimpleMultiPartRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.picking.app.pickingcloud.activity.LoginActivity;

public class Ajax extends SimpleMultiPartRequest {
    public static final String TAG = "AJAX";
    private Map<String, String> _params;
    private Context c;
    private Map<String, String> mHeaders;
    private SharedPref sharedPref;

    public interface Callback {
        void onResponse(JSONObject response) throws JSONException;
    }

    public interface ErrorCallback {
        void onError(VolleyError error);
    }

    public Ajax(Context context, String url, Callback callback) {
        this(context, url, callback, null, new HashMap<String, String>(), Method.POST);
    }

    public Ajax(Context context, String url, Callback callback, ErrorCallback errorCallback) {
        this(context, url, callback, errorCallback, new HashMap<String, String>(), Method.POST);
    }

    public Ajax(final Context context, String url, final Callback callback, final ErrorCallback errorCallback, Map<String, String> params, int method) {
        super(method, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String responseStr) {
                Log.i(TAG, "Response: " + responseStr);
                try {
                    JSONObject response = new JSONObject(responseStr);
                    JSONArray msgs = response.optJSONArray("msgs");
                    if (msgs != null && msgs.length() > 0) {
                        for (int i = 0; i < msgs.length(); i++) {
                            JSONObject msg = msgs.optJSONObject(i);
                            if (msg != null) {
                                int duration = msg.isNull("duration") ? 0 : msg.optInt("duration", 0);
                                Utils.toast(context, msg.optString("message", ""), duration);
                            }
                        }
                    }
                    if (response.optBoolean("forcelogout", false)) {
                        new SharedPref(context).logout();
                        Intent i = new Intent(context, LoginActivity.class);
                        context.getApplicationContext().startActivity(i);
                        if (context instanceof Activity) {
                            ((Activity) context).finishAffinity();
                        }
                    }
                    if (callback != null) {
                        callback.onResponse(response);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.i(TAG, "Errore JSON: " + e.getMessage());
                    Log.i(TAG, "Response: " + responseStr);
                    if (errorCallback != null) {
                        errorCallback.onError(null);
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "Errore: " + (error != null ? error.getMessage() : "null"));
                if (error != null) {
                    error.printStackTrace();
                }
                if (errorCallback != null) {
                    errorCallback.onError(error);
                }
            }
        });
        this.mHeaders = new HashMap<>(1);
        this.c = context;
        this._params = params;
        this.sharedPref = new SharedPref(context);
        Log.d(TAG, url);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        if (!"".equals(this.sharedPref.getSession())) {
            this.mHeaders.put("Cookie", SharedPref.SESSION_NAME + "=" + this.sharedPref.getSession() + ";");
        }
        return this.mHeaders;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return this._params;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        Map<String, String> headers = response.headers;
        if (headers != null && headers.containsKey("Set-Cookie")) {
            List<HttpCookie> cookies = HttpCookie.parse(headers.get("Set-Cookie"));
            for (int i = 0; i < cookies.size(); i++) {
                HttpCookie cookie = cookies.get(i);
                if (SharedPref.SESSION_NAME.equals(cookie.getName())) {
                    this.sharedPref.saveSession(cookie.getValue());
                    break;
                }
            }
        }
        return super.parseNetworkResponse(response);
    }
}
