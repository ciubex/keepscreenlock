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
package ro.ciubex.keepscreenlock.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ro.ciubex.keepscreenlock.MainApplication;
import ro.ciubex.keepscreenlock.R;
import ro.ciubex.keepscreenlock.preference.FloatEditTextPreference;
import ro.ciubex.keepscreenlock.preference.IntegerEditTextPreference;
import ro.ciubex.keepscreenlock.provider.CachedFileProvider;
import ro.ciubex.keepscreenlock.receiver.LightSensorListener;
import ro.ciubex.keepscreenlock.receiver.ProximityListener;
import ro.ciubex.keepscreenlock.receiver.ScreenLockShortcutUpdateListener;
import ro.ciubex.keepscreenlock.util.Devices;
import ro.ciubex.keepscreenlock.util.Utilities;

/**
 * This is the main activity class for the Keep Screen Lock application.
 *
 * @author Claudiu Ciobotariu
 */
public class SettingsActivity extends PreferenceActivity implements
		SharedPreferences.OnSharedPreferenceChangeListener,
		MainApplication.ProgressCancelListener,
		ProximityListener.UIProximityUpdateListener,
		LightSensorListener.UILightSensorUpdateListener,
		ScreenLockShortcutUpdateListener {
	private static final String TAG = SettingsActivity.class.getName();
	private MainApplication mApplication;

	private CheckBoxPreference mEnableKeepScreenLockService;
	private IntegerEditTextPreference mLockScreenDelay;
	private Preference mProximitySensorState;
	private CheckBoxPreference mEnableLightSensorListener;
	private FloatEditTextPreference mLimitLightSensorValue;
	private Preference mToggleNotification;
	private PreferenceCategory mOtherSettings;
	private Preference mToggleScreenLockShortcut;
	private Preference mRequestPermissions;
	private Preference mKeepScreenLockCounter;
	private Preference mBuildVersion;
	private Preference mSendDebugReport;
	private Preference mLicensePref;
	private Preference mDonatePref;

	private static final int RESULT_ENABLE = 1;
	private static final int REQUEST_SEND_REPORT = 2;
	private static final int PERMISSIONS_REQUEST_CODE = 44;

	private static final int ID_CONFIRMATION_ALERT = -1;
	private static final int ID_CONFIRMATION_DONATION = 0;
	private static final int ID_CONFIRMATION_LOCKED_SCREEN_COUNTER = 1;
	private static final int ID_CONFIRMATION_DEBUG_REPORT = 2;
	private static final int ID_CONFIRMATION_SHOW_KEEP_LOGS = 3;
	private static final int ID_CONFIRMATION_REQUEST_PERMISSIONS = 4;
	private static final int ID_ALERT_NO_MESSAGE = -1;

	private static final int BUFFER = 1024;
	private ProximityListener mProximityListener;
	private LightSensorListener mLightSensorListener;
	private float mLastProximityValue;
	private float mLastLightValue;
	private ArrayAdapter<String> mHistoryEventsItems;

	/**
	 * Called when the activity is starting.
	 *
	 * @param savedInstanceState
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		mApplication = (MainApplication) getApplication();
		mProximityListener = new ProximityListener(mApplication, this);
		mLightSensorListener = new LightSensorListener(mApplication, this);
		initPreferences();
		initCommands();
		initPreferencesByPermissions();
		checkProVersion();
	}

	/**
	 * Initialize preferences controls.
	 */
	private void initPreferences() {
		mEnableKeepScreenLockService = (CheckBoxPreference) findPreference(MainApplication.KEY_ENABLE_KEEP_SCREEN_LOCK_SERVICE);
		mLockScreenDelay = (IntegerEditTextPreference) findPreference(MainApplication.KEY_LOCK_SCREEN_DELAY);
		mProximitySensorState = findPreference("proximitySensorState");
		mEnableLightSensorListener = (CheckBoxPreference) findPreference(MainApplication.KEY_ENABLE_LIGHT_SENSOR_LISTENER);
		mLimitLightSensorValue = (FloatEditTextPreference) findPreference(MainApplication.KEY_LIMIT_LIGHT_SENSOR_VALUE);
		mToggleNotification = (Preference) findPreference(MainApplication.KEY_TOGGLE_NOTIFICATION);
		mToggleScreenLockShortcut = (Preference) findPreference("toggleScreenLockShortcut");
		mRequestPermissions = (Preference) findPreference("requestPermissions");
		mKeepScreenLockCounter = findPreference(MainApplication.KEY_KEEP_SCREEN_LOCK_COUNTER);
		mBuildVersion = findPreference("buildVersion");
		mSendDebugReport = findPreference("sendDebugReport");
		mLicensePref = (Preference) findPreference("licensePref");
		mDonatePref = findPreference("donatePref");
		mOtherSettings = (PreferenceCategory) findPreference("otherSettings");
	}

	/**
	 * Initialize the preference commands.
	 */
	private void initCommands() {
		mToggleNotification.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				onToggleNotification();
				return true;
			}
		});
		mToggleScreenLockShortcut
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						onToggleRenameShortcut();
						return true;
					}
				});
		mRequestPermissions.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				onRequestPermissions();
				return true;
			}
		});
		mKeepScreenLockCounter.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				onKeepScreenLockCounter();
				return true;
			}
		});
		mBuildVersion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				onBuildVersion();
				return true;
			}
		});
		mSendDebugReport.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				onSendDebugReport();
				return true;
			}
		});
		mLicensePref
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						onLicensePref();
						return true;
					}
				});
		mDonatePref
				.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						onDonatePref();
						return true;
					}
				});
	}

	/**
	 * Remove the permission request preference if should not be asked for permissions.
	 */
	private void initPreferencesByPermissions() {
		if (!mApplication.shouldAskPermissions()) {
			mOtherSettings.removePreference(mRequestPermissions);
		}
	}

	/**
	 * Check if the pro version is present to update the donation preference item.
	 */
	private void checkProVersion() {
		if (mApplication.isProPresent()) {
			mDonatePref.setEnabled(false);
			mDonatePref.setTitle(R.string.thank_you_title);
			mDonatePref.setSummary(R.string.thank_you_desc);
		}
	}

	/**
	 * Prepare all informations when the activity is resuming
	 */
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
		mLastProximityValue = mApplication.getLastProximityValue();
		mLastLightValue = mApplication.getLastLightValue();
		mProximityListener.registerProximityListener();
		mLightSensorListener.registerLightSensorListener();
		mApplication.updateShortcutUpdateListener(this);
		prepareProximitySensorListenerSummary();
		prepareLightSensorListenerSummary();
		prepareSummaries();
		updateShortcutFields();
		checkForFirstTime();
		checkForPermissions();
	}

	/**
	 * Method used to check for application permissions.
	 */
	@TargetApi(23)
	private void checkForPermissions() {
		if (mApplication.shouldAskPermissions()) {
			updateSettingsOptionsByPermissions();
			if (!mApplication.havePermissionsAsked()) {
				requestForPermissions(mApplication.getAllRequiredPermissions());
			}
		}
	}

	/**
	 * Method used to request for application required permissions.
	 */
	@TargetApi(23)
	private void requestForPermissions(String[] permissions) {
		if (!Utilities.isEmpty(permissions)) {
			requestPermissions(permissions, PERMISSIONS_REQUEST_CODE);
		}
	}

	/**
	 * Check if this is first time when the application was launched.
	 */
	private void checkForFirstTime() {
		if (mApplication.isFirstTime()) {
			showCustomAlertDialog(R.string.app_name,
					R.string.startup_message,
					false,
					android.R.drawable.ic_dialog_info,
					ID_CONFIRMATION_ALERT,
					true,
					null);
		}
	}

	/**
	 * Called as part of the activity lifecycle when an activity is going into
	 * the background, but has not (yet) been killed.  The counterpart to
	 * {@link #onResume}.
	 */
	@Override
	protected void onPause() {
		mProximityListener.unregisterProximityListener();
		mLightSensorListener.unregisterLightSensorListener();
		mApplication.updateShortcutUpdateListener(null);
		mHistoryEventsItems = null;
		super.onPause();
	}

	/**
	 * Invoked when the progress dialog is dismissed.
	 */
	@Override
	public void onProgressCancel() {

	}

	@Override
	public void proximitySensorUpdate(float value) {
		mLastProximityValue = value;
		prepareProximitySensorListenerSummary();
	}

	@Override
	public void lightSensorUpdate(float value) {
		mLastLightValue = value;
		prepareLightSensorListenerSummary();
	}

	/**
	 * Prepare preferences summaries
	 */
	private void prepareSummaries() {
		prepareLockScreenDelaySummary();
		mEnableKeepScreenLockService.setChecked(mApplication.isEnabledKeepScreenLockService());
		mKeepScreenLockCounter.setTitle(MainApplication.getAppContext().getString(
				R.string.locked_screen_count_title,
				mApplication.getKeepScreenLockCounter()));
		mLimitLightSensorValue.setSummary(MainApplication.getAppContext().getString(
				R.string.limit_light_sensor_value_desc,
				mApplication.getLimitLightSensorValue()));
		mBuildVersion.setSummary(mApplication.getVersionName());
	}

	/**
	 * Prepare the lock screen delay preference summary.
	 */
	private void prepareLockScreenDelaySummary() {
		int stringId;
		float delay = mApplication.getLockScreenDelay();
		if (delay == 1000) {
			stringId = R.string.lock_screen_delay_desc_1sec;
			delay = 1;
		} else if (delay < 1000) {
			stringId = R.string.lock_screen_delay_desc_msec;
		} else {
			stringId = R.string.lock_screen_delay_desc_sec;
			delay = delay / 1000;
		}
		mLockScreenDelay.setSummary(MainApplication.getAppContext().getString(stringId, delay));
	}

	/**
	 * Prepare the proximity sensor listener summary.
	 */
	private void prepareProximitySensorListenerSummary() {
		String title, summary;
		if (mLastProximityValue < MainApplication.PROXIMITY_FAR_VALUE) {
			title = MainApplication.getAppContext().getString(R.string.proximity_sensor_state_active);
		} else {
			title = MainApplication.getAppContext().getString(R.string.proximity_sensor_state_inactive);
		}
		title = MainApplication.getAppContext().getString(R.string.proximity_sensor_state_title, title);
		summary = MainApplication.getAppContext().getString(R.string.proximity_sensor_state_desc, mLastProximityValue);
		mProximitySensorState.setTitle(title);
		mProximitySensorState.setSummary(summary);
	}

	/**
	 * Prepare the light sensor listener checkbox summary.
	 */
	private void prepareLightSensorListenerSummary() {
		String summary;
		if (mLastLightValue >= 0) {
			summary = MainApplication.getAppContext().getString(
					R.string.enable_light_sensor_listener_desc,
					mLastLightValue);
		} else {
			summary = MainApplication.getAppContext().getString(
					R.string.enable_light_sensor_listener_desc_no_value);
		}
		mEnableLightSensorListener.setSummary(summary);
	}

	/**
	 * This method is invoked when a preference is changed
	 *
	 * @param sharedPreferences The shared preference
	 * @param key               Key of changed preference
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
										  String key) {
		if (MainApplication.KEY_ENABLE_KEEP_SCREEN_LOCK_SERVICE.equals(key)) {
			mApplication.checkKeepScreenLockReceiver();
			mApplication.enableScreenActionsReceiver(mApplication.isEnabledKeepScreenLockService());
			if (mApplication.isEnabledKeepScreenLockService()) {
				enableAdminPrivileges();
			} else {
				removeAdminPrivileges();
			}
		} else if (MainApplication.KEY_ENABLE_PHONE_LISTENER.equals(key)) {
			mApplication.enablePhoneCallReceiver(mApplication.isEnabledPhoneListener());
		} else if (MainApplication.KEY_NOTIFICATION_ENABLED.equals(key)) {
			mApplication.updateLockScreenNotificationState();
		} else if (MainApplication.KEY_SCREEN_LOCK_LOGS_DATETIMEFORMAT.equals(key)) {
			mApplication.checkScreenLockLogsDateTimeFormat();
			mHistoryEventsItems = null;
		}
		prepareSummaries();
	}

	/**
	 * Check to be enabled admin privileges.
	 */
	public void enableAdminPrivileges() {
		boolean active = mApplication.getDeviceManger().isAdminActive(mApplication.getComponentName());
		if (!active) {
			Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mApplication.getComponentName());
			intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
					MainApplication.getAppContext().getString(R.string.reason_admin_privileges));
			startActivityForResult(intent, RESULT_ENABLE);
		}
	}

	/**
	 * Remove admin privileges.
	 */
	public void removeAdminPrivileges() {
		boolean active = mApplication.getDeviceManger().isAdminActive(mApplication.getComponentName());
		if (active) {
			mApplication.getDeviceManger().removeActiveAdmin(mApplication.getComponentName());
		}
	}

	/**
	 * Called when an activity you launched exits, giving you the requestCode
	 * you started it with, the resultCode it returned, and any additional
	 * data from it.
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_SEND_REPORT: /* mApplication.deleteLogFile(); */
				break;
			case RESULT_ENABLE:
				if (Activity.RESULT_OK == resultCode) {
					mApplication.logD(TAG, "Keep Screen Lock: Admin enabled!");
				} else {
					mApplication.logD(TAG, "Keep Screen Lock: Admin enable FAILED!");
				}
				return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Toggle the notification used to lock screen.
	 */
	private void onToggleNotification() {
		mApplication.hideLockScreenNotification();
	}

	/**
	 * Invoked when the user click on the "Create rename service shortcut"
	 * preference.
	 */
	private void onToggleRenameShortcut() {
		boolean isCreated = mApplication.isScreenLockShortcutCreated();
		createOrRemoveScreenLockShortcut(!isCreated);
	}

	@Override
	public void updateScreenLockShortcut() {
		updateShortcutFields();
	}

	/**
	 * Create the screen lock shortcut intent.
	 *
	 * @return The screen lock shortcut intent.
	 */
	private Intent getActivityIntent() {
		Intent activityIntent = new Intent(MainApplication.getAppContext(), ScreenLockActivity.class);
		activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		activityIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		return activityIntent;
	}

	/**
	 * Create or remove rename shortcut from the home screen.
	 *
	 * @param create True if the shortcut should be created.
	 */
	private void createOrRemoveScreenLockShortcut(boolean create) {
		String action = create ? ScreenLockShortcutUpdateListener.INSTALL_SHORTCUT
				: ScreenLockShortcutUpdateListener.UNINSTALL_SHORTCUT;

		Intent shortcutIntent = new Intent();
		shortcutIntent.setAction(action);
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, getActivityIntent());
		shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
				MainApplication.getAppContext().getString(R.string.screen_lock_shortcut_name));
		shortcutIntent.putExtra("duplicate", false);
		if (create) {
			shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
					Intent.ShortcutIconResource.fromContext(
							getApplicationContext(),
							R.mipmap.ic_launcher_red));
		}
		MainApplication.getAppContext().sendBroadcast(shortcutIntent);
	}

	/**
	 * Update screen lock shortcut fields, descriptions and enabled/disabled
	 * properties.
	 */
	private void updateShortcutFields() {
		if (mApplication.isScreenLockShortcutCreated()) {
			mToggleScreenLockShortcut.setTitle(R.string.remove_screen_lock_shortcut);
			mToggleScreenLockShortcut
					.setSummary(R.string.remove_screen_lock_shortcut_desc);
		} else {
			mToggleScreenLockShortcut.setTitle(R.string.create_screen_lock_shortcut);
			mToggleScreenLockShortcut
					.setSummary(R.string.create_screen_lock_shortcut_desc);
		}
	}

	/**
	 * Method invoked when was pressed the keepScreenLockCounter preference.
	 */
	private void onKeepScreenLockCounter() {
		if (mApplication.getKeepScreenLockCounter() > 0) {
			showCustomAlertDialog(R.string.locked_screen_log_title,
					R.string.empty,
					false,
					android.R.drawable.ic_dialog_alert,
					ID_CONFIRMATION_SHOW_KEEP_LOGS,
					true,
					getLockHistoryListAdapter());
		}
	}

	/**
	 * Get an array with all history list events.
	 *
	 * @return An array with all history events.
	 */
	private ListAdapter getLockHistoryListAdapter() {
		if (mHistoryEventsItems == null) {
			String[] items = mApplication.getKeepScreenLockLogs().split(",");
			if (!Utilities.isEmpty(items)) {
				int i, len = items.length, k = 1;
				mHistoryEventsItems = new ArrayAdapter<>(this,
						R.layout.list_view_row_item);
				for (i = len; i > 0; i--) {
					mHistoryEventsItems.add(String.valueOf(k++) + ". " +
							mApplication.getFormattedDateTime(items[i - 1]));
				}
			}
		}
		return mHistoryEventsItems;
	}

	/**
	 * Show about pop up dialog message.
	 */
	private void onBuildVersion() {
		showCustomAlertDialog(R.string.app_name,
				R.string.app_description,
				false,
				android.R.drawable.ic_dialog_info,
				ID_CONFIRMATION_ALERT,
				true,
				null);
	}

	/**
	 * Method invoked when is clicked on the send debug report preference.
	 */
	private void onSendDebugReport() {
		showCustomAlertDialog(R.string.app_name,
				R.string.send_debug_confirmation,
				false,
				android.R.drawable.ic_dialog_alert,
				ID_CONFIRMATION_DEBUG_REPORT,
				true,
				null);
	}

	/**
	 * Show license info.
	 */
	private void onLicensePref() {
		Intent intent = new Intent(getBaseContext(), InfoActivity.class);
		Bundle b = new Bundle();
		b.putInt(InfoActivity.TITLE, R.string.license_title);
		b.putString(InfoActivity.FILE_NAME, "gpl-3.0-standalone.html");
		b.putBoolean(InfoActivity.HTML_MESSAGE, true);
		intent.putExtras(b);
		startActivity(intent);
	}

	/**
	 * Method invoked when was pressed the donatePref preference.
	 */
	private void onDonatePref() {
		showCustomAlertDialog(R.string.app_name,
				R.string.donate_confirmation,
				false,
				android.R.drawable.ic_dialog_info,
				ID_CONFIRMATION_DONATION,
				true,
				null);
	}

	/**
	 * Show a confirmation popup dialog.
	 *
	 * @param titleId            Dialog title.
	 * @param messageId          Message of the confirmation dialog.
	 * @param messageContainLink A boolean flag which mark if the text contain links.
	 * @param iconId             The icon ID.
	 * @param confirmationId     ID of the process to be executed if confirmed.
	 * @param cancelable         Flag used to build the message box as cancelable or not.
	 * @param adapter            This is a list adapter used to show an list of items on the dialog message place.
	 */
	private void showCustomAlertDialog(int titleId,
									   int messageId,
									   boolean messageContainLink,
									   int iconId,
									   final int confirmationId,
									   boolean cancelable,
									   ListAdapter adapter) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setIcon(iconId);
		alertDialog.setTitle(titleId);
		if (adapter != null) {
			alertDialog.setAdapter(adapter, null);
		} else if (messageId != ID_ALERT_NO_MESSAGE) {
			if (messageContainLink) {
				String message = MainApplication.getAppContext().getString(messageId);
				ScrollView scrollView = new ScrollView(this);
				SpannableString spanText = new SpannableString(message);
				Linkify.addLinks(spanText, Linkify.ALL);

				TextView textView = new TextView(this);
				textView.setMovementMethod(LinkMovementMethod.getInstance());
				textView.setText(spanText);

				scrollView.setPadding(14, 2, 10, 12);
				scrollView.addView(textView);
				alertDialog.setView(scrollView);
			} else {
				alertDialog.setMessage(messageId);
			}
		}
		alertDialog.setCancelable(cancelable);
		if (confirmationId == ID_CONFIRMATION_SHOW_KEEP_LOGS) {
			alertDialog.setNeutralButton(R.string.ok, null);
			alertDialog.setPositiveButton(R.string.locked_screen_log_clear,
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int whichButton) {
							onConfirmation(confirmationId);
						}
					});
		} else if (confirmationId == ID_CONFIRMATION_ALERT) {
			alertDialog.setNeutralButton(R.string.ok, null);
		} else {
			alertDialog.setPositiveButton(R.string.yes,
					new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int whichButton) {
							onConfirmation(confirmationId);
						}
					});
			alertDialog.setNegativeButton(R.string.no, null);
		}
		AlertDialog alert = alertDialog.create();
		alert.show();
	}

	/**
	 * Execute proper confirmation process based on received confirmation ID.
	 *
	 * @param confirmationId Received confirmation ID.
	 */
	protected void onConfirmation(int confirmationId) {
		if (confirmationId == ID_CONFIRMATION_SHOW_KEEP_LOGS) {
			mApplication.increaseKeepScreenLockCounter(-1);
		} else if (confirmationId == ID_CONFIRMATION_DONATION) {
			confirmedDonationPage();
		} else if (confirmationId == ID_CONFIRMATION_LOCKED_SCREEN_COUNTER) {
			mApplication.increaseKeepScreenLockCounter(-1);
		} else if (confirmationId == ID_CONFIRMATION_DEBUG_REPORT) {
			confirmedSendReport(MainApplication.getAppContext().getString(R.string.send_debug_email_title));
		} else if (confirmationId == ID_CONFIRMATION_REQUEST_PERMISSIONS) {
			String[] permissions = mApplication.getNotGrantedPermissions();
			if (Utilities.isEmpty(permissions)) {
				showCustomAlertDialog(R.string.app_name,
						R.string.request_permissions_ok,
						false,
						android.R.drawable.ic_dialog_info,
						ID_CONFIRMATION_ALERT,
						true,
						null);
			} else {
				requestForPermissions(mApplication.getNotGrantedPermissions());
			}
		}
	}

	/**
	 * Method invoked when was pressed the request permission preference.
	 */
	private void onRequestPermissions() {
		showCustomAlertDialog(R.string.app_name,
				R.string.request_permissions_confirmation,
				false,
				android.R.drawable.ic_dialog_info,
				ID_CONFIRMATION_REQUEST_PERMISSIONS,
				true,
				null);
	}

	/**
	 * Access the browser to open the donation page.
	 */
	private void confirmedDonationPage() {
		String url = MainApplication.getAppContext().getString(R.string.donate_url);
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		try {
			startActivity(i);
		} catch (ActivityNotFoundException ex) {
			mApplication.logE(TAG,
					"confirmedDonationPage Exception: " + ex.getMessage(), ex);
		}
	}

	/**
	 * User just confirmed to send a report.
	 */
	private void confirmedSendReport(String emailTitle) {
		mApplication.showProgressDialog(this, this,
				MainApplication.getAppContext().getString(R.string.send_debug_title), 0);
		String message = MainApplication.getAppContext().getString(R.string.report_body);
		File logsFolder = mApplication.getLogsFolder();
		File archive = getLogArchive(logsFolder);
		String[] TO = {"ciubex@yahoo.com"};
		ArrayList<String> extra_text = new ArrayList<>();
		extra_text.add(message);

		Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		emailIntent.setType("text/plain");
		emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailTitle);
		emailIntent.putExtra(Intent.EXTRA_TEXT, message);
		emailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

		ArrayList<Uri> uris = new ArrayList<Uri>();
		if (archive != null && archive.exists() && archive.length() > 0) {
			uris.add(Uri.parse("content://" + CachedFileProvider.AUTHORITY
					+ "/" + archive.getName()));
		}
		if (!uris.isEmpty()) {
			emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
			emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}
		mApplication.hideProgressDialog();
		try {
			startActivityForResult(Intent.createChooser(emailIntent,
					MainApplication.getAppContext().getString(R.string.send_report)), REQUEST_SEND_REPORT);
		} catch (ActivityNotFoundException ex) {
			mApplication.logE(TAG,
					"confirmedSendReport Exception: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Build the logs and call the archive creator.
	 *
	 * @param logsFolder The logs folder.
	 * @return The archive file which should contain the logs.
	 */
	private File getLogArchive(File logsFolder) {
		File logFile = mApplication.getLogFile();
		File logcatFile = getLogcatFile(logsFolder);
		Date now = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String fileName = "KSL_logs_" + format.format(now) + ".zip";
		return getArchives(new File[]{logFile, logcatFile}, logsFolder, fileName);
	}

	/**
	 * Method used to build a ZIP archive with log files.
	 *
	 * @param files       The log files to be added.
	 * @param logsFolder  The logs folder where should be added the archive name.
	 * @param archiveName The archive file name.
	 * @return The archive file.
	 */
	private File getArchives(File[] files, File logsFolder, String archiveName) {
		File archive = new File(logsFolder, archiveName);
		try {
			BufferedInputStream origin;
			FileOutputStream dest = new FileOutputStream(archive);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			byte data[] = new byte[BUFFER];
			File file;
			FileInputStream fi;
			ZipEntry entry;
			int count;
			for (int i = 0; i < files.length; i++) {
				file = files[i];
				if (file.exists() && file.length() > 0) {
					mApplication.logD(TAG, "Adding to archive: " + file.getName());
					fi = new FileInputStream(file);
					origin = new BufferedInputStream(fi, BUFFER);
					entry = new ZipEntry(file.getName());
					out.putNextEntry(entry);
					while ((count = origin.read(data, 0, BUFFER)) != -1) {
						out.write(data, 0, count);
					}
					Utilities.doClose(entry);
					Utilities.doClose(origin);
				}
			}
			Utilities.doClose(out);
		} catch (FileNotFoundException e) {
			mApplication.logE(TAG, "getArchives failed: FileNotFoundException", e);
		} catch (IOException e) {
			mApplication.logE(TAG, "getArchives failed: IOException", e);
		}
		return archive;
	}

	/**
	 * Generate logs file on cache directory.
	 *
	 * @param cacheFolder Cache directory where are the logs.
	 * @return File with the logs.
	 */
	private File getLogcatFile(File cacheFolder) {
		File logFile = new File(cacheFolder, "KSL_logcat.log");
		Process shell = null;
		InputStreamReader reader = null;
		FileWriter writer = null;
		char LS = '\n';
		char[] buffer = new char[BUFFER];
		String model = Build.MODEL;
		if (!model.startsWith(Build.MANUFACTURER)) {
			model = Build.MANUFACTURER + " " + model;
		}
		mApplication.logD(TAG, "Prepare Logs to be send via e-mail.");
		String oldCmd = "logcat -d -v threadtime ro.ciubex.keepscreenlock:v dalvikvm:v System.err:v *:s";
		String newCmd = "logcat -d -v threadtime";
		String command = newCmd;
		try {
			if (!logFile.exists()) {
				logFile.createNewFile();
			}
			if (mApplication.getSdkInt() <= 15) {
				command = oldCmd;
			}
			shell = Runtime.getRuntime().exec(command);
			reader = new InputStreamReader(shell.getInputStream());
			writer = new FileWriter(logFile);
			writer.write("Android version: " + Build.VERSION.SDK_INT +
					" (" + Build.VERSION.CODENAME + ")" + LS);
			writer.write("Device: " + model + LS);
			writer.write("Device name: " + Devices.getDeviceName() + LS);
			writer.write("App version: " + mApplication.getVersionName() +
					" (" + mApplication.getVersionCode() + ")" + LS);
			mApplication.writeSharedPreferences(writer);
			int n;
			do {
				n = reader.read(buffer, 0, BUFFER);
				if (n == -1) {
					break;
				}
				writer.write(buffer, 0, n);
			} while (true);
			shell.waitFor();
		} catch (IOException e) {
			mApplication.logE(TAG, "getLogcatFile failed: IOException", e);
		} catch (InterruptedException e) {
			mApplication.logE(TAG, "getLogcatFile failed: InterruptedException", e);
		} catch (Exception e) {
			mApplication.logE(TAG, "getLogcatFile failed: Exception", e);
		} finally {
			Utilities.doClose(writer);
			Utilities.doClose(reader);
			if (shell != null) {
				shell.destroy();
			}
		}
		return logFile;
	}

	/**
	 * Callback for the result from requesting permissions.
	 *
	 * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
	 * @param permissions  The requested permissions. Never null.
	 * @param grantResults The grant results for the corresponding permissions.
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (PERMISSIONS_REQUEST_CODE == requestCode) {
			mApplication.markPermissionsAsked();
			for (String permission : permissions) {
				mApplication.markPermissionAsked(permission);
			}
			updateSettingsOptionsByPermissions();
		}
	}

	/**
	 * Update settings options based on the allowed permissions.
	 */
	private void updateSettingsOptionsByPermissions() {
		boolean allowed;
		if (mApplication.shouldAskPermissions()) {
			// functionality
			allowed = mApplication.haveFunctionalPermissions();
			mEnableKeepScreenLockService.setEnabled(allowed);
			mProximitySensorState.setEnabled(allowed);
			// shortcut
			allowed = allowed && mApplication.haveShortcutPermissions();
			mToggleScreenLockShortcut.setEnabled(allowed);
			// logs
			allowed = true;// mApplication.haveLogsPermissions();
			mSendDebugReport.setEnabled(allowed);
		}
	}
}
