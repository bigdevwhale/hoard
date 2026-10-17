package app.hoard.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.hoard.R

/** A dragon's cave by torchlight: dark rock, gold for treasure, ember for the forge, cyan for runes. */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val gold: Color,
    val goldSoft: Color,
    val onGold: Color,
    val ember: Color,
    val emberSoft: Color,
    val rune: Color,
    val runeSoft: Color,
    val gem: Color,
    val gemSoft: Color,
    val leaf: Color,
    val danger: Color,
)

val CavePalette = Palette(
    bg = Color(0xFF130F18),
    surface = Color(0xFF1C1622),
    surface2 = Color(0xFF231B2A),
    line = Color(0xFF33293C),
    ink = Color(0xFFF3E8D6),
    muted = Color(0xFFB9AAB4),
    faint = Color(0xFF7D6F82),
    gold = Color(0xFFEDBB55),
    goldSoft = Color(0xFF3A2E17),
    onGold = Color(0xFF1C1206),
    ember = Color(0xFFFF8B4E),
    emberSoft = Color(0xFF3B2016),
    rune = Color(0xFF7FE3F0),
    runeSoft = Color(0xFF14323A),
    gem = Color(0xFFB896FF),
    gemSoft = Color(0xFF2C2148),
    leaf = Color(0xFF86D3A0),
    danger = Color(0xFFFF6B5E),
)

val LocalPalette = staticCompositionLocalOf { CavePalette }

val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
)

/** Engraved letters for names and titles. */
val Prata = FontFamily(Font(R.font.prata, FontWeight.Normal))

/** A bookish serif with plain figures, for legends (Prata's 1 reads as an l). */
val Lora = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_italic, FontWeight.Normal, androidx.compose.ui.text.font.FontStyle.Italic),
)

/** Prata text with its figures set in Lora, so "17" never reads as "l7". */
fun engraved(s: String): androidx.compose.ui.text.AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    s.forEach { ch ->
        if (ch.isDigit()) withStyle(androidx.compose.ui.text.SpanStyle(fontFamily = Lora)) { append(ch) } else append(ch)
    }
}

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(26.dp)
    val RLg = RoundedCornerShape(22.dp)
    val RMd = RoundedCornerShape(16.dp)
    val RSm = RoundedCornerShape(12.dp)
}

object UType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Prata, fontSize = 31.sp, lineHeight = 37.sp, color = U.c.ink, fontFeatureSettings = "lnum")
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Prata, fontSize = 24.sp, lineHeight = 30.sp, color = U.c.ink, fontFeatureSettings = "lnum")
    val Name: TextStyle @Composable get() = TextStyle(fontFamily = Prata, fontSize = 18.sp, lineHeight = 23.sp, color = U.c.ink, fontFeatureSettings = "lnum")
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Nunito, fontSize = 14.5.sp, lineHeight = 21.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.14.em, color = U.c.faint)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Nunito, fontSize = 15.sp, lineHeight = 22.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Nunito, fontSize = 13.sp, lineHeight = 18.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Prata, fontSize = 24.sp, color = U.c.ink, fontFeatureSettings = "lnum, tnum")
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    val Legend: TextStyle @Composable get() = TextStyle(fontFamily = Lora, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 15.sp, lineHeight = 22.sp, color = U.c.muted)
}

@Composable
fun HoardTheme(content: @Composable () -> Unit) {
    val p = CavePalette
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = p.gold,
                onPrimary = p.onGold,
                primaryContainer = p.goldSoft,
                onPrimaryContainer = p.ink,
                secondaryContainer = p.goldSoft,
                onSecondaryContainer = p.ink,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface2,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.danger,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 14.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
