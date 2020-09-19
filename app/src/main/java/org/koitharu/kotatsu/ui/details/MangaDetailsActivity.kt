package org.koitharu.kotatsu.ui.details

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toFile
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.coroutines.launch
import moxy.MvpDelegate
import moxy.ktx.moxyPresenter
import org.koitharu.kotatsu.BuildConfig
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.core.model.MangaSource
import org.koitharu.kotatsu.ui.browser.BrowserActivity
import org.koitharu.kotatsu.ui.common.BaseActivity
import org.koitharu.kotatsu.ui.download.DownloadService
import org.koitharu.kotatsu.utils.MangaShortcut
import org.koitharu.kotatsu.utils.ShareHelper
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import org.koitharu.kotatsu.utils.ext.getThemeColor

class MangaDetailsActivity : BaseActivity(), MangaDetailsView,
	TabLayoutMediator.TabConfigurationStrategy, OnApplyWindowInsetsListener {

	private val presenter by moxyPresenter {
		MangaDetailsPresenter.getInstance(hashCode())
	}

	private var manga: Manga? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_details)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		pager.adapter = MangaDetailsAdapter(this)
		ViewCompat.setOnApplyWindowInsetsListener(coordinator, this)
		TabLayoutMediator(tabs, pager, this).attach()
		if (savedInstanceState?.containsKey(MvpDelegate.MOXY_DELEGATE_TAGS_KEY) != true) {
			intent?.getParcelableExtra<Manga>(EXTRA_MANGA)?.let {
				presenter.loadDetails(it, true)
			} ?: intent?.getLongExtra(EXTRA_MANGA_ID, 0)?.takeUnless { it == 0L }?.let {
				presenter.findMangaById(it)
			} ?: finishAfterTransition()
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		with(insets.getInsets(WindowInsetsCompat.Type.systemBars())) {
			toolbar.updatePadding(top = top)
		}
		return insets
	}

	override fun onMangaUpdated(manga: Manga) {
		this.manga = manga
		title = manga.title
		invalidateOptionsMenu()
	}

	override fun onHistoryChanged(history: MangaHistory?) = Unit

	override fun onFavouriteChanged(categories: List<FavouriteCategory>) = Unit

	override fun onLoadingStateChanged(isLoading: Boolean) = Unit

	override fun onMangaRemoved(manga: Manga) {
		Toast.makeText(
			this, getString(R.string._s_deleted_from_local_storage, manga.title),
			Toast.LENGTH_SHORT
		).show()
		finishAfterTransition()
	}

	override fun onError(e: Throwable) {
		if (manga == null) {
			Toast.makeText(this, e.getDisplayMessage(resources), Toast.LENGTH_LONG).show()
			finishAfterTransition()
		} else {
			Snackbar.make(pager, e.getDisplayMessage(resources), Snackbar.LENGTH_LONG).show()
		}
	}

	override fun onNewChaptersChanged(newChapters: Int) {
		val tab = tabs.getTabAt(1) ?: return
		if (newChapters == 0) {
			tab.removeBadge()
		} else {
			val badge = tab.orCreateBadge
			badge.number = newChapters
			badge.isVisible = true
		}
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.opt_details, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		menu.findItem(R.id.action_save).isVisible =
			manga?.source != null && manga?.source != MangaSource.LOCAL
		menu.findItem(R.id.action_delete).isVisible =
			manga?.source == MangaSource.LOCAL
		menu.findItem(R.id.action_browser).isVisible =
			manga?.source != MangaSource.LOCAL
		menu.findItem(R.id.action_shortcut).isVisible =
			ShortcutManagerCompat.isRequestPinShortcutSupported(this)
		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
		R.id.action_share -> {
			manga?.let {
				if (it.source == MangaSource.LOCAL) {
					ShareHelper.shareCbz(this, Uri.parse(it.url).toFile())
				} else {
					ShareHelper.shareMangaLink(this, it)
				}
			}
			true
		}
		R.id.action_delete -> {
			manga?.let { m ->
				MaterialAlertDialogBuilder(this)
					.setTitle(R.string.delete_manga)
					.setMessage(getString(R.string.text_delete_local_manga, m.title))
					.setPositiveButton(R.string.delete) { _, _ ->
						presenter.deleteLocal(m)
					}
					.setNegativeButton(android.R.string.cancel, null)
					.show()
			}
			true
		}
		R.id.action_save -> {
			manga?.let {
				val chaptersCount = it.chapters?.size ?: 0
				if (chaptersCount > 5) {
					MaterialAlertDialogBuilder(this)
						.setTitle(R.string.save_manga)
						.setMessage(
							getString(
								R.string.large_manga_save_confirm,
								resources.getQuantityString(
									R.plurals.chapters,
									chaptersCount,
									chaptersCount
								)
							)
						)
						.setNegativeButton(android.R.string.cancel, null)
						.setPositiveButton(R.string.save) { _, _ ->
							DownloadService.start(this, it)
						}.show()
				} else {
					DownloadService.start(this, it)
				}
			}
			true
		}
		R.id.action_browser -> {
			manga?.let {
				startActivity(BrowserActivity.newIntent(this, it.url))
			}
			true
		}
		R.id.action_shortcut -> {
			manga?.let {
				lifecycleScope.launch {
					if (!MangaShortcut(it).requestPinShortcut(this@MangaDetailsActivity)) {
						Snackbar.make(
							pager,
							R.string.operation_not_supported,
							Snackbar.LENGTH_SHORT
						).show()
					}
				}
			}
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onConfigureTab(tab: TabLayout.Tab, position: Int) {
		tab.text = when (position) {
			0 -> getString(R.string.details)
			1 -> getString(R.string.chapters)
			2 -> getString(R.string.related)
			else -> null
		}
	}

	override fun onSupportActionModeStarted(mode: ActionMode) {
		super.onSupportActionModeStarted(mode)
		pager.isUserInputEnabled = false
		window?.statusBarColor = ContextCompat.getColor(this, R.color.grey_dark)
	}

	override fun onSupportActionModeFinished(mode: ActionMode) {
		super.onSupportActionModeFinished(mode)
		pager.isUserInputEnabled = true
		window?.statusBarColor = getThemeColor(android.R.attr.statusBarColor)
	}

	companion object {

		private const val EXTRA_MANGA = "manga"
		const val EXTRA_MANGA_ID = "manga_id"

		const val ACTION_MANGA_VIEW = "${BuildConfig.APPLICATION_ID}.action.VIEW_MANGA"

		fun newIntent(context: Context, manga: Manga) =
			Intent(context, MangaDetailsActivity::class.java)
				.putExtra(EXTRA_MANGA, manga)

		fun newIntent(context: Context, mangaId: Long) =
			Intent(context, MangaDetailsActivity::class.java)
				.putExtra(EXTRA_MANGA_ID, mangaId)
	}
}