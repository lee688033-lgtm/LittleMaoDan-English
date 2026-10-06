package com.example.cet6vocabulary.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.cet6vocabulary.ui.theme.CET6VocabularyTheme

/**
 * Public entry point for the bottom navigation.
 *
 * The visuals live in [MaoDanLiquidGlassBottomBar]; this wrapper exists so MainActivity keeps the
 * contract it has always had - items, selected index, selection callback - and so the legacy
 * Brand* icon names stay available at the call site. No navigation decision is made here.
 */
@Composable
fun BrandBottomBar(
    items: List<CET6NavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    MaoDanLiquidGlassBottomBar(
        items = items,
        selectedIndex = selectedIndex,
        onItemSelected = onItemSelected,
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F9FC, widthDp = 360, heightDp = 140)
@Composable
private fun BrandBottomBarPreview() {
    CET6VocabularyTheme {
        BrandBottomBar(
            items = listOf(
                CET6NavigationItem("首页", LucideNavigationIcons.Home),
                CET6NavigationItem("单词", LucideNavigationIcons.Word),
                CET6NavigationItem("背诵", LucideNavigationIcons.Study),
                CET6NavigationItem("拼写", LucideNavigationIcons.Spell),
                CET6NavigationItem("我的", LucideNavigationIcons.Profile)
            ),
            selectedIndex = 0,
            onItemSelected = {}
        )
    }
}

// Compatibility names keep MainActivity's navigation contract unchanged; the vectors behind
// them are the Lucide navigation icons (the legacy names are kept only for the call site).
val BrandEggIcon: ImageVector = LucideNavigationIcons.Home
val BrandDictionaryIcon: ImageVector = LucideNavigationIcons.Word
val BrandMemoryIcon: ImageVector = LucideNavigationIcons.Study
val BrandSpellingIcon: ImageVector = LucideNavigationIcons.Spell
val BrandAvatarIcon: ImageVector = LucideNavigationIcons.Profile
