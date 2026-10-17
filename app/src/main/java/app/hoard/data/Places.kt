package app.hoard.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import app.hoard.domain.Sky
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

/** Where and under what sky a moment happened: the runes on the back of a coin. */
class Runes(val place: String = "", val lat: Double? = null, val lon: Double? = null, val sky: Sky = Sky.UNKNOWN, val tempC: Int? = null)

/**
 * The phone's last known position (no Google services needed), the town's name from the system
 * geocoder, and the weather from Open-Meteo. All of it optional: no permission or no network just
 * means fewer runes.
 */
object Places {
    fun allowed(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun lastKnown(context: Context): Location? {
        if (!allowed(context)) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        return runCatching {
            lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    suspend fun name(context: Context, lat: Double, lon: Double, locale: Locale): String = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext ""
        runCatching {
            val a = Geocoder(context, locale).getFromLocation(lat, lon, 1)?.firstOrNull() ?: return@runCatching ""
            a.locality ?: a.subAdminArea ?: a.adminArea ?: a.countryName ?: ""
        }.getOrDefault("")
    }

    suspend fun weather(lat: Double, lon: Double): Pair<Sky, Int?> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code".format(Locale.US, lat, lon))
            val c = url.openConnection() as HttpURLConnection
            c.connectTimeout = 6000
            c.readTimeout = 6000
            try {
                val cur = JSONObject(c.inputStream.bufferedReader().readText()).getJSONObject("current")
                Sky.ofWmo(cur.getInt("weather_code")) to cur.optDouble("temperature_2m").takeIf { !it.isNaN() }?.roundToInt()
            } finally { c.disconnect() }
        }.getOrDefault(Sky.UNKNOWN to null)
    }

    /** Everything we can find out right now, within a few seconds. */
    suspend fun now(context: Context, locale: Locale, withWeather: Boolean = true): Runes {
        val loc = lastKnown(context) ?: return Runes()
        return at(context, loc.latitude, loc.longitude, locale, withWeather)
    }

    suspend fun at(context: Context, lat: Double, lon: Double, locale: Locale, withWeather: Boolean): Runes =
        withTimeoutOrNull(9000) {
            val place = name(context, lat, lon, locale)
            val (sky, t) = if (withWeather) weather(lat, lon) else Sky.UNKNOWN to null
            Runes(place, lat, lon, sky, t)
        } ?: Runes(lat = lat, lon = lon)
}
