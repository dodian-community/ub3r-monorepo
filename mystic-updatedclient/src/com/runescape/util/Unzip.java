package com.runescape.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Unzip {

	/**
	 * Unzip it
	 * @param zipFile input zip file
	 * @param output zip file output folder
	 * @param deleteAfter		Should the zip file be deleted afterwards?
	 */
	public static boolean unZipIt(String zipFile, String outputFolder, boolean deleteAfter) {

		byte[] buffer = new byte[1024];

		try{

			// Create output directory if needed.
			File folder = new File(outputFolder);
			if(!folder.exists() && !folder.mkdirs()){
				throw new IOException("Could not create output folder: " + folder.getAbsolutePath());
			}

			String outputCanonical = folder.getCanonicalPath() + File.separator;

			try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
				ZipEntry ze = zis.getNextEntry();

				while(ze != null){
					String fileName = ze.getName();
					File newFile = new File(folder, fileName);

					System.out.println("file unzip : " + newFile.getAbsoluteFile());

					String targetCanonical = newFile.getCanonicalPath();
					if (!targetCanonical.startsWith(outputCanonical)) {
						throw new IOException("Zip entry outside target dir: " + fileName);
					}

					if (ze.isDirectory()) {
						if (newFile.exists() && newFile.isFile() && !newFile.delete()) {
							throw new IOException("Could not replace file with directory: " + newFile.getAbsolutePath());
						}
						if (!newFile.exists() && !newFile.mkdirs()) {
							throw new IOException("Could not create directory: " + newFile.getAbsolutePath());
						}
						ze = zis.getNextEntry();
						continue;
					}

					File parent = newFile.getParentFile();
					if (parent != null) {
						if (parent.exists() && parent.isFile() && !parent.delete()) {
							throw new IOException("Could not replace file with directory: " + parent.getAbsolutePath());
						}
						if (!parent.exists() && !parent.mkdirs()) {
							throw new IOException("Could not create parent directory: " + parent.getAbsolutePath());
						}
					}

					try (FileOutputStream fos = new FileOutputStream(newFile)) {
						int len;
						while ((len = zis.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
					}

					ze = zis.getNextEntry();
				}
				zis.closeEntry();
			}

			if (deleteAfter) {
				new File(zipFile).delete();
			}

			return true;
		}catch(Exception ex){
			ex.printStackTrace();
			return false;
		}
	}

}
