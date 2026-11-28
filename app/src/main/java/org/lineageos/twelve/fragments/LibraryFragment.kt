/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.fragments

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.getViewProperty
import org.lineageos.twelve.ui.components.LibraryTabsButtonGroup
import org.lineageos.twelve.ui.theme.setTwelveExpressiveContent

/**
 * Music library.
 */
class LibraryFragment : Fragment(R.layout.fragment_library) {
    // Views
    private val tabsComposeView by getViewProperty<ComposeView>(R.id.tabsComposeView)
    private val viewPager2 by getViewProperty<ViewPager2>(R.id.viewPager2)

    private var selectedTabIndex by mutableIntStateOf(0)

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            selectedTabIndex = position
        }
    }

    // ViewPager2
    private enum class Menus(
        @param:StringRes val titleStringResId: Int,
        val fragment: () -> Fragment,
    ) {
        ALBUMS(
            R.string.library_fragment_menu_albums,
            { AlbumsFragment() },
        ),
        ARTISTS(
            R.string.library_fragment_menu_artists,
            { ArtistsFragment() },
        ),
        GENRES(
            R.string.library_fragment_menu_genres,
            { GenresFragment() },
        ),
        PLAYLISTS(
            R.string.library_fragment_menu_playlists,
            { PlaylistsFragment() },
        ),
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager2.adapter = object : FragmentStateAdapter(
            childFragmentManager, viewLifecycleOwner.lifecycle
        ) {
            override fun getItemCount() = Menus.entries.size
            override fun createFragment(position: Int) = Menus.entries[position].fragment()
        }
        viewPager2.offscreenPageLimit = Menus.entries.size
        viewPager2.registerOnPageChangeCallback(pageChangeCallback)

        val titles = Menus.entries.map { getString(it.titleStringResId) }
        tabsComposeView.setTwelveExpressiveContent {
            LibraryTabsButtonGroup(
                titles = titles,
                selectedIndex = selectedTabIndex,
                onTabSelected = { index ->
                    selectedTabIndex = index
                    viewPager2.setCurrentItem(index, true)
                },
            )
        }
    }

    override fun onDestroyView() {
        viewPager2.unregisterOnPageChangeCallback(pageChangeCallback)
        viewPager2.adapter = null

        super.onDestroyView()
    }
}
