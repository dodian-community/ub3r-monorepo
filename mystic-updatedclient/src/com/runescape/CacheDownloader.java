package com.runescape;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.JOptionPane;

import com.runescape.sign.SignLink;
import com.runescape.util.Unzip;

public class CacheDownloader {

	private static final String CACHE_PATH = "http://www.osrspk.com/client/cache/";
	private static final String CACHE_NAME = "cache.zip";

	public static void init(boolean force) {
		double current = getCurrentVersion();
		double latest = getLatestVersion();

		if(latest > current || force) {
			try {

				/**
				 * Download latest cache
				 */
				download();

				/**
				 * Unzip the downloaded cache file
				 */
				Unzip.unZipIt(SignLink.findcachedir() + CACHE_NAME, SignLink.findcachedir(), true);

				/**
				 * Write new version
				 */
				File version = new File(SignLink.findcachedir() + "version.txt");
				try (FileWriter f = new FileWriter(version)) {
					f.write(""+latest+"");
					f.close();
				}
				
			} catch(Exception e) {
				JOptionPane.showMessageDialog(null, "Cache could not be downloaded.\nPlease try again later.");
			}
		}
	}

	private static void download() throws Exception {		
		URL url = new URL(CACHE_PATH + CACHE_NAME);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.addRequestProperty("User-Agent", "Mozilla/4.76");
		int responseCode = httpConn.getResponseCode();
		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {

			// opens input stream from the HTTP connection
			InputStream inputStream = httpConn.getInputStream();
			String saveFilePath = SignLink.findcachedir() + File.separator + CACHE_NAME;

			// opens an output stream to save into file
			FileOutputStream outputStream = new FileOutputStream(saveFilePath);

			int bytesRead = -1;
			byte[] buffer = new byte[4096];
			long startTime = System.currentTimeMillis();
			int downloaded = 0;
			long numWritten = 0;
			int length = httpConn.getContentLength();
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
				numWritten += bytesRead;
				downloaded += bytesRead;
				int percentage = (int)(((double)numWritten / (double)length) * 100D);
				int downloadSpeed = (int) ((downloaded / 1024) / (1 + ((System.currentTimeMillis() - startTime) / 1000)));

				Client.instance.drawLoadingText(percentage, "Downloading cache "+percentage+"% @ "+downloadSpeed+"Kb/s");
			}

			outputStream.close();
			inputStream.close();

		} else {
			System.out.println("Cache host replied HTTP code: " + responseCode);
		}
		httpConn.disconnect();
	}

	private static double getCurrentVersion() {
		double version = 0.0;
		try {
			File file = new File(SignLink.findcachedir() + "version.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			version = Double.parseDouble(br.readLine());
			br.close();
		} catch(Exception ex) {
			//ex.printStackTrace(); 
		}
		return version;
	}

	private static double getLatestVersion() {
		double version = 0.0;
		try {
			URL url = new URL(CACHE_PATH + "version.txt");
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.addRequestProperty("User-Agent", "Mozilla/4.76");
			BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			version = Double.parseDouble(br.readLine());
			br.close();
		} catch(Exception ex) {
			//ex.printStackTrace(); 
		}
		return version;
	}
}
