package com.utsman.geolibsample.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.*

fun logi(msg: String?) = Log.i("GEOLIB_SAMPLE", msg ?: "log....")

fun <T : View> Activity.findLazy(@IdRes id: Int): Lazy<T> {
    return lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        findViewById(id)
    }
}

infix fun Context.intent(clazz: Class<*>) {
    startActivity(Intent(this, clazz))
}

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
fun Context.toast(throwable: Throwable) = toast(msg = throwable.localizedMessage ?: "Error")

fun View.hideKeyboard() {
    val methodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    methodManager.hideSoftInputFromWindow(this.windowToken, 0)
}

fun TextInputEditText.showKeyboard() {
    requestFocus()
    val methodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    methodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun TextInputEditText.watcher(scope: CoroutineScope = MainScope(), result: (String) -> Unit) {

    val watcher = object : TextWatcher {
        private var searchFor = ""
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val searchText = s.toString().trim()
            if (searchText == searchFor)
                return

            searchFor = searchText

            scope.launch {
                delay(1000)
                if (searchText != searchFor)
                    return@launch

                if (searchText.length >= 2) {
                    result.invoke(searchText)
                }
            }
        }

        override fun afterTextChanged(s: Editable?) = Unit
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    }

    addTextChangedListener(watcher)
}

fun TextInputEditText.watchFocus(result: (Boolean) -> Unit) {
    setOnFocusChangeListener { _, hasFocus ->
        result.invoke(hasFocus)
    }
}

fun Activity.getStatusBarHeight(): Int {
    var result = 0
    val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = resources.getDimensionPixelSize(resourceId)
    }
    return result
}

fun Context.inflateLayout(layoutRes: Int): View {
    return LayoutInflater.from(this).inflate(layoutRes, null)
}

fun <T> debounce(
    waitMs: Long = 2000,
    scope: CoroutineScope,
    destinationFunction: (CoroutineScope, T) -> Unit
): (T) -> Unit {
    var debounceJob: Job? = null
    return { param: T ->
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(waitMs)
            destinationFunction(this, param)
        }
    }
}