package io.securebox.core.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * SecureBox's own line-icon set. Drawn here instead of pulling a third-party icon pack so the
 * app has no external brand assets and every glyph matches the 24dp grid and 1.8dp stroke.
 * The stroke colour is white; [androidx.compose.material3.Icon] tints it at use site.
 */
object SecureBoxIcons {
    private const val STROKE: Float = 1.8f

    private fun icon(
        name: String,
        block: ImageVector.Builder.() -> Unit,
    ): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(block).build()

    private fun ImageVector.Builder.outline(block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }

    val Shield: ImageVector =
        icon("SecureBoxShield") {
            outline {
                moveTo(12f, 2.5f)
                lineTo(20f, 5.5f)
                lineTo(20f, 11f)
                lineTo(17.2f, 16.6f)
                lineTo(12f, 21f)
                lineTo(6.8f, 16.6f)
                lineTo(4f, 11f)
                lineTo(4f, 5.5f)
                close()
                moveTo(8.8f, 11.6f)
                lineTo(11.1f, 13.9f)
                lineTo(15.4f, 9.2f)
            }
        }

    val Network: ImageVector =
        icon("SecureBoxNetwork") {
            outline {
                moveTo(4f, 7f)
                lineTo(17f, 7f)
                moveTo(13.5f, 3.5f)
                lineTo(17f, 7f)
                lineTo(13.5f, 10.5f)
                moveTo(20f, 17f)
                lineTo(7f, 17f)
                moveTo(10.5f, 13.5f)
                lineTo(7f, 17f)
                lineTo(10.5f, 20.5f)
            }
        }

    val Apps: ImageVector =
        icon("SecureBoxApps") {
            outline {
                moveTo(4f, 4f)
                lineTo(10f, 4f)
                lineTo(10f, 10f)
                lineTo(4f, 10f)
                close()
                moveTo(14f, 4f)
                lineTo(20f, 4f)
                lineTo(20f, 10f)
                lineTo(14f, 10f)
                close()
                moveTo(4f, 14f)
                lineTo(10f, 14f)
                lineTo(10f, 20f)
                lineTo(4f, 20f)
                close()
                moveTo(14f, 14f)
                lineTo(20f, 14f)
                lineTo(20f, 20f)
                lineTo(14f, 20f)
                close()
            }
        }

    val Alert: ImageVector =
        icon("SecureBoxAlert") {
            outline {
                moveTo(12f, 3.5f)
                lineTo(21.5f, 20f)
                lineTo(2.5f, 20f)
                close()
                moveTo(12f, 9f)
                lineTo(12f, 14f)
                moveTo(12f, 17f)
                lineTo(12f, 17.1f)
            }
        }

    val Settings: ImageVector =
        icon("SecureBoxSettings") {
            outline {
                moveTo(7f, 4f)
                lineTo(7f, 20f)
                moveTo(4.5f, 8f)
                lineTo(9.5f, 8f)
                moveTo(12f, 4f)
                lineTo(12f, 20f)
                moveTo(9.5f, 15f)
                lineTo(14.5f, 15f)
                moveTo(17f, 4f)
                lineTo(17f, 20f)
                moveTo(14.5f, 7f)
                lineTo(19.5f, 7f)
            }
        }

    val Refresh: ImageVector =
        icon("SecureBoxRefresh") {
            outline {
                moveTo(4.5f, 12f)
                curveTo(4.5f, 7.9f, 7.9f, 4.5f, 12f, 4.5f)
                curveTo(15.4f, 4.5f, 18.3f, 6.8f, 19.2f, 10f)
                moveTo(19.6f, 6.3f)
                lineTo(19.2f, 10f)
                lineTo(15.6f, 9.4f)
                moveTo(19.5f, 12f)
                curveTo(19.5f, 16.1f, 16.1f, 19.5f, 12f, 19.5f)
                curveTo(8.6f, 19.5f, 5.7f, 17.2f, 4.8f, 14f)
                moveTo(4.4f, 17.7f)
                lineTo(4.8f, 14f)
                lineTo(8.4f, 14.6f)
            }
        }

    val Lock: ImageVector =
        icon("SecureBoxLock") {
            outline {
                moveTo(5f, 10.5f)
                lineTo(19f, 10.5f)
                lineTo(19f, 20.5f)
                lineTo(5f, 20.5f)
                close()
                moveTo(8f, 10.5f)
                lineTo(8f, 7.5f)
                curveTo(8f, 5.3f, 9.8f, 3.5f, 12f, 3.5f)
                curveTo(14.2f, 3.5f, 16f, 5.3f, 16f, 7.5f)
                lineTo(16f, 10.5f)
            }
        }

    val Check: ImageVector =
        icon("SecureBoxCheck") {
            outline {
                moveTo(4.5f, 12.5f)
                lineTo(9.5f, 17.5f)
                lineTo(20f, 6f)
            }
        }

    val ChevronRight: ImageVector =
        icon("SecureBoxChevronRight") {
            outline {
                moveTo(9.5f, 5f)
                lineTo(16.5f, 12f)
                lineTo(9.5f, 19f)
            }
        }

    val Globe: ImageVector =
        icon("SecureBoxGlobe") {
            outline {
                moveTo(12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                close()
                moveTo(3.5f, 12f)
                lineTo(20.5f, 12f)
                moveTo(12f, 3f)
                curveTo(9.5f, 5.5f, 9.5f, 18.5f, 12f, 21f)
                moveTo(12f, 3f)
                curveTo(14.5f, 5.5f, 14.5f, 18.5f, 12f, 21f)
            }
        }

    val Download: ImageVector =
        icon("SecureBoxDownload") {
            outline {
                moveTo(12f, 3.5f)
                lineTo(12f, 15f)
                moveTo(7.5f, 10.5f)
                lineTo(12f, 15f)
                lineTo(16.5f, 10.5f)
                moveTo(5f, 19.5f)
                lineTo(19f, 19.5f)
            }
        }

    val Phone: ImageVector =
        icon("SecureBoxPhone") {
            outline {
                moveTo(7f, 3f)
                lineTo(17f, 3f)
                lineTo(17f, 21f)
                lineTo(7f, 21f)
                close()
                moveTo(10.5f, 18f)
                lineTo(13.5f, 18f)
            }
        }

    val Tv: ImageVector =
        icon("SecureBoxTv") {
            outline {
                moveTo(3f, 5f)
                lineTo(21f, 5f)
                lineTo(21f, 17f)
                lineTo(3f, 17f)
                close()
                moveTo(9f, 21f)
                lineTo(15f, 21f)
                moveTo(12f, 17f)
                lineTo(12f, 21f)
            }
        }
}
