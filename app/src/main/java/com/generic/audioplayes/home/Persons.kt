package com.generic.audioplayes.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.ripple
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.generic.audioplayes.R
import com.generic.audioplayes.components.FullScreenSadMessage
import com.generic.audioplayes.components.more_options.OptionsAlertDialog
import com.generic.audioplayes.components.more_options.PersonOptions
import com.generic.audioplayes.data.music.PersonWithSongCount
import com.generic.audioplayes.ui.theme.HomeLibraryTokens
import com.generic.audioplayes.ui.theme.UiTokens

private val artistTilePalette = listOf(
    Color(0xFFE53935),
    Color(0xFF8E24AA),
    Color(0xFF3949AB),
    Color(0xFF00897B),
    Color(0xFFF4511E),
    Color(0xFF6D4C41),
    Color(0xFF5E35B1),
)

private fun tileColorForName(name: String): Color {
    val idx = name.hashCode().let { if (it < 0) -it else it } % artistTilePalette.size
    return artistTilePalette[idx]
}

@Composable
fun Persons(
    personsWithSongCount: List<PersonWithSongCount>?,
    onPersonClicked: (PersonWithSongCount) -> Unit,
    listState: LazyListState,
    selectedPerson: Person,
    onPersonSelect: (Person) -> Unit,
) {
    if (personsWithSongCount == null) return
    if (personsWithSongCount.isEmpty()) {
        FullScreenSadMessage(
            message = stringResource(R.string.no_artists_found),
            paddingValues = WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
                .asPaddingValues(),
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HomeLibraryTokens.contentHorizontalPadding),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom)
                .asPaddingValues(),
        ) {
            item {
                PersonFilter(
                    selectedPerson = selectedPerson,
                    onPersonSelect = onPersonSelect,
                )
            }
            items(
                items = personsWithSongCount,
                key = { it.name }
            ) { person ->
                PersonCard(
                    personWithSongCount = person,
                    onPersonClicked = onPersonClicked,
                )
            }
        }
    }
}

@Composable
fun PersonCard(
    personWithSongCount: PersonWithSongCount,
    onPersonClicked: (PersonWithSongCount) -> Unit,
    options: List<PersonOptions> = listOf()
) {
    val tile = tileColorForName(personWithSongCount.name)
    val letter = personWithSongCount.name.trim().take(1).uppercase().ifEmpty { "?" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = { onPersonClicked(personWithSongCount) })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tile),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = personWithSongCount.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = pluralStringResource(
                    id = R.plurals.song_count,
                    count = personWithSongCount.count,
                    personWithSongCount.count
                ),
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.72f),
            )
        }
        if (options.isNotEmpty()) {
            var optionsVisible by remember { mutableStateOf(false) }
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_menu_button),
                modifier = Modifier
                    .size(UiTokens.artworkThumbMini)
                    .clickable(
                        onClick = {
                            optionsVisible = true
                        },
                        indication = ripple(
                            bounded = false,
                            radius = UiTokens.rippleSmall
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                tint = Color.White.copy(alpha = 0.85f),
            )
            if (optionsVisible) {
                OptionsAlertDialog(
                    options = options,
                    title = personWithSongCount.name,
                    onDismissRequest = { optionsVisible = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonFilter(
    selectedPerson: Person,
    onPersonSelect: (Person) -> Unit,
) {
    val pill = Color.White.copy(alpha = 0.22f)
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.listItemHeightCompact),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = UiTokens.paddingItemTight, vertical = UiTokens.paddingItem)
    ) {
        items(
            items = Person.values(),
        ) { person ->
            FilterChip(
                selected = (person == selectedPerson),
                onClick = { onPersonSelect(person) },
                label = {
                    Text(
                        text = person.text,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                },
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = pill,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    containerColor = Color.White.copy(alpha = 0.1f),
                    labelColor = Color.White.copy(alpha = 0.65f),
                ),
            )
        }
    }
}