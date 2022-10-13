package com.localz.pinch.utils;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import com.localz.pinch.models.HttpRequest;
import com.localz.pinch.models.HttpResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpUtil {
    private static final String DEFAULT_CONTENT_TYPE = "application/json";

    private String getResponseBody(InputStream responseStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(responseStream));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        bufferedReader.close();

        return sb.toString();
    }

    private WritableMap getResponseHeaders(HttpURLConnection connection) {
        WritableMap jsonHeaders = Arguments.createMap();
        Map<String, List<String>> headerMap = connection.getHeaderFields();

        for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
            if (entry.getKey() != null) {
                jsonHeaders.putString(entry.getKey(), entry.getValue().get(0));
            }
        }

        return jsonHeaders;
    }

    private HttpURLConnection prepareRequestHeaders(HttpURLConnection connection, JSONObject headers) throws JSONException {
        connection.setRequestProperty("Content-Type", DEFAULT_CONTENT_TYPE);
        connection.setRequestProperty("Accept", DEFAULT_CONTENT_TYPE);

        if (headers != null) {
            Iterator<String> iterator = headers.keys();
            while (iterator.hasNext()) {
                String nextKey = iterator.next();
                connection.setRequestProperty(nextKey, headers.get(nextKey).toString());
            }
        }

        return connection;
    }

    private HttpURLConnection prepareRequest(HttpRequest request)
            throws IOException, KeyStoreException, CertificateException, KeyManagementException, NoSuchAlgorithmException, JSONException {
        HttpURLConnection connection;
        URL url = new URL(request.endpoint);
        String method = request.method.toUpperCase();

        if(request.endpoint.startsWith("https") && !request.ignore_ssl) {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection();
            if (request.certFilenames != null && request.certFilenames.length > 0) {
                httpsConnection.setSSLSocketFactory(KeyPinStoreUtil.getInstance(request.certFilenames).getContext().getSocketFactory());
            }
            connection = httpsConnection;
        } else if(request.endpoint.startsWith("https") && request.ignore_ssl) {
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            } };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());

            HttpsURLConnection httpsConnection = (HttpsURLConnection) url.openConnection();
            httpsConnection.setSSLSocketFactory(sc.getSocketFactory());
            connection = httpsConnection;
        }else{
            connection = (HttpURLConnection) url.openConnection();
        }
        connection.setRequestMethod(method);

        connection = prepareRequestHeaders(connection, request.headers);

        connection.setRequestProperty("Accept-Charset", "UTF-8");
        connection.setAllowUserInteraction(false);
        connection.setConnectTimeout(request.timeout);
        connection.setReadTimeout(request.timeout);

        if (request.body != null && (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
            // Set the content length of the body.
            connection.setRequestProperty("Content-length", request.body.getBytes().length + "");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // Send the JSON as body of the request.
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(request.body.getBytes("UTF-8"));
            outputStream.close();
        }

        return connection;
    }

    private InputStream prepareResponseStream(HttpURLConnection connection) throws IOException {
        try {
            return connection.getInputStream();
        } catch (IOException e) {
            return connection.getErrorStream();
        }
    }

    public HttpResponse sendHttpRequest(HttpRequest request)
            throws IOException, KeyStoreException, CertificateException, KeyManagementException, NoSuchAlgorithmException, JSONException {
        InputStream responseStream = null;
        HttpResponse response = new HttpResponse();
        HttpURLConnection connection;
        int status;
        String statusText;

        try {
            connection = prepareRequest(request);

            connection.connect();

            status = connection.getResponseCode();
            statusText = connection.getResponseMessage();
            responseStream = prepareResponseStream(connection);

            response.statusCode = status;
            response.statusText = statusText;
            response.bodyString = getResponseBody(responseStream);
            response.headers = getResponseHeaders(connection);

            return response;
        } finally {
            if (responseStream != null) {
                responseStream.close();
            }
        }
    }
}
