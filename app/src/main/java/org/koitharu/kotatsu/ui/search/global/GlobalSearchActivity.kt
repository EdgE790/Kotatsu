package org.koitharu.kotatsu.ui.search.global

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlinx.android.synthetic.main.activity_search_global.*
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.common.BaseActivity

class GlobalSearchActivity : BaseActivity(), OnApplyWindowInsetsListener {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_search_global)
		ViewCompat.setOnApplyWindowInsetsListener(toolbar, this)
		val query = intent.getStringExtra(EXTRA_QUERY)

		if (query == null) {
			finishAfterTransition()
			return
		}

		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		title = query
		supportActionBar?.subtitle = getString(R.string.search_results)
		supportFragmentManager
			.beginTransaction()
			.replace(R.id.container, GlobalSearchFragment.newInstance(query))
			.commit()
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		with(insets.getInsets(WindowInsetsCompat.Type.systemBars())) {
			v.updatePadding(left = left, top = top, right = right)
		}
		return insets
	}

	companion object {

		private const val EXTRA_QUERY = "query"

		fun newIntent(context: Context, query: String) =
			Intent(context, GlobalSearchActivity::class.java)
				.putExtra(EXTRA_QUERY, query)
	}
}