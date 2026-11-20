package cloud.picking.app.pickingcloud.helper;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class VolleyController {
    private static Context mCtx;
    private static VolleyController mInstance;
    private RequestQueue mRequestQueue;

    private VolleyController(Context context) {
        mCtx = context;
        this.mRequestQueue = getRequestQueue();
    }

    public static synchronized VolleyController getInstance(Context context) {
        synchronized (VolleyController.class) {
            if (mInstance == null) {
                mInstance = new VolleyController(context);
            }
            return mInstance;
        }
    }

    public RequestQueue getRequestQueue() {
        if (this.mRequestQueue == null) {
            this.mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return this.mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
