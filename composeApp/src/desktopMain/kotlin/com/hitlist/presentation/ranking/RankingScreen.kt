package com.hitlist.presentation.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.hitlist.domain.entity.RankedGame
import com.hitlist.presentation.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    viewModel: RankingViewModel,
    onGameClick: (Int, String) -> Unit,
    onNewsClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "HIT",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = Indigo
                        )
                        Text(
                            "LIST",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onNewsClick) {
                        Text("📰 Noticias", fontSize = 13.sp, color = OnSurfaceDim)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceColor
                )
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
                is UiState.Success -> GameList(games = gamesState.data, onGameClick = onGameClick)
                is UiState.Error -> ErrorScreen(
                    message = gamesState.error.toUserMessage(),
                    onRetry = { viewModel.retry() }
                )
            }
        }
    }
}

@Composable
private fun StaleBanner() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2508))
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("⚠", fontSize = 13.sp)
        Text(
            "Mostrando datos guardados — sin conexión",
            fontSize = 12.sp,
            color = Color(0xFFF5C842)
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            GenreChip(label = "Todos", selected = selectedGenre == null) { onGenreSelected(null) }
        }
        items(genres) { genre ->
            GenreChip(label = genre, selected = selectedGenre == genre) { onGenreSelected(genre) }
        }
    }
}

@Composable
private fun GenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Indigo else SurfaceVariantColor
    val textColor = if (selected) Color.White else OnSurfaceDim
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, color = textColor, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun GameList(games: List<RankedGame>, onGameClick: (Int, String) -> Unit) {
    if (games.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay juegos en este género", color = OnSurfaceDim)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(games, key = { _, game -> game.steamAppId }) { index, game ->
            GameCard(
                rank = index + 1,
                game = game,
                onClick = { onGameClick(game.steamAppId, game.name) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun GameCard(rank: Int, game: RankedGame, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val rankColor = when (rank) {
        1 -> GoldColor
        2 -> SilverColor
        3 -> BronzeColor
        else -> OnSurfaceDim
    }
    val borderColor = when (rank) {
        1 -> GoldColor.copy(alpha = 0.4f)
        2 -> SilverColor.copy(alpha = 0.3f)
        3 -> BronzeColor.copy(alpha = 0.3f)
        else -> OutlineColor
    }

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .clickable(onClick = onClick)
    ) {
        if (rank <= 3) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(borderColor.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )
        }

        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank number
            Box(
                Modifier.width(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (rank <= 99) "#$rank" else "$rank",
                    fontSize = if (rank <= 3) 17.sp else 14.sp,
                    fontWeight = FontWeight.Black,
                    color = rankColor
                )
            }

            Spacer(Modifier.width(10.dp))

            // Game thumbnail
            AsyncImage(
                model = game.headerImageUrl,
                contentDescription = game.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(108.dp, 50.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Spacer(Modifier.width(14.dp))

            // Info column
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = game.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (game.isTrending) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "🔥",
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(TrendingRed.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(ScoreGreen))
                        Text(
                            formatPlayers(game.currentPlayers),
                            fontSize = 11.sp,
                            color = OnSurfaceDim
                        )
                    }
                    Text(
                        "·",
                        fontSize = 11.sp,
                        color = OutlineColor
                    )
                    Text(
                        game.reviewScoreDesc,
                        fontSize = 11.sp,
                        color = OnSurfaceDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceVariantColor)
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(game.score.toFloat().coerceIn(0f, 1f))
                                .background(
                                    Brush.horizontalGradient(listOf(Indigo, ScoreGreen))
                                )
                        )
                    }
                    Text(
                        "${(game.score * 100).toInt()}",
                        fontSize = 10.sp,
                        color = OnSurfaceDim,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingList() {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(12) { index -> SkeletonCard(index + 1) }
    }
}

@Composable
private fun SkeletonCard(rank: Int) {
    val shimmer = SurfaceVariantColor
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(38.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(24.dp, 14.dp).clip(RoundedCornerShape(3.dp)).background(shimmer))
        }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(108.dp, 50.dp).clip(RoundedCornerShape(6.dp)).background(shimmer))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.fillMaxWidth(0.65f).height(13.dp).clip(RoundedCornerShape(3.dp)).background(shimmer))
            Box(Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(3.dp)).background(shimmer))
            Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(shimmer))
        }
    }
}


private fun formatPlayers(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M jugando"
    count >= 1_000 -> "${String.format("%.1f", count / 1_000.0)}K jugando"
    else -> "$count jugando"
}
