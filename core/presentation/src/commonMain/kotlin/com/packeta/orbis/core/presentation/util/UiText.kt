package com.packeta.orbis.core.presentation.util

sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    class Resource(
        val id: Int,  // Android R.string.xxx ID
        val args: Array<Any> = arrayOf()
    ) : UiText
}