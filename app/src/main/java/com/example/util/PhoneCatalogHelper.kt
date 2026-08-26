package com.example.util

import android.content.Context
import androidx.core.content.edit

object PhoneCatalogHelper {

    private const val PREFS_NAME = "phone_catalog_prefs"
    private const val KEY_CUSTOM_BRANDS = "custom_brands_list"

    val DEFAULT_BRANDS = listOf(
        "Samsung",
        "Apple (iPhone)",
        "Xiaomi",
        "Redmi",
        "Poco",
        "Motorola",
        "Huawei",
        "Honor",
        "Realme",
        "Oppo",
        "Vivo",
        "Infinix",
        "Tecno",
        "ZTE",
        "Google Pixel",
        "OnePlus",
        "Sony",
        "Nokia",
        "Asus / ROG",
        "Nothing",
        "TCL",
        "Lenovo",
        "LG",
        "HTC",
        "Alcatel",
        "BLU",
        "Itel",
        "iQOO",
        "Meizu",
        "Blackview",
        "Ulefone",
        "Doogee",
        "Oukitel",
        "Umidigi",
        "Cubot",
        "Sharp",
        "Kyocera",
        "Cat (Caterpillar)",
        "Kalley",
        "Lanix",
        "Avvio",
        "Fairphone",
        "Lava",
        "Micromax",
        "Otro"
    )

    fun getBrands(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customBrandsSet = prefs.getStringSet(KEY_CUSTOM_BRANDS, emptySet()) ?: emptySet()
        val customSorted = customBrandsSet.toList().sorted()
        val withoutOther = DEFAULT_BRANDS.filter { it != "Otro" }
        return (withoutOther + customSorted + "Otro").distinct()
    }

    fun saveCustomBrand(context: Context, brand: String) {
        val clean = brand.trim()
        if (clean.isBlank() || clean.equals("Otro", ignoreCase = true) || DEFAULT_BRANDS.contains(clean)) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_CUSTOM_BRANDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(clean)
        prefs.edit { putStringSet(KEY_CUSTOM_BRANDS, current) }
    }
}
