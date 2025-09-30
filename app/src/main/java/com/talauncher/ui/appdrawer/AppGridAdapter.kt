package com.talauncher.ui.appdrawer

import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.talauncher.utils.IconThemer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Example RecyclerView adapter that demonstrates how to theme icons using [IconThemer].
 * The adapter expects to operate inside a lifecycle-aware scope so that coroutines can be cancelled
 * automatically when the UI is destroyed.
 */
class AppGridAdapter(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private var themeColor: Int,
    private val iconSizePx: Int,
    private var entries: List<AppGridEntry>
) : RecyclerView.Adapter<AppGridAdapter.IconViewHolder>() {

    data class AppGridEntry(
        val label: String,
        val packageName: String
    )

    fun updateTheme(color: Int) {
        if (themeColor == color) return
        themeColor = color
        IconThemer.clearCache()
        notifyDataSetChanged()
    }

    fun submitList(newEntries: List<AppGridEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = FrameLayout.LayoutParams(iconSizePx, iconSizePx)
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = null
        }
        val container = FrameLayout(parent.context).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val padding = (iconSizePx * 0.1f).toInt()
            setPadding(padding, padding, padding, padding)
            addView(imageView)
        }
        return IconViewHolder(container, imageView)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val entry = entries[position]
        holder.bindPlaceholder()
        holder.loadJob?.cancel()
        holder.loadJob = coroutineScope.launch {
            val themed = IconThemer.themeIcon(context, entry.packageName, themeColor, iconSizePx)
            withContext(Dispatchers.Main) {
                holder.bindBitmap(themed, entry.label)
            }
        }
    }

    override fun getItemCount(): Int = entries.size

    override fun onViewRecycled(holder: IconViewHolder) {
        holder.loadJob?.cancel()
        holder.imageView.setImageDrawable(null)
    }

    class IconViewHolder(
        root: FrameLayout,
        val imageView: ImageView
    ) : RecyclerView.ViewHolder(root) {
        var loadJob: Job? = null

        fun bindPlaceholder() {
            imageView.setImageDrawable(null)
            imageView.contentDescription = null
        }

        fun bindBitmap(bitmap: Bitmap, label: String) {
            imageView.setImageBitmap(bitmap)
            imageView.contentDescription = label
        }
    }
}

