/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.lineageos.twelve.R
import org.lineageos.twelve.models.SortingRule
import org.lineageos.twelve.models.SortingStrategy
import org.lineageos.twelve.ui.haptics.rememberTwelveHaptics

/**
 * Elastic M3 Expressive [ButtonGroup] replacing `TabLayout` in `LibraryFragment`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryTabsButtonGroup(
    titles: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberTwelveHaptics()

    ButtonGroup(
        overflowIndicator = { menuState ->
            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        titles.forEachIndexed { index, title ->
            toggleableItem(
                checked = index == selectedIndex,
                label = title,
                onCheckedChange = { checked ->
                    if (checked && index != selectedIndex) {
                        haptics.click()
                        onTabSelected(index)
                    }
                },
                weight = 1f,
            )
        }
    }
}

/**
 * Elastic M3 Expressive [ButtonGroup] replacing `SortingChip` in library tabs, allowing direct
 * springy selection of sorting strategy and ascending/descending order.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SortingButtonGroup(
    strategies: List<Pair<SortingStrategy, String>>,
    currentRule: SortingRule,
    onSortingRuleChanged: (SortingRule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberTwelveHaptics()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconToggleButton(
            checked = currentRule.reverse,
            onCheckedChange = { reversed ->
                haptics.click()
                onSortingRuleChanged(currentRule.copy(reverse = reversed))
            },
        ) {
            Icon(
                painter = painterResource(
                    if (currentRule.reverse) {
                        R.drawable.ic_sort_alphabetical_descending
                    } else {
                        R.drawable.ic_sort_alphabetical_ascending
                    }
                ),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }

        ButtonGroup(
            overflowIndicator = { menuState ->
                ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
            },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            strategies.forEach { (strategy, label) ->
                toggleableItem(
                    checked = currentRule.strategy == strategy,
                    label = label,
                    onCheckedChange = { checked ->
                        if (checked) {
                            haptics.click()
                            onSortingRuleChanged(currentRule.copy(strategy = strategy))
                        }
                    },
                )
            }
        }
    }
}
