package com.hitlist.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.hitlist.domain.entity.Deal
import com.hitlist.domain.entity.GameDetail
import com.hitlist.presentation.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    appId: Int,
    name: String,
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onNewsClick: (appId: Int, name: String) -> Unit
) {
    LaunchedEffect(appId) { viewModel.loadDetail(appId, name) }
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(name, maxLines = 1, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = OnSurfaceDim)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val detailState = state.detailState) {
                is UiState.Loading -> CircularProgressIndicator(
                    Modifier.align(Alignment.Center),
                    color = Indigo
                )
                is UiState.Success -> DetailContent(detail = detailState.data, onNewsClick = onNewsClick)
                is UiState.Error -> ErrorContent(
                    message = detailState.error.toUserMessage(),
                    onRetry = { viewModel.loadDetail(appId, name) }
                )
            }
        }
    }
}

@Composable
private fun DetailContent(detail: GameDetail, onNewsClick: (appId: Int, name: String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {

        // Hero image with gradient overlay
        item {
            Box(Modifier.fillMaxWidth().height(220.dp)) {
                AsyncImage(
                    model = detail.headerImageUrl,
                    contentDescription = detail.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, BackgroundColor),
                                startY = 80f
                            )
                        )
                )
                Column(
                    Modifier.align(Alignment.BottomStart).padding(20.dp)
                ) {
                    Text(
                        text = detail.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    ReviewBadge(detail.reviewScoreDesc, detail.positiveRatio)
                }
            }
        }

        // Stats row
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(BackgroundColor)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Jugando ahora",
                    value = formatPlayers(detail.currentPlayers),
                    accent = ScoreGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Reviews positivas",
                    value = "${(detail.positiveRatio * 100).toInt()}%",
                    accent = Indigo,
                    modifier = Modifier.weight(1f)
                )
                detail.metacriticScore?.let { score ->
                    StatCard(
                        label = "Metacritic",
                        value = score.toString(),
                        accent = when {
                            score >= 80 -> ScoreGreen
                            score >= 60 -> Color(0xFFFFA500)
                            else -> TrendingRed
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Description
        item {
            Column(Modifier.background(BackgroundColor).padding(horizontal = 20.dp)) {
                Text(
                    text = detail.shortDescription,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = OnSurfaceDim
                )
            }
        }

        // Metadata
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionHeader("Detalles")
                MetaRow("Géneros", detail.genres.joinToString(" · "))
                MetaRow("Desarrollador", detail.developers.joinToString(", "))
                detail.releaseDate?.let { MetaRow("Lanzamiento", it) }
                MetaRow("Precio", if (detail.isFree) "Gratis" else "De pago")
            }
        }

        // Screenshots
        if (detail.screenshots.isNotEmpty()) {
            item {
                Column(Modifier.padding(vertical = 4.dp)) {
                    SectionHeader("Screenshots", modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(detail.screenshots) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(260.dp, 146.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }
        }

        // Deals
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionHeader("Precios y deals")
                if (detail.deals.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceColor)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sin deals disponibles", fontSize = 13.sp, color = OnSurfaceDim)
                    }
                } else {
                    detail.deals.forEach { DealCard(it) }
                }
            }
        }

        // News button
        item {
            Button(
                onClick = { onNewsClick(detail.steamAppId, detail.name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo)
            ) {
                Text("📰", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text("Noticias de ${detail.name}", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReviewBadge(desc: String, ratio: Double) {
    val color = when {
        ratio >= 0.8 -> ScoreGreen
        ratio >= 0.6 -> Color(0xFFFFA500)
        else -> TrendingRed
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(desc, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "${(ratio * 100).toInt()}% positivas",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceColor)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = accent)
        Text(label, fontSize = 11.sp, color = OnSurfaceDim)
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = OnSurfaceDim,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceColor)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = OnSurfaceDim)
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DealCard(deal: Deal) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(deal.storeName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                "Mínimo histórico: \$${deal.cheapestEverPrice}",
                fontSize = 11.sp,
                color = OnSurfaceDim
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (deal.savingsPercent > 0) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ScoreGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "-${deal.savingsPercent.toInt()}%",
                        fontSize = 12.sp,
                        color = ScoreGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text("\$${deal.currentPrice}", fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("⚠", fontSize = 32.sp)
            Text("No se pudo cargar el detalle", fontWeight = FontWeight.SemiBold)
            Text(message, fontSize = 12.sp, color = OnSurfaceDim)
            Spacer(Modifier.height(4.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Indigo)) {
                Text("Reintentar")
            }
        }
    }
}

private fun formatPlayers(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000 -> "${String.format("%.1f", count / 1_000.0)}K"
    else -> count.toString()
}
