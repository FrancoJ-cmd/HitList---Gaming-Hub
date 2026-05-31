package com.hitlist.presentation.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.hitlist.domain.entity.RankedGame
import com.hitlist.presentation.common.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    viewModel: RankingViewModel,
    onGameClick: (Int, String) -> Unit,
    onNewsClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HitList", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onNewsClick) { Text("News") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val gamesState = state.gamesState

            if (gamesState is UiState.Success && gamesState.isStale) {
                StaleBanner()
            }

            if (state.availableGenres.isNotEmpty()) {
                GenreFilterRow(
                    genres = state.availableGenres,
                    selectedGenre = state.selectedGenre,
                    onGenreSelected = { viewModel.selectGenre(it) }
                )
            }

            when (gamesState) {
                is UiState.Loading -> LoadingList()
                is UiState.Success -> GameList(
                    games = gamesState.data,
                    onGameClick = onGameClick
                )
                is UiState.Error -> ErrorScreen(
                    message = gamesState.message,
                    onRetry = { viewModel.loadRanking() }
                )
            }
        }
    }
}

@Composable
private fun StaleBanner() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF9C4))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            "Mostrando datos guardados — sin conexión",
            fontSize = 12.sp,
            color = Color(0xFF6D4C00)
        )
    }
}

@Composable
private fun GenreFilterRow(
    genres: List<String>,
    selectedGenre: String?,
    onGenreSelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedGenre == null,
                onClick = { onGenreSelected(null) },
                label = { Text("Todos") }
            )
        }
        items(genres) { genre ->
            FilterChip(
                selected = selectedGenre == genre,
                onClick = { onGenreSelected(genre) },
                label = { Text(genre) }
            )
        }
    }
}

@Composable
private fun GameList(games: List<RankedGame>, onGameClick: (Int, String) -> Unit) {
    if (games.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay juegos en este género")
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(games, key = { it.steamAppId }) { game ->
            GameCard(game = game, onClick = { onGameClick(game.steamAppId, game.name) })
        }
    }
}

@Composable
private fun GameCard(game: RankedGame, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = game.headerImageUrl,
                contentDescription = game.name,
                modifier = Modifier.size(80.dp, 37.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = game.name,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (game.isTrending) {
                        Spacer(Modifier.width(4.dp))
                        Text("🔥", fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${formatPlayers(game.currentPlayers)} jugando · ${game.reviewScoreDesc}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { game.score.toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun LoadingList() {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(10) { SkeletonCard() }
    }
}

@Composable
private fun SkeletonCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(80.dp, 37.dp).background(Color.LightGray, RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(12.dp))
            Column {
                Box(Modifier.fillMaxWidth(0.6f).height(14.dp).background(Color.LightGray, RoundedCornerShape(2.dp)))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth(0.4f).height(10.dp).background(Color.LightGray, RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Error al cargar el ranking", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

private fun formatPlayers(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000 -> "${String.format("%.1f", count / 1_000.0)}K"
    else -> count.toString()
}
