/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.viewmodels

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.lineageos.twelve.ext.SONGS_SORTING_REVERSE_KEY
import org.lineageos.twelve.ext.SONGS_SORTING_STRATEGY_KEY
import org.lineageos.twelve.ext.preferenceFlow
import org.lineageos.twelve.ext.songsSortingRule
import org.lineageos.twelve.models.Audio
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.FlowResult.Companion.asFlowResult
import org.lineageos.twelve.models.SortingRule

class SongsViewModel(application: Application) : TwelveViewModel(application) {
    private val refreshTrigger = MutableStateFlow(0)

    val sortingRule = sharedPreferences.preferenceFlow(
        SONGS_SORTING_STRATEGY_KEY,
        SONGS_SORTING_REVERSE_KEY,
        getter = { songsSortingRule },
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val audios = combine(
        sortingRule,
        refreshTrigger,
    ) { rule, _ -> rule }
        .flatMapLatest { mediaRepository.audios(it) }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = FlowResult.Loading,
        )

    fun setSortingRule(sortingRule: SortingRule) {
        sharedPreferences.songsSortingRule = sortingRule
    }

    fun refresh() {
        refreshTrigger.value += 1
    }

    fun playSong(audios: List<Audio>, position: Int) {
        playAudio(audios, position)
    }
}
