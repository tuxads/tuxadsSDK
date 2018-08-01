package com.durranilab.tuxads;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.durranilab.tuxads.callback.InterstitialAdListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class TuxInterstitial extends Activity {

    String AppID;
    String DeviceID="";
    public static  Boolean isAdLoaded = false;
    Activity AppContext;
    AlertDialog adDialog;
    AlertDialog privacyDialog;

    public static InterstitialAdListener interstitialAdListener;

    String CampID,AdID,AdName,Package,ImageURL,AppLink;

    String AD_URL = "https://tuxads.com/api/requestads.php";
    String ImpressionURL = "https://tuxads.com/api/logImpression.php";
    String ClicksURL = "https://tuxads.com/api/logImpression.php";

    public TuxInterstitial(Activity context,String AppIDx){
        AppID = AppIDx;
        AppContext = context;
        interstitialAdListener = new InterstitialAdListener() {
            @Override
            public void OnAdClosed() {

            }

            @Override
            public void OnError() {

            }
        };
    }
    public TuxInterstitial(){
         interstitialAdListener = null;
    }
    public void setInterstitialAdListener(InterstitialAdListener listener) {
        interstitialAdListener = listener;
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

    }

    public void loadAd(){


        DeviceID =  Settings.Secure.ANDROID_ID;

        //Creating a string request
        StringRequest stringRequest = new StringRequest(Request.Method.POST,AD_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.d("TuxAds",response.toString());

                        JSONArray j = null;

                        try {
                            //Parsing the fetched Json String to JSON Object
                            j = new JSONArray(response);

                            //Storing the Array of JSON String to our JSON Array

                            //Calling method getStudents to get the students from the JSON Array
                            getAdData(j);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TuxAds","No Internet Connectivity");
                        Log.d("TuxAds",error.toString());

                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                         params.put("app_id", AppID);
                         params.put("api_version", "1");
                         params.put("ad_type","Interstitial");
                         params.put("device_id",DeviceID);

                return params;
            }

        };
        //Creating a request queue
        RequestQueue requestQueue = Volley.newRequestQueue(AppContext);

        //Adding request to the queue
        requestQueue.add(stringRequest);

    }

    private void getAdData(JSONArray j) {

        //Traversing through all the items in the json array
        for(int i=0;i<j.length();i++){
            try {
                JSONObject obj= j.getJSONObject(i);

                CampID = obj.getString("camp_id");
                AdID = obj.getString("adid");
                AdName = obj.getString("adname");
                Package = obj.getString("package");
                ImageURL = obj.getString("imgpath");
                AppLink  = obj.getString("linkurl");


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        Log.d("TuxAds Interstitial","Ad Loaded");

        final AlertDialog.Builder builder = new AlertDialog.Builder(AppContext,R.style.TUXDialogTheme);

        LayoutInflater inflater = (AppContext).getLayoutInflater();

        View dialogView= inflater.inflate(R.layout.tux_ads_layout, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        // builder.create();
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                                 KeyEvent event) {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    finish();
                    adDialog.dismiss();
                    interstitialAdListener.OnAdClosed();
                }
                return true;
            }
        });
        adDialog = builder.create();


        ImageView close = dialogView.findViewById(R.id.tux_ads_close_btn);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                interstitialAdListener.OnAdClosed();
                adDialog.dismiss();
            }
        });



        ImageView adimage = dialogView.findViewById(R.id.tux_ads_img_view);
        new TuxInterstitial.DownLoadImageTask(adimage).execute(ImageURL);

        Button clickbtn = dialogView.findViewById(R.id.tux_ads_click_btn);

        clickbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent applink = new Intent(Intent.ACTION_VIEW);
                applink.setData(Uri.parse(AppLink));
                CountClick();
                AppContext.startActivity(applink);
                adDialog.dismiss();

            }
        });

        final Button privacy = dialogView.findViewById(R.id.tux_ads_privacy_btn);
        privacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(AppContext,android.R.style.Theme_DeviceDefault_Dialog_Alert);

                LayoutInflater inflater = (AppContext).getLayoutInflater();

                View dialogView= inflater.inflate(R.layout.tux_ads_privacy_layout, null);
                builder.setView(dialogView);
                builder.setCancelable(true);
                // builder.create();
                builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface arg0, int keyCode,
                                         KeyEvent event) {
                        // TODO Auto-generated method stub
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            finish();
                            privacyDialog.dismiss();
                        }
                        return true;
                    }
                });
                privacyDialog = builder.create();
                privacyDialog.show();
            }
        });



    }

    public void showAd(){

        if (isAdLoaded){
            adDialog.show();
            CountImpression();
            isAdLoaded= false;
            Log.d("TuxAds Interstitial","Ad Loaded : Showing Ad");

        }else{
            Log.d("TuxAds Interstitial ","Ad Not Loaded");
            interstitialAdListener.OnError();
        }

    }

    public void loadAndshowAd(){


        DeviceID =  Settings.Secure.ANDROID_ID;

        //Creating a string request
        StringRequest stringRequest = new StringRequest(Request.Method.POST,AD_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.d("TuxAds",response.toString());

                        JSONArray j = null;

                        try {
                            //Parsing the fetched Json String to JSON Object
                            j = new JSONArray(response);

                            //Storing the Array of JSON String to our JSON Array

                            //Calling method getStudents to get the students from the JSON Array
                            getAdDataLS(j);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("TuxAds","No Internet Connectivity");
                        Log.d("TuxAds",error.toString());

                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("app_id", AppID);
                params.put("api_version", "1");
                params.put("ad_type","Interstitial");
                params.put("device_id",DeviceID);

                return params;
            }

        };
        //Creating a request queue
        RequestQueue requestQueue = Volley.newRequestQueue(AppContext);

        //Adding request to the queue
        requestQueue.add(stringRequest);
    }

    private void getAdDataLS(JSONArray j) {

        //Traversing through all the items in the json array
        for(int i=0;i<j.length();i++){
            try {
                JSONObject obj= j.getJSONObject(i);

                CampID = obj.getString("camp_id");
                AdID = obj.getString("adid");
                AdName = obj.getString("adname");
                Package = obj.getString("package");
                ImageURL = obj.getString("imgpath");
                AppLink  = obj.getString("linkurl");


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        Log.d("TuxAds Interstitial","Ad Loaded");

        final AlertDialog.Builder builder = new AlertDialog.Builder(AppContext,R.style.TUXDialogTheme);

        LayoutInflater inflater = (AppContext).getLayoutInflater();

        View dialogView= inflater.inflate(R.layout.tux_ads_layout, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        // builder.create();
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                                 KeyEvent event) {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    finish();
                    adDialog.dismiss();
                    interstitialAdListener.OnAdClosed();
                }
                return true;
            }
        });
        adDialog = builder.create();


        ImageView close = dialogView.findViewById(R.id.tux_ads_close_btn);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                interstitialAdListener.OnAdClosed();
                adDialog.dismiss();
            }
        });



        ImageView adimage = dialogView.findViewById(R.id.tux_ads_img_view);
        new TuxInterstitial.DownLoadImageTaskLS(adimage).execute(ImageURL);

        Button clickbtn = dialogView.findViewById(R.id.tux_ads_click_btn);

        clickbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent applink = new Intent(Intent.ACTION_VIEW);
                applink.setData(Uri.parse(AppLink));
                CountClick();
                AppContext.startActivity(applink);
                adDialog.dismiss();

            }
        });

        final Button privacy = dialogView.findViewById(R.id.tux_ads_privacy_btn);
        privacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(AppContext,android.R.style.Theme_DeviceDefault_Dialog_Alert);

                LayoutInflater inflater = (AppContext).getLayoutInflater();
                View dialogView= inflater.inflate(R.layout.tux_ads_privacy_layout, null);
                builder.setView(dialogView);
                builder.setCancelable(true);
                // builder.create();
                builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface arg0, int keyCode,
                                         KeyEvent event) {
                        // TODO Auto-generated method stub
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            finish();
                            privacyDialog.dismiss();
                        }
                        return true;
                    }
                });
                privacyDialog = builder.create();
                privacyDialog.show();
            }
        });



    }

    private void CountImpression() {

        RequestQueue queue = Volley.newRequestQueue(AppContext);
        //this is the url where you want to send the request
        //TODO: replace with your own url to send request, as I am using my own localhost for this tutorial

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ImpressionURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the response string.

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }) {
            //adding parameters to the request
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("app_id", AppID);
                params.put("adid",AdID);
                params.put("camp_id",CampID);
                params.put("device_id",DeviceID);
                params.put("isImpression","true");
                return params;
            }
        };
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void CountClick() {

        RequestQueue queue = Volley.newRequestQueue(AppContext);
        //this is the url where you want to send the request
        //TODO: replace with your own url to send request, as I am using my own localhost for this tutorial

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, ClicksURL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the response string.

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }) {
            //adding parameters to the request
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("app_id", AppID);
                params.put("adid",AdID);
                params.put("camp_id",CampID);
                params.put("device_id",DeviceID);
                params.put("isClick","true");
                return params;
            }
        };
        // Add the request to the RequestQueue.
        queue.add(stringRequest);


    }

    private static class DownLoadImageTask extends AsyncTask<String,Void,Bitmap> {
        ImageView imageView;

        public DownLoadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        /*
            doInBackground(Params... params)
                Override this method to perform a computation on a background thread.
         */
        protected Bitmap doInBackground(String... urls) {
            String urlOfImage = urls[0];
            Bitmap logo = null;
            try {
                InputStream is = new URL(urlOfImage).openStream();
                /*
                    decodeStream(InputStream is)
                        Decode an input stream into a bitmap.
                 */
                logo = BitmapFactory.decodeStream(is);

            } catch (Exception e) { // Catch the download exception
                e.printStackTrace();
            }
            return logo;
        }

        /*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
            isAdLoaded = true;
        }
    }

    private class DownLoadImageTaskLS extends AsyncTask<String,Void,Bitmap> {
        ImageView imageView;

        public DownLoadImageTaskLS(ImageView imageView) {
            this.imageView = imageView;
        }

        /*
            doInBackground(Params... params)
                Override this method to perform a computation on a background thread.
         */
        protected Bitmap doInBackground(String... urls) {
            String urlOfImage = urls[0];
            Bitmap logo = null;
            try {
                InputStream is = new URL(urlOfImage).openStream();
                /*
                    decodeStream(InputStream is)
                        Decode an input stream into a bitmap.
                 */
                logo = BitmapFactory.decodeStream(is);

            } catch (Exception e) { // Catch the download exception
                e.printStackTrace();
            }
            return logo;
        }

        /*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
        protected void onPostExecute(Bitmap result) {
            imageView.setImageBitmap(result);
            isAdLoaded = true;
            showAd();
        }
    }



}




