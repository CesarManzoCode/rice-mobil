package dev.cesarmanzocode.ricemobile.ui.shared

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.ImeAction

/**
 * Search is shared *behavior*, never a shared visual (contract §16 "no mismo search field en los
 * cinco"): the IME search action only opens an app when there is exactly one visible result,
 * otherwise it is a no-op. Each rice builds its own `BasicTextField` decoration (underline,
 * pill, bordered rectangle...) and wires these two values into it.
 */
val SearchImeOptions = KeyboardOptions(imeAction = ImeAction.Search)

@Composable
fun rememberSearchKeyboardActions(resultCount: Int, onSearchSingleResult: () -> Unit): KeyboardActions =
    remember(resultCount) { KeyboardActions(onSearch = { if (resultCount == 1) onSearchSingleResult() }) }
