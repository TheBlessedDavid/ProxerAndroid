package me.proxer.app.manga

import android.graphics.PointF
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.manga.MangaAdapter.ViewHolder
import me.proxer.app.manga.MangaPageSingle.Input
import me.proxer.app.util.DeviceUtils
import me.proxer.app.util.extension.decodedName
import me.proxer.app.util.extension.subscribeAndLogErrors
import me.proxer.library.entity.manga.Page
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MangaAdapter(private val isVertical: Boolean) : BaseAdapter<Page, ViewHolder>() {

    val clickSubject: PublishSubject<Int> = PublishSubject.create()

    var server by Delegates.notNull<String>()
    var entryId by Delegates.notNull<String>()
    var id by Delegates.notNull<String>()
    var isLocal: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_manga_page, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])

    override fun onViewRecycled(holder: ViewHolder) {
        holder.image.recycle()

        (holder.image.tag as? Disposable)?.dispose()
        holder.image.tag = null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val shortAnimationTime = itemView.context.resources.getInteger(android.R.integer.config_shortAnimTime)
        private val mediumAnimationTime = itemView.context.resources.getInteger(android.R.integer.config_mediumAnimTime)

        internal val image: SubsamplingScaleImageView by bindView(R.id.image)

        private val smoothScrollHack = { _: View?, event: MotionEvent ->
            // Make scrolling smoother by hacking the SubsamplingScaleImageView to only
            // receive touch events when zooming.
            val shouldInterceptEvent = event.action == MotionEvent.ACTION_MOVE && event.pointerCount == 1 &&
                    image.scale == image.minScale

            if (shouldInterceptEvent) {
                image.parent.requestDisallowInterceptTouchEvent(true)
                image.onTouchEvent(event)
                image.parent.requestDisallowInterceptTouchEvent(false)

                true
            } else {
                false
            }
        }

        init {
            image.setDoubleTapZoomDuration(shortAnimationTime)
            image.setBitmapDecoderClass(RapidImageDecoder::class.java)
            image.setRegionDecoderClass(RapidImageRegionDecoder::class.java)

            @Suppress("ClickableViewAccessibility")
            image.setOnTouchListener(smoothScrollHack)

            if (!isVertical) {
                itemView.layoutParams.height = MATCH_PARENT

                image.setOnClickListener {
                    withSafeAdapterPosition(this, {
                        clickSubject.onNext(it)
                    })
                }
            }
        }

        fun bind(item: Page) {
            if (isVertical) {
                val width = DeviceUtils.getScreenWidth(image.context)
                val height = (item.height * width.toFloat() / item.width.toFloat()).toInt()
                val scale = width.toFloat() / item.width.toFloat() * 2f

                image.setDoubleTapZoomScale(scale)
                image.layoutParams.height = height
                image.maxScale = scale
            }

            image.tag = MangaPageSingle(image.context, isLocal, Input(server, entryId, id, item.decodedName))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeAndLogErrors {
                        image.setImage(ImageSource.uri(it.path))
                        image.setScaleAndCenter(0.2f, PointF(0f, 0f))

                        // Fade animations do not look good with the horizontal reader.
                        if (isVertical) {
                            image.apply { alpha = 0.2f }
                                    .animate()
                                    .alpha(1.0f)
                                    .setDuration(mediumAnimationTime.toLong())
                                    .start()
                        }
                    }
        }
    }
}
