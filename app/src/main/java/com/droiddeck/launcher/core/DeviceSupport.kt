package com.droiddeck.launcher.core

import android.content.Context
import android.os.Build
import com.droiddeck.launcher.R
import com.droiddeck.launcher.gpu.GpuInfo
import com.droiddeck.launcher.gpu.SystemVulkanDriver
import java.io.File

object DeviceSupport {
    private val switchrootDevice by lazy {
        val switchrootProducts = setOf("nx", "nx-tab", "nx-atv", "nx-tv", "odin", "modin", "vali", "fric")
        val identity = listOf(
            Build.DEVICE,
            Build.PRODUCT,
            Build.BOARD,
            Build.MODEL,
            Build.FINGERPRINT,
            Build.HARDWARE,
            GpuInfo.systemProperty("ro.product.name"),
            GpuInfo.systemProperty("ro.product.device"),
            GpuInfo.systemProperty("ro.build.product"),
            GpuInfo.systemProperty("ro.build.description"),
            GpuInfo.systemProperty("ro.switchroot.version"),
        )
        identity.any { it.trim().lowercase().replace('_', '-') in switchrootProducts } ||
            identity.any { it.contains("switchroot", ignoreCase = true) } ||
            (identity.any { it.contains("nintendo switch", ignoreCase = true) } && hasTegraPlatform())
    }

    private val adrenoGpu by lazy {
        File("/sys/class/kgsl/kgsl-3d0").exists() || File("/vendor/lib64/hw/vulkan.adreno.so").exists()
    }

    fun adreno(): Boolean = adrenoGpu

    fun switchrootTegra(): Boolean = switchrootDevice

    fun graphicsAvailable(): Boolean = graphicsAvailable(adreno(), SystemVulkanDriver.isAvailable())

    internal fun graphicsAvailable(adreno: Boolean, powerVrReady: Boolean): Boolean = adreno || powerVrReady

    fun vkBridge(context: Context): Boolean =
        bridgeUsable() && !com.droiddeck.launcher.session.VenusComponent.wanted(context) &&
            com.droiddeck.launcher.session.SessionPrefs.vkBridge(context)

    fun mali(): Boolean = isMali

    fun vkBridgeDefault(): Boolean = mali() || (switchrootTegra() && tegraVulkanAvailable())

    private val isMali by lazy {
        !adreno() && (File("/dev/mali0").exists() ||
            com.droiddeck.launcher.gpu.GpuInfo.detect().family.name.startsWith("MALI"))
    }

    fun bridgeUsable(): Boolean {
        if (adreno() || SystemVulkanDriver.isAvailable()) return false
        if (File("/dev/mali0").exists()) return true
        return switchrootTegra() && tegraVulkanAvailable()
    }

    private fun tegraVulkanAvailable(): Boolean = runCatching {
        if (Build.VERSION.SDK_INT < 34) return false
        val info = com.droiddeck.launcher.gpu.VulkanInfo.query()
        val device = info["device"].orEmpty()
        val api = info["api"]?.split('.')?.mapNotNull { it.toIntOrNull() }.orEmpty()
        (device.contains("nvidia", ignoreCase = true) || device.contains("tegra", ignoreCase = true)) &&
            api.size >= 2 && (api[0] > 1 || api[0] == 1 && api[1] >= 1)
    }.getOrDefault(false)

    private fun hasTegraPlatform(): Boolean =
        listOf(
            Build.HARDWARE,
            if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL else "",
            GpuInfo.systemProperty("ro.board.platform"),
            GpuInfo.systemProperty("ro.hardware"),
        ).any { it.contains("tegra", ignoreCase = true) }

    fun openDriverActive(context: Context): Boolean {
        if (adreno()) return false
        if (com.droiddeck.launcher.session.VenusComponent.wanted(context)) return false
        if (bridgeUsable() && com.droiddeck.launcher.session.SessionPrefs.vkBridge(context)) return false
        val linux = com.droiddeck.launcher.session.SessionPrefs.linuxDriver(context).ifEmpty { return false }
        return com.droiddeck.launcher.gpu.DriverPairs.isNonAdreno(com.droiddeck.launcher.gpu.DriverBundle.containing(context, linux, true)?.id)
    }

    fun canDraw(context: Context): Boolean = graphicsAvailable() || openDriverActive(context) || vkBridge(context) ||
        com.droiddeck.launcher.session.VenusComponent.wanted(context)

    fun gpuName(context: Context): String {
        val soc = if (Build.VERSION.SDK_INT >= 31) Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN } else null
        return soc?.let { "$it (${Build.HARDWARE})" } ?: Build.HARDWARE.ifBlank { context.getString(R.string.gpu_this_gpu) }
    }

    fun lowLatencyWifiSafe(): Boolean = Build.VERSION.SDK_INT < 31 || Build.SOC_MODEL != "CQ8725S"
}
