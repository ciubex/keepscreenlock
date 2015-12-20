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
import android.util.Log;

import ro.ciubex.keepscreenlock.MainApplication;

/**
 * This broadcast receiver is used at the end of boot time.
 *
 * @author Claudiu Ciobotariu
 *
 */
public class BootBroadcastReceiver extends BroadcastReceiver {
	private static final String TAG = BootBroadcastReceiver.class.getName();

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
		String action = String.valueOf(intent.getAction());
		Log.d(TAG, "BootBroadcastReceiver.onReceive(" + action + ")");
		if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			MainApplication mainApplication = null;
			Context appCtx = context.getApplicationContext();
			if (appCtx instanceof MainApplication) {
				mainApplication = (MainApplication) appCtx;
			}
			if (mainApplication != null) {
				mainApplication.logD(TAG, "BootBroadcastReceiver started.");
				mainApplication.checkKeepScreenLockReceiver();
			}
		}
	}
}
