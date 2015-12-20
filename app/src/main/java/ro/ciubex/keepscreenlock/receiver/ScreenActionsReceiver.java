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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ro.ciubex.keepscreenlock.MainApplication;

/**
 * This broadcast receiver is used by the screen actions.
 *
 * @author Claudiu Ciobotariu
 */
public class ScreenActionsReceiver extends BroadcastReceiver {
	private static final String TAG = ScreenActionsReceiver.class.getName();
	private static final String NULL = "null";
	private ProximityListener mProximityListener;
	private LightSensorListener mLightSensorListener;
	private String mLastAction = NULL;

	/**
	 * This method is called when the BroadcastReceiver is receiving an Intent
	 * broadcast.
	 *
	 * @param context The Context in which the receiver is running.
	 * @param intent  The Intent being received.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Context appCtx = context.getApplicationContext();
		MainApplication application = null;
		if (appCtx instanceof MainApplication) {
			application = (MainApplication) appCtx;
		}
		if (application != null) {
			String action = String.valueOf(intent.getAction());
			if (!mLastAction.equals(action)) {
				String lastAction = application.getLastAction();
				application.logD(TAG, "Received the action: " + action);
				if (Intent.ACTION_SCREEN_OFF.equals(action)) { // detected screen off.
					application.setScreenLockedFlag(true, false);
					unregisterListeners();
				} else if (Intent.ACTION_SCREEN_ON.equals(action)) { // detected screen on.
					if (application.isScreenLocked()) {
						registerListeners(application);
					} else {
						application.hideLockScreenNotification();
					}
				} else if (Intent.ACTION_USER_PRESENT.equals(action)) { // detected unlock event.
					if (application.isPhoneActive() ||
							PhoneCallReceiver.PHONE_CALL_ACTION.equals(lastAction)) {
						application.setScreenLockedFlag(true, true);
					} else {
						application.setScreenLockedFlag(false, false);
						application.hideLockScreenNotification();
						unregisterListeners();
					}
				}
				application.setLastAction(action);
				mLastAction = action;
			}
		}
	}

	/**
	 * Register all listeners.
	 */
	private void registerListeners(MainApplication application) {
		registerProximityListener(application);
		registerLightSensorListener(application);
	}

	/**
	 * Unregister all listeners.
	 */
	public void unregisterListeners() {
		unregisterProximityListener();
		unregisterLightSensorListener();
	}

	/**
	 * Register the proximity listener used to handle the proximity events.
	 */
	private void registerProximityListener(MainApplication application) {
		if (mProximityListener == null) {
			application.setLastProximityValue(MainApplication.PROXIMITY_FAR_VALUE);
			mProximityListener = new ProximityListener(application);
			mProximityListener.registerProximityListener();
		}
	}

	/**
	 * Register the light sensor listener used to handle the light sensor events.
	 */
	private void registerLightSensorListener(MainApplication application) {
		if (mLightSensorListener == null) {
			if (application.isEnableLightSensorListener()) {
				application.setLastLightValue(MainApplication.LIGHT_VALUE_INVALID);
				mLightSensorListener = new LightSensorListener(application);
				mLightSensorListener.registerLightSensorListener();
			}
		}
	}

	/**
	 * Unregister the proximity listener.
	 */
	private void unregisterProximityListener() {
		if (mProximityListener != null) {
			mProximityListener.unregisterProximityListener();
			mProximityListener = null;
		}
	}

	/**
	 * Unregister the light sensor listener.
	 */
	private void unregisterLightSensorListener() {
		if (mLightSensorListener != null) {
			mLightSensorListener.unregisterLightSensorListener();
			mLightSensorListener = null;
		}
	}
}
