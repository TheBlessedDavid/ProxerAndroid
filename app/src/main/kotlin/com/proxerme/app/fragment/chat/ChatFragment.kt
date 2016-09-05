package com.proxerme.app.fragment.chat

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import butterknife.bindView
import com.proxerme.app.R
import com.proxerme.app.activity.UserActivity
import com.proxerme.app.adapter.ChatAdapter
import com.proxerme.app.data.chatDatabase
import com.proxerme.app.entitiy.LocalConference
import com.proxerme.app.entitiy.LocalMessage
import com.proxerme.app.event.ChatMessagesEvent
import com.proxerme.app.fragment.framework.EasyChatServiceFragment
import com.proxerme.app.helper.StorageHelper
import com.proxerme.app.manager.SectionManager
import com.proxerme.app.manager.UserManager
import com.proxerme.app.service.ChatService
import com.proxerme.app.util.Utils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.inputMethodManager
import org.jetbrains.anko.toast

/**
 * TODO: Describe class
 *
 * @author Ruben Gees
 */
class ChatFragment : EasyChatServiceFragment<LocalMessage>() {

    companion object {
        private const val ARGUMENT_CONFERENCE = "conference"

        fun newInstance(conference: LocalConference): ChatFragment {

            return ChatFragment().apply {
                this.arguments = Bundle().apply {
                    this.putParcelable(ARGUMENT_CONFERENCE, conference)
                }
            }
        }
    }

    override val section: SectionManager.Section = SectionManager.Section.CHAT

    override lateinit var layoutManager: RecyclerView.LayoutManager
    override lateinit var adapter: ChatAdapter

    override val hasReachedEnd: Boolean
        get() = StorageHelper.hasConferenceReachedEnd(conference.id)
    override val isLoading: Boolean
        get() = ChatService.isLoadingMessages(conference.id)

    private lateinit var conference: LocalConference

    private var actionMode: ActionMode? = null

    private val actionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            Utils.setStatusBarColorIfPossible(activity, R.color.primary)

            if (adapter.selectedItems.size == 1 &&
                    adapter.selectedItems.first().userId != StorageHelper.user?.id ?: null) {
                menu.findItem(R.id.reply).isVisible = true
            } else {
                menu.findItem(R.id.reply).isVisible = false
            }

            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.copy -> handleCopyMenuItem()
                R.id.reply -> handleReplyMenuItem()
                else -> return false
            }

            return true
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.fragment_chat_cab, menu)

            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null

            adapter.clearSelection()
            Utils.setStatusBarColorIfPossible(activity, R.color.primary_dark)
        }
    }

    private val adapterCallback = object : ChatAdapter.OnMessageInteractionListener() {
        override fun onMessageTitleClick(v: View, message: LocalMessage) {
            UserActivity.navigateTo(activity, message.userId, message.username)
        }

        override fun onMessageSelection(count: Int) {
            if (count > 0) {
                if (actionMode == null) {
                    actionMode = (activity as AppCompatActivity)
                            .startSupportActionMode(actionModeCallback)
                    actionMode?.title = count.toString()
                } else {
                    actionMode?.title = count.toString()
                    actionMode?.invalidate()
                }
            } else {
                actionMode?.finish()
            }
        }

        override fun onMessageLinkClick(link: String) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            } catch (exception: ActivityNotFoundException) {
                context.toast(R.string.link_error_not_found)
            }
        }

        override fun onMessageLinkLongClick(link: String) {
            Utils.setClipboardContent(activity, getString(R.string.fragment_chat_link_clip_title),
                    link)

            context.toast(R.string.fragment_chat_clip_status)
        }

        override fun onMentionsClick(username: String) {
            UserActivity.navigateTo(activity, username = username)
        }
    }

    private val inputContainer: ViewGroup by bindView(R.id.inputContainer)
    private val messageInput: EditText by bindView(R.id.messageInput)
    private val sendButton: FloatingActionButton by bindView(R.id.sendButton)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        conference = arguments.getParcelable(ARGUMENT_CONFERENCE)

        adapter = ChatAdapter(savedInstanceState, conference.isGroup)
        adapter.user = UserManager.user
        adapter.callback = adapterCallback

        layoutManager = LinearLayoutManager(context)
        (layoutManager as LinearLayoutManager).reverseLayout = true
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)

        super.onStop()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()

            if (!text.isEmpty()) {
                context.chatDatabase.insertMessageToSend(StorageHelper.user!!, conference.id, text)

                if (canLoad) {
                    refresh()

                    if (!ChatService.isSynchronizing) {
                        ChatService.synchronize(context)
                    }
                }
            }

            messageInput.text.clear()
        }

        progress.setColorSchemeResources(R.color.primary)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (adapter.selectedItems.size > 0) {
            adapterCallback.onMessageSelection(adapter.selectedItems.size)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        adapter.saveInstanceState(outState)
    }

    override fun loadFromDB(): Collection<LocalMessage> {
        return context.chatDatabase.getMessages(conference.id)
    }

    override fun startLoadMore() {
        ChatService.loadMoreMessages(context, conference.id)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessagesChanged(@Suppress("UNUSED_PARAMETER") event: ChatMessagesEvent) {
        if (event.conferenceId == conference.id) {
            if (canLoad) {
                refresh()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLoadMoreMessagesFailed(exception: ChatService.LoadMoreMessagesException) {
        if (exception.conferenceId == conference.id) {
            showError(exception)
        }
    }

    override fun showError(message: String, buttonMessage: String?,
                           onButtonClickListener: View.OnClickListener?,
                           clear: Boolean) {
        super.showError(message, buttonMessage, onButtonClickListener, clear)

        if (clear) {
            inputContainer.visibility = View.GONE
        }
    }

    override fun hideError() {
        super.hideError()

        inputContainer.visibility = View.VISIBLE
    }

    private fun handleCopyMenuItem() {
        Utils.setClipboardContent(activity, getString(R.string.fragment_chat_clip_title),
                adapter.selectedItems.joinToString(separator = "\n", transform = { it.message }))

        context.toast(R.string.fragment_chat_clip_status)
        actionMode?.finish()
    }

    private fun handleReplyMenuItem() {
        messageInput.setText(getString(R.string.fragment_chat_reply,
                adapter.selectedItems.first().username))
        messageInput.setSelection(messageInput.text.length)
        messageInput.requestFocus()

        activity.inputMethodManager.showSoftInput(messageInput, InputMethodManager.SHOW_IMPLICIT)

        actionMode?.finish()
    }
}