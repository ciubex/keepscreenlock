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
 * Receiver for shortcut uninstall
 *
 * @author Claudiu Ciobotariu
 */
public class ShortcutUninstallReceiver extends BroadcastReceiver {

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent data) {
		MainApplication application = null;
		Context appCtx = context.getApplicationContext();
		if (appCtx instanceof MainApplication) {
			application = (MainApplication) appCtx;
		}
		if (application != null) {
			application.updateScreenShortcutListener(data,
					ScreenLockShortcutUpdateListener.TYPE.UNINSTALL);
		}
	}

}
