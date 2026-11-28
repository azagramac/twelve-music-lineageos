/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.getViewProperty
import org.lineageos.twelve.ext.navigateSafe
import org.lineageos.twelve.ext.updatePadding
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.Playlist
import org.lineageos.twelve.models.SortingRule
import org.lineageos.twelve.models.SortingStrategy
import org.lineageos.twelve.ui.components.ExpressiveLinearWavyLoadingBar
import org.lineageos.twelve.ui.components.SortingButtonGroup
import org.lineageos.twelve.ui.recyclerview.SimpleListAdapter
import org.lineageos.twelve.ui.recyclerview.UniqueItemDiffCallback
import org.lineageos.twelve.ui.theme.setTwelveExpressiveContent
import org.lineageos.twelve.ui.views.ListItem
import org.lineageos.twelve.utils.PermissionsChecker
import org.lineageos.twelve.utils.PermissionsUtils
import org.lineageos.twelve.viewmodels.PlaylistsViewModel

/**
 * View all music playlists.
 */
class PlaylistsFragment : Fragment(R.layout.fragment_playlists) {
    // View models
    private val viewModel by viewModels<PlaylistsViewModel>()

    // Views
    private val createNewPlaylistButton by getViewProperty<Button>(R.id.createNewPlaylistButton)
    private val loadingComposeView by getViewProperty<ComposeView>(R.id.loadingComposeView)
    private val noElementsLinearLayout by getViewProperty<LinearLayout>(R.id.noElementsLinearLayout)
    private val recyclerView by getViewProperty<RecyclerView>(R.id.recyclerView)
    private val sortingComposeView by getViewProperty<ComposeView>(R.id.sortingComposeView)

    private var isLoading by mutableStateOf(true)

    // Recyclerview
    private val addNewPlaylistItem = Playlist.Builder(Uri.EMPTY).build()
    private val adapter = object : SimpleListAdapter<Playlist, ListItem>(
        UniqueItemDiffCallback(),
        ::ListItem,
    ) {
        override fun ViewHolder.onBindView(item: Playlist) {
            when (item === addNewPlaylistItem) {
                true -> {
                    view.setOnClickListener {
                        findNavController().navigateSafe(
                            R.id.action_mainFragment_to_fragment_create_playlist_dialog,
                            CreatePlaylistDialogFragment.createBundle(
                                providerIdentifier = viewModel.navigationProvider.value?.identifier
                            )
                        )
                    }
                    view.setOnLongClickListener(null)

                    view.setLeadingIconImage(R.drawable.ic_playlist_add)
                    view.setHeadlineText(R.string.create_playlist)
                }

                false -> {
                    view.setOnClickListener {
                        findNavController().navigateSafe(
                            R.id.action_mainFragment_to_fragment_playlist,
                            PlaylistFragment.createBundle(item.uri)
                        )
                    }
                    view.setOnLongClickListener {
                        findNavController().navigateSafe(
                            R.id.action_mainFragment_to_fragment_media_item_bottom_sheet_dialog,
                            MediaItemBottomSheetDialogFragment.createBundle(item.uri)
                        )
                        true
                    }

                    view.setLeadingIconImage(
                        when (item.type) {
                            Playlist.Type.PLAYLIST -> R.drawable.ic_playlist_play
                            Playlist.Type.FAVORITES -> R.drawable.ic_favorite
                        }
                    )
                    view.headlineText = item.name ?: getString(
                        when (item.type) {
                            Playlist.Type.PLAYLIST -> R.string.playlist_unknown
                            Playlist.Type.FAVORITES -> R.string.favorites_playlist
                        }
                    )
                }
            }
        }
    }

    // Permissions
    private val permissionsChecker = PermissionsChecker(
        this, PermissionsUtils.mainPermissions
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            v.updatePadding(
                insets,
                start = true,
                end = true,
                bottom = true,
            )

            windowInsets
        }

        val strategies = listOf(
            SortingStrategy.CREATION_DATE to getString(R.string.sort_by_creation_date),
            SortingStrategy.MODIFICATION_DATE to getString(R.string.sort_by_last_modified),
            SortingStrategy.NAME to getString(R.string.sort_by_name),
            SortingStrategy.PLAY_COUNT to getString(R.string.sort_by_play_count),
        )
        sortingComposeView.setTwelveExpressiveContent {
            val currentRule by viewModel.sortingRule.collectAsState(
                initial = SortingRule(SortingStrategy.NAME, false)
            )
            SortingButtonGroup(
                strategies = strategies,
                currentRule = currentRule,
                onSortingRuleChanged = viewModel::setSortingRule,
            )
        }
        loadingComposeView.setTwelveExpressiveContent {
            if (isLoading) {
                ExpressiveLinearWavyLoadingBar()
            }
        }

        recyclerView.adapter = adapter

        createNewPlaylistButton.setOnClickListener {
            findNavController().navigateSafe(
                R.id.action_mainFragment_to_fragment_create_playlist_dialog,
                CreatePlaylistDialogFragment.createBundle(
                    providerIdentifier = viewModel.navigationProvider.value?.identifier
                )
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                permissionsChecker.withPermissionsGranted {
                    loadData()
                }
            }
        }
    }

    override fun onDestroyView() {
        recyclerView.adapter = null

        super.onDestroyView()
    }

    private suspend fun loadData() {
        coroutineScope {
            launch {
                viewModel.playlists.collectLatest {
                    isLoading = it is FlowResult.Loading
                    loadingComposeView.isVisible = isLoading

                    when (it) {
                        is FlowResult.Loading -> {
                            // Do nothing
                        }

                        is FlowResult.Success -> {
                            val isEmpty = it.data.isEmpty()

                            adapter.submitList(
                                when (isEmpty) {
                                    true -> emptyList()
                                    false -> listOf(
                                        addNewPlaylistItem,
                                        *it.data.toTypedArray(),
                                    )
                                }
                            )

                            recyclerView.isVisible = !isEmpty
                            noElementsLinearLayout.isVisible = isEmpty
                        }

                        is FlowResult.Failure -> {
                            Log.e(
                                LOG_TAG,
                                "Failed to load playlists, error: ${it.error}",
                                it.throwable
                            )

                            adapter.submitList(emptyList())

                            recyclerView.isVisible = false
                            noElementsLinearLayout.isVisible = true
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val LOG_TAG = PlaylistsFragment::class.simpleName!!
    }
}
