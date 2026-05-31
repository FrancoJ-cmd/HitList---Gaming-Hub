# HitList — Gamer Hub

**Proyecto 2 | Diseño y Desarrollo de Sistemas | LCC - UNS**

---

## Concepto

Hub de videojuegos que muestra un ranking dinámico de los ~100 juegos de Steam más jugados
del momento, ordenados por un score compuesto de **jugadores concurrentes en vivo** +
**valoración de la comunidad de Steam**. Sin perfiles de usuario, toda la experiencia es global.

Los datos de jugadores son en tiempo real (mismos números que muestra la store de Steam).
El rating es el review score nativo de Steam (porcentaje de reviews positivas).

---

## APIs Utilizadas

Todas gratuitas, **ninguna requiere API key**.

| API | Uso | Clave |
|---|---|---|
| SteamSpy `steamspy.com/api.php` | Semilla diaria: lista de los 100 AppIDs más jugados | No |
| Steam Web API `api.steampowered.com` | Jugadores concurrentes en vivo por AppID | No |
| Steam Store API `store.steampowered.com/api/appdetails` | Metadata: descripción, géneros, screenshots, Metacritic | No |
| Steam Store Reviews `store.steampowered.com/appreviews` | Rating nativo de Steam (% reviews positivas) | No |
| CheapShark `apidocs.cheapshark.com` | Precios y deals por tienda en detalle del juego | No |
| NewsAPI `newsapi.org` | Noticias de gaming (requiere key gratuita) | Sí (gratis) |

> Seis fuentes de datos distintas. El mínimo de la consigna es 2.

---

## APIs — Documentación Técnica

### 1. SteamSpy

Scraper de terceros de datos de Steam. Se usa **una sola vez por día** para obtener la lista
de AppIDs a consultar. El player count de SteamSpy se descarta — se reemplaza por datos en
vivo de la Steam Web API.

```
GET https://steamspy.com/api.php?request=top100in2weeks
```

**Response:** mapa `{ "appid": { ...campos } }` con los 100 juegos más jugados en las
últimas 2 semanas.

**Campos usados:**
```json
{
  "appid": 570,
  "name": "Dota 2"
}
```

Solo necesitamos `appid` y `name`. El resto de los campos se obtienen de Steam directamente.

**Notas:**
- 1 sola call devuelve los 100 juegos. Sin paginación.
- Datos actualizados ~1 vez por día. Cachear con TTL de 24 horas.
- Rate limit: ~1 req/s. Con 1 call total por día, no es problema.

---

### 2. Steam Web API — Jugadores en vivo

Endpoint oficial de Valve. Sin API key requerida.

```
GET https://api.steampowered.com/ISteamUserStats/GetNumberOfCurrentPlayers/v1/?appid=570
```

**Response:**
```json
{
  "response": {
    "player_count": 412847,
    "result": 1
  }
}
```

**Notas:**
- Devuelve el conteo exacto de jugadores concurrentes en el momento de la consulta.
- Es el mismo número que Steam muestra en la store.
- Sin API key. Sin rate limit documentado — respetar ~1 req/s con throttling.
- Se hacen 100 calls en paralelo (con `async/await` en coroutines) al refrescar el ranking.
- Cachear por 5 minutos máximo (es dato en vivo, no tiene sentido cachear más).

---

### 3. Steam Store API — Metadata

```
GET https://store.steampowered.com/api/appdetails?appids=570
```

**No soporta batching múltiple** para metadata completa. Una call por juego.

**Campos útiles del response:**
```json
{
  "570": {
    "success": true,
    "data": {
      "name": "Dota 2",
      "short_description": "...",
      "detailed_description": "...",
      "header_image": "https://...",
      "screenshots": [
        { "path_thumbnail": "https://...", "path_full": "https://..." }
      ],
      "genres": [{ "id": "1", "description": "Action" }],
      "developers": ["Valve"],
      "publishers": ["Valve"],
      "release_date": { "date": "9 Jul, 2013" },
      "metacritic": { "score": 90, "url": "https://..." },
      "platforms": { "windows": true, "mac": true, "linux": true },
      "is_free": true,
      "price_overview": {
        "currency": "USD",
        "initial": 999,
        "final": 499,
        "discount_percent": 50,
        "final_formatted": "$4.99"
      }
    }
  }
}
```

**Notas:**
- `success: false` si el appid no existe o es un DLC sin página propia → manejar silenciosamente (skip).
- Rate limit: ~200 requests / 5 minutos. Con 100 juegos y caché agresivo (TTL 24h para
  metadata que casi no cambia), esto no es un problema en la práctica.
- `screenshots` puede estar vacío en juegos muy viejos → UI debe manejar lista vacía.
- `metacritic` puede ser null → campo opcional en la entidad.

---

### 4. Steam Store Reviews — Rating nativo

```
GET https://store.steampowered.com/appreviews/{appid}?json=1&num_per_page=0&language=all
```

**Response:**
```json
{
  "query_summary": {
    "review_score": 8,
    "review_score_desc": "Very Positive",
    "total_positive": 1823441,
    "total_negative": 210332,
    "total_reviews": 2033773
  }
}
```

**Escala `review_score` de Valve (0–9):**
| Score | Descripción |
|---|---|
| 9 | Overwhelmingly Positive (≥95% positivas, ≥500 reviews) |
| 8 | Very Positive (≥80%) |
| 7 | Mostly Positive (≥70%) |
| 6 | Mixed (40–70%) |
| 5 | Mostly Negative (≤40%) |
| ... | ... |

**Campos usados para el score compuesto:**
- `total_positive` y `total_reviews` → `positiveRatio = total_positive / total_reviews` (0.0–1.0)
- `review_score_desc` → etiqueta de texto para mostrar en UI ("Very Positive")
- `total_reviews` → filtro mínimo: juegos con menos de 50 reviews se excluyen del ranking
  (evita que juegos oscuros con 1 review positiva suban artificialmente).

**Notas:**
- Una call por juego. Cachear con TTL de 6 horas (cambia lentamente).
- Sin API key.

---

### 5. CheapShark

Se usa solo en la pantalla de detalle para mostrar deals por tienda.

```
GET https://www.cheapshark.com/api/1.0/games?title=Dota+2&limit=3
→ Devuelve gameID de CheapShark

GET https://www.cheapshark.com/api/1.0/games?id=612
→ Devuelve deals actuales y precio mínimo histórico
```

**Campos útiles:**
```json
{
  "info": { "title": "Dota 2" },
  "cheapestPriceEver": { "price": "0.00", "date": 1600000000 },
  "deals": [
    {
      "storeID": "1",
      "price": "0.00",
      "retailPrice": "0.00",
      "savings": "0.000000"
    }
  ]
}
```

**StoreIDs útiles:** 1=Steam, 25=GOG, 11=Humble Store.

**Notas:**
- Matching por nombre (el único que requiere name matching en todo el proyecto).
- Si no hay resultado o `deals` está vacío → mostrar sección vacía, no error.
- Cachear por 1 hora (los precios cambian con frecuencia pero no al instante).

---

### 6. NewsAPI

```
GET https://newsapi.org/v2/everything?q=gaming&language=en&sortBy=publishedAt&apiKey=KEY
GET https://newsapi.org/v2/everything?q=Dota+2&language=en&sortBy=publishedAt&apiKey=KEY
```

**Límites del free tier:**
- **100 requests/día** — suficiente para desarrollo y demo.
- Error 429 al exceder.

**Mitigación:** Cachear resultados de noticias con TTL de 30 minutos. Si el caché es válido,
no hacer request aunque haya conexión. De esta forma ~50 requests/día alcanzan para uso normal.

---

## Estrategia de Datos — Flujo por Pantalla

### Carga inicial del ranking (primer arranque o caché expirado)

```
1. SteamSpy top100in2weeks          → 1 call    → lista de 100 {appid, name}
2. Steam GetNumberOfCurrentPlayers  → 100 calls → player_count por appid (en paralelo)
3. Steam appreviews                 → 100 calls → positiveRatio + review_score_desc (en paralelo)
4. Combinar + calcular score + ordenar
5. Guardar en Room
6. Mostrar en UI
```

Los pasos 2 y 3 corren concurrentemente entre sí (todos los appids en paralelo).
Total: ~200 calls al arranque. Con el rate limit de ~200/5min de Steam Store, esto toma
al rededor de 5 minutos de refresh completo.

**Optimización**: Los pasos 1 y 3 tienen TTL largo (24h y 6h respectivamente).
En refrescos normales (cada 5 min), solo se hacen las 100 calls de jugadores en vivo.

### Carga del detalle de un juego (navegación desde el ranking)

```
1. Steam appdetails?appids={id}    → 1 call   → metadata completa
2. CheapShark games?title={name}  → 1-2 calls → deals
(appreviews ya está cacheado del ranking)
```

### Carga de noticias

```
1. NewsAPI everything?q={query}   → 1 call (si caché expirado)
```

---

## Lógica del Score Compuesto

Vive íntegramente en `GetRankedGamesUseCase`. Sin dependencias externas → testeable con
datos fijos.

```kotlin
fun calculateScore(
    currentPlayers: Int,
    maxPlayersInDataset: Int,
    positiveRatio: Double,
    totalReviews: Int
): Double {
    if (totalReviews < MIN_REVIEWS_THRESHOLD) return 0.0  // excluir del ranking
    val trendScore = if (maxPlayersInDataset == 0) 0.0
                     else currentPlayers.toDouble() / maxPlayersInDataset
    val ratingScore = positiveRatio  // ya está en 0.0–1.0
    return TREND_WEIGHT * trendScore + RATING_WEIGHT * ratingScore
}

companion object {
    const val TREND_WEIGHT = 0.6
    const val RATING_WEIGHT = 0.4
    const val MIN_REVIEWS_THRESHOLD = 50
}
```

**Edge cases:**
- `maxPlayersInDataset == 0` → `trendScore = 0.0` (no dividir por cero)
- `totalReviews < 50` → score = 0.0, juego excluido del ranking final
- `positiveRatio = 0.0` (sin reviews positivas) → score solo por tendencia
- Empate en score → mantener orden original de SteamSpy (más jugadores primero)
- Lista vacía → devolver lista vacía, no error

---

## Detección de Tendencia (`isTrending`)

Un juego se marca como trending si subió posiciones respecto al ranking guardado anteriormente.

```kotlin
fun markTrending(current: List<RankedGame>, previous: List<RankedGame>): List<RankedGame> {
    val prevPositions = previous.mapIndexed { idx, g -> g.steamAppId to idx }.toMap()
    return current.mapIndexed { currentIdx, game ->
        val prevIdx = prevPositions[game.steamAppId]
        game.copy(isTrending = prevIdx != null && currentIdx < prevIdx)
    }
}
```

---

## Entidades del Dominio

```kotlin
// domain/entity/RankedGame.kt
data class RankedGame(
    val steamAppId: Int,
    val name: String,
    val headerImageUrl: String,
    val score: Double,                  // 0.0–1.0, calculado en UseCase
    val currentPlayers: Int,            // live, de Steam Web API
    val positiveRatio: Double,          // 0.0–1.0, de appreviews
    val reviewScoreDesc: String,        // "Very Positive", etc.
    val totalReviews: Int,
    val genres: List<String>,
    val isTrending: Boolean
)

// domain/entity/GameDetail.kt
data class GameDetail(
    val steamAppId: Int,
    val name: String,
    val shortDescription: String,
    val headerImageUrl: String,
    val screenshots: List<String>,
    val metacriticScore: Int?,          // null si no tiene
    val genres: List<String>,
    val developers: List<String>,
    val releaseDate: String?,
    val isFree: Boolean,
    val currentPlayers: Int,
    val positiveRatio: Double,
    val reviewScoreDesc: String,
    val totalReviews: Int,
    val deals: List<Deal>               // puede ser lista vacía
)

// domain/entity/Deal.kt
data class Deal(
    val storeName: String,
    val currentPrice: String,
    val retailPrice: String,
    val savingsPercent: Double,
    val cheapestEverPrice: String
)

// domain/entity/NewsArticle.kt
data class NewsArticle(
    val title: String,
    val description: String?,
    val sourceName: String,
    val url: String,
    val imageUrl: String?,
    val publishedAt: String
)
```

---

## Arquitectura — Clean Architecture + MVVM

### Estructura de paquetes

```
com.hitlist/
├── domain/
│   ├── entity/
│   │   ├── RankedGame.kt
│   │   ├── GameDetail.kt
│   │   ├── Deal.kt
│   │   └── NewsArticle.kt
│   ├── repository/
│   │   ├── GameRepository.kt        ← interfaz
│   │   ├── NewsRepository.kt        ← interfaz
│   │   └── DealsRepository.kt       ← interfaz
│   └── usecase/
│       ├── GetRankedGamesUseCase.kt
│       ├── GetGameDetailUseCase.kt
│       ├── GetGameNewsUseCase.kt
│       └── GetGameDealsUseCase.kt
│
├── data/
│   ├── remote/
│   │   ├── steamspy/
│   │   │   ├── SteamSpyApi.kt
│   │   │   ├── SteamSpyDto.kt
│   │   │   └── SteamSpyRemoteDataSource.kt
│   │   ├── steamweb/
│   │   │   ├── SteamWebApi.kt       ← GetNumberOfCurrentPlayers
│   │   │   ├── SteamWebDto.kt
│   │   │   └── SteamWebRemoteDataSource.kt
│   │   ├── steamstore/
│   │   │   ├── SteamStoreApi.kt     ← appdetails + appreviews
│   │   │   ├── SteamStoreDto.kt
│   │   │   └── SteamStoreRemoteDataSource.kt
│   │   ├── cheapshark/
│   │   │   ├── CheapSharkApi.kt
│   │   │   ├── CheapSharkDto.kt
│   │   │   └── CheapSharkRemoteDataSource.kt
│   │   └── newsapi/
│   │       ├── NewsApi.kt
│   │       ├── NewsDto.kt
│   │       └── NewsRemoteDataSource.kt
│   ├── local/
│   │   ├── db/
│   │   │   ├── AppDatabase.kt
│   │   │   ├── RankedGameEntity.kt
│   │   │   ├── RankedGameDao.kt
│   │   │   ├── GameDetailEntity.kt
│   │   │   ├── GameDetailDao.kt
│   │   │   ├── NewsArticleEntity.kt
│   │   │   └── NewsArticleDao.kt
│   │   ├── GameLocalDataSource.kt
│   │   └── NewsLocalDataSource.kt
│   └── repository/
│       ├── GameRepositoryImpl.kt
│       ├── NewsRepositoryImpl.kt
│       └── DealsRepositoryImpl.kt
│
├── presentation/
│   ├── ranking/
│   │   ├── RankingViewModel.kt
│   │   ├── RankingScreen.kt
│   │   └── RankingUiState.kt
│   ├── detail/
│   │   ├── DetailViewModel.kt
│   │   ├── DetailScreen.kt
│   │   └── DetailUiState.kt
│   ├── news/
│   │   ├── NewsViewModel.kt
│   │   ├── NewsScreen.kt
│   │   └── NewsUiState.kt
│   └── navigation/
│       └── AppNavGraph.kt
│
└── di/
    ├── NetworkModule.kt             ← un Retrofit por base URL
    ├── DatabaseModule.kt
    └── RepositoryModule.kt
```

### Instancias de Retrofit (una por base URL)

```kotlin
// NetworkModule.kt
@Provides @Named("steamspy")
fun steamSpyRetrofit(): Retrofit = Retrofit.Builder()
    .baseUrl("https://steamspy.com/")...

@Provides @Named("steamweb")
fun steamWebRetrofit(): Retrofit = Retrofit.Builder()
    .baseUrl("https://api.steampowered.com/")...

@Provides @Named("steamstore")
fun steamStoreRetrofit(): Retrofit = Retrofit.Builder()
    .baseUrl("https://store.steampowered.com/")...

@Provides @Named("cheapshark")
fun cheapSharkRetrofit(): Retrofit = Retrofit.Builder()
    .baseUrl("https://www.cheapshark.com/")...

@Provides @Named("newsapi")
fun newsApiRetrofit(): Retrofit = Retrofit.Builder()
    .baseUrl("https://newsapi.org/")...
```

### Reglas de dependencia (estrictas)

- **Domain** no importa nada de Data ni de Presentation.
- **Presentation** depende de Domain (Use Cases). Nunca de Data directamente.
- **Data** implementa las interfaces de Domain.
- Los DTOs (`*Dto.kt`) nunca salen de la capa Data.
- Las entidades de dominio no tienen anotaciones de Room, Gson, ni Android.

---

## Persistencia Local (Room)

### Schema

```kotlin
@Entity(tableName = "ranked_games")
data class RankedGameEntity(
    @PrimaryKey val steamAppId: Int,
    val name: String,
    val headerImageUrl: String,
    val score: Double,
    val currentPlayers: Int,
    val positiveRatio: Double,
    val reviewScoreDesc: String,
    val totalReviews: Int,
    val genresCsv: String,          // join con ","
    val isTrending: Boolean,
    val rankPosition: Int,          // para restaurar orden offline
    val cachedAt: Long
)

@Entity(tableName = "game_details")
data class GameDetailEntity(
    @PrimaryKey val steamAppId: Int,
    val name: String,
    val shortDescription: String,
    val headerImageUrl: String,
    val screenshotsCsv: String,     // URLs join con ","
    val metacriticScore: Int?,
    val genresCsv: String,
    val developersCsv: String,
    val releaseDate: String?,
    val isFree: Boolean,
    val currentPlayers: Int,
    val positiveRatio: Double,
    val reviewScoreDesc: String,
    val totalReviews: Int,
    val cachedAt: Long
)

@Entity(tableName = "news_articles")
data class NewsArticleEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String?,
    val sourceName: String,
    val imageUrl: String?,
    val publishedAt: String,
    val query: String,
    val cachedAt: Long
)
```

### Política de TTL por fuente

```kotlin
object CachePolicy {
    const val SEED_LIST_TTL_MS      = 24 * 60 * 60 * 1000L  // SteamSpy: 24 horas
    const val LIVE_PLAYERS_TTL_MS   =       5 * 60 * 1000L  // Jugadores en vivo: 5 min
    const val REVIEWS_TTL_MS        =  6 * 60 * 60 * 1000L  // Reviews Steam: 6 horas
    const val METADATA_TTL_MS       = 24 * 60 * 60 * 1000L  // appdetails: 24 horas
    const val DEALS_TTL_MS          =      60 * 60 * 1000L  // CheapShark: 1 hora
    const val NEWS_TTL_MS           =      30 * 60 * 1000L  // NewsAPI: 30 min (rate limit)
}
```

### Lógica de caché en `GameRepositoryImpl`

```
getRankedGames():
  Si caché existe Y no expiró → devolver caché
  Si no hay red → devolver caché (aunque expirado) con isStale=true
  Si hay red:
    1. Fetch SteamSpy (si su caché de 24h expiró)
    2. Fetch jugadores en vivo (siempre, TTL 5 min)
    3. Fetch reviews (si TTL de 6h expiró)
    4. Combinar → calcular score → guardar en Room → devolver
```

---

## UiState

```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T, val isStale: Boolean = false) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

`isStale = true` activa un banner: _"Mostrando datos guardados — sin conexión"_.

**Reglas:**
- Nunca pantalla en blanco. Loading siempre visible al cargar.
- Error parcial en detalle (CheapShark falla): mostrar detalle sin deals, con texto
  inline _"Deals no disponibles"_. No es `UiState.Error`.
- Error total: pantalla de error con botón Reintentar.

---

## Pantallas y Navegación

```
RankingScreen ──(click juego)──────→ DetailScreen
RankingScreen ──(click noticias)───→ NewsScreen
DetailScreen ──(click "Ver noticias")→ NewsScreen(query=nombreJuego)
```

### Pantalla 1 — GamePulse Ranking

- Skeleton cards durante Loading.
- Lista de `RankedGame` ordenada por score compuesto.
- Chips de filtro por género (filtrado en memoria, sin re-fetch).
- Chip "Todos" como opción por defecto.
- Badge 🔥 si `isTrending == true`.
- Banner amarillo si `isStale == true`.
- Cada card: posición, thumbnail, nombre, badge trending, barra de score,
  jugadores activos formateados ("412.8K"), review desc ("Very Positive").

### Pantalla 2 — Detalle del Juego

- Header con imagen, nombre, review desc y porcentaje de positivas.
- Stats: jugadores en vivo, Metacritic score (si existe).
- Descripción corta.
- Géneros, desarrollador, fecha de lanzamiento.
- Carrusel de screenshots (máx 5). Si no hay, ocultar sección.
- Sección Deals: lista por tienda con precio y % descuento. Si no hay: texto vacío.
- Botón "Ver noticias de [nombre]".

### Pantalla 3 — Noticias Gaming

- Sin query → busca "gaming".
- Con query (desde Detalle) → busca por nombre del juego.
- Lista de artículos con imagen, título, fuente y fecha.
- Click abre URL en browser nativo (Intent).
- Caché de 30 min para proteger el rate limit de NewsAPI.

---

## Tests Unitarios

### `GetRankedGamesUseCase`

| Caso | Descripción |
|---|---|
| Score correcto | Con players y positiveRatio fijos, score calculado es exacto |
| Exclusión por reviews bajas | Juego con <50 reviews tiene score 0.0 y es excluido |
| maxPlayers == 0 | trendScore = 0.0, sin división por cero |
| positiveRatio == 0.0 | Score solo por tendencia |
| Empate en score | Orden original de SteamSpy preservado |
| Lista vacía SteamSpy | Devuelve lista vacía sin excepción |
| isTrending correcto | Subió posición → true; bajó → false; nuevo → false |

### `GetGameDetailUseCase`

| Caso | Descripción |
|---|---|
| Happy path | Datos de appdetails + appreviews + CheapShark combinados |
| appdetails falla | Devuelve Error |
| CheapShark falla | Devuelve Detail con deals = emptyList() |
| screenshots vacío | Detail carga con screenshots = emptyList() |
| metacritic null | Detail carga con metacriticScore = null |

### `GetGameNewsUseCase`

| Caso | Descripción |
|---|---|
| Happy path con query | Devuelve artículos del juego |
| Query vacío o blank | Usa "gaming" como fallback |
| Lista vacía | Devuelve lista vacía, no error |

### `GetGameDealsUseCase`

| Caso | Descripción |
|---|---|
| Deal disponible | Lista con precios y savings |
| Sin deals | Lista vacía |
| Juego gratuito | Precio "0.00", savings "0" |
| Juego no encontrado en CheapShark | Lista vacía, no error |

### `RankingViewModel`

| Caso | Descripción |
|---|---|
| Estado inicial | `UiState.Loading` |
| Carga exitosa | `UiState.Success` con lista |
| Error de red | `UiState.Error` con mensaje |
| Offline | `UiState.Success(isStale=true)` |
| Filtro por género | Lista filtrada sin re-fetch |
| Filtro "Todos" | Lista completa restaurada |

### `DetailViewModel`

| Caso | Descripción |
|---|---|
| Carga exitosa | `UiState.Success` con GameDetail completo |
| Error total | `UiState.Error` |
| CheapShark falla | `UiState.Success` con deals vacíos |

### `NewsViewModel`

| Caso | Descripción |
|---|---|
| Carga con query | Artículos del juego |
| Carga sin query | Artículos de "gaming" |
| Error API | `UiState.Error` |

### `GameRepositoryImpl`

| Caso | Descripción |
|---|---|
| Caché válido | Devuelve caché, no hace calls remotas |
| Caché expirado + red | Fetch remoto, actualiza Room |
| Caché expirado + sin red | Devuelve caché con isStale=true |
| Sin caché + sin red | Devuelve Error |

### `GameLocalDataSource`

| Caso | Descripción |
|---|---|
| Insert y query | Datos guardados se recuperan correctamente |
| TTL expirado | `isCacheValid()` devuelve false |
| Caché vacío | Query devuelve lista vacía |
| Sobreescritura | Insert nuevo sobreescribe mismo PK |

---

## Orden de Implementación

```
Fase 1 — Dominio puro (sin Android, 100% testeable)
  [ ] Entidades de dominio
  [ ] Interfaces de repositorio
  [ ] GetRankedGamesUseCase (lógica de score + trending + filtro de reviews bajas)
  [ ] GetGameDetailUseCase
  [ ] GetGameNewsUseCase
  [ ] GetGameDealsUseCase
  [ ] Tests de todos los Use Cases

Fase 2 — Capa de datos
  [ ] DTOs para cada API
  [ ] Interfaces Retrofit (5 instancias, una por base URL)
  [ ] RemoteDataSources (mapeo DTO → entidad)
  [ ] Room: entities, DAOs, AppDatabase
  [ ] LocalDataSources
  [ ] RepositoryImpl con lógica de TTL
  [ ] Tests de RepositoryImpl y LocalDataSources

Fase 3 — Inyección de dependencias
  [ ] NetworkModule
  [ ] DatabaseModule
  [ ] RepositoryModule

Fase 4 — Presentación
  [ ] AppNavGraph
  [ ] RankingScreen + RankingViewModel + tests
  [ ] DetailScreen + DetailViewModel + tests
  [ ] NewsScreen + NewsViewModel + tests

Fase 5 — Polish
  [ ] Skeleton loading cards en RankingScreen
  [ ] Banner de datos desactualizados
  [ ] Pantallas de error con botón Reintentar
  [ ] Filtro por género con chips
```

---

## Stack Técnico

| Herramienta | Uso |
|---|---|
| Kotlin + Jetpack Compose | UI |
| Retrofit 2 + Gson | Consumo de APIs REST |
| Room | Persistencia local |
| Hilt | Inyección de dependencias |
| Coroutines + Flow | Asincronismo y reactividad |
| JUnit 4 + MockK | Tests unitarios |
| kotlinx-coroutines-test | Tests de ViewModels y coroutines |
| Coil Compose | Carga de imágenes |

---

## Decisiones Tomadas (no reabrir)

- **Sin RAWG**: eliminado por completo. Steam Store API + Steam Reviews dan mejor
  cobertura con datos nativos y sin el problema de matching por nombre.
- **Sin SteamSpy para player count**: SteamSpy solo se usa como semilla de AppIDs (1
  call/día). El count en vivo viene de Steam Web API oficial.
- **Rating = positiveRatio de Steam Reviews**: más representativo que una editorial,
  es la voz de la comunidad. Se normaliza naturalmente en 0.0–1.0.
- **Exclusión de juegos con <50 reviews**: evita que juegos oscuros con pocas
  reviews manipulen el ranking.
- **5 instancias de Retrofit**: una por base URL, necesario porque Retrofit requiere
  una base URL fija por instancia.
- **Caché de 5 min para jugadores en vivo**: balance entre "tiempo real" y no
  saturar la API de Steam con refreshes constantes.
- **Caché de 30 min para NewsAPI**: protege el límite de 100 requests/día.
- **Error parcial en Detail no es Error total**: si CheapShark falla, el detalle
  se muestra igual con deals vacíos.
- **genresCsv / screenshotsCsv en Room**: evita tablas relacionales innecesarias
  para el scope del proyecto.
- **isStale en UiState.Success**: más simple que un cuarto estado separado en el ViewModel.
