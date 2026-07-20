package com.harsh.shah.saavnmp3.utils

import androidx.recyclerview.widget.RecyclerView

/**
 * Attaches a StartSnapHelper so RecyclerView items snap smoothly to the
 * START edge when scrolling stops — like the fluid "magnetic" feel in Apple apps.
 *
 * Uses StartSnapHelper (not LinearSnapHelper) to prevent the first / last item
 * from being clipped when they can't reach the viewport center.
 *
 * Safe to call multiple times; will not attach duplicate helpers.
 */
fun RecyclerView.attachSnapHelper() {
    // Avoid attaching multiple snap helpers
    if (onFlingListener != null) return
    StartSnapHelper().attachToRecyclerView(this)
}

