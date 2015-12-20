/**
 * This file is part of Keep Screen Lock application.
 *
 * Copyright (C) 2015 Claudiu Ciobotariu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ro.ciubex.keepscreenlock.util;

import android.database.Cursor;
import android.util.Log;

import java.io.Closeable;

/**
 * This is an utilities class with some useful methods.
 * Created by claudiu on 24.10.2015.
 */
public class Utilities {
	private static final String TAG = Utilities.class.getName();

	/**
	 * Returns true if the object is null or is empty.
	 *
	 * @param object The object to be examined.
	 * @return True if object is null or zero length.
	 */
	public static boolean isEmpty(Object object) {
		if (object instanceof CharSequence)
			return ((CharSequence) object).length() == 0;
		return object == null;
	}

	/**
	 * Close a closeable object.
	 *
	 * @param closeable Object to be close.
	 */
	public static void doClose(Object closeable) {
		if (closeable instanceof Closeable) {
			try {
				((Closeable) closeable).close();
			} catch (RuntimeException rethrown) {
				throw rethrown;
			} catch (Exception e) {
				Log.e(TAG, "doClose Exception: " + e.getMessage(), e);
			}
		} else if (closeable instanceof Cursor) {
			try {
				((Cursor) closeable).close();
			} catch (RuntimeException rethrown) {
				throw rethrown;
			} catch (Exception e) {
				Log.e(TAG, "doClose Exception: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Parse string value to integer value.
	 * @param value The string value used for parsing.
	 * @param defaultValue Default integer value.
	 * @return The parsed string value or default provided value.
	 */
	public static int getInteger(String value, int defaultValue) {
		int result = defaultValue;
		if (!isEmpty(value)) {
			try {
				result = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				Log.e(TAG, "getInteger(" + value + ") Exception: " + e.getMessage(), e);
			}
		}
		return result;
	}

	/**
	 * Parse string value to long value.
	 * @param value The string value used for parsing.
	 * @param defaultValue Default long value.
	 * @return The parsed string value or default provided value.
	 */
	public static long getLong(String value, long defaultValue) {
		long result = defaultValue;
		if (!isEmpty(value)) {
			try {
				result = Long.parseLong(value);
			} catch (NumberFormatException e) {
				Log.e(TAG, "getLong(" + value + ") Exception: " + e.getMessage(), e);
			}
		}
		return result;
	}

	/**
	 * Parse string value to float value.
	 * @param value The string value used for parsing.
	 * @param defaultValue Default float value.
	 * @return The parsed string value or default provided value.
	 */
	public static float getFloat(String value, float defaultValue) {
		float result = defaultValue;
		if (!isEmpty(value)) {
			try {
				result = Float.parseFloat(value);
			} catch (NumberFormatException e) {
				Log.e(TAG, "getFloat(" + value + ") Exception: " + e.getMessage(), e);
			}
		}
		return result;
	}
}
