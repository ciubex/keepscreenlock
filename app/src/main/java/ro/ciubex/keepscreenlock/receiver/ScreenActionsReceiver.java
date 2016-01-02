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
				application.logD(TAG, "Received the action: " + action);
				if (Intent.ACTION_SCREEN_OFF.equals(action)) { // detected screen off.
					handleScreenOffCase(application);
				} else if (Intent.ACTION_SCREEN_ON.equals(action)) { // detected screen on.
					handleScreenOnCase(application);
				} else if (Intent.ACTION_USER_PRESENT.equals(action)) { // detected unlock event.
					handleUserPresentCase(application);
				}
				application.setLastAction(action);
				mLastAction = action;
			}
		}
	}

	/**
	 * Handle cases when the screen is turned OFF.
	 */
	private void handleScreenOffCase(MainApplication application) {
		application.setScreenLockedFlag(true, false);
		application.unregisterListeners();
	}

	/**
	 * Checking when the screen is turned ON.
	 */
	private void handleScreenOnCase(MainApplication application) {
		if (application.isScreenLocked()) { // screen is marked as locked.
			if (application.isPhoneActive() &&
					PhoneCallReceiver.PHONE_CALL_ACTION.equals(application.getLastAction())) { // check for calls
				if (application.isEnableWhenHeadset()) { // check the headset
					if (application.isHeadsetConnected()) {
						application.registerListeners();
					} else {
						application.hideLockScreenNotification();
					}
				} else { // not checking for headset or bluetooth state
					application.registerListeners();
				}
			} else { // no phone call was made
				application.registerListeners();
			}
		} else { // screen is not locked
			application.hideLockScreenNotification();
		}
	}

	/**
	 * Handle cases when the device is unlocked. In some cases the device can be unlocked
	 * by starting directly an application or by made a call from an headset.
	 */
	private void handleUserPresentCase(MainApplication application) {
		if (application.isPhoneActive() ||
				PhoneCallReceiver.PHONE_CALL_ACTION.equals(application.getLastAction())) { // check for calls
			handlePhoneCallCases(application);
		} else { // no calls, maybe a camera app is started disable locking
			application.setScreenLockedFlag(false, false);
			application.hideLockScreenNotification();
			application.unregisterListeners();
		}
	}

	/**
	 * Checking when a phone call is made.
	 */
	private void handlePhoneCallCases(MainApplication application) {
		if (application.isEnableWhenHeadset()) { // check the headset
			if (application.isHeadsetConnected()) {
				application.setScreenLockedFlag(true, true);
			} else {
				application.setScreenLockedFlag(false, false);
				application.hideLockScreenNotification();
				application.unregisterListeners();
			}
		} else { // no headset check (default)
			application.setScreenLockedFlag(true, true);
		}
	}
}
