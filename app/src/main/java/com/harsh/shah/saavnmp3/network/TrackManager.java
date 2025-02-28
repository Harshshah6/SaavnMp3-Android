package com.harsh.shah.saavnmp3.network;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.harsh.shah.saavnmp3.ApplicationClass;
import com.harsh.shah.saavnmp3.utils.TrackCacheHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class TrackManager extends AsyncTask<String, String, String> {

    private static final String TAG = "TrackManager";
    private final Context context;
    private final String musicURL, trackName, trackId, trackImage;
    private final boolean toCache;
    private final OkHttpClient okHttpClient;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;

    public TrackManager(Context context, String musicURL, String trackName, String trackId, String trackImage, boolean toCache) {
        this.context = context;
        
        // Try to convert HTTP URLs to HTTPS
        if (musicURL != null && musicURL.startsWith("http:")) {
            this.musicURL = musicURL.replace("http:", "https:");
            Log.i(TAG, "Converted URL to HTTPS: " + this.musicURL);
        } else {
            this.musicURL = musicURL;
        }
        
        this.trackName = trackName;
        this.trackId = trackId;
        this.trackImage = trackImage;
        this.toCache = toCache;
        
        // Create a trust manager that accepts all certificates
        X509TrustManager trustAllCerts = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        // Create a hostname verifier that accepts all hostnames
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Initialize OkHttpClient with the trust manager
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .hostnameVerifier(hostnameVerifier);

        try {
            // Create an SSL context that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllCerts}, new java.security.SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up SSL context", e);
        }

        this.okHttpClient = builder.build();
    }

    @Override
    protected String doInBackground(String... strings) {
        // First try with OkHttp as it's more reliable
        String result = downloadWithOkHttp();
        
        // If it fails, try with URLConnection
        if ("FAILED".equals(result) && retryCount < MAX_RETRIES) {
            Log.i(TAG, "OkHttp download failed, trying URLConnection...");
            result = downloadWithURLConnection();
            
            // Try alternate URL schemes if still failing
            if ("FAILED".equals(result) && musicURL.startsWith("https:")) {
                retryCount++;
                Log.i(TAG, "Retry " + retryCount + " with HTTP URL");
                
                // Try with HTTP URL instead of HTTPS
                String httpUrl = musicURL.replace("https:", "http:");
                Log.i(TAG, "Trying HTTP URL: " + httpUrl);
                
                // Try with OkHttp first
                result = downloadWithOkHttpUrl(httpUrl);
                
                // If still failing, try URLConnection with HTTP
                if ("FAILED".equals(result)) {
                    result = downloadWithURLConnectionUrl(httpUrl);
                }
            }
        }
        
        return result;
    }
    
    private String downloadWithURLConnection() {
        return downloadWithURLConnectionUrl(musicURL);
    }
    
    private String downloadWithURLConnectionUrl(String urlString) {
        int count;
        try {
            URL url = new URL(urlString);
            Log.i(TAG, "URLConnection connecting to URL: " + url.toString());
            
            // Set up the connection with proper timeout and properties
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(60000); // 60 seconds
            connection.setReadTimeout(60000);   // 60 seconds
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setRequestProperty("Accept", "*/*");
            connection.connect();
            
            InputStream input = new BufferedInputStream(url.openStream(), 16384);
            File file = getOutputFile();
            
            Log.i(TAG, "URLConnection saving file to: " + file.getAbsolutePath());
            OutputStream output = new FileOutputStream(file);

            byte[] data = new byte[16384]; // Large buffer size

            long total = 0;
            int contentLength = connection.getContentLength();
            
            while ((count = input.read(data)) != -1) {
                total += count;
                if (contentLength > 0) {
                    int progress = (int) ((total * 100) / contentLength);
                    publishProgress(String.valueOf(progress));
                }
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            Log.i(TAG, "URLConnection download completed: " + file.getAbsolutePath());

            if (toCache) {
                new TrackCacheHelper(context).setTrackToCache(trackId, file.getAbsolutePath());
                ApplicationClass.isTrackDownloaded = true;
            }

            return file.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "URLConnection download failed: " + e.getMessage(), e);
            return "FAILED";
        }
    }
    
    private String downloadWithOkHttp() {
        return downloadWithOkHttpUrl(musicURL);
    }
    
    private String downloadWithOkHttpUrl(String urlString) {
        Response response = null;
        try {
            Request request = new Request.Builder()
                    .url(urlString)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .addHeader("Accept", "*/*")
                    .build();
            
            Log.i(TAG, "OkHttp connecting to URL: " + urlString);        
            response = okHttpClient.newCall(request).execute();
            
            if (!response.isSuccessful()) {
                Log.e(TAG, "OkHttp download failed with status code: " + response.code());
                return "FAILED";
            }
            
            File file = getOutputFile();
            Log.i(TAG, "OkHttp saving file to: " + file.getAbsolutePath());
            
            ResponseBody body = response.body();
            if (body == null) {
                Log.e(TAG, "OkHttp response body is null");
                return "FAILED";
            }
            
            InputStream input = body.byteStream();
            OutputStream output = new FileOutputStream(file);
            
            byte[] data = new byte[16384]; // Large buffer size
            long total = 0;
            long contentLength = body.contentLength();
            int count;
            
            while ((count = input.read(data)) != -1) {
                total += count;
                if (contentLength > 0) {
                    int progress = (int) ((total * 100) / contentLength);
                    publishProgress(String.valueOf(progress));
                }
                output.write(data, 0, count);
            }
            
            output.flush();
            output.close();
            input.close();
            
            Log.i(TAG, "OkHttp download completed: " + file.getAbsolutePath());
            
            if (toCache) {
                new TrackCacheHelper(context).setTrackToCache(trackId, file.getAbsolutePath());
                ApplicationClass.isTrackDownloaded = true;
            }
            
            return file.getAbsolutePath();
            
        } catch (Exception e) {
            Log.e(TAG, "OkHttp download failed: " + e.getMessage(), e);
            return "FAILED";
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
    
    private File getOutputFile() {
        if (toCache) {
            File cacheFile = new File(context.getCacheDir(), trackId + ".mp3");
            // Make sure parent directory exists
            if (!cacheFile.getParentFile().exists()) {
                cacheFile.getParentFile().mkdirs();
            }
            return cacheFile;
        } else {
            // Ensure valid file name by removing invalid characters
            String sanitizedName = trackName.replaceAll("[\\\\/:*?\"<>|]", "_");
            
            File musicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Melotune");
            if (!musicDir.exists()) {
                boolean created = musicDir.mkdirs();
                Log.i(TAG, "Music directory created: " + created);
            }
            return new File(musicDir, sanitizedName + ".mp3");
        }
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        Log.i(TAG, "onPostExecute: " + s);
        if (s.equals("FAILED")) {
            Log.e(TAG, "Download completely failed after all retries");
            return;
        }
        
        if (toCache) {
            try {
                new TrackCacheHelper(context).setTrackToCache(trackId, s);
                ApplicationClass.isTrackDownloaded = true;
                Log.i(TAG, "Successfully cached track: " + trackId);
            } catch (Exception e) {
                Log.e(TAG, "Error setting track to cache", e);
            }
        } else {
            Log.i(TAG, "Successfully downloaded track to: " + s);
        }
    }
    
    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        Log.d(TAG, "Download progress: " + values[0] + "%");
    }
}
