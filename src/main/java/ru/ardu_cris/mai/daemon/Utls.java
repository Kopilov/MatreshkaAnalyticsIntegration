package ru.ardu_cris.mai.daemon;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

/**
 *
 * @author aleksandr
 */
public class Utls {
	public static void addJsonString(JsonObjectBuilder builder, String name, String value) {
		if (value == null) {
			builder.addNull(name);
		} else {
			builder.add(name, value);
		}
	}
	public static void addJsonString(JsonArrayBuilder builder, String value) {
		if (value == null) {
			builder.addNull();
		} else {
			builder.add(value);
		}
	}
}
