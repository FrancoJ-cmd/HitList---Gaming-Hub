# HitList -- Gamer Hub

KMP Desktop app that ranks Steam's most-played games in real time by a composite score of live concurrent players + community rating. Built with Compose Multiplatform and Clean Architecture (MVVM).

---

## Features

- **Real-time ranking:** Steam's live most-played list, scored by concurrent players + community review ratio. The ranking re-polls on Steam's own cadence (anchored to the feed's `last_update`) and re-orders with animation as games climb.
- **Trending detection:** games that climbed positions since the last refresh are marked 🔥
- **Genre filters:** filter the ranking in-memory without re-fetching
- **Game detail:** screenshots carousel, Metacritic score, live player count, CheapShark deals
- **Game news:** official Steam news feed per game (patch notes, updates, dev blogs)
- **General gaming news:** NewsAPI feed for general gaming headlines
- **Offline support:** all data cached locally with TTL-aware invalidation; stale data is shown with a warning banner instead of an error screen

## Data Sources

| API | Used for                           | Key required |
|---|------------------------------------|---|
| [Steam Charts (ISteamChartsService)](https://api.steampowered.com/ISteamChartsService/GetGamesByConcurrentPlayers/v1/) | Live most-played ranking: membership, order, concurrent players (1 call) | No |
| [SteamSpy](https://steamspy.com/api.php) | Bulk ranking metadata (name, genres, reviews) + per-app fallback | No |
| [Steam Web API](https://api.steampowered.com) | Live concurrent player count (detail screen) | No |
| [Steam Store API](https://store.steampowered.com/api) | Game metadata, screenshots, genres | No |
| [Steam Reviews](https://store.steampowered.com/appreviews) | Community review score             | No |
| [CheapShark](https://apidocs.cheapshark.com) | Game deals by store                | No |
| [Steam News (ISteamNews)](https://api.steampowered.com/ISteamNews) | Official game news feed            | No |
| [NewsAPI](https://newsapi.org) | General gaming headlines           | **Yes (free)** |

## Score Formula

```
score = 0.6 × (concurrentPlayers / maxPlayersInDataset) + 0.4 × positiveRatio
```

`concurrentPlayers` is the live value from the Steam Charts feed; `positiveRatio` comes from the SteamSpy metadata. Games with fewer than 50 reviews are excluded from the ranking. Ties preserve the live Steam order.

## Refresh & Cache TTLs

The ranking polls the live feed on Steam's cadence: each fetch reads the feed's `last_update` and schedules the next poll just after Steam is expected to refresh (period ≈ 60s + jitter, floored at 20s). Slow metadata is fetched once and reused across polls.

| Source | TTL |
|---|---|
| Live ranking (concurrent players) | 60 seconds |
| Ranking metadata (SteamSpy bulk) | 24 hours |
| Steam reviews | 6 hours |
| Game metadata | 24 hours |
| CheapShark deals | 1 hour |
| News (Steam + NewsAPI) | 30 minutes |

---

## Architecture

Clean Architecture (MVVM) with dependencies pointing inward: **presentation → domain → data**.

- **presentation** — Compose screens + `ViewModel`s exposing a `StateFlow<UiState>`.
- **domain** — entities, repository interfaces, use cases, and `RankingCalculator` (score, trending, review-score description). No framework or network types.
- **data** — repository implementations, remote `*Source` proxies (Ktor), and the disk cache (`LocalDataSource`).

### Real-time ranking data flow

```
SteamChartsProxy (LiveRankingSource)        SteamSpyProxy (RankingMetadataSource)
  GetGamesByConcurrentPlayers                 top100in2weeks  (bulk, 24h cache)
  → order + concurrent players + last_update  appdetails      (per-app fallback)
                    \                         /
                     v                       v
            GameRepositoryImpl.observeRankedGames(): Flow
              · join live players + metadata by appId
              · composite score, drop games < 50 reviews
              · mark 🔥 trending vs the previous emission
              · schedule next poll anchored to last_update
                                  |
                                  v
            GetRankedGamesUseCase.observe()  →  RankingViewModel
                                  |                 (collects, applies genre filter)
                                  v
                       RankingScreen (LazyColumn + animateItem)
```

The ranking is a cold `Flow` polled in a loop: the membership, order and live player counts come from a single Steam Charts call, while the slower name/genre/review metadata is fetched once from SteamSpy (with a per-app fallback for games not in the bulk list) and reused across polls. Each poll anchors the next wake-up to the feed's `last_update` instead of a blind timer, so it stays in phase with Steam and avoids redundant fetches.

## Tech Stack

| | |
|---|---|
| **Language** | Kotlin |
| **UI** | Compose Multiplatform (Desktop) |
| **HTTP** | Ktor + OkHttp |
| **Serialization** | kotlinx.serialization |
| **Local cache** | JSON files on disk (`./cache/`, relative to the working dir) |
| **DI** | Manual (object injector) |
| **Testing** | kotlin.test + MockK + kotlinx-coroutines-test |

---

## Build & Run

Requires JDK 21 (the build pins `jvmToolchain(21)`).

```bash
# Run the desktop app
./gradlew :composeApp:run

# Run the test suite
./gradlew :composeApp:desktopTest
```

All ranking data sources work without an API key. Only the **general gaming news** feed needs a free [NewsAPI](https://newsapi.org) key — without it every other feature still works. To enable it, add the key to `local.properties` at the repo root:

```properties
NEWS_API_KEY=your_key_here
```