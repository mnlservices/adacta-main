package nl.defensie.adacta.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

public class ReadProperties {


    public static HashMap<String, String> getPropsInHashMap(String bundlename) {
    	ResourceBundle rb;
		try {
			rb = ResourceBundle.getBundle(bundlename);
		} catch (Exception e) {
			return null;
		}
    	HashMap<String, String> h = new HashMap<String, String>();
    	Enumeration<String> x = rb.getKeys();
    	while (x.hasMoreElements()){
    		String key=x.nextElement();
    		h.put(key, rb.getString(key));
    	}    		
        return h;
    }

    public static List<String> getPropsInArray(String bundlename) {
    	ResourceBundle rb;
		try {
			rb = ResourceBundle.getBundle(bundlename);
		} catch (Exception e) {
			return null;
		}
    	List<String> l = new ArrayList<String>();
    	Enumeration<String> x = rb.getKeys();
    	while (x.hasMoreElements()){
    		String key=x.nextElement();
    		l.add(key);
    	}    		
        return l;
    }

}
