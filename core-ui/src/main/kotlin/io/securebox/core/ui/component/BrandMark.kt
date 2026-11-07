package io.securebox.core.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.securebox.core.ui.R

/** The 2.5D SecureBox shield mark, shared by onboarding, dashboard and TV sidebar. */
@Composable
fun BrandMark(
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.logo_mark),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}
