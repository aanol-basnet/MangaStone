package com.personal.mangastone.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.personal.mangastone.domain.models.Manga

val LocalTitlePreference = compositionLocalOf { "en" }

@Composable
fun Manga.displayTitle(): String {
    val pref = LocalTitlePreference.current
    return if (pref == "romanji" && titleRomanji != null) titleRomanji else title
}
