package org.jetbrains.plugins.designer.services

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.io.File
import java.util.zip.ZipFile
import javax.swing.SwingUtilities

class GitHubEmulatorInstaller(private val project: Project) {

    private val emulatorInstallPath = "/Users/enesalgan/dinamoemulator"

    companion object {
        private const val LOCAL_EMULATOR_ZIP = "/Users/enesalgan/dinamoemulator.zip"
    }

    fun isEmulatorInstalled(): Boolean {
        val emulatorDir = File(emulatorInstallPath)
        val emulatorExe = if (SystemInfo.isWindows) "emulator.exe" else "emulator"
        return emulatorDir.exists() && File(emulatorDir, "dinamoemulator/emulator/$emulatorExe").exists()
    }

    fun installEmulator(onComplete: (Boolean, String?) -> Unit) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Setting up Android Emulator", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Checking ZIP file..."
                    indicator.fraction = 0.05

                    val zipFile = File(LOCAL_EMULATOR_ZIP)
                    if (!zipFile.exists()) {
                        SwingUtilities.invokeLater {
                            onComplete(false, "ZIP file not found: $LOCAL_EMULATOR_ZIP\nPlease make sure dinamoemulator.zip exists in /Users/enesalgan/")
                        }
                        return
                    }

                    indicator.text = "Cleaning up old installation..."
                    indicator.fraction = 0.1
                    val installDir = File(emulatorInstallPath)
                    if (installDir.exists()) {
                        installDir.deleteRecursively()
                    }

                    indicator.text = "Extracting emulator files..."
                    indicator.fraction = 0.2

                    extractAndFlattenZip(zipFile, installDir, indicator, 0.2, 0.6)

                    indicator.text = "Setting up platforms directory..."
                    indicator.fraction = 0.6
                    createPlatformsDirectory()

                    indicator.text = "Setting executable permissions..."
                    indicator.fraction = 0.7
                    setAllExecutablePermissions()

                    indicator.text = "Creating default AVD..."
                    indicator.fraction = 0.85
                    createDefaultAvd()

                    indicator.text = "Setup complete!"
                    indicator.fraction = 1.0

                    SwingUtilities.invokeLater {
                        onComplete(true, emulatorInstallPath)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    SwingUtilities.invokeLater {
                        onComplete(false, "Setup failed: ${e.message}")
                    }
                }
            }
        })
    }

    private fun extractAndFlattenZip(
        zipFile: File,
        destDir: File,
        indicator: ProgressIndicator,
        startFraction: Double,
        endFraction: Double
    ) {
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries().toList()
            val totalEntries = entries.size

            val rootPrefix = entries
                .firstOrNull { !it.isDirectory }
                ?.name
                ?.substringBefore("/", "")
                ?.let { if (it.isNotEmpty()) "$it/" else "" }
                ?: ""

            entries.forEachIndexed { index, entry ->
                if (entry.name.contains("__MACOSX") || entry.name.contains(".DS_Store")) {
                    return@forEachIndexed
                }

                val relativePath = if (rootPrefix.isNotEmpty() && entry.name.startsWith(rootPrefix)) {
                    entry.name.substring(rootPrefix.length)
                } else {
                    entry.name
                }

                if (relativePath.isEmpty()) return@forEachIndexed

                val targetFile = File(destDir, "dinamoemulator/$relativePath")

                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val progress = (index + 1).toDouble() / totalEntries
                indicator.fraction = startFraction + (progress * (endFraction - startFraction))
                indicator.text2 = "Extracting: $relativePath"
            }
        }
    }

    private fun setAllExecutablePermissions() {
        if (SystemInfo.isWindows) return

        try {
            val emulatorDir = File(emulatorInstallPath, "dinamoemulator")

            val criticalExecutables = listOf(
                "emulator/emulator",
                "emulator/crashpad_handler",
                "emulator/crashreport",
                "emulator/emulator-check",
                "emulator/mksdcard",
                "emulator/netsimd",
                "emulator/nimble_bridge",
                "platform-tools/adb"
            )

            criticalExecutables.forEach { path ->
                val file = File(emulatorDir, path)
                if (file.exists()) {
                    file.setExecutable(true)
                    println("✅ Set executable: ${file.name}")
                }
            }

            val qemuDir = File(emulatorDir, "emulator/qemu/darwin-aarch64")
            if (qemuDir.exists()) {
                qemuDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.setExecutable(true)
                        println("✅ Set executable: qemu/${file.name}")
                    }
                }
            }

            listOf("emulator/lib64", "emulator/bin64").forEach { dirPath ->
                val libDir = File(emulatorDir, dirPath)
                if (libDir.exists()) {
                    libDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            file.setExecutable(true)
                        }
                    }
                }
            }

            println("✅ All executable permissions set successfully")
        } catch (e: Exception) {
            println("⚠️ Failed to set some executable permissions: ${e.message}")
        }
    }

    private fun createPlatformsDirectory() {
        try {
            val platformsDir = File(emulatorInstallPath, "dinamoemulator/platforms/android-35")
            platformsDir.mkdirs()

            val buildProp = File(platformsDir, "build.prop")
            buildProp.writeText("""
                ro.build.version.sdk=35
                ro.build.version.codename=REL
            """.trimIndent())

            val sourceProps = File(platformsDir, "source.properties")
            sourceProps.writeText("""
                Pkg.Desc=Android SDK Platform 35
                Pkg.UserSrc=false
                Pkg.Revision=1
                AndroidVersion.ApiLevel=35
            """.trimIndent())

            println("✅ Created platforms directory")
        } catch (e: Exception) {
            println("⚠️ Failed to create platforms directory: ${e.message}")
        }
    }

    private fun createDefaultAvd() {
        try {
            val avdDir = File(emulatorInstallPath, "dinamoemulator/avd/Dinamo_Pixel_5.avd")
            avdDir.mkdirs()

            val systemImagesPath = File(emulatorInstallPath, "dinamoemulator/android-35/google_apis_playstore/arm64-v8a")
            val configIni = File(avdDir, "config.ini")
            configIni.writeText("""
                avd.ini.encoding=UTF-8
                hw.device.name=pixel_5
                hw.lcd.density=440
                hw.lcd.height=2340
                hw.lcd.width=1080
                image.sysdir.1=${systemImagesPath.absolutePath}/
                skin.name=1080x2340
                tag.display=Google Play
                tag.id=google_apis_playstore
                abi.type=arm64-v8a
                hw.cpu.arch=arm64
                disk.dataPartition.size=6G
                hw.ramSize=2048
                hw.keyboard=yes
                hw.gpu.enabled=yes
                hw.gpu.mode=swiftshader_indirect
            """.trimIndent())

            val avdIni = File(emulatorInstallPath, "dinamoemulator/avd/Dinamo_Pixel_5.ini")
            avdIni.parentFile.mkdirs()
            avdIni.writeText("path=${avdDir.absolutePath}\n")

            println("✅ Created default AVD: Dinamo_Pixel_5 at ${avdDir.absolutePath}")
        } catch (e: Exception) {
            println("⚠️ Failed to create AVD: ${e.message}")
        }
    }

    fun getEmulatorPath(): String? {
        if (!isEmulatorInstalled()) return null

        val emulatorExe = if (SystemInfo.isWindows) "emulator.exe" else "emulator"
        val emulatorPath = File(emulatorInstallPath, "dinamoemulator/emulator/$emulatorExe")

        return if (emulatorPath.exists()) emulatorPath.absolutePath else null
    }

    fun getAdbPath(): String? {
        if (!isEmulatorInstalled()) return null

        val adbExe = if (SystemInfo.isWindows) "adb.exe" else "adb"
        val adbPath = File(emulatorInstallPath, "dinamoemulator/platform-tools/$adbExe")

        return if (adbPath.exists()) adbPath.absolutePath else null
    }
}
