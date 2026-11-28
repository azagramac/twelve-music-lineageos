/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.lineageos.twelve.R
import org.lineageos.twelve.models.SortingRule
import org.lineageos.twelve.models.SortingStrategy
import org.lineageos.twelve.ui.haptics.rememberTwelveHaptics

/**
 * Centered, springy Material 3 Expressive pill tabs replacing `TabLayout` in `LibraryFragment`.
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

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            titles.forEachIndexed { index, title ->
                val selected = index == selectedIndex
                ExpressivePillTab(
                    label = title,
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            haptics.click()
                            onTabSelected(index)
                        }
                    },
                )
            }
        }
    }
}

/**
 * Centered, springy Material 3 Expressive sorting control bar replacing `SortingChip`.
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

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
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

            strategies.forEach { (strategy, label) ->
                val selected = currentRule.strategy == strategy
                ExpressivePillTab(
                    label = label,
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            haptics.click()
                            onSortingRuleChanged(currentRule.copy(strategy = strategy))
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressivePillTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (selected) 24.dp else 12.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "PillTabCorner",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "PillTabContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "PillTabContent",
    )

    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            style = if (selected) {
                MaterialTheme.typography.labelLargeEmphasized
            } else {
                MaterialTheme.typography.labelLarge
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
