package com.github.se.pooltrack.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * A flame in [color] with a bright white core, for the week streak; material-icons-extended isn't
 * used. Draw it untinted (with `Image`), since a tint would paint over the core.
 */
fun flameIcon(color: Color): ImageVector =
    ImageVector.Builder(
            name = "Flame",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        .addPath(pathData = addPathNodes(FLAME_PATH), fill = SolidColor(color))
        .addPath(
            pathData = addPathNodes(CORE_PATH),
            fill = SolidColor(Color.White),
            fillAlpha = 0.55f,
        )
        .build()

/** The outline of the flame, in a 24 × 24 box. */
private const val FLAME_PATH =
    "M12 2c1.2 3.2-.6 4.9-2 6.6C8.6 10.3 7 12 7 15a5 5 0 0 0 10 0" +
        "c0-2.3-1-3.9-2.1-5.3.1 1.6-.5 2.7-1.6 3.3.5-3.6-.3-7.6-1.3-11Z"

/** The bright core near the flame's base. */
private const val CORE_PATH =
    "M12 13c.6 1.5-.9 2.2-.9 3.6a1.9 1.9 0 0 0 3.8 0c0-1.4-1.3-2.4-2.9-3.6Z"
