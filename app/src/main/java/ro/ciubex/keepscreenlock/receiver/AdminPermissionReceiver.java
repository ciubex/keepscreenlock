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

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import ro.ciubex.keepscreenlock.MainApplication;
import ro.ciubex.keepscreenlock.R;

/**
 * This receiver is used to handle device admin receiver.
 *
 * @author Claudiu Ciobotariu
 *
 */
public class AdminPermissionReceiver extends DeviceAdminReceiver {

	private static final int STATE_ENABLED = 0;
	private static final int STATE_DISABLED = 1;

	private void showToast(Context context, CharSequence msg) {
		Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onEnabled(Context context, Intent intent) {
		onDeviceAdminReceiver(context, STATE_ENABLED);
	}

	@Override
	public CharSequence onDisableRequested(Context context, Intent intent) {
		return context.getString(R.string.device_admin_disable);
	}

	@Override
	public void onDisabled(Context context, Intent intent) {
		onDeviceAdminReceiver(context, STATE_DISABLED);
	}

	/**
	 * Common method to handle device admin receiver events.
	 * @param context Current context.
	 * @param state State of the admin privileges.
	 */
	private void onDeviceAdminReceiver(Context context, int state) {
		if (STATE_ENABLED == state) {
			showToast(context, context.getString(R.string.device_admin_enabled));
			saveKeepScreenLockEnabled(context, true);
		} else if (STATE_DISABLED == state) {
			showToast(context, context.getString(R.string.device_admin_disabled));
			saveKeepScreenLockEnabled(context, false);
		}
	}

	/**
	 * Store the admin privileges state.
	 * @param context Current context.
	 * @param flag The admin privileges state to be stored.
	 */
	private void saveKeepScreenLockEnabled(Context context, boolean flag) {
		MainApplication mainApplication = null;
		Context appCtx = context.getApplicationContext();
		if (appCtx instanceof MainApplication) {
			mainApplication = (MainApplication) appCtx;
		}
		if (mainApplication != null) {
			mainApplication.setEnableKeepScreenLockService(flag);
			mainApplication.checkKeepScreenLockReceiver();
		}
	}
}
