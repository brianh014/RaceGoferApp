package com.example.RaceGoferApp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import org.apache.http.params.CoreProtocolPNames;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Brian on 10/26/2014.
 */
public class HttpConc {

    int responseCode;
    SharedPreferences settings;

    public HttpConc(Context context){
        settings = context.getSharedPreferences("com.example.RaceGoferApp", Context.MODE_PRIVATE);
    }

    // HTTP GET request
    public String sendGet(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header - do authorization
        String username = settings.getString("username", "__failure__");
        String password = settings.getString("password", "__failure__");
        Log.v("HTTP Auth", username + " : " + password);
        String credentials = username + ":" + password;
        String encoding = Base64.encodeToString(credentials.getBytes("UTF-8"), Base64.DEFAULT);
        con.setRequestProperty("Authorization", "Basic " + encoding);

        responseCode = con.getResponseCode();
        Log.v("HTTP","\nSending 'GET' request to URL : " + url);
        Log.v("HTTP","Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    // HTTP POST request
    public String sendPost(String url, String urlParameters) throws Exception {

        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        //add request header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", CoreProtocolPNames.USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        // Send post request
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        responseCode = con.getResponseCode();
        Log.v("HTTP","\nSending 'POST' request to URL : " + url);
        Log.v("HTTP","Post parameters : " + urlParameters);
        Log.v("HTTP","Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    public int checkLogin(String username, String password) throws Exception{
        URL obj = new URL("http://racegofer.com/api/greeting");
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header - do authorization
        URLParamEncoder encoder = new URLParamEncoder();
        String credentials = username + ":" + password;
        String encoding = Base64.encodeToString(credentials.getBytes("UTF-8"),Base64.DEFAULT);
        Log.v("Auth", encoding);
        con.setRequestProperty("Authorization", "Basic " + encoding);

        responseCode = con.getResponseCode();
        Log.v("HTTP","\nChecking login");
        Log.v("HTTP","Response Code : " + responseCode);

        return responseCode;
    }

    public String sendGetNoAuth(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", CoreProtocolPNames.USER_AGENT);

        responseCode = con.getResponseCode();
        Log.v("HTTP","\nSending 'GET No Auth' request to URL : " + url);
        Log.v("HTTP","Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }
}
