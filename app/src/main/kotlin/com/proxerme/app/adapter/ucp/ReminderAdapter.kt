package com.proxerme.app.adapter.ucp

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.proxerme.app.R
import com.proxerme.app.adapter.framework.PagingAdapter
import com.proxerme.app.util.Utils
import com.proxerme.app.util.bindView
import com.proxerme.library.connection.ucp.entitiy.Reminder
import com.proxerme.library.info.ProxerUrlHolder

/**
 * TODO: Describe Class
 *
 * @author Ruben Gees
 */
class ReminderAdapter : PagingAdapter<Reminder>() {

    var callback: ReminderAdapterCallback? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = list[position].id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagingViewHolder<Reminder> {
        return ReminderViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_reminder, parent, false))
    }

    override fun removeCallback() {
        callback = null
    }

    inner class ReminderViewHolder(itemView: View) : PagingViewHolder<Reminder>(itemView) {

        private val title: TextView by bindView(R.id.title)
        private val medium: TextView by bindView(R.id.medium)
        private val image: ImageView by bindView(R.id.image)
        private val episode: TextView by bindView(R.id.episode)
        private val language: ImageView by bindView(R.id.language)
        private val removeButton: ImageButton by bindView(R.id.removeButton)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onItemClick(list[adapterPosition])
                }
            }

            removeButton.setImageDrawable(IconicsDrawable(removeButton.context)
                    .icon(CommunityMaterial.Icon.cmd_bookmark_remove)
                    .colorRes(R.color.icon)
                    .sizeDp(48)
                    .paddingDp(12))

            removeButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    callback?.onRemoveClick(list[adapterPosition])
                }
            }
        }

        override fun bind(item: Reminder) {
            title.text = item.name
            medium.text = item.medium
            episode.text = episode.context.getString(R.string.reminder_episode, item.episode)
            language.setImageResource(when (Utils.getLanguages(item.language).firstOrNull()) {
                Utils.Language.ENGLISH -> R.drawable.ic_united_states
                Utils.Language.GERMAN -> R.drawable.ic_germany
                else -> throw IllegalArgumentException("Unknown language: ${item.language}")
            })

            Glide.with(image.context)
                    .load(ProxerUrlHolder.getCoverImageUrl(item.entryId).toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(image)
        }
    }

    abstract class ReminderAdapterCallback {

        open fun onItemClick(item: Reminder) {

        }

        open fun onRemoveClick(item: Reminder) {

        }

    }
}