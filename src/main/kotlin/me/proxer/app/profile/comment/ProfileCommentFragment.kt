package me.proxer.app.profile.comment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.iconics.utils.IconicsMenuInflaterUtil
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.proxer.app.R
import me.proxer.app.base.PagedContentFragment
import me.proxer.app.comment.CommentActivity
import me.proxer.app.comment.LocalComment
import me.proxer.app.media.MediaActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.util.extension.getSafeParcelableExtra
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.enums.Category
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ProfileCommentFragment : PagedContentFragment<ParsedUserComment>() {

    companion object {
        private const val CATEGORY_ARGUMENT = "category"

        fun newInstance() = ProfileCommentFragment().apply {
            arguments = bundleOf()
        }
    }

    override val emptyDataMessage = R.string.error_no_data_comments
    override val isSwipeToRefreshEnabled = false
    override val pagingThreshold = 3

    override val viewModel by viewModel<ProfileCommentViewModel> {
        parametersOf(userId, username, category)
    }

    override val hostingActivity: ProfileActivity
        get() = activity as ProfileActivity

    private val userId: String?
        get() = hostingActivity.userId

    private val username: String?
        get() = hostingActivity.username

    private var category: Category?
        get() = requireArguments().getSerializable(CATEGORY_ARGUMENT) as? Category
        set(value) {
            requireArguments().putSerializable(CATEGORY_ARGUMENT, value)

            viewModel.category = value
        }

    override val layoutManager by unsafeLazy { LinearLayoutManager(context) }
    override var innerAdapter by Delegates.notNull<ProfileCommentAdapter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        innerAdapter = ProfileCommentAdapter(savedInstanceState, storageHelper)

        innerAdapter.titleClickSubject
            .autoDisposable(this.scope())
            .subscribe {
                MediaActivity.navigateTo(requireActivity(), it.entryId, it.entryName, it.category)
            }

        innerAdapter.editClickSubject
            .autoDisposable(this.scope())
            .subscribe {
                CommentActivity.navigateTo(this, it.id, it.entryId, it.entryName)
            }

        setHasOptionsMenu(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CommentActivity.COMMENT_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Single.fromCallable { data.getSafeParcelableExtra<LocalComment>(CommentActivity.COMMENT_EXTRA) }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .autoDisposable(this.scope())
                .subscribeAndLogErrors { viewModel.updateComment(it) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        IconicsMenuInflaterUtil.inflate(inflater, requireContext(), R.menu.fragment_user_comments, menu, true)

        when (category) {
            Category.ANIME -> menu.findItem(R.id.anime).isChecked = true
            Category.MANGA -> menu.findItem(R.id.manga).isChecked = true
            else -> menu.findItem(R.id.all).isChecked = true
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.anime -> category = Category.ANIME
            R.id.manga -> category = Category.MANGA
            R.id.all -> category = null
        }

        item.isChecked = true

        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        innerAdapter.saveInstanceState(outState)
    }
}
