package ro.ciubex.keepscreenlock.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

import ro.ciubex.keepscreenlock.util.Utilities;

/**
 * Created by claudiu on 02.11.2015.
 */
public class IntegerEditTextPreference extends EditTextPreference {
	private static final String PREFERENCE_NS = "http://schemas.android.com/apk/res/ro.ciubex.keepscreenlock";
	private static final int DEFAULT_VALUE = 0;

	private static final String ATTR_MIN_VALUE = "minValue";
	private static final String ATTR_MAX_VALUE = "maxValue";

	// Default values for defaults
	private static final int DEFAULT_MIN_VALUE = 0;
	private static final int DEFAULT_MAX_VALUE = 100;

	// Real defaults
	private int mMaxValue;
	private int mMinValue;

	// Current value
	private int mCurrentValue = DEFAULT_VALUE;

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public IntegerEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		setValuesFromXml(attrs);
	}

	public IntegerEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setValuesFromXml(attrs);
	}

	public IntegerEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setValuesFromXml(attrs);
	}

	/**
	 * Initialize internal parameter based on the provided attributes.
	 *
	 * @param attrs
	 *            The attributes of the XML tag that is inflating the
	 *            preference.
	 */
	private void setValuesFromXml(AttributeSet attrs) {
		// Read parameters from attributes
		mMinValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MIN_VALUE, DEFAULT_MIN_VALUE);
		mMaxValue = attrs.getAttributeIntValue(PREFERENCE_NS, ATTR_MAX_VALUE, DEFAULT_MAX_VALUE);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			prepareCurrentValue();
		}
		super.onDialogClosed(positiveResult);
	}

	@Override
	protected String getPersistedString(String defaultReturnValue) {
		return String.valueOf(getPersistedInt(mCurrentValue));
	}

	@Override
	protected boolean persistString(String value) {
		return persistInt(Utilities.getInteger(value, mCurrentValue));
	}

	/**
	 * Prepare the current value of the field.
	 */
	private void prepareCurrentValue() {
		String value = getEditText().getText().toString();
		int result = Utilities.getInteger(value, mCurrentValue);
		if (result < mMinValue || result > mMaxValue) {
			getEditText().setText("" + mCurrentValue);
		} else {
			mCurrentValue = result;
		}
	}
}
