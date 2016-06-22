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
import android.telephony.TelephonyManager;

import ro.ciubex.keepscreenlock.MainApplication;

/**
 * This broadcast receiver is used to monitor the phone calls.
 *
 * @author Claudiu Ciobotariu
 *
 */
public class PhoneCallReceiver extends BroadcastReceiver {
	private static final String TAG = PhoneCallReceiver.class.getName();
	public static final String PHONE_CALL_ACTION = "phoneCallAction";
	private int mLastCallStateId = TelephonyManager.CALL_STATE_IDLE;

	/**
	 * This method is called when the BroadcastReceiver is receiving an Intent
	 * broadcast.
	 *
	 * @param context
	 *            The Context in which the receiver is running.
	 * @param intent
	 *            The Intent being received.
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
			if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) { // outgoing call

			} else {
				String extraState = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
				int callStateId = TelephonyManager.CALL_STATE_IDLE;
				if (TelephonyManager.EXTRA_STATE_IDLE.equals(extraState)) {
					callStateId = TelephonyManager.CALL_STATE_IDLE;
				} else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(extraState)) {
					callStateId = TelephonyManager.CALL_STATE_OFFHOOK;
				} else if (TelephonyManager.EXTRA_STATE_RINGING.equals(extraState)) {
					callStateId = TelephonyManager.CALL_STATE_RINGING;
				}
				onCallStateChanged(application, callStateId);
			}
		}
	}

	/**
	 * Handle the phone state changed.
	 * @param callStateId
	 */
	private void onCallStateChanged(MainApplication application, int callStateId) {
		if (callStateId != mLastCallStateId) { // handle only the differences
			application.logD(TAG, "Phone call state: " + callStateId + " last call state: " + mLastCallStateId);
			application.setPhoneActive(TelephonyManager.CALL_STATE_IDLE != callStateId);
			application.setLastAction(PHONE_CALL_ACTION);
			application.setLastPhoneState(callStateId);
			if (TelephonyManager.CALL_STATE_IDLE == callStateId) {
				if (application.isScreenLocked()) {
					application.setScreenLockedFlag(true, false);
					application.callKeepScreenLockTask();
				}
			}
			mLastCallStateId = callStateId;
		}
	}
}
