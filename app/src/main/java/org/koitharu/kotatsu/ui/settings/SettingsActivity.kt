package org.koitharu.kotatsu.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.android.synthetic.main.activity_settings.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.common.BaseActivity

class SettingsActivity : BaseActivity(),
	PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, OnApplyWindowInsetsListener {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_settings)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		ViewCompat.setOnApplyWindowInsetsListener(toolbar, this)

		if (supportFragmentManager.findFragmentById(R.id.container) == null) {
			supportFragmentManager.commit {
				replace(R.id.container, MainSettingsFragment())
			}
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		with(insets.getInsets(WindowInsetsCompat.Type.systemBars())) {
			v.updatePadding(top = top, left = left, right = right)
		}
		return insets
	}

	@Suppress("DEPRECATION")
	override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
		val fm = supportFragmentManager
		val fragment = fm.fragmentFactory.instantiate(classLoader, pref.fragment)
		fragment.arguments = pref.extras
		fragment.setTargetFragment(caller, 0)
		openFragment(fragment)
		return true
	}

	fun openMangaSourceSettings(mangaSource: MangaSource) {
		openFragment(SourceSettingsFragment.newInstance(mangaSource))
	}

	fun openNotificationSettingsLegacy() {
		openFragment(NotificationSettingsLegacyFragment())
	}

	private fun openFragment(fragment: Fragment) {
		supportFragmentManager.commit {
			replace(R.id.container, fragment)
			addToBackStack(null)
		}
	}

	companion object {

		fun newIntent(context: Context) = Intent(context, SettingsActivity::class.java)
	}
}