<div align="center">

<img src="docs/logo.png" width="96" alt="Hoard logo" />

# Hoard

### A dragon's hoard of your moments

*Photos and short clips, struck into pixel-art treasure. A coin for an everyday moment, a gem for something special, a relic for a milestone, a mirror for a clip. Free, no ads, no account.*

[![Android CI](https://github.com/bigdevwhale/hoard/actions/workflows/android.yml/badge.svg)](https://github.com/bigdevwhale/hoard/actions/workflows/android.yml)
[![Release](https://img.shields.io/github/v/release/bigdevwhale/hoard?color=EDBB55&label=download&logo=android)](https://github.com/bigdevwhale/hoard/releases/latest)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-130F18?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Languages](https://img.shields.io/badge/lang-EN%20%7C%20RU-EDBB55)
[![License: MIT](https://img.shields.io/badge/license-MIT-130F18)](LICENSE)

**English** · [Русский](README.ru.md) · [Changelog](CHANGELOG.md)

<img src="docs/screenshots/overview.png" alt="Hoard on a phone: the cave with the sleeping dragon, a coin struck from a photo, a ruby heart, the halls and the chronicle" width="100%" />

</div>

---

## The idea

You are a dragon, and a dragon keeps a hoard. Yours is made of moments. Every day you carry a few of them into your cave: the camera catches the moment, and the forge turns it into treasure. The pile grows, the cave fills with new things, and the dragon sleeps on it, growing bigger with every year.

It's a camera roll that makes you choose. **Three moments a day** (you can change it) and **the forge cools overnight**: what you put in tonight you'll see in the morning. That keeps you living the moment instead of looking at it.

## The treasures

Everything is pixel art painted by the phone itself from your photo, so no two treasures are alike:

| | Treasure | For | How it's made |
|---|---|---|---|
| 🪙 | **Coin** | an everyday moment | the photo is struck in relief in gold, silver, copper or rarer metals; the back carries the hall's sign, the runes of the day and the year |
| 💎 | **Gem** | something special | the photo is caught inside a cut stone: clear in the table, broken up in the facets. Tilt the phone and the light moves |
| 👑 | **Relic** | a milestone | a crown, chalice, sword, amulet, horn, key, ring or lantern, with the moment set in a medallion |
| 🪞 | **Mirror** | a clip (up to 5 s, or 30 s from the gallery) | an oval wall mirror; the clip plays inside the glass |

## What's inside

| | Feature | How it works |
|---|---|---|
| 🔥 | **The forge** | A photo or a clip up to five seconds with the phone's own camera, or one from the gallery (it keeps its own date). Choose what it becomes, the hall, a name and a legend. It cools at 7:00 (or when you say); one quiet notice then. |
| 🐉 | **The dragon** | Name it, pick its colour. It sleeps on the pile and grows from a hatchling to an elder wyrm. Tap it: it stirs and rolls out an old treasure, first of all one from this day a year ago. |
| 🏰 | **The cave** | Glowing mushrooms, stalactites, a crystal vein, banners, an underground pool, bats, a fallen knight, a crack to the sky: each appears as the hoard grows. |
| 🏛️ | **Eight halls** | Travels, Kin, Feasts, Beasts, Skies, Hearth, Deeds, Wonders, with a filter for favourites. |
| ᚱ | **Runes** | Place, weather and time of day struck into every treasure (rough location + Open-Meteo, optional). |
| 🍄 | **The gnomes' errands** | Every three days the gnomes ask for something: "something silver", "a door you'd like to open". Bring it and they pay in materials: rose gold, mithril, dragonglass, star-metal, amethyst, topaz, diamond, moonstone. |
| 🔒 | **Sealed chests** | Lock a treasure away for a month, a year or until a date. Even you can't look until its day. |
| 📜 | **The chronicle** | Your year by the dragon's calendar (Snowfast … Leaffall … Longnight), and the whole year as a printable PDF book. |
| 💾 | **Backup** | One zip with every photo and clip. Everything else stays on the phone. |

## Privacy

No account, no analytics, no ads. Photos and clips are kept in the app's own storage, not in the shared gallery. The only network request is to [Open-Meteo](https://open-meteo.com) for the weather rune, and only if runes are on.

## Build

```bash
./gradlew assembleDebug                 # app.hoard.dev, next to a real install
./gradlew assembleDebug -Pdemo          # app.hoard.demo with a made-up autumn in Prague
./gradlew testDebugUnitTest -Prender    # paints the art and the screens to app/build/art
```

Kotlin, Jetpack Compose, Room, DataStore, Media3. Photos and clips come from the phone's own camera app, so Hoard needs no camera permission. All the art is generated at runtime from shaded shapes (`ui/pix`).

Demo photos are from [Unsplash](https://unsplash.com) via [Lorem Picsum](https://picsum.photos), used under the Unsplash License, and ship only in the demo build.

## License

[MIT](LICENSE) · part of [Stillcraft](https://bigdevwhale.github.io/stillcraft/)
