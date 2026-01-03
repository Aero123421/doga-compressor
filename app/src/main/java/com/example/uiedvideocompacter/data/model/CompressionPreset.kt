package com.example.uiedvideocompacter.data.model

@Deprecated("Use percentage based compression instead")
enum class CompressionPreset(
    val title: String,
    val description: String,
    val bitrate: Int, // Just an example, maybe complex logic
    val height: Int? = null // 1080, 720, 480 etc. Null means keep original.
) {
    QUALITY("Quality", "1.5GB/hour - 1080p", 3_300_000, 1080), // ~1.5GB/h = ~25MB/min = ~416KB/s = ~3.3Mbps
    BALANCED("Balanced", "800MB/hour - 1080p", 1_800_000, 1080), // ~800MB/h = ~13MB/min = ~216KB/s = ~1.7Mbps
    LIGHT("Light", "470MB/hour - 1080p", 1_000_000, 1080), // ~470MB/h = ~7.8MB/min = ~130KB/s = ~1Mbps
    SMALL("Small", "350MB/hour - 720p", 780_000, 720), // ~350MB/h = ~5.8MB/min = ~97KB/s = ~0.78Mbps
    EXTRA_SMALL("Extra Small", "200MB/hour - 480p", 450_000, 480); // ~200MB/h = ~3.3MB/min = ~55KB/s = ~0.44Mbps

    fun getEstimatedSize(durationMs: Long): Long {
        return (bitrate * (durationMs / 1000.0) / 8).toLong()
    }
}
