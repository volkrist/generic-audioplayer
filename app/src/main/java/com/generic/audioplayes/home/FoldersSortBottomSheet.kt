package com.generic.audioplayes.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.generic.audioplayes.R
import com.generic.audioplayes.components.SortOptions

private val sheetBg = Color(0xFF1C1C1E)
private val accentOrange = Color(0xFFFF9800)

@Composable
private fun SortOptions.labelResForFolders(): Int = when (this) {
    SortOptions.Default -> R.string.sort_folders_default
    SortOptions.NameASC -> R.string.sort_folders_name_asc
    SortOptions.NameDSC -> R.string.sort_folders_name_dsc
    else -> R.string.sort
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersSortBottomSheet(
    visible: Boolean,
    options: List<SortOptions>,
    selectedOrdinal: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    if (!visible || options.isEmpty()) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pending by remember(selectedOrdinal, visible) { mutableStateOf(selectedOrdinal) }
    LaunchedEffect(visible, selectedOrdinal) {
        if (visible) pending = selectedOrdinal
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.songs_sort_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(options, key = { it.ordinal }) { option ->
                    val selected = pending == option.ordinal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pending = option.ordinal },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(option.labelResForFolders()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected) accentOrange else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                        )
                        RadioButton(
                            selected = selected,
                            onClick = { pending = option.ordinal },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentOrange,
                                unselectedColor = Color.White.copy(alpha = 0.45f),
                            ),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                    ),
                ) {
                    Text(stringResource(R.string.songs_sort_cancel).uppercase())
                }
                Button(
                    onClick = {
                        onConfirm(pending)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentOrange,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(stringResource(R.string.songs_sort_ok))
                }
            }
        }
    }
}
