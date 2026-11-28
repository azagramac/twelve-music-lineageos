/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lineageos.twelve.R
import org.lineageos.twelve.ui.haptics.rememberTwelveHaptics

data class SplitButtonMenuAction(
    val label: String,
    val iconRes: Int,
    val onClick: () -> Unit,
)

/**
 * Material 3 Expressive [SplitButtonLayout] for compound track actions (primary: "Add to playlist",
 * secondary expandable menu: "Track details", "Add to queue", "Play next") with springy motion
 * and lightweight tactile haptics.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrackActionSplitButton(
    primaryLabel: String = stringResource(R.string.add_to_playlist),
    primaryIconRes: Int = R.drawable.ic_playlist_add,
    onPrimaryClick: () -> Unit,
    secondaryActions: List<SplitButtonMenuAction>,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberTwelveHaptics()
    var menuExpanded by remember { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (menuExpanded) 180f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "SplitButtonChevronRotation",
    )

    Box(modifier = modifier) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.TonalLeadingButton(
                    onClick = {
                        haptics.click()
                        onPrimaryClick()
                    },
                ) {
                    Icon(
                        painter = painterResource(primaryIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = primaryLabel,
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        maxLines = 1,
                    )
                }
            },
            trailingButton = {
                SplitButtonDefaults.TonalTrailingButton(
                    checked = menuExpanded,
                    onCheckedChange = { checked ->
                        haptics.click()
                        menuExpanded = checked
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                        contentDescription = stringResource(R.string.track_details),
                        modifier = Modifier
                            .size(SplitButtonDefaults.TrailingIconSize)
                            .graphicsLayer { rotationZ = chevronRotation },
                    )
                }
            },
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            secondaryActions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(action.iconRes),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        haptics.click()
                        action.onClick()
                    },
                )
            }
        }
    }
}
