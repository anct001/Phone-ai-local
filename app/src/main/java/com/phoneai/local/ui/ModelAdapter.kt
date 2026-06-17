package com.phoneai.local.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.phoneai.local.R
import com.phoneai.local.utils.DeviceInfo

class ModelAdapter(
    private val onDownload: (ModelItem) -> Unit,
    private val onCancel:   (ModelItem) -> Unit,
    private val onUse:      (ModelItem) -> Unit,
    private val onDelete:   (ModelItem) -> Unit
) : ListAdapter<ModelItem, ModelAdapter.VH>(DIFF) {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName:        TextView    = v.findViewById(R.id.tvModelName)
        val tvMeta:        TextView    = v.findViewById(R.id.tvModelMeta)
        val tvBadge:       TextView    = v.findViewById(R.id.tvFitBadge)
        val tvStars:       TextView    = v.findViewById(R.id.tvStars)
        val tvTags:        TextView    = v.findViewById(R.id.tvTags)
        val tvDescription: TextView    = v.findViewById(R.id.tvDescription)
        val tvFitReason:   TextView    = v.findViewById(R.id.tvFitReason)
        val progress:      ProgressBar = v.findViewById(R.id.progressDownload)
        val tvProgress:    TextView    = v.findViewById(R.id.tvProgressLabel)
        val btnDownload:   Button      = v.findViewById(R.id.btnDownload)
        val btnUse:        Button      = v.findViewById(R.id.btnUse)
        val btnCancel:     Button      = v.findViewById(R.id.btnCancel)
        val btnDelete:     ImageButton = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val c = item.config

        holder.tvName.text = "${c.displayName} · ${c.quant}"
        holder.tvMeta.text = "${c.paramsLabel} · ${"%.1f".format(c.sizeGb)} GB · ${c.speedLabel}"
        holder.tvStars.text = "★".repeat(c.qualityStars) + "☆".repeat(5 - c.qualityStars)
        holder.tvTags.text = c.tags.joinToString("  •  ")
        holder.tvDescription.text = c.description
        holder.tvFitReason.text = item.compatibility.reason

        // Device-fit badge with color coding
        holder.tvBadge.text = item.compatibility.label
        val badgeColor = when (item.compatibility.fit) {
            DeviceInfo.Fit.RECOMMENDED  -> Color.parseColor("#22C55E")
            DeviceInfo.Fit.GOOD         -> Color.parseColor("#3B82F6")
            DeviceInfo.Fit.HEAVY        -> Color.parseColor("#F59E0B")
            DeviceInfo.Fit.INSUFFICIENT -> Color.parseColor("#EF4444")
        }
        holder.tvBadge.setBackgroundColor(badgeColor)

        // Reset visibility
        holder.progress.visibility    = View.GONE
        holder.tvProgress.visibility  = View.GONE
        holder.btnDownload.visibility = View.GONE
        holder.btnUse.visibility      = View.GONE
        holder.btnCancel.visibility   = View.GONE
        holder.btnDelete.visibility   = View.GONE

        when (val s = item.status) {
            is DownloadStatus.NotDownloaded -> {
                holder.btnDownload.visibility = View.VISIBLE
                holder.btnDownload.isEnabled =
                    item.compatibility.fit != DeviceInfo.Fit.INSUFFICIENT
                holder.btnDownload.text =
                    if (holder.btnDownload.isEnabled) "Tải xuống (${"%.1f".format(c.sizeGb)} GB)"
                    else "Không đủ RAM"
                holder.btnDownload.setOnClickListener { onDownload(item) }
            }
            is DownloadStatus.Downloading -> {
                holder.progress.visibility   = View.VISIBLE
                holder.tvProgress.visibility = View.VISIBLE
                holder.btnCancel.visibility  = View.VISIBLE
                holder.progress.isIndeterminate = s.percent <= 0
                holder.progress.progress = s.percent
                holder.tvProgress.text = "Đang tải ${s.percent}%  (${s.downloadedMb}/${s.totalMb} MB)"
                holder.btnCancel.setOnClickListener { onCancel(item) }
            }
            is DownloadStatus.Downloaded -> {
                holder.btnUse.visibility    = View.VISIBLE
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnUse.setOnClickListener { onUse(item) }
                holder.btnDelete.setOnClickListener { onDelete(item) }
            }
            is DownloadStatus.Error -> {
                holder.btnDownload.visibility = View.VISIBLE
                holder.btnDownload.text = "Thử lại"
                holder.btnDownload.isEnabled = true
                holder.tvProgress.visibility = View.VISIBLE
                holder.tvProgress.text = "Lỗi: ${s.message}"
                holder.btnDownload.setOnClickListener { onDownload(item) }
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ModelItem>() {
            override fun areItemsTheSame(a: ModelItem, b: ModelItem) =
                a.config.id == b.config.id
            override fun areContentsTheSame(a: ModelItem, b: ModelItem) = a == b
        }
    }
}
