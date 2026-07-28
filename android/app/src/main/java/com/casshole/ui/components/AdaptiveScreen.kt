package com.casshole.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Window width class (Material [WindowWidthSizeClass]), computed once in
 * MainActivity via `calculateWindowSizeClass` and made available to any
 * screen through [LocalWindowWidthSizeClass.current]. Defaults to Compact
 * (phone) if read outside the composition tree MainActivity provides.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
val LocalWindowWidthSizeClass: ProvidableCompositionLocal<WindowWidthSizeClass> =
    compositionLocalOf { WindowWidthSizeClass.Compact }

/**
 * Max content width outside the Compact class — tablets and open foldable
 * screens fall into Medium or Expanded, and without this limit phone-first
 * lists/cards stretch edge to edge with huge empty gaps. In Compact (the
 * vast majority of phones) this has no effect at all.
 */
private val MAX_CONTENT_WIDTH = 600.dp

/**
 * Wraps a screen's main content, centering it and limiting its width
 * outside the Compact class. Replaces the top-level `Modifier.fillMaxWidth()`
 * of each screen — the inner content (LazyColumn/Column) keeps using
 * `fillMaxWidth()` as usual, it just now fills up to the ceiling imposed
 * here instead of the whole screen.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AdaptiveScreen(
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopCenter,
    content: @Composable () -> Unit,
) {
    val windowWidth = LocalWindowWidthSizeClass.current
    Box(modifier = modifier.fillMaxSize(), contentAlignment = alignment) {
        Box(
            modifier = if (windowWidth == WindowWidthSizeClass.Compact) {
                Modifier.fillMaxWidth()
            } else {
                Modifier
                    .widthIn(max = MAX_CONTENT_WIDTH)
                    .fillMaxWidth()
            },
        ) {
            content()
        }
    }
}
