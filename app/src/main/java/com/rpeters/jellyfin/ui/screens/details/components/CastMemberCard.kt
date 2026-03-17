package com.rpeters.jellyfin.ui.screens.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rpeters.jellyfin.ui.theme.ImmersiveDimens

@Composable
fun CastMemberCard(
    person: org.jellyfin.sdk.model.api.BaseItemPerson,
    imageUrl: String?,
    onPersonClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(ImmersiveDimens.CastMemberWidth)
            .clickable {
                onPersonClick(person.id.toString(), person.name ?: "Unknown")
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Person Image
        Box(
            modifier = Modifier
                .size(ImmersiveDimens.CastMemberImageSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = person.name,
                modifier = Modifier.fillaspectRatio(1f),
                contentScale = ContentScale.Crop,
                error = {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp),
                    )
                },
                loading = {
                    Box(
                        modifier = Modifier
                            .fillaspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                },
            )
        }

        // Actor Name
        Text(
            text = person.name ?: "Unknown",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Role/Character
        person.role?.let { role ->
            Text(
                text = role,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// Extension function to fix the fillaspectRatio issue if it doesn't exist
private fun Modifier.fillaspectRatio(ratio: Float): Modifier = this.aspectRatio(ratio)
