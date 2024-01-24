package com.inka.simple.sample;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.inka.ncg2.Ncg2Agent.HttpRequestCallback;
import com.inka.ncg2.Ncg2Agent.NcgHttpRequestException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public class ProxyNcgHttpRequestCallbackImpl implements HttpRequestCallback {

    static final int HTTP_OK = 200;
    static final String proxyURL = "http://112.136.244.48/drm/ncg";
    private Context mContext;

    public ProxyNcgHttpRequestCallbackImpl(Context context) {
        mContext = context;
    }

    private boolean checkNetwordState() {
        ConnectivityManager connManager =(ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo state_3g = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo state_wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if( state_3g != null && state_3g.isConnected() ){
            return true;
        }

        if( state_wifi != null && state_wifi.isConnected() ){
            return true;
        }

        return false;
    }

    /**
     * @brief	sends request to server and receives response data as string.
     *
     * @param   url server URL
     * @param   param get-type parameter
     * @return	responsedata
     */
    @Override
    public String sendRequest(String url, String param)
            throws NcgHttpRequestException {

        if( checkNetwordState() == false ){
            throw new NcgHttpRequestException(0, "", "[sendRequest]Network Not Connected");
        }

        int responseCode = 0;
        String responseMsg = "";
        param = param.trim();

        try {
            URL urlObj;
            HttpURLConnection urlConn;
            if (param.equals("mode=getserverinfo")) {
                if( url.indexOf('?') != -1 || param.indexOf('?' ) != -1) { // check whether '?' is included in URL or parameter.
                    urlObj = new URL(proxyURL + param);
                }else {
                    urlObj = new URL(proxyURL + "?" + param);
                }
                urlConn = (HttpURLConnection) urlObj.openConnection();
                urlConn.setRequestMethod("GET");
            } else {
                urlObj = new URL(proxyURL);
                urlConn = (HttpURLConnection) urlObj.openConnection();
                urlConn.setRequestMethod("POST");
                DataOutputStream outputStream = new DataOutputStream(urlConn.getOutputStream());
                outputStream.writeBytes(param);
                outputStream.flush();
                outputStream.close();
            }

            responseCode = urlConn.getResponseCode();
            responseMsg = urlConn.getResponseMessage();

            if (responseCode != HTTP_OK) {
                throw new NcgHttpRequestException(responseCode,
                        responseMsg, "Error. Http response status code is "
                        + responseCode);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    urlConn.getInputStream()));
            StringBuffer buffer = new StringBuffer();
            int c;
            while ((c = in.read()) != -1) {
                buffer.append((char) c);
            }
            in.close();
            String responseData = buffer.toString();
            return responseData;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new NcgHttpRequestException(responseCode, responseMsg,
                    "MalformedURLException Exception Occured!: "
                            + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new NcgHttpRequestException(responseCode, responseMsg,
                    "IOException Exception Occured!: " + e.getMessage());
        }
    }

    /**
     * @brief	sends request to server and receives response data as byte[].
     *
     * @param   url server URL
     * @param   param get-type parameter to be sent
     * @return	responsedata
     */
    @Override
    public byte[] sendRequestResponseBytes(String url, String param)
            throws NcgHttpRequestException {
//        Log.d(DemoLibrary.TAG, "url : ["+ url +"]");
//        Log.d(DemoLibrary.TAG, "param : ["+ param +"]");
        if( checkNetwordState() == false ){
            throw new NcgHttpRequestException(0, "", "[sendRequest]Netword Not Connected");
        }

        int responseCode = 0;
        String responseMsg = "";
        byte[] result;

        param = param.trim();
        String proxy = proxyURL;

        HttpURLConnection httpConn;
        int questionMarkIndex = url.indexOf('?');
        if(questionMarkIndex != -1 )  {
            param = proxy.substring(questionMarkIndex, proxy.length());
            proxy = proxy.substring(0, questionMarkIndex);
        }

        try {
            URL urlObj;
            HttpURLConnection urlConn;
            if (param.equals("mode=getserverinfo")) {
                if( url.indexOf('?') != -1 || param.indexOf('?' ) != -1) { // check whether '?' is included in URL or parameter.
                    urlObj = new URL(proxyURL + param);
                }else {
                    urlObj = new URL(proxyURL + "?" + param);
                }
                urlConn = (HttpURLConnection) urlObj.openConnection();
                urlConn.setRequestMethod("GET");
            } else {
                urlObj = new URL(proxyURL);
                urlConn = (HttpURLConnection) urlObj.openConnection();
                urlConn.setRequestMethod("POST");
                DataOutputStream outputStream = new DataOutputStream(urlConn.getOutputStream());
                outputStream.writeBytes(param);
                outputStream.flush();
                outputStream.close();
            }

            responseCode = urlConn.getResponseCode();
            responseMsg = urlConn.getResponseMessage();
            if (responseCode != 200) {
                throw new NcgHttpRequestException(responseCode,
                        responseMsg, "Error. Http response status code is "
                        + responseCode);
            }

            BufferedInputStream inputStream = (new BufferedInputStream(urlConn.getInputStream()));
            byte[] responseData = new byte[ 10240000 ];
            int totalReadBytes = 0;
            while( true  ) {
                int readBytes = inputStream.read( responseData, totalReadBytes, 512 );
                if( readBytes == -1  ) {
                    break;
                }
                totalReadBytes += readBytes;
            }
            Log.d("NCG_Agent", "sendRequestResponseBytes : totalReadyBytes -> " + totalReadBytes);
            inputStream.close();

            result = new byte[totalReadBytes];
            System.arraycopy(responseData, 0, result, 0, totalReadBytes);
            return result;

        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new NcgHttpRequestException(responseCode, responseMsg,
                    "MalformedURLException Exception Occured!: "
                            + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new NcgHttpRequestException(responseCode, responseMsg,
                    "IOException Exception Occured!: " + e.getMessage());
        }
    }

    /**
     * @brief	sends request to server and receives response data as byte[].
     * <br> the difference between sendRequestResponseBytes method is that this method has range parameter.
     *
     * @param   url server URL
     * @param   param get-type parameter to be sent
     * @param   begin
     * @param   end
     * @return	responsedata
     */
    @Override
    public byte[] sendRequest(String url, String param, int begin, int end)
            throws NcgHttpRequestException {
        int responseCode = 0;
        String responseMsg = "";
        InputStream inputStream = null;
        if( checkNetwordState() == false ){
            throw new NcgHttpRequestException(0, "", "[sendRequest]Netword Not Connected");
        }

        param = param.trim();

        try {
            int totalReadBytes = 0;
            byte[] buffer = new byte[ end ];

            URL urlObj;
            HttpURLConnection urlConn;
            if (param.equals("mode=getserverinfo")) {
                if( url.indexOf('?') != -1 || param.indexOf('?' ) != -1) { // check whether '?' is included in URL or parameter.
                    urlObj = new URL(proxyURL + param);
                }else {
                    urlObj = new URL(proxyURL + "?" + param);
                }
                urlConn = (HttpURLConnection) urlObj.openConnection();
                urlConn.setRequestMethod("GET");
            } else {
                urlObj = new URL(proxyURL);
                urlConn = (HttpURLConnection) urlObj.openConnection();
                urlConn.setRequestMethod("POST");
                DataOutputStream outputStream = new DataOutputStream(urlConn.getOutputStream());
                outputStream.writeBytes(param);
                outputStream.flush();
                outputStream.close();
            }

            urlConn.setRequestProperty("Range", String.format("bytes=%d-%d", begin, end) );
            urlConn.connect();
            responseCode = urlConn.getResponseCode();
            responseMsg = urlConn.getResponseMessage();
            inputStream = urlConn.getInputStream();

            while(true) {
                int readBytes = inputStream.read(buffer, totalReadBytes, end-totalReadBytes);
                if( readBytes == -1  ) {
                    break;
                }
                totalReadBytes += readBytes;
                if( totalReadBytes >= end  ) {
                    break;
                }
            }
            inputStream.close();
            return buffer;
        } catch (MalformedURLException e) {
            e.printStackTrace();

            throw new NcgHttpRequestException(responseCode, responseMsg,
                    "MalformedURLException Exception Occured!: "
                            + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new NcgHttpRequestException(responseCode, responseMsg,
                    "IOException Exception Occured!: " + e.getMessage());
        }
    }
}
