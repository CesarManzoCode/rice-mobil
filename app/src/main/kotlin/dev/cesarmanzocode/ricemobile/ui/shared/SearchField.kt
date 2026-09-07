package dev.cesarmanzocode.ricemobile.ui.shared

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import dev.cesarmanzocode.ricemobile.R

/**
 * Bottom-anchored search field (contract §7/§18.1). The IME search action only opens an app
 * when there is exactly one visible result; otherwise it is a no-op, never a "first result"
 * shortcut.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int,
    onSearchSingleResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.imePadding(),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { if (resultCount == 1) onSearchSingleResult() },
        ),
    )
}
