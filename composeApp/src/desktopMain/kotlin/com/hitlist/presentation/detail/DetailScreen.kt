package com.hitlist.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.hitlist.domain.entity.Deal
import com.hitlist.domain.entity.GameDetail
import com.hitlist.presentation.common.UiState

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
        topBar = {
            TopAppBar(
                title = { Text(name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val detailState = state.detailState) {
                is UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is UiState.Success -> DetailContent(
                    detail = detailState.data,
                    onNewsClick = onNewsClick
                )
                is UiState.Error -> ErrorContent(
                    message = detailState.message,
                    onRetry = { viewModel.loadDetail(appId, name) }
                )
            }
        }
    }
}

@Composable
private fun DetailContent(detail: GameDetail, onNewsClick: (appId: Int, name: String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            AsyncImage(
                model = detail.headerImageUrl,
                contentDescription = detail.name,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp))
            )
        }

        item {
            Text(detail.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(detail.reviewScoreDesc, color = MaterialTheme.colorScheme.primary)
            Text(
                "${(detail.positiveRatio * 100).toInt()}% positivas · ${detail.totalReviews} reviews",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatChip(label = "Jugando ahora", value = formatPlayers(detail.currentPlayers))
                detail.metacriticScore?.let { score ->
                    StatChip(label = "Metacritic", value = score.toString())
                }
            }
        }

        item {
            Text(detail.shortDescription, fontSize = 14.sp, lineHeight = 20.sp)
        }

        item {
            DetailMetaRow("Géneros", detail.genres.joinToString(", "))
            DetailMetaRow("Desarrollador", detail.developers.joinToString(", "))
            detail.releaseDate?.let { DetailMetaRow("Lanzamiento", it) }
        }

        if (detail.screenshots.isNotEmpty()) {
            item {
                Text("Screenshots", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(detail.screenshots) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.size(220.dp, 124.dp).clip(RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
        }

        item {
            Text("Deals", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            if (detail.deals.isEmpty()) {
                Text("Deals no disponibles", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    detail.deals.forEach { DealRow(it) }
                }
            }
        }

        item {
            Button(
                onClick = { onNewsClick(detail.steamAppId, detail.name) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver noticias de ${detail.name}")
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailMetaRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text("$label: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun DealRow(deal: Deal) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(deal.storeName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Mínimo histórico: \$${deal.cheapestEverPrice}", fontSize = 11.sp, color = Color.Gray)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("\$${deal.currentPrice}", fontWeight = FontWeight.Bold)
            if (deal.savingsPercent > 0) {
                Text("-${deal.savingsPercent.toInt()}%", fontSize = 11.sp, color = Color(0xFF2E7D32))
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No se pudo cargar el detalle", fontWeight = FontWeight.SemiBold)
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
