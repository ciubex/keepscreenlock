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

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import ro.ciubex.keepscreenlock.MainApplication;

/**
 * This listener is used to handle the proximity sensor changes.
 *
 * @author Claudiu Ciobotariu
 *
 */
public class ProximityListener implements SensorEventListener {
	private static final String TAG = ProximityListener.class.getName();
	private MainApplication mApplication;
	private SensorManager mSensorManager;
	private UIProximityUpdateListener mListener;

	public interface UIProximityUpdateListener {
		public void proximitySensorUpdate(float value);
	}

	public ProximityListener(MainApplication application) {
		this(application, null);
	}

	public ProximityListener(MainApplication application, UIProximityUpdateListener listener) {
		mApplication = application;
		mSensorManager = application.getSensorManager();
		mListener = listener;
	}

	/**
	 * Register this class as a proximity sensor listener.
	 */
	public void registerProximityListener() {
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
				SensorManager.SENSOR_DELAY_NORMAL);
		mApplication.logD(TAG, "Register the Proximity Listener.");
	}

	/**
	 * Unregister this class to listen for proximity sensor changes.
	 */
	public void unregisterProximityListener() {
		mSensorManager.unregisterListener(this);
		mApplication.logD(TAG, "Unregister the Proximity Listener.");
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// nothing to implement
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (Sensor.TYPE_PROXIMITY == event.sensor.getType()) {
			handleProximityEvent(event);
		}
	}

	/**
	 * Handle proximity sensor events.
	 * @param event The proximity sensor event.
	 */
	private void handleProximityEvent(SensorEvent event) {
		float[] values = event.values;
		float value = values.length > 0 ? values[0] : MainApplication.PROXIMITY_FAR_VALUE;
		mApplication.setLastProximityValue(value);
		if (mListener != null) {
			mListener.proximitySensorUpdate(value);
		}
		if (Intent.ACTION_SCREEN_ON.equals(mApplication.getLastAction())) {
			mApplication.callKeepScreenLockTask();
		}
	}
}
