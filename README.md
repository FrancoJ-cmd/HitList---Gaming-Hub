# HitList -- Gamer Hub

KMP Desktop app that ranks the top 100 Steam games by a composite score of live player count + community rating. Built with Compose Multiplatform and Clean Architecture (MVVM).

---

## Features
vol
- **Live ranking:** top 100 Steam games scored by concurrent players + community review ratio
- **Trending detection:** games that climbed positions since the last refresh are marked 🔥
- **Genre filters:** filter the ranking in-memory without re-fetching
- **Game detail:** screenshots carousel, Metacritic score, live player count, CheapShark deals
- **Game news:** official Steam news feed per game (patch notes, updates, dev blogs)
- **General gaming news:** NewsAPI feed for general gaming headlines
- **Offline support:** all data cached locally with TTL-aware invalidation; stale data is shown with a warning banner instead of an error screen

## Data Sources

| API | Used for                           | Key required |
|---|------------------------------------|---|
| [SteamSpy](https://steamspy.com/api.php) | Daily seed, list of top 100 AppIDs | No |
| [Steam Web API](https://api.steampowered.com) | Live concurrent player count       | No |
| [Steam Store API](https://store.steampowered.com/api) | Game metadata, screenshots, genres | No |
| [Steam Reviews](https://store.steampowered.com/appreviews) | Community review score             | No |
| [CheapShark](https://apidocs.cheapshark.com) | Game deals by store                | No |
| [Steam News (ISteamNews)](https://api.steampowered.com/ISteamNews) | Official game news feed            | No |
| [NewsAPI](https://newsapi.org) | General gaming headlines           | **Yes (free)** |

## Score Formula

```
score = 0.6 × (currentPlayers / maxPlayersInDataset) + 0.4 × positiveRatio
```

Games with fewer than 50 reviews are excluded from the ranking. Ties preserve the original SteamSpy order.

## Cache TTLs

| Source | TTL |
|---|---|
| SteamSpy seed list | 24 hours |
| Live player counts | 5 minutes |
| Steam reviews | 6 hours |
| Game metadata | 24 hours |
| CheapShark deals | 1 hour |
| News (Steam + NewsAPI) | 30 minutes |

---

## Tech Stack

| | |
|---|---|
| **Language** | Kotlin |
| **UI** | Compose Multiplatform (Desktop) |
| **HTTP** | Ktor + OkHttp |
| **Serialization** | kotlinx.serialization |
| **Local cache** | JSON files on disk (`~/.hitlist/cache/`) |
| **DI** | Manual (object injector) |
| **Testing** | kotlin.test + MockK + kotlinx-coroutines-test |