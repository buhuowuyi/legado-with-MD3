package io.legado.app.ui.widget.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import io.legado.app.R
import io.legado.app.ui.theme.LegadoTheme
import io.legado.app.ui.theme.ThemeResolver
import io.legado.app.ui.widget.components.icon.AppIcon
import io.legado.app.ui.widget.components.icon.AppIcons
import io.legado.app.ui.widget.components.text.AppText
import io.legado.app.utils.activity
import kotlinx.coroutines.CoroutineScope
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit = {},
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = {
        AppIcon(
            modifier = Modifier.padding(horizontal = 12.dp),
            imageVector = AppIcons.Search,
            contentDescription = null
        )
    },
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    scrollState: LazyListState? = null,
    scope: CoroutineScope = rememberCoroutineScope(),
    trailingIcon: @Composable (() -> Unit)? = null,
    autoFocus: Boolean = true,
    dropdownMenu: (@Composable (onDismiss: () -> Unit) -> Unit)? = null
) {
    val textFieldState = rememberTextFieldState(initialText = query)
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    var hasFocused by rememberSaveable { mutableStateOf(false) }

    // 在输入法动作回调内直接隐藏键盘，部分输入法 / ROM 会忽略这次请求：
    // 先释放焦点，再分别走 Compose 控制器与窗口 Insets 两条通道。
    val hideKeyboard = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        view.activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).hide(WindowInsetsCompat.Type.ime())
        }
    }

    val submitSearch: (String) -> Unit = { value ->
        onSearch(value)
        hideKeyboard()
        // 回调返回后的下一轮消息循环再兜底一次，避免隐藏请求被同一帧的后续焦点 / 输入命令覆盖
        view.post { hideKeyboard() }
    }

    LaunchedEffect(autoFocus) {
        if (autoFocus && !hasFocused) {
            focusRequester.requestFocus()
            // 某些情况下需要手动调用 show() 确保键盘弹出
            keyboardController?.show()
            hasFocused = true
        } else if (!autoFocus) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    LaunchedEffect(query) {
        if (query != textFieldState.text.toString()) {
            textFieldState.edit {
                replace(0, length, query)
            }
        }
    }

    LaunchedEffect(textFieldState.text) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { newText ->
                if (newText != query) {
                    onQueryChange(newText)
                }
            }
    }

    val isMiuix = ThemeResolver.isMiuixEngine(LegadoTheme.composeEngine)
    val resolvedBackgroundColor = if (backgroundColor != Color.Unspecified) {
        backgroundColor
    } else {
        if (isMiuix) MiuixTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow
    }
    val resolvedPlaceholder = placeholder ?: stringResource(R.string.search_placeholder)

    if (isMiuix) {
        AppDenseTextField(
            state = textFieldState,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .focusRequester(focusRequester),
            placeholder = { AppText(resolvedPlaceholder) },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            onKeyboardAction = {
                submitSearch(textFieldState.text.toString())
            },
            lineLimits = TextFieldLineLimits.SingleLine,
            backgroundColor = resolvedBackgroundColor,
            miuixUseSearchBarInputField = true,
            miuixSearchBarLabel = resolvedPlaceholder,
            miuixOnSearch = submitSearch,
        )
    } else {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            shape = RoundedCornerShape(32.dp),
            color = resolvedBackgroundColor
        ) {
            AppDenseTextField(
                state = textFieldState,
                modifier = modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { AppText(resolvedPlaceholder) },
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onKeyboardAction = {
                    submitSearch(textFieldState.text.toString())
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                backgroundColor = Color.Transparent
            )
        }
    }
}
