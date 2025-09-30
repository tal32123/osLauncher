package com.talauncher.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Simple RecyclerView adapter demonstrating how to consume [themeIcon] inside a launcher grid.
 */
class ThemedAppAdapter(
    private val context: Context,
    @ColorInt themeColor: Int,
    @Px private val iconSizePx: Int,
    scope: CoroutineScope? = null
) : RecyclerView.Adapter<ThemedAppAdapter.ViewHolder>() {

    private var items: List<ThemedAppGridItem> = emptyList()
    private var currentThemeColor: Int = themeColor
    private var iconScope: CoroutineScope = scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ownsScope: Boolean = scope == null

    class ViewHolder(container: FrameLayout) : RecyclerView.ViewHolder(container) {
        val iconView: ImageView = ImageView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            isClickable = false
            isFocusable = false
        }
        val labelView: TextView = TextView(container.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 12f
            isClickable = false
            isFocusable = false
        }
        var loadJob: Job? = null

        init {
            container.addView(iconView)
            container.addView(labelView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val frame = FrameLayout(parent.context).apply {
            this.layoutParams = layoutParams
            minimumHeight = (iconSizePx * 1.35f).toInt()
            setPadding(0, (iconSizePx * 0.08f).toInt(), 0, (iconSizePx * 0.2f).toInt())
        }
        return ViewHolder(frame)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.labelView.text = item.label
        holder.labelView.contentDescription = item.label
        holder.labelView.setTextColor(ensureContrast(holder.labelView.currentTextColor, currentThemeColor))
        holder.iconView.layoutParams = (holder.iconView.layoutParams as FrameLayout.LayoutParams).apply {
            width = iconSizePx
            height = iconSizePx
        }
        holder.iconView.setImageDrawable(null)
        holder.loadJob?.cancel()
        holder.loadJob = iconScope.launch {
            val themedBitmap: Bitmap = themeIcon(context, item.packageName, currentThemeColor, iconSizePx)
            withContext(Dispatchers.Main) {
                holder.iconView.setImageBitmap(themedBitmap)
                holder.iconView.contentDescription = item.label
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.loadJob?.cancel()
        holder.loadJob = null
        holder.iconView.setImageDrawable(null)
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<ThemedAppGridItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateThemeColor(@ColorInt color: Int) {
        if (color == currentThemeColor) return
        currentThemeColor = color
        notifyDataSetChanged()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        if (ownsScope) {
            iconScope.cancel()
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (ownsScope && !iconScope.isActive) {
            iconScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        }
    }
}

/** Simple value object representing an item in the grid. */
data class ThemedAppGridItem(
    val packageName: String,
    val label: String
)

/**
 * Helper extension for quickly wiring a grid layout to the RecyclerView when using the adapter
 * from Compose interop or activities.
 */
fun RecyclerView.useThemedGrid(columns: Int = 4) {
    if (layoutManager !is GridLayoutManager) {
        layoutManager = GridLayoutManager(context, columns)
    }
    setHasFixedSize(true)
}

