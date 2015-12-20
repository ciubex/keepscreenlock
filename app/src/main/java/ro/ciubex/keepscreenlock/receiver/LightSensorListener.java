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
package ro.ciubex.keepscreenlock.receiver;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import ro.ciubex.keepscreenlock.MainApplication;

/**
 * This listener is used to handle the light sensor changes.
 *
 * @author Claudiu Ciobotariu
 *
 */
public class LightSensorListener implements SensorEventListener {
	private static final String TAG = LightSensorListener.class.getName();
	private MainApplication mApplication;
	private UILightSensorUpdateListener mUpdateListener;
	private SensorManager mSensorManager;

	public interface UILightSensorUpdateListener {
		public void lightSensorUpdate(float value);
	}

	public LightSensorListener(MainApplication application) {
		this(application, null);
	}

	public LightSensorListener(MainApplication application, UILightSensorUpdateListener updateListener) {
		mApplication = application;
		mUpdateListener = updateListener;
		mSensorManager = application.getSensorManager();
	}

	/**
	 * Register this class as a light sensor listener.
	 */
	public void registerLightSensorListener() {
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
				SensorManager.SENSOR_DELAY_NORMAL);
		mApplication.logD(TAG, "Register the Light Sensor Listener.");
	}

	/**
	 * Unregister this class to listen for light sensor changes.
	 */
	public void unregisterLightSensorListener() {
		mSensorManager.unregisterListener(this);
		mApplication.logD(TAG, "Unregister the Light Sensor Listener.");
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// nothing to implement
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (Sensor.TYPE_LIGHT == event.sensor.getType()) {
			handleLightSensorEvent(event);
		}
	}

	/**
	 * Handle light sensor events.
	 * @param event The light sensor event.
	 */
	private void handleLightSensorEvent(SensorEvent event) {
		float[] values = event.values;
		float lightValue = values.length > 0 ? values[0] : -1.0f;
		if (lightValue >= 0.0f) {
			mApplication.setLastLightValue(lightValue);
			if (mUpdateListener != null) {
				mUpdateListener.lightSensorUpdate(lightValue);
			}
		}
	}
}
