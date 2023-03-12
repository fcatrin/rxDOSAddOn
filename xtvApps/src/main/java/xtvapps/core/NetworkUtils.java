package xtvapps.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.util.Log;

public class NetworkUtils {
	private static final String LOGTAG = NetworkUtils.class.getSimpleName();
	private static final int HTTP_CONNECT_TIMEOUT = 5000;
	private static final int HTTP_READ_TIMEOUT = 8000;
	private static final int BUF_SIZE = 65536;
	
	private static Map<String, String> persistentHeaders = new HashMap<String, String>();


	public static byte[] postContent(String resource, Map<String, String> headers, String mime, String data) throws IOException {
		Map<String, String> mHeaders = headers;
		if (mHeaders == null) mHeaders = new HashMap<String, String>();
		
		mHeaders.putAll(persistentHeaders);
		mHeaders.put("Content-Type", mime);
		mHeaders.put("Content-Length", data.length() + "");
		return post(resource, mHeaders, data);
	}

	public static byte[] postContent(String resource, Map<String, String> headers, String mime, InputStream is, long knownSize) throws IOException {
		return postContent(resource, headers, mime, is, knownSize, null);
	}
	
	public static byte[] postContent(String resource, Map<String, String> headers, String mime, InputStream is, long knownSize, NetworkProgressListener listener) throws IOException {
		Log.d(LOGTAG, "postContent " + resource);
		Map<String, String> mHeaders = headers;
		if (mHeaders == null) mHeaders = new HashMap<String, String>();
		
		mHeaders.putAll(persistentHeaders);
		mHeaders.put("Content-Type", mime);
		if (knownSize>=0) {
			mHeaders.put("Content-Length", knownSize + "");
		}
		return post(resource, mHeaders, is, knownSize, listener);
	}

	public static byte[] postContent(String resource, Map<String, String> headers, Map<String, String> data) throws IOException {
		StringBuffer sData = new StringBuffer();
		for(Entry<String, String> entry : data.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue(); // URLEncoder.encode(entry.getValue());
			if (sData.length() != 0) sData.append("&");
			sData.append(key).append("=").append(value);
		}
		String mime = "application/x-www-form-urlencoded";
		Log.d(LOGTAG, "postContent data " + sData);
		return postContent(resource, headers, mime, sData.toString());
	}
	
	public static void fillHeaders(HttpURLConnection urlConnection, Map<String, String> headers) {
		if (headers == null) return;
		
		for(Entry<String, String> entry : headers.entrySet()) {
			urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
		}
	}
	
	public static byte[] post(String resource, Map<String, String>headers, String data) throws IOException {
		URL url;
		ByteArrayOutputStream baos = null;
		OutputStreamWriter wr = null;
		HttpURLConnection urlConnection = null;
		try {
			url = new URL(resource);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
			urlConnection.setReadTimeout(HTTP_READ_TIMEOUT);

			fillHeaders(urlConnection, headers);
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.connect();
			
		    wr = new OutputStreamWriter(urlConnection.getOutputStream());
		    wr.write(data);
		    wr.flush();

			baos = new ByteArrayOutputStream();
			InputStream inputStream = urlConnection.getInputStream();

			byte[] buffer = new byte[BUF_SIZE];
			int bufferLength = 0;

			while ((bufferLength = inputStream.read(buffer)) > 0) {
				baos.write(buffer, 0, bufferLength);
			}
			return baos.toByteArray();
		} catch (IOException e) {
			if (urlConnection!=null) {
				Log.d(LOGTAG, "Error response code " + urlConnection.getResponseCode());
				Log.d(LOGTAG, "Error response message " + urlConnection.getResponseMessage());
			}
			if (baos!=null) {
				Log.d(LOGTAG, "Error response body " + baos.toString());
			}
			throw e;
		} finally {
			if (baos != null) baos.close();
			if (wr!=null) wr.close();
		}
	}

	// duplicate, to avoid errors on legacy code
	@Deprecated
	public static byte[] post(String resource, Map<String, String>headers, InputStream is, long knownSize, NetworkProgressListener listener) throws IOException {
		URL url;
		ByteArrayOutputStream baos = null;
		OutputStream os = null;
		InputStream inputStream = null;
		try {
			url = new URL(resource);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
			urlConnection.setReadTimeout(HTTP_READ_TIMEOUT);

			fillHeaders(urlConnection, headers);
			urlConnection.setRequestMethod("POST");
			urlConnection.setDoOutput(true);
			urlConnection.connect();
			
			int uploadProgress = 0;
			
			byte[] buffer = new byte[BUF_SIZE];
			int bufferLength = 0;

			// send content
			os = urlConnection.getOutputStream();
			while ((bufferLength = is.read(buffer)) > 0) {
				os.write(buffer, 0, bufferLength);
				if (listener!=null) listener.updateProgress(uploadProgress, (int)knownSize);
				uploadProgress += bufferLength;
				os.flush();
			}

			// read response
			baos = new ByteArrayOutputStream();
			inputStream = urlConnection.getInputStream();

			while ((bufferLength = inputStream.read(buffer)) > 0) {
				baos.write(buffer, 0, bufferLength);
			}
			return baos.toByteArray();
		} finally {
			if (inputStream!=null) inputStream.close();
			if (baos != null) baos.close();
			if (os!=null) os.close();
		}
	}

}
