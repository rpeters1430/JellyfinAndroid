package com.rpeters.jellyfin.ui.screens.details.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rpeters.jellyfin.ui.components.immersive.rememberImmersivePerformanceConfig
import com.rpeters.jellyfin.ui.theme.ImmersiveDimens

@Composable
fun DetailCastAndCrewSection(
    directors: List<org.jellyfin.sdk.model.api.BaseItemPerson>,
    writers: List<org.jellyfin.sdk.model.api.BaseItemPerson>,
    producers: List<org.jellyfin.sdk.model.api.BaseItemPerson>,
    cast: List<org.jellyfin.sdk.model.api.BaseItemPerson>,
    getPersonImageUrl: (org.jellyfin.sdk.model.api.BaseItemPerson) -> String?,
    onPersonClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val perfConfig = rememberImmersivePerformanceConfig()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Section Title
        Text(
            text = "Cast & Crew",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        // Key Crew Information (Compact)
        if (directors.isNotEmpty() || writers.isNotEmpty() || producers.isNotEmpty()) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Directors
                    if (directors.isNotEmpty()) {
                        CrewInfoRow(
                            label = if (directors.size == 1) "Director" else "Directors",
                            people = directors,
                            onPersonClick = onPersonClick,
                        )
                    }

                    // Writers
                    if (writers.isNotEmpty()) {
                        CrewInfoRow(
                            label = if (writers.size == 1) "Writer" else "Writers",
                            people = writers,
                            onPersonClick = onPersonClick,
                        )
                    }

                    // Producers
                    if (producers.isNotEmpty()) {
                        CrewInfoRow(
                            label = if (producers.size == 1) "Producer" else "Producers",
                            people = producers,
                            onPersonClick = onPersonClick,
                        )
                    }
                }
            }
        }

        // Cast Section
        if (cast.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Cast",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(ImmersiveDimens.SpacingRowTight),
                ) {
                    items(cast.take(perfConfig.maxRowItems), key = { it.id.toString() }) { person ->
                        CastMemberCard(
                            person = person,
                            imageUrl = getPersonImageUrl(person),
                            onPersonClick = onPersonClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CrewInfoRow(
    label: String,
    people: List<org.jellyfin.sdk.model.api.BaseItemPerson>,
    onPersonClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            people.forEachIndexed { index, person ->
                Text(
                    text = (person.name ?: "Unknown") + if (index < people.size - 1) "," else "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        onPersonClick(person.id.toString(), person.name ?: "Unknown")
                    },
                )
            }
        }
    }
}
