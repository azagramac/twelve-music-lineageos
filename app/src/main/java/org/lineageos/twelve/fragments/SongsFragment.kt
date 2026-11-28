/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.navigateSafe
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.SortingRule
import org.lineageos.twelve.models.SortingStrategy
import org.lineageos.twelve.repositories.MediaRepository
import org.lineageos.twelve.ui.components.ExpressiveLinearWavyLoadingBar
import org.lineageos.twelve.ui.components.SortingButtonGroup
import org.lineageos.twelve.ui.components.TrackListItem
import org.lineageos.twelve.ui.theme.setTwelveExpressiveContent
import org.lineageos.twelve.viewmodels.SongsViewModel

/**
 * View all songs in the active music provider, rendered in 100% Compose M3 Expressive.
 */
class SongsFragment : Fragment() {
    private val viewModel by viewModels<SongsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setTwelveExpressiveContent {
            val sortingRule by viewModel.sortingRule.collectAsState(
                initial = MediaRepository.defaultAudiosSortingRule
            )
            val audiosResult by viewModel.audios.collectAsState()

            val strategies = listOf(
                SortingStrategy.NAME to stringResource(R.string.sort_by_title),
                SortingStrategy.ARTIST_NAME to stringResource(R.string.sort_by_artist_name),
                SortingStrategy.CREATION_DATE to stringResource(R.string.sort_by_release_date),
                SortingStrategy.PLAY_COUNT to stringResource(R.string.sort_by_play_count),
            )

            Column(modifier = Modifier.fillMaxSize()) {
                SortingButtonGroup(
                    strategies = strategies,
                    currentRule = sortingRule,
                    onSortingRuleChanged = viewModel::setSortingRule,
                )

                Box(modifier = Modifier.weight(1f)) {
                    when (val result = audiosResult) {
                        is FlowResult.Loading -> {
                            ExpressiveLinearWavyLoadingBar()
                        }

                        is FlowResult.Success -> {
                            val songs = result.data
                            if (songs.isEmpty()) {
                                EmptySongsState()
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .navigationBarsPadding(),
                                    contentPadding = PaddingValues(bottom = 88.dp),
                                ) {
                                    itemsIndexed(
                                        items = songs,
                                        key = { _, audio -> audio.uri.toString() },
                                    ) { index, audio ->
                                        TrackListItem(
                                            title = audio.title,
                                            artistName = audio.artistName,
                                            durationMs = audio.durationMs,
                                            mimeType = audio.mimeType,
                                            onClick = {
                                                viewModel.playSong(songs, index)
                                            },
                                            onLongClick = {
                                                findNavController().navigateSafe(
                                                    R.id.action_mainFragment_to_fragment_media_item_bottom_sheet_dialog,
                                                    MediaItemBottomSheetDialogFragment.createBundle(
                                                        audio.uri,
                                                    ),
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }

                        is FlowResult.Failure -> {
                            EmptySongsState()
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun EmptySongsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = stringResource(R.string.no_songs),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.no_songs),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
