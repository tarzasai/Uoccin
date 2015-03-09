package net.ggelardi.uoccin.serv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleCache {
	
	private Map<String, Object> cache = new HashMap<String, Object>();
	private Map<String, Long> access = new HashMap<String, Long>();
	private int max = 100;
	
	public SimpleCache(int maxLength) {
		max = maxLength;
	}
	
	public synchronized Object get(String key) {
		Object res = cache.get(key);
		if (res != null)
			access.put(key, System.currentTimeMillis());
		return res;
	}
	
	public synchronized void add(String key, Object item) {
		if (!cache.containsKey(key))
			while (cache.size() >= max) {
				String old = null;
				long acc = System.currentTimeMillis();
				for (Map.Entry<String, Long> chk : access.entrySet())
					if (chk.getValue() < acc) {
						old = chk.getKey();
						acc = chk.getValue();
					}
				cache.remove(old);
				access.remove(old);
			}
		cache.put(key, item);
		access.put(key, System.currentTimeMillis());
	}
	
	public synchronized void del(String key) {
		cache.remove(key);
		access.remove(key);
	}
	
	public synchronized void clear() {
		cache = new HashMap<String, Object>();
		access = new HashMap<String, Long>();
	}
	
	public synchronized List<String> getKeys() {
		return new ArrayList<String>(cache.keySet());
	}
}