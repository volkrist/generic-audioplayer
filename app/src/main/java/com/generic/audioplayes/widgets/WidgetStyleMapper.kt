package com.generic.audioplayes.widgets

import com.generic.audioplayes.data.UserPreferences

fun WidgetStyle.toProto(): UserPreferences.WidgetStyle = when (this) {
    WidgetStyle.CLASSIC -> UserPreferences.WidgetStyle.WIDGET_STYLE_CLASSIC
    WidgetStyle.LITE -> UserPreferences.WidgetStyle.WIDGET_STYLE_LITE
    WidgetStyle.VINYL -> UserPreferences.WidgetStyle.WIDGET_STYLE_VINYL
    WidgetStyle.SIMPLE -> UserPreferences.WidgetStyle.WIDGET_STYLE_SIMPLE
    WidgetStyle.ROUND -> UserPreferences.WidgetStyle.WIDGET_STYLE_ROUND
    WidgetStyle.MINI -> UserPreferences.WidgetStyle.WIDGET_STYLE_MINI
    WidgetStyle.STANDARD -> UserPreferences.WidgetStyle.WIDGET_STYLE_STANDARD
    WidgetStyle.CARD -> UserPreferences.WidgetStyle.WIDGET_STYLE_CARD
    WidgetStyle.PRACTICAL -> UserPreferences.WidgetStyle.WIDGET_STYLE_PRACTICAL
    WidgetStyle.STYLISH -> UserPreferences.WidgetStyle.WIDGET_STYLE_STYLISH
    WidgetStyle.ICON -> UserPreferences.WidgetStyle.WIDGET_STYLE_ICON
}

fun UserPreferences.WidgetStyle.toUiWidgetStyle(): WidgetStyle = when (this) {
    UserPreferences.WidgetStyle.WIDGET_STYLE_CLASSIC -> WidgetStyle.CLASSIC
    UserPreferences.WidgetStyle.WIDGET_STYLE_LITE -> WidgetStyle.LITE
    UserPreferences.WidgetStyle.WIDGET_STYLE_VINYL -> WidgetStyle.VINYL
    UserPreferences.WidgetStyle.WIDGET_STYLE_SIMPLE -> WidgetStyle.SIMPLE
    UserPreferences.WidgetStyle.WIDGET_STYLE_ROUND -> WidgetStyle.ROUND
    UserPreferences.WidgetStyle.WIDGET_STYLE_MINI -> WidgetStyle.MINI
    UserPreferences.WidgetStyle.WIDGET_STYLE_STANDARD -> WidgetStyle.STANDARD
    UserPreferences.WidgetStyle.WIDGET_STYLE_CARD -> WidgetStyle.CARD
    UserPreferences.WidgetStyle.WIDGET_STYLE_PRACTICAL -> WidgetStyle.PRACTICAL
    UserPreferences.WidgetStyle.WIDGET_STYLE_STYLISH -> WidgetStyle.STYLISH
    UserPreferences.WidgetStyle.WIDGET_STYLE_ICON -> WidgetStyle.ICON
    else -> WidgetStyle.CLASSIC
}
