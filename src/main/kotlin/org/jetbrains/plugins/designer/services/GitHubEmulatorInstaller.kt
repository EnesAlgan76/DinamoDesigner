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

    private val emulatorInstallPath = System.getProperty("user.home") + "/dinamoemulator"

    companion object {
        // Local emulator zip file path - embedded in plugin
        private const val LOCAL_EMULATOR_ZIP = "/Users/enesalgan/Projeler/DinamoDesigner/src/main/kotlin/org/jetbrains/plugins/template/dinamoemulator.zip"
    }

    fun isEmulatorInstalled(): Boolean {
        val emulatorDir = File(emulatorInstallPath)
        val emulatorExe = if (SystemInfo.isWindows) "emulator.exe" else "emulator"
        return emulatorDir.exists() && File(emulatorDir, "dinamoemulator/emulator/$emulatorExe").exists()
    }

    fun installEmulator(onComplete: (Boolean, String?) -> Unit) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Installing emulator from local file", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Checking local emulator file..."
                    indicator.fraction = 0.1

                    val localZip = File(LOCAL_EMULATOR_ZIP)
                    if (!localZip.exists()) {
                        SwingUtilities.invokeLater {
                            onComplete(false, "Local emulator file not found: $LOCAL_EMULATOR_ZIP")
                        }
                        return
                    }

                    indicator.text = "Creating installation directory..."
                    indicator.fraction = 0.2

                    val emulatorDir = File(emulatorInstallPath)
                    emulatorDir.mkdirs()

                    indicator.text = "Extracting emulator files..."
                    indicator.fraction = 0.3

                    extractZip(localZip, emulatorDir, indicator, 0.3, 0.7)

                    indicator.text = "Setting executable permissions..."
                    indicator.fraction = 0.7
                    setExecutablePermissions(emulatorDir)

                    indicator.text = "Creating default AVD..."
                    indicator.fraction = 0.9
                    createDefaultAvd()

                    indicator.text = "Installation complete!"
                    indicator.fraction = 1.0

                    SwingUtilities.invokeLater {
                        onComplete(true, emulatorInstallPath)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    SwingUtilities.invokeLater {
                        onComplete(false, "Installation failed: ${e.message}")
                    }
                }
            }
        })
    }

    private fun extractZip(
        zipFile: File,
        destDir: File,
        indicator: ProgressIndicator,
        startFraction: Double,
        endFraction: Double
    ) {
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries().toList()
            val totalEntries = entries.size

            entries.forEachIndexed { index, entry ->
                val file = File(destDir, entry.name)

                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val progress = (index + 1).toDouble() / totalEntries
                indicator.fraction = startFraction + (progress * (endFraction - startFraction))
            }
        }
    }

    private fun setExecutablePermissions(dir: File) {
        if (SystemInfo.isWindows) return

        // Set executable permissions for emulator and related binaries
        val executablePatterns = listOf("emulator", "adb", "avdmanager", "sdkmanager")

        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                executablePatterns.forEach { pattern ->
                    if (file.name.contains(pattern)) {
                        file.setExecutable(true)
                    }
                }
            }
        }
    }

    private fun createDefaultAvd() {
        try {
            // Create AVD directory inside our custom emulator installation
            val avdDir = File(emulatorInstallPath, "avd/Dinamo_Pixel_5.avd")
            avdDir.mkdirs()

            // Create config.ini with absolute path to system images
            val systemImagesPath = File(emulatorInstallPath, "system-images/android-33/google_apis/x86_64")
            val configIni = File(avdDir, "config.ini")
            configIni.writeText("""
                avd.ini.encoding=UTF-8
                hw.device.name=pixel_5
                hw.lcd.density=440
                hw.lcd.height=2340
                hw.lcd.width=1080
                image.sysdir.1=${systemImagesPath.absolutePath}/
                skin.name=1080x2340
                tag.display=Google APIs
                tag.id=google_apis
                abi.type=x86_64
                hw.cpu.arch=x86_64
                disk.dataPartition.size=6G
                hw.ramSize=2048
                hw.keyboard=yes
                hw.gpu.enabled=yes
                hw.gpu.mode=host
            """.trimIndent())

            // Create AVD ini file in our custom location
            val avdIni = File(emulatorInstallPath, "avd/Dinamo_Pixel_5.ini")
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
        val adbPath = File(emulatorInstallPath, "platform-tools/$adbExe")

        return if (adbPath.exists()) adbPath.absolutePath else null
    }
}

