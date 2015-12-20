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

import android.app.Activity;
import android.os.Bundle;

import ro.ciubex.keepscreenlock.MainApplication;

/**
 * This is an activity invoked by the screen lock shortcut.
 *
 * @author Claudiu Ciobotariu
 */
public class ScreenLockActivity extends Activity {
	private static final String TAG = ScreenLockActivity.class.getName();
	private MainApplication mApplication;

	/**
	 * Method called when the activity is created.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApplication = (MainApplication) getApplication();
		mApplication.logD(TAG, "ScreenLockActivity invoked and call executeLockScreen!");
		mApplication.executeLockScreen(false);
		finish();
	}
}
