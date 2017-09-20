package me.proxer.app.manga.local;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

import io.reactivex.annotations.NonNull;
import me.proxer.library.entity.info.AdaptionInfo;
import me.proxer.library.entity.info.EntryCore;
import me.proxer.library.enums.Category;
import me.proxer.library.enums.FskConstraint;
import me.proxer.library.enums.Genre;
import me.proxer.library.enums.License;
import me.proxer.library.enums.MediaState;
import me.proxer.library.enums.Medium;

/**
 * @author Ruben Gees
 */
@Entity(tableName = "entries")
public class LocalEntryCore {

    @PrimaryKey(autoGenerate = true)
    private final long id;

    @NotNull
    private final String name;

    @NotNull
    private final Set<Genre> genres;

    @NotNull
    private final Set<FskConstraint> fskConstraints;

    @NotNull
    private final String description;

    @NotNull
    private final Medium medium;

    private final int episodeAmount;

    @NotNull
    private final MediaState state;

    private final int ratingSum;
    private final int ratingAmount;
    private final int clicks;

    @NotNull
    private final Category category;

    @NotNull
    private final License license;

    @Embedded(prefix = "adaption_")
    @NotNull
    private final AdaptionInfo adaptionInfo;

    public LocalEntryCore(long id, @NotNull String name, @NotNull Set<Genre> genres,
                          @NotNull Set<FskConstraint> fskConstraints, @NotNull String description,
                          @NotNull Medium medium, int episodeAmount, @NotNull MediaState state, int ratingSum,
                          int ratingAmount, int clicks, @NotNull Category category, @NotNull License license,
                          @NonNull AdaptionInfo adaptionInfo) {
        this.id = id;
        this.name = name;
        this.genres = genres;
        this.fskConstraints = fskConstraints;
        this.description = description;
        this.medium = medium;
        this.episodeAmount = episodeAmount;
        this.state = state;
        this.ratingSum = ratingSum;
        this.ratingAmount = ratingAmount;
        this.clicks = clicks;
        this.category = category;
        this.license = license;
        this.adaptionInfo = adaptionInfo;
    }

    @NotNull
    public EntryCore toNonLocalEntryCore() {
        return new EntryCore(String.valueOf(id), name, genres, fskConstraints, description, medium, episodeAmount,
                state, ratingSum, ratingAmount, clicks, category, license, adaptionInfo);
    }

    public long getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public Set<Genre> getGenres() {
        return genres;
    }

    @NotNull
    public Set<FskConstraint> getFskConstraints() {
        return fskConstraints;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    public Medium getMedium() {
        return medium;
    }

    public int getEpisodeAmount() {
        return episodeAmount;
    }

    @NotNull
    public MediaState getState() {
        return state;
    }

    public int getRatingSum() {
        return ratingSum;
    }

    public int getRatingAmount() {
        return ratingAmount;
    }

    public int getClicks() {
        return clicks;
    }

    @NotNull
    public Category getCategory() {
        return category;
    }

    @NotNull
    public License getLicense() {
        return license;
    }

    @NotNull
    public AdaptionInfo getAdaptionInfo() {
        return adaptionInfo;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalEntryCore that = (LocalEntryCore) o;

        if (id != that.id) return false;
        if (episodeAmount != that.episodeAmount) return false;
        if (ratingSum != that.ratingSum) return false;
        if (ratingAmount != that.ratingAmount) return false;
        if (clicks != that.clicks) return false;
        if (!name.equals(that.name)) return false;
        if (!genres.equals(that.genres)) return false;
        if (!fskConstraints.equals(that.fskConstraints)) return false;
        if (!description.equals(that.description)) return false;
        if (medium != that.medium) return false;
        if (state != that.state) return false;
        if (category != that.category) return false;
        if (license != that.license) return false;
        return adaptionInfo.equals(that.adaptionInfo);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + name.hashCode();
        result = 31 * result + genres.hashCode();
        result = 31 * result + fskConstraints.hashCode();
        result = 31 * result + description.hashCode();
        result = 31 * result + medium.hashCode();
        result = 31 * result + episodeAmount;
        result = 31 * result + state.hashCode();
        result = 31 * result + ratingSum;
        result = 31 * result + ratingAmount;
        result = 31 * result + clicks;
        result = 31 * result + category.hashCode();
        result = 31 * result + license.hashCode();
        result = 31 * result + adaptionInfo.hashCode();
        return result;
    }
}
