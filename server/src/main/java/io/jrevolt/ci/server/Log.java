package io.jrevolt.ci.server;

import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class Log {
	
	static public void debug(Object source, String message, Object ... args) {
		LoggerFactory.getLogger(source.getClass()).debug(message, args);
	}
	
	static public void warn(Object source, String message, Object ... args) {
		LoggerFactory.getLogger(source.getClass()).warn(message, args);
	}

}
