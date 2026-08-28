package com.example.pool.ui.theme

import com.example.pool.R

data class AppIconOption(
    val id: String,
    val displayName: String,
    val previewDrawableRes: Int,
    val aliasClassName: String,
)

object AppIconCatalog {
    const val POND_ID = "pond"
    const val LEAF_ID = "leaf"

    val Pond: AppIconOption = AppIconOption(
        id = POND_ID,
        displayName = "深池",
        previewDrawableRes = R.drawable.ic_launcher_pond,
        aliasClassName = "com.example.pool.MainActivityAliasPond",
    )

    val Leaf: AppIconOption = AppIconOption(
        id = LEAF_ID,
        displayName = "清叶",
        previewDrawableRes = R.drawable.ic_launcher_leaf,
        aliasClassName = "com.example.pool.MainActivityAliasLeaf",
    )

    val Default: AppIconOption = Pond

    val all: List<AppIconOption> = listOf(Pond, Leaf)

    fun byId(id: String): AppIconOption = all.firstOrNull { it.id == id } ?: Default
}
