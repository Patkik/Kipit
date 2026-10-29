package com.kipit.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Light=lightColorScheme(
    primary=Color(0xFF176349),onPrimary=Color(0xFFF5FAF6),primaryContainer=Color(0xFFDDECE3),onPrimaryContainer=Color(0xFF143C2B),
    secondary=Color(0xFF4E665A),onSecondary=Color(0xFFF5FAF6),secondaryContainer=Color(0xFFE5EBE6),onSecondaryContainer=Color(0xFF21372B),
    tertiary=Color(0xFF4C617D),tertiaryContainer=Color(0xFFE0E8EF),onTertiaryContainer=Color(0xFF25354B),
    background=Color(0xFFF4F6F2),onBackground=Color(0xFF1A2A21),surface=Color(0xFFF4F6F2),onSurface=Color(0xFF1A2A21),
    surfaceContainerLowest=Color(0xFFFDFEFA),surfaceContainerLow=Color(0xFFEDF1EB),surfaceContainer=Color(0xFFE5EBE3),surfaceContainerHigh=Color(0xFFDDE5DB),surfaceContainerHighest=Color(0xFFD5DFD3),
    surfaceVariant=Color(0xFFE5EBE3),onSurfaceVariant=Color(0xFF526156),outline=Color(0xFF737F74),outlineVariant=Color(0xFFC9D3C8),
    error=Color(0xFFAD3D30),errorContainer=Color(0xFFFFDAD3),onErrorContainer=Color(0xFF5A170F))
private val Dark=darkColorScheme(
    primary=Color(0xFFA3D9BE),onPrimary=Color(0xFF0C3826),primaryContainer=Color(0xFF284D3D),onPrimaryContainer=Color(0xFFD8EDDE),
    secondary=Color(0xFFB7CCBD),onSecondary=Color(0xFF21372B),secondaryContainer=Color(0xFF304539),onSecondaryContainer=Color(0xFFDAE7DC),
    tertiary=Color(0xFFB9CBE3),tertiaryContainer=Color(0xFF2D4058),onTertiaryContainer=Color(0xFFDCE8F8),
    background=Color(0xFF122019),onBackground=Color(0xFFE4EBE2),surface=Color(0xFF122019),onSurface=Color(0xFFE4EBE2),
    surfaceContainerLowest=Color(0xFF0E1913),surfaceContainerLow=Color(0xFF192A20),surfaceContainer=Color(0xFF213328),surfaceContainerHigh=Color(0xFF2B3D31),surfaceContainerHighest=Color(0xFF35473B),
    surfaceVariant=Color(0xFF35473B),onSurfaceVariant=Color(0xFFBAC9BD),outline=Color(0xFF8A9D8E),outlineVariant=Color(0xFF465B4B),
    error=Color(0xFFFFB4A8),errorContainer=Color(0xFF64251B),onErrorContainer=Color(0xFFFFDAD3))
private val Base=Typography()
private val Type=Typography(
    displaySmall=Base.displaySmall.copy(fontSize=38.sp,lineHeight=46.sp,fontWeight=FontWeight.SemiBold,fontFeatureSettings="tnum"),
    headlineLarge=Base.headlineLarge.copy(fontFeatureSettings="tnum"),headlineMedium=Base.headlineMedium.copy(fontFeatureSettings="tnum"),
    titleLarge=Base.titleLarge.copy(fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.SemiBold),
    titleMedium=Base.titleMedium.copy(fontSize=16.sp,lineHeight=24.sp,fontWeight=FontWeight.SemiBold,fontFeatureSettings="tnum"),
    bodyLarge=Base.bodyLarge.copy(fontSize=16.sp,lineHeight=24.sp),bodyMedium=Base.bodyMedium.copy(fontSize=14.sp,lineHeight=21.sp),
    bodySmall=Base.bodySmall.copy(fontSize=13.sp,lineHeight=18.sp),labelLarge=Base.labelLarge.copy(fontSize=14.sp,lineHeight=20.sp,fontWeight=FontWeight.Medium))
@Composable fun KipitTheme(content:@Composable ()->Unit) { MaterialTheme(colorScheme=if(isSystemInDarkTheme())Dark else Light,typography=Type,content=content) }
