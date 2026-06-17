package com.phoneai.local.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phoneai.local.model.ModelCatalog
import com.phoneai.local.model.ModelConfig
import com.phoneai.local.utils.DeviceInfo
import com.phoneai.local.utils.ModelDownloader
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class DownloadStatus {
    object NotDownloaded : DownloadStatus()
    data class Downloading(val percent: Int, val downloadedMb: Int, val totalMb: Int) : DownloadStatus()
    object Downloaded : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}

data class ModelItem(
    val config: ModelConfig,
    val compatibility: DeviceInfo.Compatibility,
    val status: DownloadStatus
)

data class ModelSelectionState(
    val items: List<ModelItem> = emptyList(),
    val totalRamMb: Long = 0,
    val availableRamMb: Long = 0
)

class ModelSelectionViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(ModelSelectionState())
    val state: StateFlow<ModelSelectionState> = _state.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()

    init {
        refresh()
    }

    fun refresh() {
        val ctx = getApplication<Application>()
        val items = ModelCatalog.MODELS.map { config ->
            val downloaded = ModelDownloader.destFile(ctx, config).exists()
            ModelItem(
                config = config,
                compatibility = DeviceInfo.assess(ctx, config),
                status = if (downloaded) DownloadStatus.Downloaded else DownloadStatus.NotDownloaded
            )
        }
        _state.update {
            it.copy(
                items = items,
                totalRamMb = DeviceInfo.totalRamMb(ctx),
                availableRamMb = DeviceInfo.availableRamMb(ctx)
            )
        }
    }

    fun download(config: ModelConfig) {
        if (downloadJobs[config.id]?.isActive == true) return

        downloadJobs[config.id] = viewModelScope.launch {
            ModelDownloader.download(getApplication(), config).collect { p ->
                val newStatus = when {
                    p.error != null -> DownloadStatus.Error(p.error)
                    p.isDone        -> DownloadStatus.Downloaded
                    else -> DownloadStatus.Downloading(
                        percent = p.percent,
                        downloadedMb = (p.bytesDownloaded / (1024 * 1024)).toInt(),
                        totalMb = (p.totalBytes / (1024 * 1024)).toInt()
                    )
                }
                updateStatus(config.id, newStatus)
            }
        }
    }

    fun cancelDownload(config: ModelConfig) {
        downloadJobs[config.id]?.cancel()
        downloadJobs.remove(config.id)
        updateStatus(config.id, DownloadStatus.NotDownloaded)
    }

    fun deleteModel(config: ModelConfig) {
        ModelDownloader.destFile(getApplication(), config).delete()
        updateStatus(config.id, DownloadStatus.NotDownloaded)
    }

    private fun updateStatus(id: String, status: DownloadStatus) {
        _state.update { s ->
            s.copy(items = s.items.map {
                if (it.config.id == id) it.copy(status = status) else it
            })
        }
    }
}
