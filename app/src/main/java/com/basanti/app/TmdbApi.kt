package com.basanti.app

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("language") language: String = "en-US"
    ): TmdbSearchResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbMovieDetail

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): TmdbMovieDetail
}

@Keep
data class TmdbSearchResponse(
    @SerializedName("results") val results: List<TmdbMovie>
)

@Keep
data class TmdbMovie(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?, // For TV Shows
    @SerializedName("release_date") val release_date: String?,
    @SerializedName("first_air_date") val first_air_date: String?,
    @SerializedName("poster_path") val poster_path: String?,
    @SerializedName("backdrop_path") val backdrop_path: String?,
    @SerializedName("media_type") val media_type: String,
    @SerializedName("overview") val overview: String?,
    @SerializedName("popularity") val popularity: Double?
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val displayDate: String get() = (release_date ?: first_air_date ?: "").take(4)
    val fullPosterUrl: String get() = "https://image.tmdb.org/t/p/w200$poster_path"
    val fullBackdropUrl: String get() = "https://image.tmdb.org/t/p/w500$backdrop_path"
}

@Keep
data class TmdbMovieDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val poster_path: String?,
    @SerializedName("backdrop_path") val backdrop_path: String?,
    @SerializedName("release_date") val release_date: String?,
    @SerializedName("first_air_date") val first_air_date: String?,
    @SerializedName("vote_average") val vote_average: Double?
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val fullPosterUrl: String get() = "https://image.tmdb.org/t/p/w200$poster_path"
    val fullBackdropUrl: String get() = "https://image.tmdb.org/t/p/w500$backdrop_path"
}
