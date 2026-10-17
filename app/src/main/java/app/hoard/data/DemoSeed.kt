package app.hoard.data

import android.content.Context
import app.hoard.AppContainer
import app.hoard.domain.Cut
import app.hoard.domain.Feature
import app.hoard.domain.Hall
import app.hoard.domain.Hue
import app.hoard.domain.Kind
import app.hoard.domain.Material
import app.hoard.domain.Metal
import app.hoard.domain.Quests
import app.hoard.domain.RelicForm
import app.hoard.domain.Sky
import app.hoard.domain.Stage
import app.hoard.domain.Stone
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * A made-up autumn in Prague for the demo build (screenshots): a crimson dragon, a dog called Bruno,
 * a hike, a birthday, two treasures still in the forge and one sealed for a year. Photos come from
 * the demo assets, which only the demo build carries.
 */
object DemoSeed {
    private class D(
        val photo: String, val kind: Kind, val hall: Hall, val days: Int, val hour: Int,
        val en: String, val ru: String,
        val metal: Metal = Metal.GOLD, val stone: Stone = Stone.RUBY, val cut: Cut = Cut.ROUND, val relic: RelicForm = RelicForm.CROWN,
        val sky: Sky = Sky.CLEAR, val temp: Int? = null, val noteEn: String = "", val noteRu: String = "",
        val placeEn: String = "Prague", val placeRu: String = "Прага", val fav: Boolean = false, val quest: Boolean = false,
        val video: Boolean = false, val forge: Boolean = false, val sealDays: Int = 0,
    )

    private val all = listOf(
        D("237", Kind.COIN, Hall.BEASTS, 70, 10, "Bruno's first day", "Первый день Бруно", sky = Sky.CLEAR, temp = 24, fav = true,
            noteEn = "He fell asleep on my shoe before we even left the shelter.", noteRu = "Уснул у меня на ботинке, мы даже не успели выйти из приюта."),
        D("63", Kind.COIN, Hall.HEARTH, 66, 8, "Morning in red", "Утро в красном", metal = Metal.COPPER, sky = Sky.CLOUDS, temp = 18),
        D("288", Kind.RELIC, Hall.TRAVELS, 62, 18, "The key to Prague", "Ключ от Праги", relic = RelicForm.KEY, sky = Sky.CLEAR, temp = 22, fav = true,
            noteEn = "First evening in the new city. Everything is uphill and everything is beautiful.", noteRu = "Первый вечер в новом городе. Всё в гору, и всё красиво."),
        D("129", Kind.GEM, Hall.KIN, 60, 19, "Us, at sunset", "Мы на закате", stone = Stone.RUBY, cut = Cut.HEART, sky = Sky.CLEAR, temp = 21, fav = true),
        D("75", Kind.COIN, Hall.FEASTS, 55, 13, "Grapes from Mělník", "Виноград из Мельника", sky = Sky.CLEAR, temp = 26, placeEn = "Mělník", placeRu = "Мельник"),
        D("174", Kind.GEM, Hall.WONDERS, 52, 6, "Balloon at dawn", "Шар на рассвете", stone = Stone.TOPAZ, cut = Cut.PEAR, sky = Sky.CLEAR, temp = 12, quest = true),
        D("38", Kind.COIN, Hall.SKIES, 50, 16, "A cloud like a dragon", "Облако-дракон", metal = Metal.SILVER, sky = Sky.CLOUDS, temp = 20),
        D("177", Kind.RELIC, Hall.DEEDS, 47, 11, "Up Sněžka", "На Снежку", relic = RelicForm.SWORD, metal = Metal.SILVER, sky = Sky.FOG, temp = 7,
            placeEn = "Sněžka", placeRu = "Снежка", noteEn = "1,603 m. My legs gave up at the top; I didn't.", noteRu = "1603 метра. Ноги сдались на вершине, я — нет."),
        D("64", Kind.GEM, Hall.KIN, 45, 17, "Anya and the meadow", "Аня и луг", stone = Stone.EMERALD, cut = Cut.OVAL, sky = Sky.CLEAR, temp = 23),
        D("40", Kind.COIN, Hall.BEASTS, 43, 22, "The neighbour's cat", "Соседский кот", sky = Sky.CLEAR, temp = 15),
        D("152", Kind.COIN, Hall.WONDERS, 40, 12, "Petunias on the sill", "Петунии на подоконнике", metal = Metal.ROSE, sky = Sky.CLOUDS, temp = 19),
        D("195", Kind.GEM, Hall.FEASTS, 38, 21, "Náplavka at night", "Наплавка ночью", stone = Stone.AMETHYST, cut = Cut.CUSHION, sky = Sky.CLEAR, temp = 17),
        D("225", Kind.COIN, Hall.HEARTH, 36, 9, "Rainy-day tea", "Чай на дождь", metal = Metal.COPPER, sky = Sky.RAIN, temp = 13),
        D("164", Kind.COIN, Hall.TRAVELS, 33, 15, "Český Krumlov", "Чески-Крумлов", metal = Metal.SILVER, sky = Sky.CLOUDS, temp = 18, placeEn = "Český Krumlov", placeRu = "Чески-Крумлов"),
        D("54", Kind.COIN, Hall.SKIES, 31, 14, "September sky", "Сентябрьское небо", sky = Sky.CLEAR, temp = 20),
        D("102", Kind.RELIC, Hall.FEASTS, 29, 18, "Mum's birthday", "День рождения мамы", relic = RelicForm.CHALICE, sky = Sky.CLEAR, temp = 18, fav = true,
            noteEn = "Raspberries from her own garden. She said the cake was 'fine'. She had three slices.", noteRu = "Малина с её огорода. Сказала, что торт «нормальный». Съела три куска."),
        D("24", Kind.COIN, Hall.HEARTH, 27, 23, "One more chapter", "Ещё одна глава", metal = Metal.COPPER, sky = Sky.RAIN, temp = 11),
        D("1025", Kind.GEM, Hall.BEASTS, 25, 8, "Bruno under the blanket", "Бруно под пледом", stone = Stone.SAPPHIRE, cut = Cut.ROUND, sky = Sky.RAIN, temp = 10),
        D("167", Kind.COIN, Hall.WONDERS, 22, 16, "First yellow leaves", "Первые жёлтые листья", sky = Sky.CLOUDS, temp = 14, quest = true),
        D("142", Kind.COIN, Hall.TRAVELS, 20, 13, "Hluboká castle", "Замок Глубока", metal = Metal.SILVER, sky = Sky.CLEAR, temp = 16, placeEn = "Hluboká", placeRu = "Глубока"),
        D("158", Kind.GEM, Hall.DEEDS, 18, 22, "Front row", "Первый ряд", stone = Stone.RUBY, cut = Cut.ROUND, sky = Sky.CLOUDS, temp = 12),
        D("65", Kind.COIN, Hall.KIN, 16, 19, "Golden hour", "Золотой час", metal = Metal.ROSE, sky = Sky.CLEAR, temp = 15),
        D("292", Kind.COIN, Hall.FEASTS, 14, 19, "Borscht, attempt two", "Борщ, попытка вторая", metal = Metal.COPPER, sky = Sky.RAIN, temp = 9),
        D("176", Kind.GEM, Hall.TRAVELS, 12, 19, "Last swim of the year", "Последнее купание года", stone = Stone.AMETHYST, cut = Cut.OVAL, sky = Sky.CLEAR, temp = 19, placeEn = "Rovinj", placeRu = "Ровинь"),
        D("239", Kind.COIN, Hall.WONDERS, 10, 15, "Make a wish", "Загадай желание", metal = Metal.MITHRIL, sky = Sky.CLOUDS, temp = 13, quest = true),
        D("301", Kind.COIN, Hall.TRAVELS, 8, 11, "Stromovka in October", "Стромовка в октябре", sky = Sky.FOG, temp = 8),
        D("146", Kind.RELIC, Hall.KIN, 7, 12, "Grandpa's tricycle", "Дедушкин велосипед", relic = RelicForm.AMULET, metal = Metal.ROSE, sky = Sky.CLOUDS, temp = 10,
            noteEn = "Grandpa painted it red for me in 1996. Found it in the shed, still red.", noteRu = "Дед покрасил его мне в красный в 1996-м. Нашёлся в сарае, всё ещё красный."),
        D("250", Kind.COIN, Hall.HEARTH, 6, 20, "Dad's old Zenit", "Папин старый «Зенит»", metal = Metal.SILVER, sky = Sky.RAIN, temp = 9),
        D("30", Kind.COIN, Hall.HEARTH, 5, 8, "Monday", "Понедельник", metal = Metal.COPPER, sky = Sky.FOG, temp = 6, quest = true),
        D("169", Kind.COIN, Hall.BEASTS, 4, 17, "Bruno vs. the meadow", "Бруно против луга", sky = Sky.CLEAR, temp = 12),
        D("1080", Kind.GEM, Hall.FEASTS, 3, 13, "The last strawberries", "Последняя клубника", stone = Stone.RUBY, cut = Cut.HEART, sky = Sky.CLOUDS, temp = 11),
        D("clip_rain", Kind.MIRROR, Hall.SKIES, 2, 21, "Rain on the window", "Дождь по стеклу", metal = Metal.SILVER, sky = Sky.RAIN, temp = 8, video = true),
        D("76", Kind.COIN, Hall.TRAVELS, 2, 11, "Bike ride to Roztoky", "На велике в Розтоки", metal = Metal.COPPER, sky = Sky.CLOUDS, temp = 10, placeEn = "Roztoky", placeRu = "Розтоки"),
        D("clip_bruno", Kind.MIRROR, Hall.BEASTS, 1, 18, "Bruno hears the word 'walk'", "Бруно слышит слово «гулять»", sky = Sky.CLOUDS, temp = 9, video = true, fav = true),
        D("175", Kind.COIN, Hall.WONDERS, 1, 22, "For me, a year from now", "Мне через год", sky = Sky.CLEAR, temp = 7, sealDays = 365,
            noteEn = "Did you keep going to the mountains? Is Bruno still afraid of the hoover?", noteRu = "Ты всё ещё ходишь в горы? Бруно всё так же боится пылесоса?"),
        D("306", Kind.GEM, Hall.WONDERS, 0, 0, "", "", stone = Stone.MOONSTONE, cut = Cut.CUSHION, forge = true),
        D("312", Kind.COIN, Hall.FEASTS, 0, 0, "", "", forge = true),
    )

    /** [open] reads a demo file by its asset path (the screen renders read them from disk instead). */
    suspend fun seedIfEmpty(context: Context, c: AppContainer, open: (String) -> java.io.InputStream = { context.assets.open(it) }) {
        val dao = c.db.dao()
        if (dao.all().isNotEmpty()) return
        val ru = context.resources.configuration.locales[0].language == "ru"
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)
        val claimed = mutableSetOf<String>()
        all.forEachIndexed { i, d ->
            val ext = if (d.video) "mp4" else "jpg"
            val tmp = File(context.cacheDir, "demo-$i.$ext")
            val ok = runCatching { open("demo/${d.photo}.$ext").use { a -> tmp.outputStream().use { a.copyTo(it) } } }.isSuccess
            if (!ok) return@forEachIndexed
            val at = if (d.forge) now - (90 - i) * 60_000L
            else today.minusDays(d.days.toLong()).atTime(d.hour, (i * 7) % 60).atZone(zone).toInstant().toEpochMilli()
            val reveal = if (d.forge) today.plusDays(1).atTime(7, 0).atZone(zone).toInstant().toEpochMilli() else at + 9 * 3600_000L
            val duration = if (d.video) c.vault.durationMs(tmp) else 0L
            val media = c.vault.keep(tmp, d.video)
            val quest = if (d.quest) Quests.active(today.minusDays(d.days.toLong()).toEpochDay()).key.also { claimed += it } else ""
            dao.insert(
                Treasure(
                    kind = d.kind.name, title = if (ru) d.ru else d.en, note = if (ru) d.noteRu else d.noteEn, hall = d.hall.name,
                    metal = d.metal.name, stone = d.stone.name, cut = d.cut.name, relic = d.relic.name,
                    media = media, video = d.video, durationMs = duration, takenAt = at, momentAt = at, revealAt = reveal,
                    seen = !d.forge, place = if (d.forge) "" else if (ru) d.placeRu else d.placeEn, lat = 50.087, lon = 14.421,
                    sky = d.sky.name, tempC = d.temp, quest = quest,
                    sealedUntil = if (d.sealDays > 0) today.plusDays(d.sealDays.toLong()).atStartOfDay(zone).toInstant().toEpochMilli() else null,
                    seed = i * 7919 + 101, favorite = d.fav,
                )
            )
        }
        c.settings.update {
            it.copy(
                onboarded = true,
                dragonName = if (ru) "Уголёк" else "Cinder",
                hue = Hue.CRIMSON,
                claimed = claimed,
                unlocked = Material.rewards.take(claimed.size).map { m -> m.key }.toSet(),
                seen = Stage.entries.map { s -> "stage:" + s.name }.toSet() + Feature.entries.map { f -> "feat:" + f.name },
            )
        }
    }
}
