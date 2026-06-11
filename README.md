# HitList - Gamer Hub

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

Clean Architecture (MVVM) with dependencies pointing inward: **presentation → domain ← data**. The codebase is organised by feature, not by layer.

```
com.hitlist/
├── common/
│   ├── domain/         AppError, AppResult
│   ├── data/           CachePolicy, cache source interfaces, LocalDataSourceImpl, ErrorMapper
│   └── presentation/   Theme, UiState, shared composables (ErrorScreen, LoadingIndicator)
├── ranking/
│   ├── domain/         RankedGame, RankingRepository, GetRankedGamesUseCase
│   ├── data/           RankingRepositoryImpl, CombinedRankingSource(Impl), LiveRankingSource,
│   │                   RankingMetadataSource, steamcharts/, steamspy/
│   └── presentation/   RankingViewModel, RankingScreen, RankingUiState
├── detail/
│   ├── domain/         GameDetail, Deal, GameDetailRepository, GetGameDetailUseCase
│   ├── data/           GameDetailRepositoryImpl, source interfaces,
│   │                   steamstore/, steamweb/, cheapshark/
│   └── presentation/   DetailViewModel, DetailScreen, DetailUiState
├── news/
│   ├── domain/         NewsArticle, NewsRepository, GetGameNewsUseCase, GetGeneralNewsUseCase
│   ├── data/           NewsRepositoryImpl, GameNewsSource, GeneralNewsSource,
│   │                   steamnews/, newsapi/
│   └── presentation/   NewsViewModel, NewsScreen, NewsUiState
├── navigation/         AppNavGraph
└── di/                 HitListDependencyInjector (manual service locator)
```

Each feature's layers follow the same rules: presentation depends only on its own domain and `common`, data depends only on its own domain and `common`, domain depends only on `common.domain`.

### Real-time ranking data flow

```
SteamChartsProxy (LiveRankingSource)        SteamSpyProxy (RankingMetadataSource)
  GetGamesByConcurrentPlayers                 top100in2weeks  (bulk, 24h cache)
  → order + concurrent players + last_update  appdetails      (per-app fallback)
                    \                         /
                     v                       v
            RankingRepositoryImpl.observeRankedGames(): Flow
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

The ranking is a cold `Flow` polled in a loop: membership, order, and live player counts come from a single Steam Charts call, while the slower name/genre/review metadata is fetched once from SteamSpy (with a per-app fallback for games not in the bulk list) and reused across polls. Each poll anchors the next wake-up to the feed's `last_update` instead of a blind timer, so it stays in phase with Steam and avoids redundant fetches.

## Domain Design Decisions

The domain is split into three independent objects. Each one earned that status by having its own lifecycle, its own screen, its own repository contract, and its own use case. Objects that failed any of those tests were either absorbed into another domain object or deleted.

---

### RankedGame

**What it is:** a game as it appears in the real-time ranking — its current concurrent player count, a composite score, and a trending flag.

**Why it is a domain object:** the ranking has a distinct, continuous lifecycle (live polling on Steam's cadence) that is completely independent from any other screen or user action. The data it needs (player counts from Steam Charts, bulk metadata from SteamSpy) differs from what the detail screen needs, and so does its cache TTL (60 seconds vs. 24 hours). Merging it with `GameDetail` would force the detail screen to depend on live polling infrastructure it does not need.

**What that implies in code:**
- `ranking/domain/RankedGame.kt` — the entity
- `ranking/domain/RankingRepository.kt` — contract: `observeRankedGames(): Flow<...>`
- `ranking/domain/GetRankedGamesUseCase.kt` — `observe(): Flow<...>`
- `ranking/data/RankingRepositoryImpl.kt` — joins the two remote sources, scores, marks trending, polls
- `ranking/presentation/RankingViewModel.kt` + `RankingScreen.kt`

**Scoring logic:** the composite score formula and the trending/review-description helpers are private members of `RankingRepositoryImpl`, not a separate class. They were originally a standalone `RankingCalculator` utility, but that class had only one caller and no domain meaning of its own (it was a bag of static functions). Inlining it into the repository removes a layer of indirection that added no value.

---

### GameDetail

**What it is:** the full profile of a specific game — static metadata, screenshots, Metacritic score, live player count, community reviews, and current store deals.

**Why it is a domain object:** a user navigates to a game's detail page on demand; the data is fetched once and cached for 24 hours. This is a pull-on-demand lifecycle, entirely different from the live-polled ranking. It also draws from a completely different set of sources (Steam Store, Steam Web API, Steam Reviews, CheapShark) that the ranking never touches.

**What that implies in code:**
- `detail/domain/GameDetail.kt` — the entity
- `detail/domain/Deal.kt` — value object embedded in `GameDetail` (see below)
- `detail/domain/GameDetailRepository.kt` — contract: `getGameDetail(appId, name): AppResult<GameDetail>`
- `detail/domain/GetGameDetailUseCase.kt` — `execute(appId, name): AppResult<GameDetail>`
- `detail/data/GameDetailRepositoryImpl.kt` — assembles the full detail from four remote sources in parallel
- `detail/presentation/DetailViewModel.kt` + `DetailScreen.kt`

---

### Deal

**What it is:** a store deal for a game — store name, price, discount percentage, and a link.

**Why it is NOT a domain object:** it has no lifecycle of its own. Deals are always fetched as part of assembling a `GameDetail` and are never requested independently. There is no screen, no use case, and no repository that exists solely for deals. Making it a first-class domain object would add `DealsRepository` and `GetDealsUseCase` with a single caller each — pointless indirection.

**What that implies in code:** `Deal` is a plain data class inside `detail/domain/` and a field on `GameDetail`. When CheapShark is unavailable, `deals` is an empty list; the rest of the detail loads normally. A `DealsRepository` and `GetGameDealsUseCase` existed in an earlier version of the codebase and were deleted as dead code.

---

### NewsArticle

**What it is:** a news article — title, source, URL, and publication date. It can come from Steam's official news feed for a specific game or from NewsAPI's general gaming headlines.

**Why it is a domain object:** it has its own screen (`NewsScreen`), its own repository, and its own cache TTL (30 minutes). It is never embedded in another entity.

**Why there are two use cases for one entity:** the app has two distinct news contexts. `GetGameNewsUseCase` fetches Steam's official news for a specific game and is used when the user taps the "Noticias" button from a game's detail screen. `GetGeneralNewsUseCase` fetches general gaming headlines from NewsAPI and is used in the main news tab. Both contexts render the same `NewsScreen` with the same `NewsViewModel`, which adapts based on whether it receives a game `appId` or a free-text query. Because they share the screen, the entity, and the ViewModel, they belong to the same feature. The distinction between the two is a navigation concern (which parameters are passed), not a domain boundary.

**What that implies in code:**
- `news/domain/NewsArticle.kt` — the entity
- `news/domain/NewsRepository.kt` — contract: `getNews(query): AppResult<List<NewsArticle>>` and `getNewsForGame(appId): AppResult<List<NewsArticle>>`
- `news/domain/GetGameNewsUseCase.kt` / `GetGeneralNewsUseCase.kt` — one use case per context
- `news/data/NewsRepositoryImpl.kt` — delegates to `SteamNewsProxy` or `NewsApiProxy` depending on the method called
- `news/presentation/NewsViewModel.kt` + `NewsScreen.kt`

---

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

All ranking data sources work without an API key. Only the **general gaming news** feed needs a free [NewsAPI](https://newsapi.org) key; without it every other feature still works. To enable it, add the key to `local.properties` at the repo root:

```properties
NEWS_API_KEY=your_key_here
```