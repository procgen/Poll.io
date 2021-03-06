package cecs343.pollio;

import android.content.Context;
import android.util.Log;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Requestor {

    private static Requestor instance; // Does not cause memory leak as long as you always pass getApplicationContext (Not an activity's context)
    private RequestQueue requestQueue;
    private static Context ctx; // Same as above

    private Requestor(Context context) {
        ctx = context;
        requestQueue = getRequestQueue();

    }

    public interface HTTPCallback{
        void onSuccess();
    }

    public interface ObjHTTPCallback {
        void onSuccess(JSONObject o);
    }

    // Passed context MUST be applicationContext, not activity
    public static synchronized Requestor getInstance(Context context) {
        if (instance == null) {
            instance = new Requestor(context);
        }
        return instance;
    }

    public static void postRequest(final Context context, final String path, FirebaseUser user, final HashMap<String, String> args) {

        user.getIdToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
            @Override
            public void onSuccess(GetTokenResult result) {
                final String idToken = result.getToken();

                final String url = "https://polls.lorenzen.dev/" + path;


                StringRequest stringRequest = new StringRequest
                        (Request.Method.POST, url, new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Log.d("HTTPRequest", response);
                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                                if (error.getMessage() != null) {
                                    Log.d("HTTPRequest", error.getMessage());
                                }
                            }
                        })
                {
                    @Override
                    public Map<String, String> getParams() {
                        args.put("token", idToken);
                        Log.d("HTTPRequest", "POST; url: " + url + " args: " + args.toString());
                        return args;
                    }
                };
                getInstance(context).addToRequestQueue(stringRequest);

            }
        });


    }

    public static void getRequest(Context context, String uid, String path, final ObjHTTPCallback callback) {
        getRequest(context, uid, path, callback, new HashMap<String, String>());
    }


    public static void getRequest(Context context, String uid, String path, final ObjHTTPCallback callback, HashMap<String, String> args) {
        final ArrayList<PollItem> newPolls = new ArrayList<>();

        String url = "https://polls.lorenzen.dev/" + path + "?uid=" + uid;
        for(Map.Entry<String, String> entry : args.entrySet()) {
            url += "&" + entry.getKey() + "=" + entry.getValue();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("HTTPRequest", response.toString());
                        try{
                            callback.onSuccess(response);
                        } catch (Exception e) {

                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.getMessage() != null) {
                            Log.d("HTTPRequest", error.getMessage());
                        }
                    }
                });
        getInstance(context).addToRequestQueue(jsonObjectRequest);

        return;
    }

    public static ArrayList<PollItem> getPolls(Context context, String uid, String route, final HTTPCallback callback) {
        final ArrayList<PollItem> newPolls = new ArrayList<>();

        String url = "https://polls.lorenzen.dev/" + route + "?uid=" + uid;

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d("HTTPRequest", response.toString());
                        try{
                            for (int i = 0, size = response.length(); i < size; i++) {
                                JSONObject jsonPoll = response.getJSONObject(i);
                                PollItem poll = new PollItem(jsonPoll.get("title").toString(), (boolean)(jsonPoll.get("favorited")), Integer.parseInt(jsonPoll.get("pollID").toString()));
                                poll.setLatitude(jsonPoll.getDouble("latitude"));
                                poll.setLongitude(jsonPoll.getDouble("longitude"));
                                JSONArray options = jsonPoll.getJSONArray("options");
                                JSONArray votes = jsonPoll.getJSONArray("votes");
                                for(int j = 0; j < options.length(); j++)
                                {
                                    poll.addPollOption(new PollOption(options.getString(j), Integer.parseInt(votes.getString(j))));
                                }
                                poll.checkVoted(jsonPoll.get("voted").toString());
                                newPolls.add(poll);
                            }

                            callback.onSuccess();
                        }
                        catch (Exception e){
                            Log.d("HTTPRequest", "JSON EXCEPTION: " + e.getMessage());
                            for (StackTraceElement s : e.getStackTrace()) {
                                Log.d("HTTPRequest", s.toString());
                            }
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.getMessage() != null) {
                            Log.d("HTTPRequest", error.getMessage());
                        }
                    }
                })
        {
//
//            @Override
//            public Map<String, String> getParams() {
//                HashMap<String, String> params = new HashMap<>();
//                params.put("uid", uid);
//                return params;
//            }
        };
        getInstance(context).addToRequestQueue(jsonArrayRequest);

        return newPolls;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

}
