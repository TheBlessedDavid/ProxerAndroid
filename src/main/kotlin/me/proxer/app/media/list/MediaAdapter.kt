package me.proxer.app.media.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.view.clicks
import com.uber.autodispose.kotlin.autoDisposable
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.AutoDisposeViewHolder
import me.proxer.app.base.BaseAdapter
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.util.extension.getQuantityString
import me.proxer.app.util.extension.mapAdapterPosition
import me.proxer.app.util.extension.toAppDrawable
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.entity.list.MediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class MediaAdapter(private val category: Category) : BaseAdapter<MediaListEntry, MediaAdapter.ViewHolder>() {

    var glide: GlideRequests? = null
    val clickSubject: PublishSubject<Pair<ImageView, MediaListEntry>> = PublishSubject.create()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_media_entry, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        glide?.clear(holder.image)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        glide = null
    }

    override fun swapDataAndNotifyWithDiffing(newData: List<MediaListEntry>) {
        super.swapDataAndNotifyWithDiffing(newData.distinctBy { it.id })
    }

    inner class ViewHolder(itemView: View) : AutoDisposeViewHolder(itemView) {

        internal val title: TextView by bindView(R.id.title)
        internal val medium: TextView by bindView(R.id.medium)
        internal val image: ImageView by bindView(R.id.image)
        internal val ratingContainer: ViewGroup by bindView(R.id.ratingContainer)
        internal val rating: RatingBar by bindView(R.id.rating)
        internal val state: ImageView by bindView(R.id.state)
        internal val episodes: TextView by bindView(R.id.episodes)
        internal val english: ImageView by bindView(R.id.english)
        internal val german: ImageView by bindView(R.id.german)

        fun bind(item: MediaListEntry) {
            itemView.clicks()
                .mapAdapterPosition({ adapterPosition }) { image to data[it] }
                .autoDisposable(this)
                .subscribe(clickSubject)

            ViewCompat.setTransitionName(image, "media_${item.id}")

            title.text = item.name
            medium.text = item.medium.toAppString(medium.context)
            episodes.text = episodes.context.getQuantityString(
                when (category) {
                    Category.ANIME -> R.plurals.media_episode_count
                    Category.MANGA -> R.plurals.media_chapter_count
                }, item.episodeAmount
            )

            item.languages
                .asSequence()
                .map { it.toGeneralLanguage() }
                .distinct()
                .toList()
                .let { generalLanguages ->
                    english.isVisible = generalLanguages.contains(Language.ENGLISH)
                    german.isVisible = generalLanguages.contains(Language.GERMAN)
                }

            if (item.rating > 0) {
                ratingContainer.isVisible = true
                rating.rating = item.rating / 2.0f

                episodes.updateLayoutParams<RelativeLayout.LayoutParams> {
                    addRule(RelativeLayout.ALIGN_BOTTOM, 0)
                    addRule(RelativeLayout.BELOW, R.id.state)
                }
            } else {
                ratingContainer.isGone = true

                episodes.updateLayoutParams<RelativeLayout.LayoutParams> {
                    addRule(RelativeLayout.ALIGN_BOTTOM, R.id.languageContainer)
                    addRule(RelativeLayout.BELOW, R.id.medium)
                }
            }

            state.setImageDrawable(item.state.toAppDrawable(state.context))
            glide?.defaultLoad(image, ProxerUrls.entryImage(item.id))
        }
    }
}
