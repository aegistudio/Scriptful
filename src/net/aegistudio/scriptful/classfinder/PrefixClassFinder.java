package net.aegistudio.scriptful.classfinder;

/**
 * Surrogate class with prefix, until one class matches.
 * 
 * @author aegistudio
 */

public class PrefixClassFinder implements ClassFinder {
	private final String[] prefices;
	public PrefixClassFinder(String[] prefices) {
		this.prefices = prefices;
	}
	
	public Class<?> findClass(String abbreviation) {
		try {
			return Class.forName(abbreviation);
		} catch(ClassNotFoundException e) {		}
		
		for(String prefix : prefices) try {
			Class<?> clazz = Class.forName(prefix + "." + abbreviation);
			if(clazz != null) return clazz;
		} catch(ClassNotFoundException e) {
			// next;
		}
		return null;
	}
}