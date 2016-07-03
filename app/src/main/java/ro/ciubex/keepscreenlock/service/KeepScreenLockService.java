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
package ro.ciubex.keepscreenlock.service;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.view.Display;

import ro.ciubex.keepscreenlock.MainApplication;
import ro.ciubex.keepscreenlock.receiver.PhoneCallReceiver;
import ro.ciubex.keepscreenlock.receiver.ScreenActionsReceiver;
import ro.ciubex.keepscreenlock.util.Utilities;

/**
 * This is a service which should keep the screen locked.
 *
 * @author Claudiu Ciobotariu
 */
public class KeepScreenLockService extends Service {
	private static final String TAG = KeepScreenLockService.class.getName();
	private MainApplication mApplication;
	private AlarmManager mAlarmService;

	private ScreenActionsReceiver mScreenActionsReceiver;
	private BroadcastReceiver mPhoneCallReceiver;

	public static final String ENABLE_SCREEN_ACTIONS_RECEIVER = "enableScreenActionsReceiver";
	public static final String ENABLE_PHONE_CALL_RECEIVER = "enablePhoneCallReceiver";

	final Messenger mMessenger = new Messenger(new IncomingHandler());

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Bundle data = msg.getData();
			if (data != null) {
				handleMessageScreenActionsReceiver(data);
				handleMessageEnablePhoneCallReceiver(data);
			}
		}
	}

	private void handleMessageScreenActionsReceiver(Bundle data) {
		String value = data.getString(KeepScreenLockService.ENABLE_SCREEN_ACTIONS_RECEIVER);
		if (!Utilities.isEmpty(value)) {
			if ("true".equalsIgnoreCase(value)) {
				registerScreenActionsReceiver();
			} else {
				unregisterScreenActionsReceiver();
			}
		}
	}

	private void handleMessageEnablePhoneCallReceiver(Bundle data) {
		String value = data.getString(KeepScreenLockService.ENABLE_PHONE_CALL_RECEIVER);
		if (!Utilities.isEmpty(value)) {
			if ("true".equalsIgnoreCase(value)) {
				registerPhoneCallReceiver();
			} else {
				unregisterPhoneCallReceiver();
			}
		}
	}

	/**
	 * Called by the system when the service is first created.
	 */
	@Override
	public void onCreate() {
		mApplication = null;
		Context appCtx = getApplicationContext();
		if (appCtx instanceof MainApplication) {
			mApplication = (MainApplication) appCtx;
			mAlarmService = (AlarmManager) mApplication.getSystemService(Context.ALARM_SERVICE);
			mApplication.logD(TAG, "KeepScreenLockService onCreate.");
		}
	}

	/**
	 * Called by the system to notify a Service that it is no longer used and is
	 * being removed.
	 */
	@Override
	public void onDestroy() {
		unregisterReceivers();
		mApplication.logD(TAG, "KeepScreenLockService destroyed.");
	}

	/**
	 * Method used to unregister all receivers.
	 */
	private void unregisterReceivers() {
		unregisterScreenActionsReceiver();
		unregisterPhoneCallReceiver();
		mApplication.logD(TAG, "KeepScreenLockService unregister receivers.");
	}

	/**
	 * Return the communication channel to the service. Return null because
	 * clients can not bind to the service.
	 *
	 * @param intent Not used.
	 * @return The messenger binder.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	/**
	 * Return the communication channel to the service.
	 *
	 * @param intent The Intent that was used to bind to this service.
	 * @return Return an IBinder through which clients can call on to the
	 * service.
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}

	/**
	 * This is called if the service is currently running and the user has
	 * removed a task that comes from the service's application.
	 */
	@Override
	public void onTaskRemoved(Intent rootIntent) {
		mApplication.logD(TAG, "onTaskRemoved");
		unregisterReceivers();
		prepareForRestartService();
		super.onTaskRemoved(rootIntent);
	}

	/**
	 * This is a workaround method to ensure service restart.
	 */
	private void prepareForRestartService() {
		Intent restartService = new Intent(mApplication, KeepScreenLockService.class);
		restartService.setPackage(getPackageName());
		PendingIntent restartServicePI = PendingIntent.getService(
				mApplication, 1, restartService, PendingIntent.FLAG_ONE_SHOT);
		mAlarmService.set(AlarmManager.ELAPSED_REALTIME,
				SystemClock.elapsedRealtime() + 1000, restartServicePI);
	}

	/**
	 * Called by the system every time a client explicitly starts the service by
	 * calling startService(Intent), providing the arguments it supplied and a
	 * unique integer token representing the start request. Do not call this
	 * method directly.
	 *
	 * @param intent  The Intent supplied to startService(Intent), as given.
	 * @param flags   Additional data about this start request.
	 * @param startId A unique integer representing this specific request to start.
	 * @return We want this service to continue running until it is explicitly
	 * stopped, so return sticky.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mApplication.logD(TAG, "KeepScreenLockService onStartCommand: " + startId);
		if (intent != null && intent.getFlags() == 0) {
			registerReceivers();
		}
		return Service.START_STICKY;
	}

	/**
	 * Register all receivers.
	 */
	private void registerReceivers() {
		mApplication.logD(TAG, "KeepScreenLockService register receivers.");

		if (isScreenOff()) {
			mApplication.setScreenLockedFlag(true, false);
		} else {
			if (mApplication.isDeviceLocked()) {
				mApplication.setScreenLockedFlag(true, false);
				mApplication.callKeepScreenLockTask();
			} else {
				mApplication.setScreenLockedFlag(false, false);
			}
		}
		registerScreenActionsReceiver();
		if (mApplication.isEnabledPhoneListener()) {
			registerPhoneCallReceiver();
		}
	}

	/**
	 * Register the broadcast receiver used to handle the screen events.
	 */
	private void registerScreenActionsReceiver() {
		if (mScreenActionsReceiver == null) {
			mScreenActionsReceiver = new ScreenActionsReceiver();
			final IntentFilter theFilter = new IntentFilter();
			/** System Defined Broadcast */
			theFilter.addAction(Intent.ACTION_SCREEN_ON);
			theFilter.addAction(Intent.ACTION_SCREEN_OFF);
			theFilter.addAction(Intent.ACTION_USER_PRESENT);
			mApplication.getApplicationContext().registerReceiver(mScreenActionsReceiver, theFilter);
			mApplication.logD(TAG, "Register the ScreenActionsReceiver.");
		}
	}

	/**
	 * Unregister the broadcast receiver used to handle the screen events.
	 */
	private void unregisterScreenActionsReceiver() {
		mApplication.logD(TAG, "Unregister the ScreenActionsReceiver.");
		if (mScreenActionsReceiver != null) {
			mApplication.unregisterListeners();
			unregisterBroadcastReceiver(mScreenActionsReceiver);
		}
		mScreenActionsReceiver = null;
	}

	/**
	 * Register the broadcast receiver used to handle the phone calls.
	 */
	private void registerPhoneCallReceiver() {
		if (mPhoneCallReceiver == null) {
			mPhoneCallReceiver = new PhoneCallReceiver();
			final IntentFilter theFilter = new IntentFilter();
			theFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
			theFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
			mApplication.getApplicationContext().registerReceiver(mPhoneCallReceiver, theFilter);
			mApplication.logD(TAG, "Register the PhoneCallReceiver.");
		}
	}

	/**
	 * Unregister the phone call receiver.
	 */
	private void unregisterPhoneCallReceiver() {
		mApplication.logD(TAG, "Unregister the PhoneCallReceiver.");
		unregisterBroadcastReceiver(mPhoneCallReceiver);
		mPhoneCallReceiver = null;
	}

	/**
	 * Method used to unregister a broadcast receiver
	 *
	 * @param receiver The receiver to be unregistered.
	 */
	private void unregisterBroadcastReceiver(BroadcastReceiver receiver) {
		if (receiver != null) {
			int apiLevel = Build.VERSION.SDK_INT;
			if (apiLevel >= 7) {
				try {
					mApplication.getApplicationContext().unregisterReceiver(receiver);
				} catch (IllegalArgumentException e) {
				}
			} else {
				mApplication.getApplicationContext().unregisterReceiver(receiver);
			}
		}
	}

	/**
	 * Check if the screen is OFF
	 *
	 * @return True, if the screen is off.
	 */
	private boolean isScreenOff() {
		boolean result;
		int apiLevel = Build.VERSION.SDK_INT;
		if (apiLevel < Build.VERSION_CODES.KITKAT_WATCH) { // old API
			result = isScreenOffOldAPI();
		} else { // new API
			result = isScreenOffNewAPI20();
		}
		return result;
	}

	/**
	 * Check if the screen is OFF used for old API implementation (< API 20)
	 *
	 * @return True, if the screen is Off.
	 */
	private boolean isScreenOffOldAPI() {
		PowerManager powerManager = (PowerManager)mApplication.getSystemService(POWER_SERVICE);
		return !powerManager.isScreenOn();
	}

	/**
	 * Check if the screen is OFF used for new API implementation (API 20 >=)
	 *
	 * @return True, if the screen is off.
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
	private boolean isScreenOffNewAPI20() {
		int state;
		DisplayManager dm = (DisplayManager)mApplication.getSystemService(Context.DISPLAY_SERVICE);
		for (Display display : dm.getDisplays()) {
			state = display.getState();
			mApplication.logD(TAG, "display.getState()=" + state);
			if (Display.STATE_OFF == state) {
				return true;
			}
		}
		return false;
	}
}
