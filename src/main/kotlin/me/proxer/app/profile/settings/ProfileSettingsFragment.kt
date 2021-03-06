package me.proxer.app.profile.settings

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer
import androidx.preference.Preference
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import me.proxer.app.R
import me.proxer.app.util.KotterKnifePreference
import me.proxer.app.util.bindPreference
import me.proxer.app.util.extension.changes
import me.proxer.library.enums.UcpSettingConstraint
import me.proxer.library.util.ProxerUtils
import net.xpece.android.support.preference.ListPreference
import net.xpece.android.support.preference.SwitchPreference
import net.xpece.android.support.preference.XpPreferenceFragment
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.Locale

/**
 * @author Ruben Gees
 */
class ProfileSettingsFragment : XpPreferenceFragment() {

    companion object {
        fun newInstance() = ProfileSettingsFragment().apply {
            arguments = bundleOf()
        }
    }

    private val viewModel by sharedViewModel<ProfileSettingsViewModel>()

    private val bannerAdsEnabled by bindPreference<SwitchPreference>("banner_ads_enabled")
    private val videoAdsInterval by bindPreference<ListPreference>("video_ads_interval")
    private val profile by bindPreference<ListPreference>("profile")
    private val topten by bindPreference<ListPreference>("topten")
    private val anime by bindPreference<ListPreference>("anime")
    private val manga by bindPreference<ListPreference>("manga")
    private val comment by bindPreference<ListPreference>("comment")
    private val forum by bindPreference<ListPreference>("forum")
    private val friend by bindPreference<ListPreference>("friend")
    private val friendRequest by bindPreference<ListPreference>("friend_request")
    private val about by bindPreference<ListPreference>("about")
    private val history by bindPreference<ListPreference>("history")
    private val guestBook by bindPreference<ListPreference>("guest_book")
    private val guestBookEntry by bindPreference<ListPreference>("guest_book_entry")
    private val gallery by bindPreference<ListPreference>("gallery")
    private val article by bindPreference<ListPreference>("article")

    override fun onCreatePreferences2(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.profile_preferences)

        // Hide this setting until it are actually implemented.
        bannerAdsEnabled.isVisible = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.data.observe(viewLifecycleOwner, Observer {
            showData(it)
        })

        bannerAdsEnabled.changes<Boolean>()
            .autoDisposable(this.scope())
            .subscribe {
                val currentSettings = viewModel.data.value

                if (currentSettings != null) {
                    val newSettings = currentSettings.copy(shouldShowAds = it)

                    viewModel.update(newSettings)
                }
            }

        videoAdsInterval.changes<String>()
            .autoDisposable(this.scope())
            .subscribe {
                val currentSettings = viewModel.data.value

                if (currentSettings != null) {
                    val newSettings = currentSettings.copy(adInterval = it.toInt())

                    viewModel.update(newSettings)
                }

                updateVideoAdsIntervalSummary()
            }

        initPreference(profile) { settings, constraint -> settings.copy(profileVisibility = constraint) }
        initPreference(topten) { settings, constraint -> settings.copy(topTenVisibility = constraint) }
        initPreference(anime) { settings, constraint -> settings.copy(animeVisibility = constraint) }
        initPreference(manga) { settings, constraint -> settings.copy(mangaVisibility = constraint) }
        initPreference(comment) { settings, constraint -> settings.copy(commentVisibility = constraint) }
        initPreference(forum) { settings, constraint -> settings.copy(forumVisibility = constraint) }
        initPreference(friend) { settings, constraint -> settings.copy(friendVisibility = constraint) }
        initPreference(friendRequest) { settings, constraint -> settings.copy(friendRequestConstraint = constraint) }
        initPreference(about) { settings, constraint -> settings.copy(aboutVisibility = constraint) }
        initPreference(history) { settings, constraint -> settings.copy(historyVisibility = constraint) }
        initPreference(guestBook) { settings, constraint -> settings.copy(guestBookVisibility = constraint) }
        initPreference(guestBookEntry) { settings, constraint -> settings.copy(guestBookEntryConstraint = constraint) }
        initPreference(gallery) { settings, constraint -> settings.copy(galleryVisibility = constraint) }
        initPreference(article) { settings, constraint -> settings.copy(articleVisibility = constraint) }

        listView.isFocusable = false
    }

    override fun onDestroyView() {
        KotterKnifePreference.reset(this)

        super.onDestroyView()
    }

    private fun showData(profileSettings: LocalProfileSettings) {
        bannerAdsEnabled.isChecked = profileSettings.shouldShowAds
        videoAdsInterval.setValue(normalizeAdInterval(profileSettings.adInterval).toString())
        profile.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.profileVisibility))
        topten.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.topTenVisibility))
        anime.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.animeVisibility))
        manga.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.mangaVisibility))
        comment.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.commentVisibility))
        forum.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.forumVisibility))
        friend.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.friendVisibility))
        friendRequest.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.friendRequestConstraint))
        about.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.aboutVisibility))
        history.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.historyVisibility))
        guestBook.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.guestBookVisibility))
        guestBookEntry.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.guestBookEntryConstraint))
        gallery.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.galleryVisibility))
        article.setValue(ProxerUtils.getSafeApiEnumName(profileSettings.articleVisibility))

        updateVideoAdsIntervalSummary()
    }

    private fun initPreference(
        preference: Preference,
        copyCallback: (LocalProfileSettings, UcpSettingConstraint) -> LocalProfileSettings
    ) = preference.changes<String>()
        .autoDisposable(this.scope())
        .subscribe {
            val currentSettings = viewModel.data.value

            if (currentSettings != null) {
                val newConstraint = ProxerUtils.toSafeApiEnum<UcpSettingConstraint>(it)
                val newSettings = copyCallback(currentSettings, newConstraint)

                viewModel.update(newSettings)
            }
        }

    private fun normalizeAdInterval(source: Int): Int {
        return resources.getStringArray(R.array.profile_settings_video_ads_interval_values)
            .map { it.toInt() }
            .sortedDescending()
            .find { source >= it }
            ?: 0
    }

    private fun updateVideoAdsIntervalSummary() {
        val value = videoAdsInterval.value ?: "0"
        val index = resources.getStringArray(R.array.profile_settings_video_ads_interval_values).indexOf(value)
        val keyword = resources.getStringArray(R.array.profile_settings_video_ads_interval_titles)[index]
        val newSummary = getString(R.string.profile_preference_video_ads_summary, keyword.toLowerCase(Locale.GERMANY))

        videoAdsInterval.summary = newSummary
    }
}
