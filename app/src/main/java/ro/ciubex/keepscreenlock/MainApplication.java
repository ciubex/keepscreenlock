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
package ro.ciubex.keepscreenlock;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import ro.ciubex.keepscreenlock.receiver.AdminPermissionReceiver;
import ro.ciubex.keepscreenlock.receiver.LightSensorListener;
import ro.ciubex.keepscreenlock.receiver.ProximityListener;
import ro.ciubex.keepscreenlock.service.KeepScreenLockService;
import ro.ciubex.keepscreenlock.receiver.ScreenLockReceiver;
import ro.ciubex.keepscreenlock.receiver.ScreenLockShortcutUpdateListener;
import ro.ciubex.keepscreenlock.task.KeepScreenLockTask;
import ro.ciubex.keepscreenlock.task.LogThread;
import ro.ciubex.keepscreenlock.util.Utilities;

/**
 * This is the application class for the Keep Screen Lock application.
 *
 * @author Claudiu Ciobotariu
 */
public class MainApplication extends Application {
	private static final String TAG = MainApplication.class.getName();
	private static Context mContext;
	private Locale mLocale;
	private SharedPreferences mSharedPreferences;
	private static int mSdkInt = 8;
	private static int mVersionCode = -1;
	private static String mVersionName = null;
	private static boolean isEmulator;

	public static final float PROXIMITY_NEAR_VALUE = 0.0f;
	public static final float PROXIMITY_FAR_VALUE = 1.0f;
	public static final float LIGHT_VALUE_INVALID = -1.0f;
	private static final int NOTIFICATION_ID = 0;

	public static final String LOGS_FOLDER_NAME = "logs";
	public static final String LOG_FILE_NAME = "KSL_app_logs.log";
	private File mLogsFolder;
	private static File mLogFile;
	private static LogThread mLogFileThread;
	private static SimpleDateFormat mSimpleDateFormatter;

	private static final String FIRST_TIME = "firstTime";
	public static final String KEY_ENABLE_KEEP_SCREEN_LOCK_SERVICE = "enableKeepScreenLockService";
	public static final String KEY_ENABLE_PHONE_LISTENER = "enablePhoneListener";
	public static final String KEY_ENABLE_LIGHT_SENSOR_LISTENER = "enableLightSensorListener";
	public static final String KEY_LIMIT_LIGHT_SENSOR_VALUE = "limitLightSensorValue";
	public static final String KEY_LOCK_SCREEN_DELAY = "lockScreenDelay";
	public static final String KEY_KEEP_SCREEN_LOCK_COUNTER = "keepScreenLockCounter";
	public static final String KEY_KEEP_SCREEN_LOCK_LOGS = "keepScreenLockLogs";
	public static final String KEY_SCREEN_LOCK_LOGS_DATETIMEFORMAT = "screenLockLogsDateTimeFormat";
	public static final String KEY_LAST_ACTION = "lastAction";
	private static final String KEY_SCREEN_LOCKED_FLAG = "screenLockedFlag";
	private static final String KEY_LAST_PROXIMITY_VALUE = "lastProximityValue";
	private static final String KEY_LAST_LIGHT_VALUE = "lastLightValue";
	private static final String KEY_PHONE_ACTIVE = "phoneActive";
	private static final String KEY_ENABLE_WHEN_HEADSET = "enableWhenHeadset";
	public static final String KEY_NOTIFICATION_ENABLED = "notificationEnabled";
	private static final String KEY_NOTIFICATION_ALWAYS_DISMISSIBLE = "notificationAlwaysDismissible";
	public static final String KEY_TOGGLE_NOTIFICATION = "toggleNotification";
	private static final String KEY_SCREEN_LOCK_SHORTCUT_CREATED = "screenLockShortcutCreated";

	private SensorManager mSensorManager;
	private AudioManager mAudioManager;

	private ProximityListener mProximityListener;
	private LightSensorListener mLightSensorListener;

	private Messenger mService = null;
	private boolean mBound;
	private Intent mKeepScreenLockServiceIntent;

	public static boolean isScreenLockRequested;
	public static boolean isKeepScreenLockTaskRunning;

	private DevicePolicyManager mDeviceManger;
	private ComponentName mComponentName;
	private KeyguardManager mKeyguardManager;
	private DateFormat mDateFormat;

	private ProgressDialog mProgressDialog;

	private NotificationManager mNotificationManager;
	private Notification mNotification;

	private ScreenLockShortcutUpdateListener mShortcutUpdateListener;

	public static final String KEY_LANGUAGE_CODE = "languageCode";
	private static final String KEY_HAVE_PERMISSIONS_ASKED = "havePermissionsAsked";
	public static final String PERMISSION_FOR_OUTGOING_CALLS = "android.permission.PROCESS_OUTGOING_CALLS";
	public static final String PERMISSION_FOR_READ_PHONE_STATE = "android.permission.READ_PHONE_STATE";
	public static final String PERMISSION_FOR_MODIFY_AUDIO_SETTINGS = "android.permission.MODIFY_AUDIO_SETTINGS";
	public static final String PERMISSION_FOR_RECEIVE_BOOT_COMPLETED = "android.permission.RECEIVE_BOOT_COMPLETED";
	public static final String PERMISSION_FOR_INSTALL_SHORTCUT = "com.android.launcher.permission.INSTALL_SHORTCUT";
	public static final String PERMISSION_FOR_UNINSTALL_SHORTCUT = "com.android.launcher.permission.UNINSTALL_SHORTCUT";
	public static final String PERMISSION_FOR_LOGS = "android.permission.READ_LOGS";

	public static final List<String> FUNCTIONAL_PERMISSIONS = Arrays.asList(
			PERMISSION_FOR_OUTGOING_CALLS,
			PERMISSION_FOR_READ_PHONE_STATE,
			PERMISSION_FOR_MODIFY_AUDIO_SETTINGS,
			PERMISSION_FOR_RECEIVE_BOOT_COMPLETED
	);

	public static final List<String> SHORTCUT_PERMISSIONS = Arrays.asList(
			PERMISSION_FOR_INSTALL_SHORTCUT,
			PERMISSION_FOR_UNINSTALL_SHORTCUT
	);

	public static final List<String> LOGS_PERMISSIONS = Arrays.asList(PERMISSION_FOR_LOGS);

	public interface ProgressCancelListener {
		public void onProgressCancel();
	}

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = new Messenger(service);
			mBound = true;
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
			mBound = false;
		}
	};

	/**
	 * Called when the application is starting, before any activity, service,
	 * or receiver objects (excluding content providers) have been created.
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		MainApplication.mContext = getApplicationContext();
		MainApplication.isEmulator = String.valueOf(Build.PRODUCT).startsWith("sdk");
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mDeviceManger = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
		mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		mComponentName = new ComponentName(this, AdminPermissionReceiver.class);
		mSdkInt = android.os.Build.VERSION.SDK_INT;
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		initLocale();
		checkKeepScreenLockReceiver();
	}

	/**
	 * Obtain the application context.
	 *
	 * @return The application context.
	 */
	public static Context getAppContext() {
		return mContext;
	}

	/**
	 * Initialize the application locale.
	 */
	public void initLocale() {
		mLocale = getLocaleSharedPreferences();
		Locale.setDefault(mLocale);
		android.content.res.Configuration config = new android.content.res.Configuration();
		config.locale = mLocale;
		MainApplication.mContext.getResources().updateConfiguration(config, MainApplication.mContext.getResources().getDisplayMetrics());
	}

	/**
	 * Get the locale from the shared preference or device default locale.
	 *
	 * @return The locale which should be used on the application.
	 */
	private Locale getLocaleSharedPreferences() {
		Locale locale = Locale.getDefault();
		String language = mSharedPreferences.getString(KEY_LANGUAGE_CODE, "en");
		if (!Utilities.isEmpty(language)) {
			String[] arr = language.split("_");
			try {
				switch (arr.length) {
					case 1:
						locale = new Locale(arr[0]);
						break;
					case 2:
						locale = new Locale(arr[0], arr[1]);
						break;
					case 3:
						locale = new Locale(arr[0], arr[1], arr[2]);
						break;
				}
			} catch (Exception e) {
				Log.e(TAG, "getLocaleSharedPreferences: " + language, e);
			}
		}
		return locale;
	}

	/**
	 * Obtain the application locale.
	 *
	 * @return The locale of the application.
	 */
	public Locale getLocale() {
		return mLocale;
	}

	/**
	 * The user-visible SDK version of the framework.
	 *
	 * @return The user-visible SDK version of the framework
	 */
	public int getSdkInt() {
		return mSdkInt;
	}

	/**
	 * Get the sensor manager.
	 *
	 * @return The sensor manager.
	 */
	public SensorManager getSensorManager() {
		return mSensorManager;
	}

	/**
	 * Check if the application is launched for the first time.
	 *
	 * @return True if is the first time when the application is launched.
	 */
	public boolean isFirstTime() {
		String key = FIRST_TIME;// + getVersionCode();
		boolean result = mSharedPreferences.getBoolean(key, true);
		if (result) {
			saveBooleanValue(key, false);
		}
		return result;
	}

	/**
	 * Generic method used to check if the device is locked.
	 *
	 * @return True if the device is locked.
	 */
	public boolean isDeviceLocked() {
		boolean result;
		if (mSdkInt >= Build.VERSION_CODES.LOLLIPOP_MR1) {
			result = isDeviceLockedAPI22();
		} else { // old API
			result = mKeyguardManager.inKeyguardRestrictedInputMode();
		}
		logD(TAG, "isDeviceLocked: " + result);
		return result;
	}

	/**
	 * Check if the device is locked designed for API 22
	 *
	 * @return True if the device is locked.
	 */
	@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
	public boolean isDeviceLockedAPI22() {
		boolean result = mKeyguardManager.isDeviceLocked() ||
				mKeyguardManager.inKeyguardRestrictedInputMode();
		logD(TAG, "API 22 isDeviceLocked: " + result);
		return result;
	}

	/**
	 * Retrieve the application version code.
	 *
	 * @return The application version code.
	 */
	public int getVersionCode() {
		if (mVersionCode == -1) {
			try {
				mVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
			} catch (PackageManager.NameNotFoundException e) {
			}
		}
		return mVersionCode;
	}

	/**
	 * Retrieve the application version name.
	 *
	 * @return The application version name.
	 */
	public String getVersionName() {
		if (mVersionName == null) {
			try {
				mVersionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			} catch (PackageManager.NameNotFoundException e) {
			}
		}
		return mVersionName;
	}

	/**
	 * Get the Device Policy Manager for enable or disable admin support.
	 *
	 * @return The Device Policy Manager.
	 */
	public DevicePolicyManager getDeviceManger() {
		return mDeviceManger;
	}

	/**
	 * Get the Component Name for enable or disable admin support.
	 *
	 * @return The Component Name.
	 */
	public ComponentName getComponentName() {
		return mComponentName;
	}

	/**
	 * Send a {@link #ERROR} log message and log the exception.
	 *
	 * @param tag Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 */
	public void logE(String tag, String msg) {
		Log.e(tag, msg);
		writeLogFile(System.currentTimeMillis(), "ERROR\t" + tag + "\t" + msg);
	}

	/**
	 * Send a {@link #ERROR} log message and log the exception.
	 *
	 * @param tag Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 * @param tr  An exception to log
	 */
	public void logE(String tag, String msg, Throwable tr) {
		Log.e(tag, msg, tr);
		writeLogFile(System.currentTimeMillis(), "ERROR\t" + tag + "\t" + msg
				+ "\t" + Log.getStackTraceString(tr));
	}

	/**
	 * Send a {@link #DEBUG} log message.
	 *
	 * @param tag Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg The message you would like logged.
	 */
	public void logD(String tag, String msg) {
		Log.d(tag, msg);
		writeLogFile(System.currentTimeMillis(), "DEBUG\t" + tag + "\t" + msg);
	}

	/**
	 * Write the log message to the app log file.
	 *
	 * @param logmessage The log message.
	 */
	private void writeLogFile(long milliseconds, String logmessage) {
		if (checkLogFileThread()) {
			mLogFileThread.addLog(mSimpleDateFormatter.format(new Date(milliseconds))
					+ "\t" + logmessage);
		}
	}

	/**
	 * Check if log file thread exist and create it if not.
	 */
	private boolean checkLogFileThread() {
		if (mLogFileThread == null) {
			try {
				mLogFile = new File(getLogsFolder(), MainApplication.LOG_FILE_NAME);
				mLogFileThread = new LogThread(mLogFile);
				mSimpleDateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",
						mLocale);
				mSimpleDateFormatter.setTimeZone(TimeZone.getDefault());
				new Thread(mLogFileThread).start();
			} catch (Exception e) {
				logE(TAG, "Exception: " + e.getMessage(), e);
			}
		}
		return mLogFileThread != null;
	}

	/**
	 * Get logs folder. If is not defined then is initialized and created.
	 *
	 * @return Logs folder.
	 */
	public File getLogsFolder() {
		if (mLogsFolder == null) {
			mLogsFolder = new File(getCacheDir() + File.separator + MainApplication.LOGS_FOLDER_NAME);
			if (!mLogsFolder.exists()) {
				mLogsFolder.mkdirs();
			}
		}
		return mLogsFolder;
	}

	/**
	 * Obtain the log file.
	 *
	 * @return The log file.
	 */
	public File getLogFile() {
		return mLogFile;
	}

	/**
	 * Remove log file from disk.
	 */
	public void deleteLogFile() {
		if (mLogFile != null && mLogFile.exists()) {
			try {
				mLogFileThread.close();
				while (!mLogFileThread.isClosed()) {
					Thread.sleep(1000);
				}
			} catch (IOException e) {
				Log.e(TAG, "deleteLogFile: " + e.getMessage(), e);
			} catch (InterruptedException e) {
				Log.e(TAG, "deleteLogFile: " + e.getMessage(), e);
			}
			mLogFileThread = null;
			mLogFile.delete();
		}
	}

	/**
	 * Check if the keep screen lock is enabled.
	 *
	 * @return True, if the keep screen lock is enabled.
	 */
	public boolean isEnabledKeepScreenLockService() {
		return mSharedPreferences.getBoolean(KEY_ENABLE_KEEP_SCREEN_LOCK_SERVICE, false);
	}

	/**
	 * Change the keep screen lock enabled state.
	 *
	 * @param flag The flag value to be saved.
	 */
	public void setEnableKeepScreenLockService(boolean flag) {
		saveBooleanValue(KEY_ENABLE_KEEP_SCREEN_LOCK_SERVICE, flag);
	}

	/**
	 * Check if the phone listener is enabled.
	 *
	 * @return True if the phone listener is enabled.
	 */
	public boolean isEnabledPhoneListener() {
		return mSharedPreferences.getBoolean(KEY_ENABLE_PHONE_LISTENER, false);
	}

	/**
	 * Check if the light sensor listener is enabled.
	 *
	 * @return True if the light sensor listener is enabled.
	 */
	public boolean isEnableLightSensorListener() {
		return mSharedPreferences.getBoolean(KEY_ENABLE_LIGHT_SENSOR_LISTENER, false);
	}

	/**
	 * Get the light sensor value used to check if the phone is into a pocket.
	 *
	 * @return The limit light sensor value, default value is 5.0.
	 */
	public float getLimitLightSensorValue() {
		return mSharedPreferences.getFloat(KEY_LIMIT_LIGHT_SENSOR_VALUE, 10.0f);
	}

	/**
	 * Get lock screen delay.
	 *
	 * @return The lock screen delay.
	 */
	public int getLockScreenDelay() {
		return mSharedPreferences.getInt(KEY_LOCK_SCREEN_DELAY, 100);
	}

	/**
	 * Retrieved the last recorded proximity value.
	 *
	 * @return The last recorded proximity value. By default 5.0 mean the sensor is not active.
	 */
	public float getLastProximityValue() {
		return mSharedPreferences.getFloat(KEY_LAST_PROXIMITY_VALUE, 5.0f);
	}

	/**
	 * Save the last proximity value.
	 *
	 * @param value The float value to be saved.
	 */
	public void setLastProximityValue(float value) {
		saveFloatValue(KEY_LAST_PROXIMITY_VALUE, value);
	}

	/**
	 * Retrieved the last recorded light sensor value.
	 *
	 * @return The last recorded light value value. By default -1.0 mean the sensor is not active.
	 */
	public float getLastLightValue() {
		return mSharedPreferences.getFloat(KEY_LAST_LIGHT_VALUE, -1.0f);
	}

	/**
	 * Save the last light sensor value.
	 *
	 * @param value The float value to be saved.
	 */
	public void setLastLightValue(float value) {
		saveFloatValue(KEY_LAST_LIGHT_VALUE, value);
	}

	/**
	 * Save current screen locked state, true if the screen is locked, otherwise false.
	 *
	 * @param flag         True or false state of the screen lock.
	 * @param allowDismiss Flag used to create a dismissible notification.
	 */
	public void setScreenLockedFlag(boolean flag, boolean allowDismiss) {
		logD(TAG, "setScreenLockedFlag(" + flag + ")");
		saveBooleanValue(KEY_SCREEN_LOCKED_FLAG, flag);
		if (flag) {
			showLockScreenNotification(allowDismiss);
		}
	}

	/**
	 * Return screen lock state.
	 *
	 * @return True, if currently the screen is locked.
	 */
	public boolean isScreenLocked() {
		return mSharedPreferences.getBoolean(KEY_SCREEN_LOCKED_FLAG, false);
	}

	/**
	 * Retrieved the last recorded action.
	 *
	 * @return The last recorded action.
	 */
	public String getLastAction() {
		return mSharedPreferences.getString(KEY_LAST_ACTION, "null");
	}

	/**
	 * Store as a last action the provided action name.
	 *
	 * @param action The last action to be stored.
	 */
	public void setLastAction(String action) {
		saveStringValue(KEY_LAST_ACTION, action);
	}

	/**
	 * Set current phone status, true if the phone is active.
	 *
	 * @param flag True, if the phone is active.
	 */
	public void setPhoneActive(boolean flag) {
		saveBooleanValue(KEY_PHONE_ACTIVE, flag);
	}

	/**
	 * Check if the phone is active.
	 *
	 * @return True, if the phone is active.
	 */
	public boolean isPhoneActive() {
		return isEnabledPhoneListener() && mSharedPreferences.getBoolean(KEY_PHONE_ACTIVE, false);
	}

	/**
	 * Check if the enable when headset option is enabled.
	 *
	 * @return True if the option is enabled.
	 */
	public boolean isEnableWhenHeadset() {
		return mSharedPreferences.getBoolean(KEY_ENABLE_WHEN_HEADSET, false);
	}

	/**
	 * Check if an headset or a bluetooth device is connected.
	 *
	 * @return True if an headset or a bluetooth device is connected.
	 */
	public boolean isHeadsetConnected() {
		return mAudioManager.isWiredHeadsetOn() ||
				mAudioManager.isBluetoothA2dpOn() ||
				mAudioManager.isBluetoothScoOn();
	}

	/**
	 * Store a string value on the shared preferences.
	 *
	 * @param key   The shared preference key.
	 * @param value The string value to be saved.
	 */
	private void saveStringValue(String key, String value) {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}

	/**
	 * Store a float value on the shared preferences.
	 *
	 * @param key   The shared preference key.
	 * @param value The float value to be saved.
	 */
	private void saveFloatValue(String key, float value) {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putFloat(key, value);
		editor.commit();
	}

	/**
	 * Store an integer value on the shared preferences.
	 *
	 * @param key   The shared preference key.
	 * @param value The integer value to be saved.
	 */
	private void saveIntegerValue(String key, int value) {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putInt(key, value);
		editor.commit();
	}

	/**
	 * Store a boolean value on the shared preferences.
	 *
	 * @param key   The shared preference key.
	 * @param value The boolean value to be saved.
	 */
	private void saveBooleanValue(String key, boolean value) {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}

	/**
	 * Remove a shared preference.
	 *
	 * @param key The key of the shared preference to be removed.
	 */
	private void removeSharedPreference(String key) {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.remove(key);
		editor.commit();
	}

	/**
	 * Check if the screen lock shortcut is created on home screen.
	 *
	 * @return True if the shortcut for screen lock is created on home
	 * screen.
	 */
	public boolean isScreenLockShortcutCreated() {
		return mSharedPreferences.getBoolean(KEY_SCREEN_LOCK_SHORTCUT_CREATED, false);
	}

	/**
	 * Set the boolean value of created shortcut for screen lock.
	 *
	 * @param flag True or False.
	 */
	public void setScreenLockShortcutCreated(boolean flag) {
		saveBooleanValue(KEY_SCREEN_LOCK_SHORTCUT_CREATED, flag);
	}

	/**
	 * Set the rename shortcut update listener.
	 *
	 * @param listener The rename shortcut update listener.
	 */
	public void updateShortcutUpdateListener(
			ScreenLockShortcutUpdateListener listener) {
		this.mShortcutUpdateListener = listener;
	}

	/**
	 * Get the rename shortcut update listener.
	 *
	 * @return The rename shortcut update listener.
	 */
	public ScreenLockShortcutUpdateListener getShortcutUpdateListener() {
		return mShortcutUpdateListener;
	}

	/**
	 * Method invoked by the shortcut broadcast.
	 *
	 * @param data Intent data from the shortcut broadcast.
	 * @param type Type of the event, uninstall or install.
	 */
	public void updateScreenShortcutListener(Intent data, ScreenLockShortcutUpdateListener.TYPE type) {
		Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
		String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
		if (intent != null && name != null && intent.getComponent() != null) {
			String cls = String.valueOf(intent.getComponent().getClassName());
			if (cls.indexOf("keepscreenlock") > 0) {
				updateScreenShortcutPref(type);
			}
		}
	}

	/**
	 * Update the preferences related with the shortcut.
	 *
	 * @param type Type of the event, uninstall or install.
	 */
	private void updateScreenShortcutPref(ScreenLockShortcutUpdateListener.TYPE type) {
		ScreenLockShortcutUpdateListener listener = getShortcutUpdateListener();
		boolean update = false;
		if (ScreenLockShortcutUpdateListener.TYPE.INSTALL == type) {
			setScreenLockShortcutCreated(true);
			update = true;
		} else if (ScreenLockShortcutUpdateListener.TYPE.UNINSTALL == type) {
			setScreenLockShortcutCreated(false);
			update = true;
		}
		if (listener != null && update) {
			listener.updateScreenLockShortcut();
		}
	}

	/**
	 * Check if the keep screen lock receiver should be enabled or disabled.
	 */
	public void checkKeepScreenLockReceiver() {
		if (isEnabledKeepScreenLockService()) {
			registerKeepScreenLockService();
			enableScreenActionsReceiver(true);
			enablePhoneCallReceiver(isEnabledPhoneListener());
		} else {
			unregisterKeepScreenLockService();
			enableScreenActionsReceiver(false);
			enablePhoneCallReceiver(false);
		}
	}

	/**
	 * Enable or disable the screen actions broadcast receiver.
	 *
	 * @param flag True if the broadcast receiver should be enabled.
	 */
	public void enableScreenActionsReceiver(boolean flag) {
		sendToKeepScreenLockService(
				KeepScreenLockService.ENABLE_SCREEN_ACTIONS_RECEIVER,
				String.valueOf(flag));
	}

	/**
	 * Enable or disable the phone call broadcast receiver.
	 *
	 * @param flag True if the broadcast receiver should be enabled.
	 */
	public void enablePhoneCallReceiver(boolean flag) {
		sendToKeepScreenLockService(
				KeepScreenLockService.ENABLE_PHONE_CALL_RECEIVER,
				String.valueOf(flag));
	}

	/**
	 * Method used to start the service used to keep screen locked.
	 */
	public void registerKeepScreenLockService() {
		logD(TAG, "registerKeepScreenLockService - startService");
		try {
			if (mKeepScreenLockServiceIntent == null) {
				mKeepScreenLockServiceIntent = new Intent(this, KeepScreenLockService.class);
				startService(mKeepScreenLockServiceIntent);
			}
			this.bindService(mKeepScreenLockServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
		} catch (Exception e) {
			logE(TAG, "registerKeepScreenLockService: " + e.getMessage(), e);
		}
	}

	/**
	 * Method used to unregister the service for keeping screen locked.
	 */
	private void unregisterKeepScreenLockService() {
		logD(TAG, "unregisterKeepScreenLockService - stopService");
		try {
			if (mBound) {
				this.unbindService(mConnection);
				mBound = false;
			}
		} catch (Exception e) {
			logE(TAG, "unregisterKeepScreenLockService: " + e.getMessage(), e);
		}
	}

	/**
	 * Register all listeners.
	 */
	public void registerListeners() {
		registerProximityListener();
		registerLightSensorListener();
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
	private void registerProximityListener() {
		if (mProximityListener == null) {
			this.setLastProximityValue(MainApplication.PROXIMITY_FAR_VALUE);
			mProximityListener = new ProximityListener(this);
			mProximityListener.registerProximityListener();
		}
	}

	/**
	 * Register the light sensor listener used to handle the light sensor events.
	 */
	private void registerLightSensorListener() {
		if (mLightSensorListener == null) {
			if (isEnableLightSensorListener()) {
				setLastLightValue(MainApplication.LIGHT_VALUE_INVALID);
				mLightSensorListener = new LightSensorListener(this);
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

	/**
	 * Send a message to service.
	 */
	public void sendToKeepScreenLockService(String messageKey, String messageValue) {
		if (mBound) {
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putString(messageKey, messageValue);
			msg.setData(bundle);
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				logE(TAG,
						"sendMessageToService(" + messageKey + "," + messageValue + "): " +
								e.getMessage(), e);
			}
		}
	}

	/**
	 * Call for the screen lock.
	 */
	public void callKeepScreenLockTask() {
		MainApplication.isScreenLockRequested = true;
		if (!MainApplication.isKeepScreenLockTaskRunning) {
			MainApplication.isKeepScreenLockTaskRunning = true;
			new KeepScreenLockTask(this).execute();
		}
	}

	/**
	 * Check if the application have admin privileges.
	 *
	 * @return True if the application have admin privileges.
	 */
	public boolean isAdminActive() {
		return mDeviceManger.isAdminActive(mComponentName);
	}

	/**
	 * Execute screen lock command.
	 */
	public void executeLockScreen(boolean increaseCounter) {
		if (isAdminActive()) {
			logD(TAG, "Lock the screen now!");
			saveBooleanValue(KEY_SCREEN_LOCKED_FLAG, true);
			if (increaseCounter) {
				increaseKeepScreenLockCounter(1);
			}
			if (!MainApplication.isEmulator) {
				mDeviceManger.lockNow();
			}
		} else {
			logD(TAG, "No admin privileges to lock the screen.");
		}
	}

	/**
	 * Obtain the number of renamed files.
	 *
	 * @return Number of renamed files.
	 */
	public int getKeepScreenLockCounter() {
		return mSharedPreferences.getInt(KEY_KEEP_SCREEN_LOCK_COUNTER, 0);
	}

	/**
	 * Increase the rename file counter.
	 *
	 * @param value The integer value to be increased the rename file counter.
	 */
	public void increaseKeepScreenLockCounter(int value) {
		if (value == -1) {
			removeSharedPreference(KEY_KEEP_SCREEN_LOCK_COUNTER);
			removeSharedPreference(KEY_KEEP_SCREEN_LOCK_LOGS);
		} else {
			int oldValue = getKeepScreenLockCounter();
			saveIntegerValue(KEY_KEEP_SCREEN_LOCK_COUNTER, oldValue + value);
			recordToKeepScreenLockLogs();
		}
	}

	/**
	 * Obtain the times when the device was locked.
	 *
	 * @return The string with the times when the device was locked.
	 */
	public String getKeepScreenLockLogs() {
		return mSharedPreferences.getString(KEY_KEEP_SCREEN_LOCK_LOGS, "");
	}

	/**
	 * Save the current timestamp to record when the device was locked.
	 */
	private void recordToKeepScreenLockLogs() {
		String value = getKeepScreenLockLogs();
		value += (value.length() == 0 ? "" : ",") + System.currentTimeMillis();
		saveStringValue(KEY_KEEP_SCREEN_LOCK_LOGS, value);
	}

	/**
	 * Validate the screen lock logs DateTimeFormat.
	 */
	public void checkScreenLockLogsDateTimeFormat() {
		String defaultFormatPattern = MainApplication.getAppContext().
				getString(R.string.locked_screen_log_format);
		String formatPattern = mSharedPreferences.getString(KEY_SCREEN_LOCK_LOGS_DATETIMEFORMAT,
				defaultFormatPattern);
		try {
			mDateFormat = new SimpleDateFormat(formatPattern, mLocale);
		} catch (Exception e) {
			mDateFormat = new SimpleDateFormat(defaultFormatPattern, mLocale);
			saveStringValue(KEY_SCREEN_LOCK_LOGS_DATETIMEFORMAT, defaultFormatPattern);
		}
	}

	/**
	 * Get formatted date time.
	 *
	 * @param dateTimeTimestamp Date time timestamp.
	 * @return Formatted date time.
	 */
	public String getFormattedDateTime(String dateTimeTimestamp) {
		String result = null;
		try {
			if (mDateFormat == null) {
				checkScreenLockLogsDateTimeFormat();
			}
			long timestamp = Long.parseLong(dateTimeTimestamp);
			result = mDateFormat.format(new Date(timestamp));
		} catch (NumberFormatException e) {
			// ignored
		}
		return result;
	}

	/**
	 * This will show a progress dialog using a context and the message to be
	 * showed on the progress dialog.
	 *
	 * @param listener The listener class which should listen for cancel.
	 * @param context  The context where should be displayed the progress dialog.
	 * @param message  The message displayed inside of progress dialog.
	 */
	public void showProgressDialog(final ProgressCancelListener listener,
								   Context context, String message, int max) {
		hideProgressDialog();
		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.setTitle(R.string.please_wait);
		mProgressDialog.setMessage(message);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
				MainApplication.getAppContext().getString(R.string.cancel),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (listener != null) {
							listener.onProgressCancel();
						}
					}
				});
		if (max > 0) {
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setMax(max);
		}
		if (!mProgressDialog.isShowing()) {
			mProgressDialog.show();
		}
	}

	/**
	 * Hide the progress dialog.
	 */
	public void hideProgressDialog() {
		if (mProgressDialog != null) {
			try {
				if (mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}
			} catch (Exception e) {
				logE(TAG, "hideProgressDialog: " + e.getMessage(), e);
			} finally {
				mProgressDialog = null;
			}
		}
	}

	/**
	 * Write the shared preferences to provided writer.
	 *
	 * @param writer The writer used to write the shared preferences.
	 */
	public void writeSharedPreferences(Writer writer) {
		Map<String, ?> allEntries = mSharedPreferences.getAll();
		try {
			for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
				writer.write(entry.getKey());
				writer.write(": \"");
				writer.write(String.valueOf(entry.getValue()));
				writer.write("\"");
				writer.write('\n');
			}
		} catch (IOException e) {
			logE(TAG, "writeSharedPreferences: " + e.getMessage(), e);
		}
	}

	/**
	 * Check if the lock screen notification is enabled.
	 *
	 * @return True if the lock screen notification is enabled.
	 */
	public boolean isLockScreenNotificationEnabled() {
		return mSharedPreferences.getBoolean(KEY_NOTIFICATION_ENABLED, true);
	}

	/**
	 * Check if the notification should be always dismissible.
	 *
	 * @return True, if the notification should be dismissible always.
	 */
	public boolean isNotificationAlwaysDismissible() {
		return mSharedPreferences.getBoolean(KEY_NOTIFICATION_ALWAYS_DISMISSIBLE, false);
	}

	/**
	 * Get the notification manager.
	 *
	 * @return The notification manager.
	 */
	private NotificationManager getNotificationManager() {
		if (mNotificationManager == null) {
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		}
		return mNotificationManager;
	}

	/**
	 * Method used to hide the notification used to lock screen.
	 */
	public void hideLockScreenNotification() {
		getNotificationManager().cancel(NOTIFICATION_ID);
	}

	/**
	 * Method used to prepare and show the lock screen notification message.
	 *
	 * @param allowDismiss Flag used to create a dismissible notification.
	 */
	public void showLockScreenNotification(boolean allowDismiss) {
		if (isLockScreenNotificationEnabled()) {
			getNotificationManager().notify(NOTIFICATION_ID, getNotification(allowDismiss));
		}
	}

	/**
	 * Obtain the screen lock intent.
	 *
	 * @return The screen lock intent.
	 */
	public Intent getNotificationScreenLockIntent() {
		Intent screenLockIntent = new Intent(this, ScreenLockReceiver.class);
		screenLockIntent.setAction(ScreenLockReceiver.LOCK_SCREEN);
		return screenLockIntent;
	}

	/**
	 * Build the notification item.
	 *
	 * @param allowDismiss Flag used to create a dismissible notification.
	 * @return The notification item.
	 */
	private Notification getNotification(boolean allowDismiss) {
		if (mNotification == null) {
			PendingIntent pendingScreenLockIntent = PendingIntent.getBroadcast(this, 0,
					getNotificationScreenLockIntent(), PendingIntent.FLAG_UPDATE_CURRENT);

			RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
			remoteViews.setOnClickPendingIntent(R.id.lockScreenView, pendingScreenLockIntent);

			NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this);
			notifBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
			notifBuilder.setSmallIcon(R.mipmap.ic_launcher_red);
			notifBuilder.setContent(remoteViews);

			notifBuilder.setContentTitle(getString(R.string.app_name));
			notifBuilder.setContentText(getString(R.string.notification_text));

			mNotification = notifBuilder.build();
		}
		if (isNotificationAlwaysDismissible() || allowDismiss) {
			mNotification.flags &= ~Notification.FLAG_NO_CLEAR;
		} else {
			mNotification.flags |= Notification.FLAG_NO_CLEAR;
		}
		return mNotification;
	}

	/**
	 * Update the lock screen notification state.
	 */
	public void updateLockScreenNotificationState() {
		if (!isLockScreenNotificationEnabled()) {
			hideLockScreenNotification();
		}
	}

	/**
	 * Check for pro version.
	 *
	 * @return True if pro version exist.
	 */
	public boolean isProPresent() {
		PackageManager pm = getPackageManager();
		boolean success = false;
		try {
			success = (PackageManager.SIGNATURE_MATCH == pm.checkSignatures(
					this.getPackageName(), "ro.ciubex.keepscreenlockpro"));
			logD(TAG, "isProPresent: " + success);
		} catch (Exception e) {
			logE(TAG, "isProPresent: " + e.getMessage(), e);
		}
		return success;
	}

	/**
	 * Check if should be asked for permissions.
	 *
	 * @return True if should be asked for permissions.
	 */
	public boolean shouldAskPermissions() {
		return mSdkInt > 22;
	}

	/**
	 * Check if the permissions were asked.
	 *
	 * @return True if the permissions were asked.
	 */
	public boolean havePermissionsAsked() {
		return mSharedPreferences.getBoolean(KEY_HAVE_PERMISSIONS_ASKED, false);
	}

	/**
	 * Set the permission asked flag to true.
	 */
	public void markPermissionsAsked() {
		saveBooleanValue(KEY_HAVE_PERMISSIONS_ASKED, true);
	}

	/**
	 * Check if a permission was asked.
	 *
	 * @param permission The permission to be asked.
	 * @return True if the permission was asked before.
	 */
	public boolean isPermissionAsked(String permission) {
		return mSharedPreferences.getBoolean(permission, false);
	}

	/**
	 * Mark a permission as asked.
	 *
	 * @param permission Permission to be marked as asked.
	 */
	public void markPermissionAsked(String permission) {
		saveBooleanValue(permission, true);
	}

	/**
	 * Remove the permission asked flag.
	 *
	 * @param permission The permission for which will be removed the asked flag.
	 */
	public void removePermissionAskedMark(String permission) {
		removeSharedPreference(permission);
	}

	/**
	 * Check if a permission is allowed.
	 *
	 * @param permission The permission to be checked.
	 * @return True if the permission is allowed.
	 */
	public boolean hasPermission(String permission) {
		if (shouldAskPermissions()) {
			return hasPermission23(permission);
		}
		return true;
	}

	/**
	 * Check if a permission is allowed. (API 23)
	 *
	 * @param permission The permission to be checked.
	 * @return True if the permission is allowed.
	 */
	@TargetApi(23)
	private boolean hasPermission23(String permission) {
		return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
	}

	/**
	 * Check if the application have functional permissions.
	 *
	 * @return True if all functional permissions are allowed.
	 */
	public boolean haveFunctionalPermissions() {
		for (String permission : MainApplication.FUNCTIONAL_PERMISSIONS) {
			if (!hasPermission(permission)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if the application have shortcut permissions.
	 *
	 * @return True if all shortcut permissions are allowed.
	 */
	public boolean haveShortcutPermissions() {
		for (String permission : MainApplication.SHORTCUT_PERMISSIONS) {
			if (!hasPermission(permission)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if the application have logs permissions.
	 *
	 * @return True if all logs permissions are allowed.
	 */
	public boolean haveLogsPermissions() {
		for (String permission : MainApplication.LOGS_PERMISSIONS) {
			if (!hasPermission(permission)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Get all not granted permissions.
	 */
	public String[] getNotGrantedPermissions() {
		List<String> permissions = new ArrayList<>();
		buildRequiredPermissions(permissions, MainApplication.FUNCTIONAL_PERMISSIONS, true);
		buildRequiredPermissions(permissions, MainApplication.SHORTCUT_PERMISSIONS, true);
//		buildRequiredPermissions(permissions, MainApplication.LOGS_PERMISSIONS, true);
		String[] array = null;
		if (!permissions.isEmpty()) {
			array = new String[permissions.size()];
			array = permissions.toArray(array);
		}
		return array;
	}

	/**
	 * Get an array with all required permissions.
	 *
	 * @return Array with permissions to be requested.
	 */
	public String[] getAllRequiredPermissions() {
		List<String> permissions = new ArrayList<>();
		buildRequiredPermissions(permissions, MainApplication.FUNCTIONAL_PERMISSIONS, false);
		buildRequiredPermissions(permissions, MainApplication.SHORTCUT_PERMISSIONS, false);
//		buildRequiredPermissions(permissions, DSCApplication.LOGS_PERMISSIONS, false);
		String[] array = null;
		if (!permissions.isEmpty()) {
			array = new String[permissions.size()];
			array = permissions.toArray(array);
		}
		return array;
	}

	/**
	 * Put on the permissions all required permissions which is missing and was not asked.
	 *
	 * @param permissions         List of permissions to be requested.
	 * @param requiredPermissions List with all required permissions to be checked.
	 */
	private void buildRequiredPermissions(List<String> permissions, List<String> requiredPermissions, boolean force) {
		for (String permission : requiredPermissions) {
			if ((force && !hasPermission(permission)) ||
					(!isPermissionAsked(permission) && !hasPermission(permission))) {
				permissions.add(permission);
			}
		}
	}
}
