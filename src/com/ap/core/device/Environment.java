/**
 * AgingPlaceMobile
 */
package com.ap.core.device;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * @author jungwhan
 * Environment.java
 * 1:02:36 AM Feb 11, 2016 2016
 */
public class Environment {
	private final static String PROPERTIES_FILE = "env";
	
	private static ResourceBundle bundle = ResourceBundle.getBundle(PROPERTIES_FILE);
	
	private static String environment;
	
	static {
		environment = bundle.getString("env");
	}
	
	public Environment() {
		
	}
	
	public String getUrl(String restPath) {
		String domain = bundle.getString(environment + ".url");
		if (!restPath.startsWith("/")) {
			domain = domain + "/";
		}
		return MessageFormat.format(domain, new Object[]{restPath});
	}
	
	public String getEnv(String key) {
		return bundle.getString(key);
	}
}
