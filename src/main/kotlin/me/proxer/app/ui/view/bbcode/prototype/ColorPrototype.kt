package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.core.text.parseAsHtml
import androidx.core.text.set
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object ColorPrototype : TextMutatorPrototype {

    private val ATTRIBUTE_REGEX = Regex("color *= *(.+?)( |$)", REGEX_OPTIONS)
    private const val COLOR_ARGUMENT = "color"

    override val startRegex = Regex(" *color *= *\"?.*?\"?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *color *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val value = BBUtils.cutAttribute(code, ATTRIBUTE_REGEX) ?: ""

        val color = "<font color='$value'>dummy</font>".parseAsHtml()
            .let { it.getSpans(0, it.length, ForegroundColorSpan::class.java) }
            .firstOrNull()?.foregroundColor

        return BBTree(this, parent, args = BBArgs(custom = *arrayOf(COLOR_ARGUMENT to color)))
    }

    override fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder {
        val color = args[COLOR_ARGUMENT] as Int?

        return when (color) {
            null -> text
            else -> text.apply {
                this[0..length] = ForegroundColorSpan(color)
            }
        }
    }
}
