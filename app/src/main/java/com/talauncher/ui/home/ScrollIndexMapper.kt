package com.talauncher.ui.home

import com.talauncher.data.model.AppSectionLayoutOption
import kotlin.math.ceil

/**
 * Maps global app indices (based on SectionIndex order: Pinned → Recent → All)
 * to adapter indices in the HomeScreen's LazyColumn, accounting for headers,
 * spacers, and grid row chunking. Also provides helpers for row snapping.
 */
object ScrollIndexMapper {

    /**
     * Maps a global app index to the adapter index in the LazyColumn.
     *
     * - List mode: one adapter item per app (plus optional headers/spacers)
     * - Grid mode: one adapter item per row; we snap to the row that contains the app
     *
     * Headers/spacers are included only for sections that are in LIST layout.
     */
    fun globalToAdapterIndex(
        globalIndex: Int,
        pinnedCount: Int,
        recentCount: Int,
        allCount: Int,
        pinnedLayout: AppSectionLayoutOption,
        recentLayout: AppSectionLayoutOption,
        allLayout: AppSectionLayoutOption
    ): Int {
        val pinnedHeader = if (pinnedCount > 0 && pinnedLayout == AppSectionLayoutOption.LIST) 1 else 0
        val pinnedSpacer = if (pinnedCount > 0 && pinnedLayout == AppSectionLayoutOption.LIST) 1 else 0
        val pinnedRows = rowsFor(pinnedCount, pinnedLayout)

        val recentHeader = if (recentCount > 0 && recentLayout == AppSectionLayoutOption.LIST) 1 else 0
        val recentRows = rowsFor(recentCount, recentLayout)
        // The combined Spacer + "All Apps" title exists only when recent exists and All Apps is LIST
        val allAppsHeader = if (recentCount > 0 && allLayout == AppSectionLayoutOption.LIST) 1 else 0

        // Compute adapter offsets for section starts
        val pinnedStartAdapter = 0 + pinnedHeader
        val recentStartAdapter = pinnedStartAdapter + pinnedRows + pinnedSpacer
        val allStartAdapter = recentStartAdapter + recentHeader + recentRows + allAppsHeader

        return when {
            globalIndex < pinnedCount -> {
                // Pinned
                val intra = globalIndex
                pinnedStartAdapter + indexWithinLayoutToAdapterDelta(intra, pinnedLayout)
            }
            globalIndex < pinnedCount + recentCount -> {
                // Recent
                val intra = globalIndex - pinnedCount
                recentStartAdapter + indexWithinLayoutToAdapterDelta(intra, recentLayout)
            }
            else -> {
                // All Apps
                val intra = (globalIndex - pinnedCount - recentCount).coerceAtLeast(0)
                allStartAdapter + indexWithinLayoutToAdapterDelta(intra, allLayout)
            }
        }
    }

    /**
     * Returns the row-aligned global index for a given global index in grid layouts.
     * For list layouts, returns the original index.
     */
    fun snapGlobalIndexToRowStart(
        globalIndex: Int,
        pinnedCount: Int,
        recentCount: Int,
        pinnedLayout: AppSectionLayoutOption,
        recentLayout: AppSectionLayoutOption,
        allLayout: AppSectionLayoutOption
    ): Int {
        return when {
            globalIndex < pinnedCount -> {
                val cols = pinnedLayout.columns
                if (pinnedLayout == AppSectionLayoutOption.LIST) globalIndex
                else (globalIndex / cols) * cols
            }
            globalIndex < pinnedCount + recentCount -> {
                val intra = globalIndex - pinnedCount
                val cols = recentLayout.columns
                if (recentLayout == AppSectionLayoutOption.LIST) globalIndex
                else pinnedCount + (intra / cols) * cols
            }
            else -> {
                val intra = globalIndex - pinnedCount - recentCount
                val cols = allLayout.columns
                if (allLayout == AppSectionLayoutOption.LIST) globalIndex
                else pinnedCount + recentCount + (intra / cols) * cols
            }
        }
    }

    /**
     * Helper: converts an intra-section index into adapter delta based on layout.
     * List → one per item; Grid → one per row (floor(intra/columns)).
     */
    private fun indexWithinLayoutToAdapterDelta(
        intraIndex: Int,
        layout: AppSectionLayoutOption
    ): Int {
        return when (layout) {
            AppSectionLayoutOption.LIST -> intraIndex
            AppSectionLayoutOption.GRID_3, AppSectionLayoutOption.GRID_4 -> intraIndex / layout.columns
        }
    }

    /**
     * Helper: number of adapter rows for a section.
     */
    private fun rowsFor(count: Int, layout: AppSectionLayoutOption): Int {
        return when (layout) {
            AppSectionLayoutOption.LIST -> count
            AppSectionLayoutOption.GRID_3, AppSectionLayoutOption.GRID_4 ->
                if (count == 0) 0 else ceil(count / layout.columns.toFloat()).toInt()
        }
    }
}

