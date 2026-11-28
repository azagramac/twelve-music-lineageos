/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.Bundle
import org.lineageos.twelve.ext.getParcelable
import org.lineageos.twelve.ext.getViewProperty
import org.lineageos.twelve.ext.loadThumbnail
import org.lineageos.twelve.ext.navigateSafe
import org.lineageos.twelve.ext.setProgressCompat
import org.lineageos.twelve.ext.updatePadding
import org.lineageos.twelve.models.Error
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.ui.components.TrackListItem
import org.lineageos.twelve.ui.recyclerview.SimpleListAdapter
import org.lineageos.twelve.ui.recyclerview.UniqueItemDiffCallback
import org.lineageos.twelve.ui.theme.setTwelveExpressiveContent
import org.lineageos.twelve.utils.PermissionsChecker
import org.lineageos.twelve.utils.PermissionsUtils
import org.lineageos.twelve.viewmodels.AlbumViewModel

/**
 * Single music album viewer.
 */
class AlbumFragment : CollapsingToolbarLayoutFragment(R.layout.fragment_album) {
    // View models
    private val viewModel by viewModels<AlbumViewModel>()

    // Views
    private val addToQueueButton by getViewProperty<MaterialButton>(R.id.addToQueue)
    private val albumTitleTextView by getViewProperty<TextView>(R.id.albumTitleTextView)
    override val appBarLayout by getViewProperty<AppBarLayout>(R.id.appBarLayout)
    private val artistNameTextView by getViewProperty<TextView>(R.id.artistNameTextView)
    override val coordinatorLayout by getViewProperty<CoordinatorLayout>(R.id.coordinatorLayout)
    private val fileTypeMaterialCardView by getViewProperty<MaterialCardView>(R.id.fileTypeMaterialCardView)
    private val fileTypeTextView by getViewProperty<TextView>(R.id.fileTypeTextView)
    private val infoNestedScrollView by getViewProperty<NestedScrollView?>(R.id.infoNestedScrollView)
    private val linearProgressIndicator by getViewProperty<LinearProgressIndicator>(R.id.linearProgressIndicator)
    private val noElementsNestedScrollView by getViewProperty<NestedScrollView>(R.id.noElementsNestedScrollView)
    private val playAllButton by getViewProperty<FloatingActionButton>(R.id.playAllButton)
    private val playButtonsLinearLayout by getViewProperty<LinearLayout>(R.id.playButtonsLinearLayout)
    private val playNextButton by getViewProperty<MaterialButton>(R.id.playNextButton)
    private val recyclerView by getViewProperty<RecyclerView>(R.id.recyclerView)
    private val shufflePlayButton by getViewProperty<MaterialButton>(R.id.shufflePlayButton)
    private val thumbnailImageView by getViewProperty<ImageView>(R.id.thumbnailImageView)
    private val toolbar by getViewProperty<MaterialToolbar>(R.id.toolbar)
    private val tracksInfoTextView by getViewProperty<TextView>(R.id.tracksInfoTextView)
    private val yearTextView by getViewProperty<TextView>(R.id.yearTextView)

    // Recyclerview
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private val adapter by lazy {
        object : SimpleListAdapter<AlbumViewModel.AlbumContent, ComposeView>(
            UniqueItemDiffCallback(),
            { context ->
                ComposeView(context).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                }
            },
        ) {
            override fun ViewHolder.onBindView(item: AlbumViewModel.AlbumContent) {
                view.setTwelveExpressiveContent {
                    when (item) {
                        is AlbumViewModel.AlbumContent.DiscHeader -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_album),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = stringResource(
                                        R.string.album_disc_header,
                                        item.discNumber,
                                    ),
                                    style = MaterialTheme.typography.titleSmallEmphasized,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        is AlbumViewModel.AlbumContent.AudioItem -> {
                            TrackListItem(
                                title = item.audio.title,
                                artistName = item.audio.artistName,
                                durationMs = item.audio.durationMs,
                                mimeType = item.audio.mimeType,
                                trackNumber = item.audio.trackNumber,
                                isCurrent = item.isCurrent,
                                onClick = {
                                    viewModel.playAlbum(item.audio)
                                },
                                onLongClick = {
                                    findNavController().navigateSafe(
                                        R.id.action_albumFragment_to_fragment_media_item_bottom_sheet_dialog,
                                        MediaItemBottomSheetDialogFragment.createBundle(
                                            item.audio.uri,
                                            fromAlbum = true,
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Arguments
    private val albumUri: Uri
        get() = requireArguments().getParcelable(ARG_ALBUM_URI, Uri::class)!!

    // Permissions
    private val permissionsChecker = PermissionsChecker(
        this, PermissionsUtils.mainPermissions
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())

            v.updatePadding(
                insets,
                start = true,
                end = true,
            )

            windowInsets
        }

        infoNestedScrollView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

                v.updatePadding(
                    insets,
                    bottom = true,
                )

                windowInsets
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updatePadding(
                insets,
                bottom = true,
            )

            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(noElementsNestedScrollView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updatePadding(
                insets,
                bottom = true,
            )

            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(playButtonsLinearLayout) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updatePadding(
                insets,
                bottom = true,
            )

            windowInsets
        }

        toolbar.setupWithNavController(findNavController())

        recyclerView.adapter = adapter

        addToQueueButton.setOnClickListener {
            viewModel.addToQueue()
        }

        playAllButton.setOnClickListener {
            viewModel.playAlbum()
        }

        playNextButton.setOnClickListener {
            viewModel.playNext()
        }

        shufflePlayButton.setOnClickListener {
            viewModel.shufflePlayAlbum()
        }

        viewModel.loadAlbum(albumUri)

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
                viewModel.album.collectLatest {
                    linearProgressIndicator.setProgressCompat(it)

                    when (it) {
                        is FlowResult.Loading -> {
                            // Do nothing
                        }

                        is FlowResult.Success -> {
                            val (album, audios) = it.data

                            album.title?.also { albumTitle ->
                                toolbar.title = albumTitle
                                albumTitleTextView.text = albumTitle
                            } ?: run {
                                toolbar.setTitle(R.string.album_unknown)
                                albumTitleTextView.setText(R.string.album_unknown)
                            }

                            thumbnailImageView.loadThumbnail(
                                album.thumbnail,
                                placeholder = R.drawable.ic_album
                            )

                            album.artistName?.also { artistName ->
                                artistNameTextView.text = artistName
                            } ?: artistNameTextView.setText(R.string.artist_unknown)
                            artistNameTextView.setOnClickListener {
                                album.artistUri?.let { artistUri ->
                                    findNavController().navigateSafe(
                                        R.id.action_albumFragment_to_fragment_artist,
                                        ArtistFragment.createBundle(artistUri)
                                    )
                                }
                            }

                            album.year?.also { year ->
                                yearTextView.isVisible = true
                                yearTextView.text = getString(R.string.year_format, year)
                            } ?: run {
                                yearTextView.isVisible = false
                            }

                            val totalDurationMs = audios.sumOf { audio ->
                                audio.durationMs ?: 0L
                            }
                            val totalDurationMinutes = (totalDurationMs / 1000 / 60).toInt()

                            val tracksCount = resources.getQuantityString(
                                R.plurals.tracks_count,
                                audios.size,
                                audios.size
                            )
                            val tracksDuration = resources.getQuantityString(
                                R.plurals.tracks_duration,
                                totalDurationMinutes,
                                totalDurationMinutes
                            )
                            tracksInfoTextView.text = getString(
                                R.string.tracks_info,
                                tracksCount, tracksDuration
                            )
                        }

                        is FlowResult.Failure -> {
                            Log.e(
                                LOG_TAG,
                                "Error loading album, error: ${it.error}",
                                it.throwable
                            )

                            toolbar.title = ""
                            albumTitleTextView.text = ""

                            if (it.error == Error.NOT_FOUND) {
                                // Get out of here
                                findNavController().navigateUp()
                            }
                        }
                    }
                }
            }

            launch {
                viewModel.albumContent.collectLatest {
                    adapter.submitList(it)

                    val isEmpty = it.isEmpty()
                    recyclerView.isVisible = !isEmpty
                    noElementsNestedScrollView.isVisible = isEmpty
                    when (isEmpty) {
                        true -> {
                            playButtonsLinearLayout.isVisible = false
                        }

                        false -> {
                            playButtonsLinearLayout.isVisible = true
                        }
                    }
                }
            }

            launch {
                viewModel.albumFileTypes.collectLatest {
                    fileTypeMaterialCardView.isVisible = it.isNotEmpty()
                    fileTypeTextView.text = it.joinToString(" / ")
                }
            }
        }
    }

    companion object {
        private val LOG_TAG = AlbumFragment::class.simpleName!!

        private const val ARG_ALBUM_URI = "album_uri"

        /**
         * Create a [Bundle] to use as the arguments for this fragment.
         * @param albumUri The URI of the album to display
         */
        fun createBundle(
            albumUri: Uri,
        ) = Bundle {
            putParcelable(ARG_ALBUM_URI, albumUri)
        }
    }
}
