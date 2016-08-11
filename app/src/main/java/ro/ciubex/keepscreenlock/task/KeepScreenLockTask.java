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
package ro.ciubex.keepscreenlock.task;

import android.content.Intent;
import android.os.AsyncTask;

import ro.ciubex.keepscreenlock.MainApplication;

/**
 * A thread used to handle the locking decisions.
 *
 * @author Claudiu Ciobotariu
 *
 */
public class KeepScreenLockTask extends AsyncTask<Void, Void, Void> {
	private static final String TAG = KeepScreenLockTask.class.getName();
	private MainApplication mApplication;
	private long mDelayMilliseconds;

	public KeepScreenLockTask(MainApplication mApplication) {
		this.mApplication = mApplication;
	}

	/**
	 * Runs on the UI thread before doInBackground(Params...).
	 */
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	/**
	 * Method invoked on the background thread.
	 *
	 * @param params In this case are not used parameters.
	 * @return Is returned a void value.
	 */
	@Override
	protected Void doInBackground(Void... params) {
		mDelayMilliseconds = mApplication.getLockScreenDelay();
		while(MainApplication.isScreenLockRequested) {
			MainApplication.isScreenLockRequested = false;
			executeDelay();
			checkScreenLockConditions();
		}
		MainApplication.isKeepScreenLockTaskRunning = false;
		return null;
	}

	/**
	 * Wait for the time mentioned on the settings.
	 */
	private void executeDelay() {
		try {
			Thread.sleep(mDelayMilliseconds);
		} catch (InterruptedException e) {
			mApplication.logE(TAG, "InterruptedException", e);
		}
	}

	/**
	 * Check for the screen lock conditions.
	 */
	private void checkScreenLockConditions() {
		String lastAction = mApplication.getLastAction();
		boolean lockScreen = mApplication.isDeviceLocked();
		mApplication.logD(TAG, "checkScreenLockConditions");
		if (mApplication.isEnabledKeepScreenLockService() &&
				Intent.ACTION_SCREEN_ON.equals(lastAction)) {
			float proximityValue = mApplication.getLastProximityValue();
			mApplication.logD(TAG, "proximityValue: " + proximityValue);
			// check if the proximity sensor is not covered
			if (proximityValue > MainApplication.PROXIMITY_NEAR_VALUE) {
				lockScreen = false;
			} else { // check the light sensor values
				if (mApplication.isEnableLightSensorListener()) {
					float lightValue = mApplication.getLastLightValue();
					mApplication.logD(TAG, "lightValue: " + lightValue);
					if (lightValue < 0 || lightValue > mApplication.getLimitLightSensorValue()) {
						lockScreen = false;
						MainApplication.isScreenLockRequested = true;
						mDelayMilliseconds = 100;
					}
				}
			}
		}
		if (lockScreen) {
			mApplication.logD(TAG, "Try to lock the screen!");
			mApplication.executeLockScreen(true);
		}
	}

	/**
	 * Runs on the UI thread after doInBackground(Params...). The specified
	 * result is the value returned by doInBackground(Params...).
	 *
	 * @param result The result of the operation computed by
	 *              doInBackground(Params...).
	 */
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
	}
}
