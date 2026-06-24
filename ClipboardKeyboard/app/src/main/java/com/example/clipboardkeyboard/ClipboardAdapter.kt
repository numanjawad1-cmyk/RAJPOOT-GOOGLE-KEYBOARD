package com.example.clipboardkeyboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.clipboardkeyboard.databinding.ItemClipboardBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClipboardAdapter(
    private val onItemClick: (ClipboardItem) -> Unit,
    private val onItemLongClick: (ClipboardItem) -> Unit,
    private val onPinClick: (ClipboardItem) -> Unit,
    private val onDeleteClick: (ClipboardItem) -> Unit,
    private val isSelected: (Long) -> Boolean,
    private val isSelectionMode: () -> Boolean
) : ListAdapter<ClipboardItem, ClipboardAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemClipboardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ClipboardItem) {
            binding.tvText.text = item.text
            binding.tvTimestamp.text = formatTime(item.timestamp)

            val selected = isSelected(item.id)
            binding.root.isSelected = selected

            if (selected) {
                binding.root.setBackgroundColor(
                    binding.root.context.getColor(R.color.item_selected_bg)
                )
            } else {
                binding.root.setBackgroundColor(
                    binding.root.context.getColor(android.R.color.transparent)
                )
            }

            binding.ivPin.visibility = if (item.isPinned) View.VISIBLE else View.GONE
            binding.ivPinAction.setImageResource(
                if (item.isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin_outline
            )

            if (isSelectionMode()) {
                binding.ivDelete.visibility = View.GONE
                binding.ivPinAction.visibility = View.GONE
                binding.cbSelect.visibility = View.VISIBLE
                binding.cbSelect.isChecked = selected
            } else {
                binding.ivDelete.visibility = View.VISIBLE
                binding.ivPinAction.visibility = View.VISIBLE
                binding.cbSelect.visibility = View.GONE
            }

            binding.root.setOnClickListener { onItemClick(item) }
            binding.root.setOnLongClickListener { onItemLongClick(item); true }
            binding.ivPinAction.setOnClickListener { onPinClick(item) }
            binding.ivDelete.setOnClickListener { onDeleteClick(item) }
        }

        private fun formatTime(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemClipboardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ClipboardItem>() {
            override fun areItemsTheSame(oldItem: ClipboardItem, newItem: ClipboardItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ClipboardItem, newItem: ClipboardItem) =
                oldItem == newItem
        }
    }
}
