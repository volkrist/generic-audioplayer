package com.github.pakka_papad.home

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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.github.pakka_papad.R
import com.github.pakka_papad.components.FullScreenSadMessage
import com.github.pakka_papad.components.more_options.OptionsAlertDialog
import com.github.pakka_papad.components.more_options.PersonOptions
import com.github.pakka_papad.data.music.PersonWithSongCount
import com.github.pakka_papad.ui.theme.HomeLibraryTokens
import com.github.pakka_papad.ui.theme.UiTokens

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.listItemHeightCompact)
            .clip(RoundedCornerShape(UiTokens.cornerMedium))
            .clickable(onClick = { onPersonClicked(personWithSongCount) })
            .padding(horizontal = UiTokens.paddingSection, vertical = UiTokens.metaSpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = personWithSongCount.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        indication = rememberRipple(
                            bounded = false,
                            radius = UiTokens.rippleSmall
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    )
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
    val scheme = MaterialTheme.colorScheme
    val pill = HomeLibraryTokens.navPill(scheme)
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
                    selectedLabelColor = scheme.onSurface,
                    selectedLeadingIconColor = scheme.onSurface,
                    containerColor = scheme.surfaceVariant.copy(alpha = 0.35f),
                    labelColor = scheme.onSurfaceVariant,
                ),
            )
        }
    }
}