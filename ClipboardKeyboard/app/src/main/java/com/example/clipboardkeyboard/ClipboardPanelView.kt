package com.example.clipboardkeyboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clipboardkeyboard.databinding.ClipboardPanelBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ClipboardPanelView(
    private val context: Context,
    private val repository: ClipboardRepository,
    private val scope: CoroutineScope
) {
    val rootView: View
    private val binding: ClipboardPanelBinding
    private val adapter: ClipboardAdapter
    private var selectedIds = mutableSetOf<Long>()
    private var isSelectionMode = false
    private var observeJob: Job? = null

    var onItemPaste: ((String) -> Unit)? = null

    init {
        binding = ClipboardPanelBinding.inflate(LayoutInflater.from(context))
        rootView = binding.root

        adapter = ClipboardAdapter(
            onItemClick = { item ->
                if (isSelectionMode) {
                    toggleSelection(item.id)
                } else {
                    onItemPaste?.invoke(item.text)
                }
            },
            onItemLongClick = { item ->
                enterSelectionMode()
                toggleSelection(item.id)
            },
            onPinClick = { item ->
                scope.launch { repository.togglePin(item) }
            },
            onDeleteClick = { item ->
                scope.launch { repository.deleteItem(item) }
            },
            isSelected = { id -> selectedIds.contains(id) },
            isSelectionMode = { isSelectionMode }
        )

        binding.rvClipboard.layoutManager = LinearLayoutManager(context)
        binding.rvClipboard.adapter = adapter

        setupSearch()
        setupActions()
        observeAllItems()
    }

    private fun observeAllItems() {
        observeJob?.cancel()
        observeJob = scope.launch {
            repository.getAllItems().collectLatest { items ->
                adapter.submitList(items)
                updateEmpty(items.isEmpty())
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            val query = text?.toString()?.trim() ?: ""
            observeJob?.cancel()
            observeJob = scope.launch {
                if (query.isBlank()) {
                    repository.getAllItems().collectLatest { items ->
                        adapter.submitList(items)
                        updateEmpty(items.isEmpty())
                    }
                } else {
                    repository.searchItems(query).collectLatest { items ->
                        adapter.submitList(items)
                        updateEmpty(items.isEmpty())
                    }
                }
            }
        }
    }

    private fun setupActions() {
        binding.btnClearAll.setOnClickListener {
            scope.launch { repository.clearAll() }
        }

        binding.btnDeleteSelected.setOnClickListener {
            scope.launch {
                repository.deleteItems(selectedIds.toList())
                exitSelectionMode()
            }
        }

        binding.btnCancelSelection.setOnClickListener {
            exitSelectionMode()
        }

        binding.btnSelectAll.setOnClickListener {
            val allIds = adapter.currentList.map { it.id }.toSet()
            selectedIds = allIds.toMutableSet()
            adapter.notifyDataSetChanged()
            updateSelectionToolbar()
        }
    }

    private fun toggleSelection(id: Long) {
        if (selectedIds.contains(id)) selectedIds.remove(id)
        else selectedIds.add(id)
        if (selectedIds.isEmpty()) exitSelectionMode()
        else updateSelectionToolbar()
        adapter.notifyDataSetChanged()
    }

    private fun enterSelectionMode() {
        isSelectionMode = true
        binding.toolbarNormal.visibility = View.GONE
        binding.toolbarSelection.visibility = View.VISIBLE
        adapter.notifyDataSetChanged()
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        selectedIds.clear()
        binding.toolbarNormal.visibility = View.VISIBLE
        binding.toolbarSelection.visibility = View.GONE
        adapter.notifyDataSetChanged()
    }

    private fun updateSelectionToolbar() {
        binding.tvSelectedCount.text = "${selectedIds.size} selected"
    }

    private fun updateEmpty(isEmpty: Boolean) {
        binding.tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvClipboard.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
