/**
 * AgingPlaceMobile
 */
package com.ap.core.device.context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

/* Copyright (C) Projecteria LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Jungwhan Kim Projecteria LLC, 2016 April 19
 */
public class LogUtils {
	/**
	 * Save a log file
	 * @param logFile
	 * @param content
	 */
	public static void saveLog(String content, FileOutputStream out) {
		try {
			out.write(content.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String readFile(String file) {
		BufferedReader br = null;
		StringBuilder text = new StringBuilder();
		try {
			br = new BufferedReader(new FileReader(new File(file)));
		    String line;
		    while ((line = br.readLine()) != null) {
		        text.append(line);
		    }
		} catch (Exception e) {

		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				br = null;
			}
		}
		return text.toString();
	}
}
