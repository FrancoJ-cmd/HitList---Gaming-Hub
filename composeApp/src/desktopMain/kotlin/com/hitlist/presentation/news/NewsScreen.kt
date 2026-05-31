package com.hitlist.presentation.news

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.hitlist.domain.entity.NewsArticle
import com.hitlist.presentation.common.UiState
import java.awt.Desktop
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    query: String,
    appId: Int? = null,
    viewModel: NewsViewModel,
    isApiKeyConfigured: Boolean,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(query, appId) {
        if (appId != null) {
            viewModel.loadNews(query, appId)
        } else if (!isApiKeyConfigured) {
            viewModel.setApiKeyMissing()
        } else {
            viewModel.loadNews(query)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (query == "gaming") "Gaming News" else "Noticias: $query") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isApiKeyMissing) {
                ApiKeyMissingContent()
            } else {
                when (val articlesState = state.articlesState) {
                    is UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    is UiState.Success -> ArticleList(articles = articlesState.data)
                    is UiState.Error -> ErrorContent(
                        message = articlesState.message,
                        onRetry = { viewModel.loadNews(query) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleList(articles: List<NewsArticle>) {
    if (articles.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No se encontraron artículos")
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(articles, key = { it.url }) { article ->
            ArticleCard(article = article)
        }
    }
}

@Composable
private fun ArticleCard(article: NewsArticle) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openUrl(article.url) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(12.dp)) {
            article.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                article.description?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = "${article.sourceName} · ${article.publishedAt.take(10)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ApiKeyMissingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("NewsAPI no configurada", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Creá un archivo local.properties en la raíz del proyecto con:\nNEWS_API_KEY=tu_clave\n\nObtené tu clave gratis en newsapi.org",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Error al cargar noticias", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

private fun openUrl(url: String) {
    runCatching { Desktop.getDesktop().browse(URI(url)) }
}
