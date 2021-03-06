package org.koitharu.kotatsu.ui.list.favourites

import kotlinx.android.synthetic.main.fragment_list.*
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.ui.list.MangaListFragment
import org.koitharu.kotatsu.ui.list.MangaListView
import org.koitharu.kotatsu.utils.ext.withArgs

class FavouritesListFragment : MangaListFragment<Unit>(),
	MangaListView<Unit> {

	private val presenter by moxyPresenter(factory = ::FavouritesListPresenter)

	private val categoryId: Long
		get() = arguments?.getLong(ARG_CATEGORY_ID) ?: 0L

	override fun onRequestMoreItems(offset: Int) {
		presenter.loadList(categoryId, offset)
	}

	override fun setUpEmptyListHolder() {
		textView_holder.setText(if (categoryId == 0L) {
			R.string.you_have_not_favourites_yet
		} else {
			R.string.favourites_category_empty
		})
		textView_holder.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
	}

	companion object {

		private const val ARG_CATEGORY_ID = "category_id"

		fun newInstance(categoryId: Long) = FavouritesListFragment().withArgs(1) {
			putLong(ARG_CATEGORY_ID, categoryId)
		}
	}
}