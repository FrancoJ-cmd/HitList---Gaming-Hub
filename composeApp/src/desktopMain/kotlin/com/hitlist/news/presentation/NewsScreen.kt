package com.hitlist.news.presentation

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.hitlist.common.presentation.*
import com.hitlist.news.domain.NewsArticle
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
            viewModel.loadGameNews(appId)
        } else if (!isApiKeyConfigured) {
            viewModel.setApiKeyMissing()
        } else {
            viewModel.loadGeneralNews(query)
        }
    }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (query == "gaming") "Gaming News" else query,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        if (appId != null) {
                            Text("Noticias de Steam", fontSize = 11.sp, color = OnSurfaceDim)
                        }
                    }
                },
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
            if (state.isApiKeyMissing) {
                ApiKeyMissingContent()
            } else {
                when (val articlesState = state.articlesState) {
                    is UiState.Loading -> CircularProgressIndicator(
                        Modifier.align(Alignment.Center),
                        color = Indigo
                    )
                    is UiState.Success -> Column(Modifier.fillMaxSize()) {
                        if (articlesState.isStale) StaleBanner()
                        ArticleList(articles = articlesState.data)
                    }
                    is UiState.Error -> ErrorScreen(
                        message = articlesState.error.toUserMessage(),
                        onRetry = {
                            if (appId != null) viewModel.loadGameNews(appId)
                            else viewModel.loadGeneralNews(query)
                        }
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📰", fontSize = 40.sp)
                Text("Sin artículos disponibles", color = OnSurfaceDim)
            }
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(articles, key = { it.url }) { article ->
            ArticleCard(article = article)
        }
    }
}

@Composable
private fun ArticleCard(article: NewsArticle) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceColor)
            .clickable { openUrl(article.url) }
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        article.imageUrl?.let { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(90.dp, 64.dp).clip(RoundedCornerShape(8.dp))
            )
        } ?: Box(
            Modifier.size(90.dp, 64.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceVariantColor),
            contentAlignment = Alignment.Center
        ) {
            Text("📰", fontSize = 22.sp)
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = article.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            article.description?.let { desc ->
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = OnSurfaceDim,
                    lineHeight = 18.sp
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(IndigoDim.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(article.sourceName, fontSize = 10.sp, color = Indigo, fontWeight = FontWeight.SemiBold)
                }
                Text(article.publishedAt.take(10), fontSize = 10.sp, color = OnSurfaceDim)
            }
        }
    }
}

@Composable
private fun ApiKeyMissingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .padding(32.dp)
        ) {
            Text("🔑", fontSize = 40.sp)
            Text("NewsAPI no configurada", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            HorizontalDivider(color = OutlineColor)
            Text("Creá local.properties en la raíz del proyecto:", fontSize = 13.sp, color = OnSurfaceDim)
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(SurfaceVariantColor).padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text("NEWS_API_KEY=tu_clave_aquí", fontSize = 13.sp, color = ScoreGreen, fontWeight = FontWeight.Medium)
            }
            Text("Clave gratuita en newsapi.org", fontSize = 12.sp, color = Indigo)
        }
    }
}

private fun openUrl(url: String) {
    runCatching { Desktop.getDesktop().browse(URI(url)) }
}
