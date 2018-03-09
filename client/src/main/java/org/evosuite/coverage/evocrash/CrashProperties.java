/**
 * Copyright (C) 2017 Mozhan Soltani, Annibale Panichella, and Arie van Deursen
 *
 * This file is part of EvoCrash.
 *
 * EvoCrash is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoCrash is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */

package org.evosuite.coverage.evocrash;

import java.util.Map;

//import org.crash.client.log.LogParser;
import org.evosuite.Properties;
import org.evosuite.Properties.NoSuchParameterException;
import org.evosuite.utils.LoggingUtils;
import java.util.HashMap;
import java.lang.reflect.Field;
import java.util.Set;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class CrashProperties {


	/** Singleton instance */
	private static CrashProperties instance = null;
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Parameter {
		String key();

		String group() default "Experimental";

		String description();
	}
	
	/** Internal properties hashmap */
	private java.util.Properties crash_properties;
	
	
	/** All fields representing values, inserted via reflection */
	private static Map<String, Field> parameterMap = new HashMap<String, Field>();
	
	private static Map<Field, Object> defaultMap = new HashMap<Field, Object>();

	static {
		// need to do it once, to capture all the default values
		reflectMap();
	}
	/**
	 * Determine fields that are declared as parameters
	 */
	private static void reflectMap() {
		for (Field f : CrashProperties.class.getFields()) {
			if (f.isAnnotationPresent(Parameter.class)) {
				Parameter p = f.getAnnotation(Parameter.class);
				parameterMap.put(p.key(), f);
				try {
					defaultMap.put(f, f.get(null));
				} catch (Exception e) {
					LoggingUtils.getEvoLogger().error("Exception: " + e.getMessage(), e);
				}
			}
		}
	}
	
	
	/**
	 * Get all parameters that are available
	 *
	 * @return a {@link java.util.Set} object.
	 */
	public static Set<String> getParameters() {
		return parameterMap.keySet();
	}
	
	private CrashProperties(boolean loadProperties, boolean silent) {
		if (loadProperties)
			initializeProperties();
	}
	
	/**
	 * Initialize properties from property file or command line parameters
	 */
	private void initializeProperties() throws IllegalStateException{
		for (String parameter : parameterMap.keySet()) {
			try {
				String property = System.getProperty(parameter);
				
//				if (property == null) {
//					LoggingUtils.getEvoLogger().info("CP, initialize: " + property + parameter);
					
				    // FIXME: Loading from the properties file is not implemented in CrashProperties!
//					property = crash_properties.getProperty(parameter);
//					LoggingUtils.getEvoLogger().info("CP, initialize: " + property);
//				}
				if (property != null) {
					setValue(parameter, property);
				}
			} catch (Exception e) {
                throw new IllegalStateException("Wrong parameter settings for '" + parameter + "': " + e.getMessage());
            }
		}

	}
	
	

	/**
	 * Set the given <code>key</code> variable to the given input Object
	 * <code>value</code>
	 *
	 * @param key
	 * @param value
	 * @throws NoSuchParameterException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public void setValue(String key, String value)
			throws NoSuchParameterException, IllegalArgumentException,
			IllegalAccessException {
		if (!parameterMap.containsKey(key)) {
			throw new NoSuchParameterException(key);
		}
		
		Field f = parameterMap.get(key);
		//Enum
		if (f.getType().isEnum()) {
			f.set(null, Enum.valueOf((Class<Enum>) f.getType(),
					value.toUpperCase()));
		}
		
		else if (f.getType().equals(int.class)) {
			f.set(null, Integer.parseInt(value));
		}
		else {
			f.set(this, value);
		}
	}
	
	
	/**
	 * Singleton accessor
	 *
	 * @return a {@link org.evosuite.Properties} object.
	 */
	public static CrashProperties getInstance() {
		if (instance == null)
			instance = new CrashProperties(true, false);
		return instance;
	}

//	/**
//	 * Singleton accessor
//	 *
//	 * @return a {@link org.evosuite.Properties} object.
//	 */
//	public static CrashProperties getInstanceSilent() {
//		if (instance == null)
//			instance = new CrashProperties(true, true);
//		return instance;
//	}
	
	
	
	/** The target frame in the crash stack trace */
	@Parameter(key = "max_init_rounds", group = "Runtime", description = "The maximum number of times the search tries to initialize the population which involves the target method.")
	public static int MAX_INIT_ROUNDS = 150;
	
	/** Where Stack Trace file may be found*/
	@Parameter(key = "EXP", group = "Runtime", description = "The filepath of the target Exception file")
	//TODO: Clean up the path!
	public static String EXP = "";
	
	/** The full name of the exception to generate*/
	@Parameter(key = "target_exception_crash", group = "Runtime", description = "The full name of the exception to generate")
	public static String TARGET_EXCEPTION_CRASH = "";
	
	
	/** The target frame in the crash stack trace */
	@Parameter(key = "target_frame", group = "Runtime", description = "The target frame in the crash stack trace")
	public static int TARGET_FRAME = -1;
	
	
//	/** The exception to be reproduced */
//	@Parameter(key = "TARGET_EXCEPTION", group = "Runtime", description = "The exception causing crash")
	public static Throwable TARGET_EXCEPTION;
	
	public static void setTargetException (String path) {
		try {
		CrashProperties.TARGET_EXCEPTION = LogParser.getTargetException(path);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	
	public enum CrashAlgorithm {
		 PICKYMONOTONICGA , MONOTONICGA
	}

	@Parameter(key = "crashalgorithm", group = "Search Algorithm", description = "Search algorithm")
	public static CrashAlgorithm CRASHALGORITHM = CrashAlgorithm.PICKYMONOTONICGA;
	
	
	public enum RootMethodTestFactory {
		ROOT_PUBLIC_METHOD, ARCHIVE
	}

	/** Constant <code>ROOT_TEST_FACTORY</code> */
	@Parameter(key = "root_test_factory", description = "Which factory creates tests")
	public static RootMethodTestFactory ROOT_TEST_FACTORY = RootMethodTestFactory.ROOT_PUBLIC_METHOD;

}
