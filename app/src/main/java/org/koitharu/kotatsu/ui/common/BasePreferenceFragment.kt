package org.koitharu.kotatsu.ui.common

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.koin.android.ext.android.inject
import org.koitharu.kotatsu.core.prefs.AppSettings

abstract class BasePreferenceFragment(@StringRes private val titleId: Int) :
	PreferenceFragmentCompat(), OnApplyWindowInsetsListener {

	protected val settings by inject<AppSettings>()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		listView.clipToPadding = false
		ViewCompat.setOnApplyWindowInsetsListener(view, this)
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		with(insets.getInsets(WindowInsetsCompat.Type.systemBars())) {
			listView.updatePadding(left = left, right = right, bottom = bottom)
		}
		return WindowInsetsCompat.CONSUMED
	}

	override fun onResume() {
		super.onResume()
		activity?.setTitle(titleId)
	}

	fun <T : Preference> findPreference(@StringRes keyId: Int): T? =
		findPreference(getString(keyId))

}