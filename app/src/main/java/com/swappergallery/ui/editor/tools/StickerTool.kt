package com.swappergallery.ui.editor.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swappergallery.data.model.LayerData
import com.swappergallery.data.model.LayerType
import com.swappergallery.ui.editor.components.SliderControl

val stickerEmojis = listOf(
    // Smileys
    "\uD83D\uDE00", "\uD83D\uDE03", "\uD83D\uDE04", "\uD83D\uDE01", "\uD83D\uDE06",
    "\uD83D\uDE05", "\uD83D\uDE02", "\uD83E\uDD23", "\uD83D\uDE0A", "\uD83D\uDE07",
    "\uD83D\uDE42", "\uD83D\uDE43", "\uD83D\uDE09", "\uD83D\uDE0C", "\uD83D\uDE0D",
    "\uD83E\uDD70", "\uD83D\uDE18", "\uD83D\uDE17", "\uD83D\uDE1A", "\uD83D\uDE19",
    "\uD83E\uDD29", "\uD83D\uDE0B", "\uD83D\uDE1B", "\uD83D\uDE1C", "\uD83E\uDD2A",
    "\uD83D\uDE1D", "\uD83E\uDD11", "\uD83E\uDD17", "\uD83E\uDD2D", "\uD83E\uDD2B",
    "\uD83E\uDD14", "\uD83E\uDD28", "\uD83D\uDE10", "\uD83D\uDE11", "\uD83D\uDE36",
    "\uD83D\uDE0F", "\uD83D\uDE12", "\uD83D\uDE44", "\uD83D\uDE2C", "\uD83E\uDD25",
    "\uD83D\uDE0E", "\uD83E\uDD13", "\uD83E\uDD78",
    "\uD83D\uDE14", "\uD83D\uDE1E", "\uD83D\uDE1F", "\uD83D\uDE15", "\uD83D\uDE41",
    "\uD83D\uDE23", "\uD83D\uDE16", "\uD83D\uDE2B", "\uD83D\uDE29", "\uD83E\uDD7A",
    "\uD83D\uDE22", "\uD83D\uDE2D", "\uD83D\uDE24", "\uD83D\uDE20", "\uD83D\uDE21",
    "\uD83E\uDD2C", "\uD83E\uDD2F", "\uD83D\uDE33", "\uD83E\uDD75", "\uD83E\uDD76",
    "\uD83D\uDE31", "\uD83D\uDE28", "\uD83D\uDE30", "\uD83D\uDE25", "\uD83D\uDE13",
    "\uD83D\uDE2A", "\uD83D\uDE34", "\uD83D\uDE35", "\uD83E\uDD10", "\uD83E\uDD74",
    "\uD83E\uDD22", "\uD83E\uDD2E", "\uD83E\uDD27", "\uD83D\uDE37", "\uD83E\uDD12",
    "\uD83E\uDD15", "\uD83D\uDE08", "\uD83D\uDC7F", "\uD83D\uDC79", "\uD83D\uDC7A",
    "\uD83D\uDCA9", "\uD83D\uDC7B", "\uD83D\uDC80", "\u2620\uFE0F", "\uD83D\uDC7D",
    "\uD83E\uDD16", "\uD83C\uDF83", "\uD83D\uDE3A", "\uD83D\uDE38", "\uD83D\uDE39",
    "\uD83D\uDE3B", "\uD83D\uDE3C", "\uD83D\uDE3D", "\uD83D\uDE40", "\uD83D\uDE3F",
    "\uD83D\uDE3E",
    // Hands & gestures
    "\uD83D\uDC4B", "\uD83E\uDD1A", "\uD83D\uDD90\uFE0F", "\u270B", "\uD83D\uDD96",
    "\uD83D\uDC4C", "\uD83E\uDD0C", "\uD83E\uDD0F", "\u270C\uFE0F", "\uD83E\uDD1E",
    "\uD83E\uDD1F", "\uD83E\uDD18", "\uD83E\uDD19", "\uD83D\uDC48", "\uD83D\uDC49",
    "\uD83D\uDC46", "\uD83D\uDC47", "\u261D\uFE0F", "\uD83D\uDC4D", "\uD83D\uDC4E",
    "\u270A", "\uD83D\uDC4A", "\uD83E\uDD1B", "\uD83E\uDD1C", "\uD83D\uDC4F",
    "\uD83D\uDE4C", "\uD83D\uDC50", "\uD83E\uDD32", "\uD83E\uDD1D", "\uD83D\uDE4F",
    "\u270D\uFE0F", "\uD83D\uDCAA", "\uD83E\uDDB5", "\uD83E\uDDB6",
    // Hearts
    "\u2764\uFE0F", "\uD83E\uDDE1", "\uD83D\uDC9B", "\uD83D\uDC9A", "\uD83D\uDC99",
    "\uD83D\uDC9C", "\uD83E\uDD0E", "\uD83D\uDDA4", "\uD83E\uDD0D", "\uD83D\uDC94",
    "\u2763\uFE0F", "\uD83D\uDC95", "\uD83D\uDC9E", "\uD83D\uDC93", "\uD83D\uDC97",
    "\uD83D\uDC96", "\uD83D\uDC98", "\uD83D\uDC9D", "\uD83D\uDC9F", "\u2665\uFE0F",
    // Animals
    "\uD83D\uDC36", "\uD83D\uDC31", "\uD83D\uDC2D", "\uD83D\uDC39", "\uD83D\uDC30",
    "\uD83E\uDD8A", "\uD83D\uDC3B", "\uD83D\uDC3C", "\uD83D\uDC28", "\uD83D\uDC2F",
    "\uD83E\uDD81", "\uD83D\uDC2E", "\uD83D\uDC37", "\uD83D\uDC38", "\uD83D\uDC35",
    "\uD83D\uDE48", "\uD83D\uDE49", "\uD83D\uDE4A", "\uD83D\uDC12", "\uD83D\uDC14",
    "\uD83D\uDC27", "\uD83D\uDC26", "\uD83D\uDC24", "\uD83E\uDD86", "\uD83E\uDD85",
    "\uD83E\uDD89", "\uD83E\uDD87", "\uD83D\uDC3A", "\uD83D\uDC17", "\uD83D\uDC34",
    "\uD83E\uDD84", "\uD83D\uDC1D", "\uD83D\uDC1B", "\uD83E\uDD8B", "\uD83D\uDC0C",
    "\uD83D\uDC1A", "\uD83D\uDC1E", "\uD83D\uDC1C", "\uD83E\uDD97", "\uD83E\uDD82",
    "\uD83D\uDC0D", "\uD83E\uDD8E", "\uD83D\uDC22", "\uD83D\uDC20", "\uD83D\uDC21",
    "\uD83D\uDC19", "\uD83D\uDC1F", "\uD83D\uDC2C", "\uD83D\uDC33", "\uD83E\uDD88",
    "\uD83D\uDC0A", "\uD83D\uDC05", "\uD83D\uDC06", "\uD83E\uDD93", "\uD83E\uDD8D",
    "\uD83D\uDC18", "\uD83E\uDD9B", "\uD83D\uDC2A", "\uD83E\uDD92", "\uD83E\uDD98",
    // Nature
    "\uD83C\uDF3A", "\uD83C\uDF3B", "\uD83C\uDF3C", "\uD83C\uDF39", "\uD83C\uDF37",
    "\uD83C\uDF3E", "\uD83C\uDF3F", "\u2618\uFE0F", "\uD83C\uDF40", "\uD83C\uDF41",
    "\uD83C\uDF42", "\uD83C\uDF43", "\uD83C\uDF32", "\uD83C\uDF33", "\uD83C\uDF34",
    "\uD83C\uDF35", "\uD83C\uDF1E", "\uD83C\uDF1D", "\uD83C\uDF1B", "\uD83C\uDF1C",
    "\u2B50", "\uD83C\uDF1F", "\u2728", "\u26A1", "\uD83D\uDD25",
    "\uD83C\uDF08", "\u2601\uFE0F", "\u26C5", "\u2744\uFE0F", "\uD83C\uDF0A",
    // Food & drink
    "\uD83C\uDF4E", "\uD83C\uDF4F", "\uD83C\uDF4A", "\uD83C\uDF4B", "\uD83C\uDF4C",
    "\uD83C\uDF49", "\uD83C\uDF47", "\uD83C\uDF53", "\uD83E\uDED0", "\uD83C\uDF48",
    "\uD83C\uDF52", "\uD83C\uDF51", "\uD83E\uDD6D", "\uD83C\uDF4D", "\uD83E\uDD65",
    "\uD83E\uDD51", "\uD83C\uDF45", "\uD83C\uDF46", "\uD83E\uDD52", "\uD83E\uDD66",
    "\uD83C\uDF3D", "\uD83C\uDF36\uFE0F", "\uD83E\uDD54", "\uD83E\uDD55",
    "\uD83C\uDF54", "\uD83C\uDF55", "\uD83C\uDF2E", "\uD83C\uDF2F", "\uD83E\uDD59",
    "\uD83C\uDF73", "\uD83E\uDD5E", "\uD83E\uDDC0", "\uD83C\uDF56", "\uD83C\uDF57",
    "\uD83C\uDF5E", "\uD83E\uDD50", "\uD83E\uDD56", "\uD83C\uDF5F", "\uD83C\uDF5D",
    "\uD83C\uDF5C", "\uD83C\uDF63", "\uD83C\uDF5B", "\uD83C\uDF5A", "\uD83C\uDF59",
    "\uD83C\uDF58", "\uD83C\uDF70", "\uD83C\uDF82", "\uD83C\uDF67", "\uD83C\uDF68",
    "\uD83C\uDF66", "\uD83C\uDF6B", "\uD83C\uDF6C", "\uD83C\uDF6D", "\uD83C\uDF6E",
    "\u2615", "\uD83C\uDF75", "\uD83E\uDDC3", "\uD83C\uDF7A", "\uD83C\uDF77",
    "\uD83E\uDD42", "\uD83C\uDF78", "\uD83E\uDDC9", "\uD83E\uDD64",
    // Activities & sports
    "\u26BD", "\uD83C\uDFC0", "\uD83C\uDFC8", "\u26BE", "\uD83E\uDD4E",
    "\uD83C\uDFBE", "\uD83C\uDFD0", "\uD83C\uDFC9", "\uD83E\uDD4F", "\uD83C\uDFB1",
    "\uD83C\uDFD3", "\uD83C\uDFF8", "\uD83E\uDD4A", "\uD83E\uDD4B", "\u26F3",
    "\uD83C\uDFC4", "\uD83C\uDFCA", "\uD83D\uDEB4", "\uD83C\uDFCB\uFE0F", "\uD83E\uDD38",
    "\uD83C\uDFC6", "\uD83C\uDFC5", "\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49",
    "\uD83C\uDFAF", "\uD83C\uDFAE", "\uD83C\uDFB2", "\uD83E\uDDE9",
    // Travel & places
    "\uD83D\uDE97", "\uD83D\uDE95", "\uD83D\uDE8C", "\uD83D\uDE8E", "\uD83D\uDE91",
    "\uD83D\uDE92", "\uD83D\uDE93", "\uD83D\uDE99", "\uD83D\uDE9A", "\uD83D\uDE9B",
    "\uD83D\uDEB2", "\uD83D\uDEF5", "\uD83C\uDFCD\uFE0F", "\u2708\uFE0F", "\uD83D\uDE80",
    "\uD83D\uDEF8", "\uD83D\uDEA2", "\u26F5", "\uD83D\uDE82", "\uD83D\uDE81",
    "\uD83C\uDFE0", "\uD83C\uDFE2", "\uD83C\uDFEB", "\uD83C\uDFE5", "\uD83C\uDFEA",
    "\u26EA", "\uD83D\uDD4C", "\uD83D\uDD4D", "\uD83C\uDFF0", "\uD83C\uDFEF",
    "\uD83D\uDDFC", "\uD83D\uDDFD", "\uD83C\uDFDD\uFE0F", "\uD83C\uDFD6\uFE0F", "\uD83C\uDF05",
    // Objects
    "\u231A", "\uD83D\uDCF1", "\uD83D\uDCBB", "\u2328\uFE0F", "\uD83D\uDCF7",
    "\uD83D\uDCF9", "\uD83C\uDFA5", "\uD83D\uDCFA", "\uD83D\uDCFB", "\uD83C\uDFA7",
    "\uD83C\uDFB5", "\uD83C\uDFB6", "\uD83C\uDFA4", "\uD83C\uDFB8", "\uD83C\uDFB9",
    "\uD83C\uDFBA", "\uD83C\uDFBB", "\uD83E\uDE95", "\uD83C\uDFA8",
    "\uD83D\uDC8D", "\uD83D\uDC8E", "\uD83D\uDC51", "\uD83D\uDC5C", "\uD83D\uDC53",
    "\uD83C\uDF93", "\uD83D\uDCDA", "\uD83D\uDCDD", "\u270F\uFE0F", "\uD83D\uDD8A\uFE0F",
    "\uD83D\uDCC5", "\uD83D\uDCCE", "\u2702\uFE0F", "\uD83D\uDD12", "\uD83D\uDD13",
    "\uD83D\uDD11", "\uD83D\uDCA1", "\uD83D\uDD26", "\uD83D\uDD0B", "\uD83D\uDD0C",
    "\uD83E\uDDEA", "\uD83D\uDC8A", "\uD83E\uDE79", "\uD83E\uDDF9", "\uD83E\uDDFC",
    "\uD83E\uDDFB", "\uD83D\uDEBF", "\uD83D\uDEBD",
    "\uD83C\uDF81", "\uD83C\uDF88", "\uD83C\uDF89", "\uD83C\uDF8A", "\uD83C\uDF8E",
    "\uD83C\uDF8F", "\uD83C\uDFEE",
    // Arrows
    "\u27A1\uFE0F", "\u2B05\uFE0F", "\u2B06\uFE0F", "\u2B07\uFE0F",
    "\u2197\uFE0F", "\u2198\uFE0F", "\u2196\uFE0F", "\u2199\uFE0F",
    "\u21A9\uFE0F", "\u21AA\uFE0F", "\uD83D\uDD04", "\uD83D\uDD01", "\uD83D\uDD00",
    "\u25B6\uFE0F", "\u23E9", "\u25C0\uFE0F", "\u23EA", "\u23EB", "\u23EC",
    // Symbols & shapes
    "\u2B55", "\uD83D\uDD34", "\uD83D\uDD35", "\uD83D\uDFE2", "\uD83D\uDFE1",
    "\uD83D\uDFE0", "\uD83D\uDFE3", "\uD83D\uDFE4", "\u26AB", "\u26AA",
    "\uD83D\uDD36", "\uD83D\uDD37", "\uD83D\uDD38", "\uD83D\uDD39",
    "\u25AA\uFE0F", "\u25AB\uFE0F", "\u2B1B", "\u2B1C", "\u25FE", "\u25FD",
    "\u2714\uFE0F", "\u274C", "\u274E", "\u2753", "\u2754", "\u2755", "\u2757",
    "\u26A0\uFE0F", "\uD83D\uDEAB", "\u267B\uFE0F", "\u267E\uFE0F",
    "\uD83D\uDCAF", "\uD83D\uDCA5", "\uD83D\uDCAB", "\uD83D\uDCA2", "\uD83D\uDCAC",
    "\uD83D\uDC40", "\uD83D\uDDE3\uFE0F", "\uD83D\uDCA4", "\uD83D\uDCA8",
    "\uD83C\uDFF3\uFE0F", "\uD83C\uDFF4", "\uD83C\uDFC1",
    "\u00A9\uFE0F", "\u00AE\uFE0F", "\u2122\uFE0F",
    "\u0023\uFE0F\u20E3", "\u002A\uFE0F\u20E3",
    "\u0030\uFE0F\u20E3", "\u0031\uFE0F\u20E3", "\u0032\uFE0F\u20E3",
    "\u0033\uFE0F\u20E3", "\u0034\uFE0F\u20E3", "\u0035\uFE0F\u20E3",
    "\u0036\uFE0F\u20E3", "\u0037\uFE0F\u20E3", "\u0038\uFE0F\u20E3",
    "\u0039\uFE0F\u20E3", "\uD83D\uDD1F",
    // Flags (popular)
    "\uD83C\uDDEE\uD83C\uDDF1", "\uD83C\uDDFA\uD83C\uDDF8", "\uD83C\uDDEC\uD83C\uDDE7",
    "\uD83C\uDDEB\uD83C\uDDF7", "\uD83C\uDDE9\uD83C\uDDEA", "\uD83C\uDDEA\uD83C\uDDF8",
    "\uD83C\uDDEE\uD83C\uDDF9", "\uD83C\uDDEF\uD83C\uDDF5", "\uD83C\uDDE8\uD83C\uDDF3",
    "\uD83C\uDDF7\uD83C\uDDFA", "\uD83C\uDDE7\uD83C\uDDF7", "\uD83C\uDDEE\uD83C\uDDF3",
    "\uD83C\uDDF9\uD83C\uDDF7", "\uD83C\uDDE6\uD83C\uDDEA", "\uD83C\uDDF8\uD83C\uDDE6",
    "\uD83C\uDDF5\uD83C\uDDF8",
)

@Composable
fun StickerToolPanel(
    existingData: LayerData.StickerData? = null,
    onAddSticker: (LayerType, LayerData) -> Unit,
    onUpdateSticker: (LayerData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // When a sticker is selected, show editing controls + emoji picker
    if (existingData != null) {
        var rotation by remember { mutableFloatStateOf(existingData.rotation) }
        var scale by remember { mutableFloatStateOf(existingData.scale) }

        // Sync rotation/scale from external changes (e.g. canvas pinch gesture)
        LaunchedEffect(existingData.rotation) { rotation = existingData.rotation }
        LaunchedEffect(existingData.scale) { scale = existingData.scale }

        fun currentData() = existingData.copy(rotation = rotation, scale = scale)

        fun onChanged() { onUpdateSticker(currentData()) }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Selected: ${existingData.emoji}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            SliderControl(
                label = "Scale",
                value = scale,
                onValueChange = { scale = it; onChanged() },
                valueRange = 0.1f..10f
            )

            SliderControl(
                label = "Rotation",
                value = rotation,
                onValueChange = { rotation = it; onChanged() },
                valueRange = -180f..180f
            )

            Text(
                text = "Change sticker",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().height(200.dp)
            ) {
                items(stickerEmojis) { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 28.sp,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable {
                                // Replace the selected sticker's emoji
                                onUpdateSticker(currentData().copy(emoji = emoji))
                            }
                            .padding(4.dp),
                    )
                }
            }
        }
    } else {
        // No sticker selected — just show the emoji picker to add new stickers
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "Tap a sticker to add it",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().height(340.dp)
            ) {
                items(stickerEmojis) { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 28.sp,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable {
                                val data = LayerData.StickerData(emoji = emoji)
                                onAddSticker(LayerType.STICKER, data)
                            }
                            .padding(4.dp),
                    )
                }
            }
        }
    }
}
