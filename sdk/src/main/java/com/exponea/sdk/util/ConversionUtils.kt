package com.exponea.sdk.util

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import com.exponea.sdk.style.LayoutSpacing
import com.exponea.sdk.style.PlatformSize
import kotlin.math.max
import kotlin.text.RegexOption.IGNORE_CASE

internal class ConversionUtils {
    companion object {

        private val HEX_VALUE = Regex("[0-9A-F]", IGNORE_CASE)
        private val SHORT_HEX_FORMAT = Regex("^#([0-9A-F]){3}\$", IGNORE_CASE)
        private val SHORT_HEXA_FORMAT = Regex("^#[0-9A-F]{4}\$", IGNORE_CASE)
        private val HEX_FORMAT = Regex("^#[0-9A-F]{6}\$", IGNORE_CASE)
        private val HEXA_FORMAT = Regex("^#[0-9A-F]{8}\$", IGNORE_CASE)
        private val RGBA_FORMAT = Regex(
            "^rgba\\([ ]*([0-9]{1,3})[, ]+([0-9]{1,3})[, ]+([0-9]{1,3})[,/ ]+([0-9.]+)[ ]*\\)\$",
            IGNORE_CASE
        )
        private val ARGB_FORMAT = Regex(
            "^argb\\([ ]*([0-9.]{1,3})[, ]+([0-9]{1,3})[, ]+([0-9]{1,3})[, ]+([0-9]+)[ ]*\\)\$",
            IGNORE_CASE
        )
        private val RGB_FORMAT = Regex(
            "^rgb\\([ ]*([0-9]{1,3})[, ]+([0-9]{1,3})[, ]+([0-9]{1,3})[ ]*\\)\$",
            IGNORE_CASE
        )
        private val NAMED_COLORS = mapOf(
            // any color name from here: https://www.w3.org/wiki/CSS/Properties/color/keywords
            "aliceblue" to Color.parseColor("#f0f8ff"),
            "antiquewhite" to Color.parseColor("#faebd7"),
            "aqua" to Color.parseColor("#00ffff"),
            "aquamarine" to Color.parseColor("#7fffd4"),
            "azure" to Color.parseColor("#f0ffff"),
            "beige" to Color.parseColor("#f5f5dc"),
            "bisque" to Color.parseColor("#ffe4c4"),
            "black" to Color.parseColor("#000000"),
            "blanchedalmond" to Color.parseColor("#ffebcd"),
            "blue" to Color.parseColor("#0000ff"),
            "blueviolet" to Color.parseColor("#8a2be2"),
            "brown" to Color.parseColor("#a52a2a"),
            "burlywood" to Color.parseColor("#deb887"),
            "cadetblue" to Color.parseColor("#5f9ea0"),
            "chartreuse" to Color.parseColor("#7fff00"),
            "chocolate" to Color.parseColor("#d2691e"),
            "coral" to Color.parseColor("#ff7f50"),
            "cornflowerblue" to Color.parseColor("#6495ed"),
            "cornsilk" to Color.parseColor("#fff8dc"),
            "crimson" to Color.parseColor("#dc143c"),
            "cyan" to Color.parseColor("#00ffff"),
            "darkblue" to Color.parseColor("#00008b"),
            "darkcyan" to Color.parseColor("#008b8b"),
            "darkgoldenrod" to Color.parseColor("#b8860b"),
            "darkgray" to Color.parseColor("#a9a9a9"),
            "darkgreen" to Color.parseColor("#006400"),
            "darkgrey" to Color.parseColor("#a9a9a9"),
            "darkkhaki" to Color.parseColor("#bdb76b"),
            "darkmagenta" to Color.parseColor("#8b008b"),
            "darkolivegreen" to Color.parseColor("#556b2f"),
            "darkorange" to Color.parseColor("#ff8c00"),
            "darkorchid" to Color.parseColor("#9932cc"),
            "darkred" to Color.parseColor("#8b0000"),
            "darksalmon" to Color.parseColor("#e9967a"),
            "darkseagreen" to Color.parseColor("#8fbc8f"),
            "darkslateblue" to Color.parseColor("#483d8b"),
            "darkslategray" to Color.parseColor("#2f4f4f"),
            "darkslategrey" to Color.parseColor("#2f4f4f"),
            "darkturquoise" to Color.parseColor("#00ced1"),
            "darkviolet" to Color.parseColor("#9400d3"),
            "deeppink" to Color.parseColor("#ff1493"),
            "deepskyblue" to Color.parseColor("#00bfff"),
            "dimgray" to Color.parseColor("#696969"),
            "dimgrey" to Color.parseColor("#696969"),
            "dodgerblue" to Color.parseColor("#1e90ff"),
            "firebrick" to Color.parseColor("#b22222"),
            "floralwhite" to Color.parseColor("#fffaf0"),
            "forestgreen" to Color.parseColor("#228b22"),
            "fuchsia" to Color.parseColor("#ff00ff"),
            "gainsboro" to Color.parseColor("#dcdcdc"),
            "ghostwhite" to Color.parseColor("#f8f8ff"),
            "gold" to Color.parseColor("#ffd700"),
            "goldenrod" to Color.parseColor("#daa520"),
            "gray" to Color.parseColor("#808080"),
            "green" to Color.parseColor("#008000"),
            "greenyellow" to Color.parseColor("#adff2f"),
            "grey" to Color.parseColor("#808080"),
            "honeydew" to Color.parseColor("#f0fff0"),
            "hotpink" to Color.parseColor("#ff69b4"),
            "indianred" to Color.parseColor("#cd5c5c"),
            "indigo" to Color.parseColor("#4b0082"),
            "ivory" to Color.parseColor("#fffff0"),
            "khaki" to Color.parseColor("#f0e68c"),
            "lavender" to Color.parseColor("#e6e6fa"),
            "lavenderblush" to Color.parseColor("#fff0f5"),
            "lawngreen" to Color.parseColor("#7cfc00"),
            "lemonchiffon" to Color.parseColor("#fffacd"),
            "lightblue" to Color.parseColor("#add8e6"),
            "lightcoral" to Color.parseColor("#f08080"),
            "lightcyan" to Color.parseColor("#e0ffff"),
            "lightgoldenrodyellow" to Color.parseColor("#fafad2"),
            "lightgray" to Color.parseColor("#d3d3d3"),
            "lightgreen" to Color.parseColor("#90ee90"),
            "lightgrey" to Color.parseColor("#d3d3d3"),
            "lightpink" to Color.parseColor("#ffb6c1"),
            "lightsalmon" to Color.parseColor("#ffa07a"),
            "lightseagreen" to Color.parseColor("#20b2aa"),
            "lightskyblue" to Color.parseColor("#87cefa"),
            "lightslategray" to Color.parseColor("#778899"),
            "lightslategrey" to Color.parseColor("#778899"),
            "lightsteelblue" to Color.parseColor("#b0c4de"),
            "lightyellow" to Color.parseColor("#ffffe0"),
            "lime" to Color.parseColor("#00ff00"),
            "limegreen" to Color.parseColor("#32cd32"),
            "linen" to Color.parseColor("#faf0e6"),
            "magenta" to Color.parseColor("#ff00ff"),
            "maroon" to Color.parseColor("#800000"),
            "mediumaquamarine" to Color.parseColor("#66cdaa"),
            "mediumblue" to Color.parseColor("#0000cd"),
            "mediumorchid" to Color.parseColor("#ba55d3"),
            "mediumpurple" to Color.parseColor("#9370db"),
            "mediumseagreen" to Color.parseColor("#3cb371"),
            "mediumslateblue" to Color.parseColor("#7b68ee"),
            "mediumspringgreen" to Color.parseColor("#00fa9a"),
            "mediumturquoise" to Color.parseColor("#48d1cc"),
            "mediumvioletred" to Color.parseColor("#c71585"),
            "midnightblue" to Color.parseColor("#191970"),
            "mintcream" to Color.parseColor("#f5fffa"),
            "mistyrose" to Color.parseColor("#ffe4e1"),
            "moccasin" to Color.parseColor("#ffe4b5"),
            "navajowhite" to Color.parseColor("#ffdead"),
            "navy" to Color.parseColor("#000080"),
            "oldlace" to Color.parseColor("#fdf5e6"),
            "olive" to Color.parseColor("#808000"),
            "olivedrab" to Color.parseColor("#6b8e23"),
            "orange" to Color.parseColor("#ffa500"),
            "orangered" to Color.parseColor("#ff4500"),
            "orchid" to Color.parseColor("#da70d6"),
            "palegoldenrod" to Color.parseColor("#eee8aa"),
            "palegreen" to Color.parseColor("#98fb98"),
            "paleturquoise" to Color.parseColor("#afeeee"),
            "palevioletred" to Color.parseColor("#db7093"),
            "papayawhip" to Color.parseColor("#ffefd5"),
            "peachpuff" to Color.parseColor("#ffdab9"),
            "peru" to Color.parseColor("#cd853f"),
            "pink" to Color.parseColor("#ffc0cb"),
            "plum" to Color.parseColor("#dda0dd"),
            "powderblue" to Color.parseColor("#b0e0e6"),
            "purple" to Color.parseColor("#800080"),
            "red" to Color.parseColor("#ff0000"),
            "rosybrown" to Color.parseColor("#bc8f8f"),
            "royalblue" to Color.parseColor("#4169e1"),
            "saddlebrown" to Color.parseColor("#8b4513"),
            "salmon" to Color.parseColor("#fa8072"),
            "sandybrown" to Color.parseColor("#f4a460"),
            "seagreen" to Color.parseColor("#2e8b57"),
            "seashell" to Color.parseColor("#fff5ee"),
            "sienna" to Color.parseColor("#a0522d"),
            "silver" to Color.parseColor("#c0c0c0"),
            "skyblue" to Color.parseColor("#87ceeb"),
            "slateblue" to Color.parseColor("#6a5acd"),
            "slategray" to Color.parseColor("#708090"),
            "slategrey" to Color.parseColor("#708090"),
            "snow" to Color.parseColor("#fffafa"),
            "springgreen" to Color.parseColor("#00ff7f"),
            "steelblue" to Color.parseColor("#4682b4"),
            "tan" to Color.parseColor("#d2b48c"),
            "teal" to Color.parseColor("#008080"),
            "thistle" to Color.parseColor("#d8bfd8"),
            "tomato" to Color.parseColor("#ff6347"),
            "turquoise" to Color.parseColor("#40e0d0"),
            "violet" to Color.parseColor("#ee82ee"),
            "wheat" to Color.parseColor("#f5deb3"),
            "white" to Color.parseColor("#ffffff"),
            "whitesmoke" to Color.parseColor("#f5f5f5"),
            "yellow" to Color.parseColor("#ffff00"),
            "yellowgreen" to Color.parseColor("#9acd32")
        )

        fun parseColor(colorString: String?): Int? {
            try {
                val trimmedColorString = colorString?.trim()

                if (trimmedColorString.isNullOrBlank()) {
                    return null
                }
                if (SHORT_HEX_FORMAT.matches(trimmedColorString)) {
                    val hexFormat = HEX_VALUE.replace(trimmedColorString) {
                        // doubles HEX value
                        it.value + it.value
                    }
                    return Color.parseColor(hexFormat)
                }
                if (HEX_FORMAT.matches(trimmedColorString)) {
                    return Color.parseColor(trimmedColorString)
                }
                if (SHORT_HEXA_FORMAT.matches(trimmedColorString)) {
                    val hexaFormat = HEX_VALUE.replace(trimmedColorString) {
                        // doubles HEX value
                        it.value + it.value
                    }
                    // #RRGGBBAA (css) -> #AARRGGBB (android)
                    val ahexFormat = "#${hexaFormat.substring(7, 9)}${hexaFormat.substring(1, 7)}"
                    return Color.parseColor(ahexFormat)
                }
                if (HEXA_FORMAT.matches(trimmedColorString)) {
                    // #RRGGBBAA (css) -> #AARRGGBB (android)
                    val ahexFormat = "#${trimmedColorString.substring(7, 9)}${trimmedColorString.substring(1, 7)}"
                    return Color.parseColor(ahexFormat)
                }
                if (RGBA_FORMAT.matches(trimmedColorString)) {
                    // rgba(255, 255, 255, 1.0)
                    // rgba(255 255 255 / 1.0)
                    val parts = RGBA_FORMAT.findAll(trimmedColorString).flatMap { it.groupValues }.toList()
                    return Color.argb(
                        // parts[0] skip!
                        (parts[4].toFloat() * 255).toInt(),
                        parts[1].toInt(),
                        parts[2].toInt(),
                        parts[3].toInt()
                    )
                }
                if (ARGB_FORMAT.matches(trimmedColorString)) {
                    // argb(1.0, 255, 0, 0)
                    val parts = ARGB_FORMAT.findAll(trimmedColorString).flatMap { it.groupValues }.toList()
                    return Color.argb(
                        // parts[0] skip!
                        (parts[1].toFloat() * 255).toInt(),
                        parts[2].toInt(),
                        parts[3].toInt(),
                        parts[4].toInt()
                    )
                }
                if (RGB_FORMAT.matches(trimmedColorString)) {
                    // rgb(255, 255, 255)
                    val parts = RGB_FORMAT.findAll(trimmedColorString).flatMap { it.groupValues }.toList()
                    return Color.rgb(
                        // parts[0] skip!
                        parts[1].toInt(),
                        parts[2].toInt(),
                        parts[3].toInt()
                    )
                }
                return NAMED_COLORS[trimmedColorString.lowercase()]
            } catch (e: IllegalArgumentException) {
                Logger.e(this, "Unable to parse color from $colorString")
                return null
            }
        }

        fun parseSize(size: String?): PlatformSize? {
            return size?.let {
                return PlatformSize(
                    sizeType(it),
                    sizeValue(it)
                )
            }
        }

        fun parseSize(size: Number?): PlatformSize? {
            return size?.let {
                return PlatformSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    it.toFloat()
                )
            }
        }

        internal fun sizeType(source: String): Int {
            return when {
                source.endsWith("px", true) -> TypedValue.COMPLEX_UNIT_PX
                source.endsWith("in", true) -> TypedValue.COMPLEX_UNIT_IN
                source.endsWith("mm", true) -> TypedValue.COMPLEX_UNIT_MM
                source.endsWith("pt", true) -> TypedValue.COMPLEX_UNIT_PT
                source.endsWith("dp", true) -> TypedValue.COMPLEX_UNIT_DIP
                source.endsWith("dip", true) -> TypedValue.COMPLEX_UNIT_DIP
                source.endsWith("sp", true) -> TypedValue.COMPLEX_UNIT_SP
                else -> TypedValue.COMPLEX_UNIT_PX
            }
        }

        internal fun sizeValue(source: String): Float {
            val parsed = source.filter { it.isDigit() || it == '.' }.toFloatOrNull()
            if (parsed == null) {
                Logger.e(source, "Unable to read float value from $source")
            }
            val multiplier = if (source.trim().startsWith("-")) {
                -1F
            } else {
                1F
            }
            return (parsed ?: 0F) * multiplier
        }

        internal fun sizeTypeString(unit: Int): String {
            return when (unit) {
                TypedValue.COMPLEX_UNIT_PX -> "px"
                TypedValue.COMPLEX_UNIT_IN -> "in"
                TypedValue.COMPLEX_UNIT_MM -> "mm"
                TypedValue.COMPLEX_UNIT_PT -> "pt"
                TypedValue.COMPLEX_UNIT_DIP -> "dp"
                TypedValue.COMPLEX_UNIT_SP -> "sp"
                else -> "px"
            }
        }

        fun parseTypeface(source: String?): Int {
            when (source) {
                "normal" -> return Typeface.NORMAL
                "bold" -> return Typeface.BOLD
                "100" -> return Typeface.NORMAL
                "200" -> return Typeface.NORMAL
                "300" -> return Typeface.NORMAL
                "400" -> return Typeface.NORMAL
                "500" -> return Typeface.NORMAL
                "600" -> return Typeface.NORMAL
                "700" -> return Typeface.BOLD
                "800" -> return Typeface.BOLD
                "900" -> return Typeface.BOLD
                else -> return Typeface.NORMAL
            }
        }

        fun dpToPx(source: Int): Int {
            return (source * Resources.getSystem().displayMetrics.density).toInt()
        }

        fun toPx(source: PlatformSize): Float {
            return TypedValue.applyDimension(
                source.unit,
                source.size,
                Resources.getSystem().displayMetrics
            )
        }

        fun parseNumber(source: Any?): Number? {
            if (source == null) {
                return null
            }
            return when (source) {
                is Number -> source
                is String -> source.toFloatOrNull()
                else -> {
                    Logger.w(this, "Unable to parse number from $source")
                    null
                }
            }
        }

        /**
         * Recalculates padding to simulate CSS behaviour of padding for text UI component.
         * Explanation:
         * Android adds top and bottom padding to the "edge" of the text. Edge of text is mathematically determined by max
         * of lineHeight or textSize.
         * HTML/CSS adds top and bottom padding to the "edge" of the lineHeight and textSize is ignored. In normal case,
         * lineHeight is calculated as multiplier (1.2) of textSize. In case of custom lineHeight this correlation is lost.
         * Therefore we need to calculate the correct top/bottom padding to simulate this CSS behaviour.
         */
        fun simulateCssPaddingForText(
            source: LayoutSpacing,
            textSize: PlatformSize,
            lineHeight: PlatformSize
        ): LayoutSpacing {
            // android "edge" behaviour is == MAX(textSize,lineHeight)/2 + padding
            // css "edge" behaviour is == MAX(lineHeight/2 + padding, textSize/2)
            // we need to calculate the correct top/bottom padding to simulate CSS behaviour
            val cssEdgeDistanceTop = max(
                lineHeight.toPrecisePx() / 2 + source.top.toPrecisePx(),
                textSize.toPrecisePx() / 2
            )
            val cssEdgeDistanceBottom = max(
                lineHeight.toPrecisePx() / 2 + source.bottom.toPrecisePx(),
                textSize.toPrecisePx() / 2
            )
            val androidHalfSize = max(textSize.toPrecisePx(), lineHeight.toPrecisePx()) / 2
            val simulatedPaddingTop = max(cssEdgeDistanceTop - androidHalfSize, 0.0f)
            val simulatedPaddingBottom = max(cssEdgeDistanceBottom - androidHalfSize, 0.0f)
            val cssTextPadding = LayoutSpacing(
                left = source.left,
                right = source.right,
                top = PlatformSize(TypedValue.COMPLEX_UNIT_PX, simulatedPaddingTop),
                bottom = PlatformSize(TypedValue.COMPLEX_UNIT_PX, simulatedPaddingBottom)
            )
            return cssTextPadding
        }
    }
}
