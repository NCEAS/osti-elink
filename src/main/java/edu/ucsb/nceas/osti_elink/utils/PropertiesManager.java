package edu.ucsb.nceas.osti_elink.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class PropertiesManager {
    private static final Properties properties = new Properties();

    static {
        try(InputStream input = PropertiesManager.class.getClassLoader().getResourceAsStream("urls.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find urls.properties");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getBaseAPIUrl() {
        return properties.getProperty("base.api");
    }
}
