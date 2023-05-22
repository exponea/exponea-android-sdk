package com.exponea.style

import com.exponea.sdk.util.getNullSafely

class AppInboxStyleParser(private val source: Map<String, Any?>) {
    fun parse(): AppInboxStyle {
        return AppInboxStyle(
            appInboxButton = parseButtonStyle(source.getNullSafely("appInboxButton")),
            detailView = parseDetailViewStyle(source.getNullSafely("detailView")),
            listView = parseListScreenStyle(source.getNullSafely("listView"))
        )
    }

    private fun parseListScreenStyle(source: Map<String, Any?>?): ListScreenStyle? {
        if (source == null) {
            return null
        }
        return ListScreenStyle(
            emptyMessage = parseTextViewStyle(source.getNullSafely("emptyMessage")),
            emptyTitle = parseTextViewStyle(source.getNullSafely("emptyTitle")),
            errorMessage = parseTextViewStyle(source.getNullSafely("errorMessage")),
            errorTitle = parseTextViewStyle(source.getNullSafely("errorTitle")),
            list = parseListViewStyle(source.getNullSafely("list")),
            progress = parseProgressStyle(source.getNullSafely("progress"))
        )
    }

    private fun parseProgressStyle(source: Map<String, Any?>?): ProgressBarStyle? {
        if (source == null) {
            return null
        }
        return ProgressBarStyle(
            visible = source.getNullSafely("visible"),
            backgroundColor = source.getNullSafely("backgroundColor"),
            progressColor = source.getNullSafely("progressColor")
        )
    }

    private fun parseListViewStyle(source: Map<String, Any?>?): AppInboxListViewStyle? {
        if (source == null) {
            return null
        }
        return AppInboxListViewStyle(
            backgroundColor = source.getNullSafely("backgroundColor"),
            item = parseListItemStyle(source.getNullSafely("item"))
        )
    }

    private fun parseListItemStyle(source: Map<String, Any?>?): AppInboxListItemStyle? {
        if (source == null) {
            return null
        }
        return AppInboxListItemStyle(
            backgroundColor = source.getNullSafely("backgroundColor"),
            receivedTime = parseTextViewStyle(source.getNullSafely("receivedTime")),
            title = parseTextViewStyle(source.getNullSafely("title")),
            content = parseTextViewStyle(source.getNullSafely("content")),
            image = parseImageViewStyle(source.getNullSafely("image")),
            readFlag = parseImageViewStyle(source.getNullSafely("readFlag"))
        )
    }

    private fun parseImageViewStyle(source: Map<String, Any?>?): ImageViewStyle? {
        if (source == null) {
            return null
        }
        return ImageViewStyle(
            backgroundColor = source.getNullSafely("backgroundColor"),
            visible = source.getNullSafely("visible")
        )
    }

    private fun parseTextViewStyle(source: Map<String, Any?>?): TextViewStyle? {
        if (source == null) {
            return null
        }
        return TextViewStyle(
            textSize = source.getNullSafely("textSize"),
            textColor = source.getNullSafely("textColor"),
            textOverride = source.getNullSafely("textOverride"),
            textWeight = source.getNullSafely("textWeight"),
            visible = source.getNullSafely("visible")
        )
    }

    private fun parseDetailViewStyle(source: Map<String, Any?>?): DetailViewStyle? {
        if (source == null) {
            return null
        }
        return DetailViewStyle(
            button = parseButtonStyle(source.getNullSafely("button")),
            content = parseTextViewStyle(source.getNullSafely("content")),
            image = parseImageViewStyle(source.getNullSafely("image")),
            receivedTime = parseTextViewStyle(source.getNullSafely("receivedTime")),
            title = parseTextViewStyle(source.getNullSafely("title"))
        )
    }

    private fun parseButtonStyle(source: Map<String, Any?>?): ButtonStyle? {
        if (source == null) {
            return null
        }
        return ButtonStyle(
            textOverride = source.getNullSafely("textOverride"),
            textColor = source.getNullSafely("textColor"),
            backgroundColor = source.getNullSafely("backgroundColor"),
            showIcon = source.getNullSafely("showIcon"),
            textSize = source.getNullSafely("textSize"),
            enabled = source.getNullSafely("enabled"),
            borderRadius = source.getNullSafely("borderRadius"),
            textWeight = source.getNullSafely("textWeight")
        )
    }
}
