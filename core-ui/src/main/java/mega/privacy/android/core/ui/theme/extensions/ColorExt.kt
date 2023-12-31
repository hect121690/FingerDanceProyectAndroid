package mega.privacy.android.core.ui.theme.extensions

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import mega.privacy.android.core.ui.theme.amber_300
import mega.privacy.android.core.ui.theme.amber_700
import mega.privacy.android.core.ui.theme.blue_200
import mega.privacy.android.core.ui.theme.blue_400
import mega.privacy.android.core.ui.theme.dark_blue_200
import mega.privacy.android.core.ui.theme.dark_blue_500
import mega.privacy.android.core.ui.theme.dark_grey
import mega.privacy.android.core.ui.theme.green_400
import mega.privacy.android.core.ui.theme.green_500
import mega.privacy.android.core.ui.theme.grey_100
import mega.privacy.android.core.ui.theme.grey_300
import mega.privacy.android.core.ui.theme.grey_600
import mega.privacy.android.core.ui.theme.grey_900
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_038
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.grey_alpha_060
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.lime_green_200
import mega.privacy.android.core.ui.theme.lime_green_500
import mega.privacy.android.core.ui.theme.red_300
import mega.privacy.android.core.ui.theme.red_400
import mega.privacy.android.core.ui.theme.red_600
import mega.privacy.android.core.ui.theme.red_800
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_038
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_060
import mega.privacy.android.core.ui.theme.white_alpha_087
import mega.privacy.android.core.ui.theme.yellow_300
import mega.privacy.android.core.ui.theme.yellow_600
import mega.privacy.android.core.ui.theme.yellow_700

/**
 * Text Color Secondary for Composable
 */
val Colors.textColorPrimary: Color
    get() = if (isLight) dark_grey else white

/**
 * Text Color Secondary for Composable
 */
val Colors.textColorSecondary: Color
    get() = if (isLight) grey_alpha_054 else white_alpha_054

@Deprecated(
    "Deprecated because custom color are accessed through MaterialTheme.Colors",
    ReplaceWith("mega.privacy.android.core.ui.theme.extensions.textColorSecondary")
)
/**
 * Text Color Secondary for Composable
 */
@Composable
fun MaterialTheme.textColorSecondary() = if (colors.isLight) grey_alpha_054 else white_alpha_054

/**
 * Text Color for Alpha 012 Color
 */
val Colors.grey012White038: Color
    get() = if (isLight) grey_alpha_012 else white_alpha_038

/**
 * Color theme for Red 600 when light mode and 300 when dark mode
 */
val Colors.red600_300: Color
    get() = if (isLight) red_600 else red_300

/**
 * Color theme for Yellow 600 when light mode and 300 when dark mode
 */
val Colors.yellow600_300: Color
    get() = if (isLight) yellow_600 else yellow_300

/**
 * Color theme for Green 500 when light mode and 400 when dark mode
 */
val Colors.green500_400: Color
    get() = if (isLight) green_500 else green_400

/**
 * Color theme for Lime 500 when light mode and 200 when dark mode
 */
val Colors.lime500_200: Color
    get() = if (isLight) lime_green_500 else lime_green_200

/**
 * Color theme for Dark Blue 500 when light mode and 200 when dark mode
 */
val Colors.darkBlue500_200: Color
    get() = if (isLight) dark_blue_500 else dark_blue_200

/**
 * Color theme for teal 300 when light mode and 200 when dark mode
 */
val Colors.teal_300_200: Color
    get() = if (isLight) teal_300 else teal_200

/**
 * Color theme for grey 012 when light mode and white 012 when dark mode
 */
val Colors.grey_012_white_012: Color
    get() = if (isLight) grey_alpha_012 else white_alpha_012

/**
 * Color theme for blue 400 when light mode and blue 200 when dark mode
 */
val Colors.blue_400_blue_200: Color
    get() = if (isLight) blue_400 else blue_200

/**
 * Color theme for white 087 when light mode and grey 087 when dark mode
 */
val Colors.white_087_grey_087: Color
    get() = if (isLight) white_alpha_087 else grey_alpha_087

/**
 * Color theme for grey 087 when light mode and white 087 when dark mode
 */
val Colors.grey_087_white_087: Color
    get() = if (isLight) grey_alpha_087 else white_alpha_087

/**
 * Color for taken down node
 */
val Colors.red_800_red_400: Color
    get() = if (isLight) red_800 else red_400

/**
 * Color for [grey_alpha_012] when light and [white_alpha_012] when dark
 */
val Colors.grey_white_alpha_012: Color
    get() = if (isLight) grey_alpha_012 else white_alpha_012

/**
 * Color for [grey_alpha_038] when light and [white_alpha_038] when dark
 */
val Colors.grey_white_alpha_038: Color
    get() = if (isLight) grey_alpha_038 else white_alpha_038

/**
 * Color for grey alpha when light [grey_alpha_054] and when dark [white_alpha_054]
 */
val Colors.grey_white_alpha_054: Color
    get() = if (isLight) grey_alpha_054 else white_alpha_054

/**
 * Color for grey alpha when light [grey_alpha_060] and when dark [white_alpha_060]
 */
val Colors.grey_white_alpha_060: Color
    get() = if (isLight) grey_alpha_060 else white_alpha_060

/**
 * Color for red  when light [red_600] and when dark [red_300]
 */
val Colors.red_600_red_300: Color
    get() = if (isLight) red_600 else red_300

/**
 * Color for grey alpha when light [grey_alpha_087] and yellow when dark [yellow_700]
 */
val Colors.grey_alpha_087_yellow_700: Color
    get() = if (isLight) grey_alpha_087 else yellow_700

/**
 * Color for amber 700 when light [amber_700] and amber 300 when dark [amber_300]
 */
val Colors.amber_700_amber_300: Color
    get() = if (isLight) amber_700 else amber_300

/**
 * Color for grey when light [grey_900] and when dark [grey_100]
 */
val Colors.grey_900_grey_100: Color
    get() = if (isLight) grey_900 else grey_100

/**
 * Color for grey when light [grey_300] and when dark [grey_600]
 */
val Colors.grey_300_grey_600: Color
    get() = if (isLight) grey_300 else grey_600