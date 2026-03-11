package com.swappergallery.ui.editor.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swappergallery.data.model.LayerData
import com.swappergallery.data.model.LayerType

val stickerEmojis = listOf(
    "\u2764\uFE0F", "\uD83D\uDE00", "\uD83D\uDE02", "\uD83E\uDD23", "\uD83D\uDE0D",
    "\uD83E\uDD29", "\uD83D\uDE0E", "\uD83E\uDD14", "\uD83D\uDE31", "\uD83D\uDE21",
    "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDC4B", "\u270C\uFE0F", "\uD83D\uDE4F",
    "\uD83C\uDF1F", "\u2B50", "\uD83D\uDD25", "\uD83C\uDF08", "\u2601\uFE0F",
    "\uD83C\uDF3A", "\uD83C\uDF3B", "\uD83C\uDF3C", "\uD83C\uDF39", "\uD83C\uDF37",
    "\uD83C\uDF40", "\uD83C\uDF4E", "\uD83C\uDF53", "\uD83C\uDF89", "\uD83C\uDF8A",
    "\uD83D\uDCAF", "\uD83D\uDCA5", "\uD83D\uDCAB", "\uD83D\uDC8E", "\uD83D\uDC51",
    "\uD83C\uDFB5", "\uD83C\uDFA8", "\uD83D\uDCF7", "\uD83D\uDCCC", "\u2714\uFE0F",
    "\u274C", "\u2753", "\u2757", "\u26A0\uFE0F", "\uD83D\uDEAB",
    "\u25B6\uFE0F", "\u25C0\uFE0F", "\u25B2", "\u25BC", "\u2666\uFE0F",
)

@Composable
fun StickerToolPanel(
    onAddSticker: (LayerType, LayerData) -> Unit,
    modifier: Modifier = Modifier
) {
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
            columns = GridCells.Fixed(6),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(stickerEmojis) { emoji ->
                Text(
                    text = emoji,
                    fontSize = 32.sp,
                    modifier = Modifier
                        .size(48.dp)
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
