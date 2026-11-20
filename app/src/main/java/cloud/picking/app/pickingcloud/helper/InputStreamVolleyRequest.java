package cloud.picking.app.pickingcloud.helper;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.toolbox.HttpHeaderParser;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputStreamVolleyRequest extends Request<byte[]> {
    private Map<String, String> mHeaders;
    private Response.Listener<byte[]> mListener;
    private Map<String, String> mParams;
    private SharedPref sharedPref;

    public InputStreamVolleyRequest(Context context, String mUrl, Response.Listener<byte[]> listener,
                                    Response.ErrorListener errorListener, HashMap<String, String> params) {
        super(Method.POST, mUrl, errorListener);
        this.mHeaders = new HashMap<>(1);
        setShouldCache(false);
        this.mListener = listener;
        this.mParams = params;
        this.sharedPref = new SharedPref(context);
    }

    @Override
    protected void deliverResponse(byte[] response) {
        this.mListener.onResponse(response);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        if (this.sharedPref != null && !"".equals(this.sharedPref.getSession())) {
            this.mHeaders.put("Cookie", SharedPref.SESSION_NAME + "=" + this.sharedPref.getSession() + ";");
        }
        return this.mHeaders;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return this.mParams;
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
        Map<String, String> headers = response.headers;
        if (headers != null && headers.containsKey("Set-Cookie") && this.sharedPref != null) {
            List<HttpCookie> cookies = HttpCookie.parse(headers.get("Set-Cookie"));
            for (int i = 0; i < cookies.size(); i++) {
                HttpCookie cookie = cookies.get(i);
                if (SharedPref.SESSION_NAME.equals(cookie.getName())) {
                    this.sharedPref.saveSession(cookie.getValue());
                    break;
                }
            }
        }
        this.mHeaders = headers;
        return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
    }
}
