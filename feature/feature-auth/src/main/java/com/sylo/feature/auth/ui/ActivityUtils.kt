package com.sylo.feature.auth.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/** Walks the Context chain to find the hosting [FragmentActivity] (needed for BiometricPrompt). */
internal tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
