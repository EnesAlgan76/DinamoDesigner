package org.jetbrains.plugins.designer.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.io.File
import javax.swing.SwingUtilities

class EmulatorLauncher(private val project: Project) {

    private val installer = GitHubEmulatorInstaller(project)

    fun launchEmulatorWithApp(
        avdName: String,
        apkPath: String? = null,
        packageName: String? = null,
        activityName: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        Thread {
            try {
                val sdkPath = findSdkPath()
                if (sdkPath == null) {
                    SwingUtilities.invokeLater { onError("Android SDK not found") }
                    return@Thread
                }

                val emulatorPath = getEmulatorPath(sdkPath)
                if (emulatorPath == null) {
                    SwingUtilities.invokeLater { onError("Emulator executable not found") }
                    return@Thread
                }

                // Start emulator
                println("🚀 Starting emulator: $avdName")
                val processBuilder = ProcessBuilder(emulatorPath, "-avd", avdName)

                // Set environment variables to use our custom paths - completely independent from system
                processBuilder.environment()["ANDROID_SDK_ROOT"] = sdkPath
                processBuilder.environment()["ANDROID_AVD_HOME"] = "$sdkPath/avd"
                processBuilder.environment()["ANDROID_EMULATOR_HOME"] = "$sdkPath/avd"

                // Remove any system Android paths if they exist
                processBuilder.environment().remove("ANDROID_SDK_HOME")

                println("📍 Using AVD location: $sdkPath/avd")
                println("📍 Using SDK location: $sdkPath")

                processBuilder.start()

                // Wait for emulator to boot
                println("⏳ Waiting for emulator to boot...")
                val deviceSerial = waitForEmulatorBoot(sdkPath)

                if (deviceSerial == null) {
                    SwingUtilities.invokeLater { onError("Emulator failed to boot") }
                    return@Thread
                }

                println("✅ Emulator booted: $deviceSerial")

                // Install and launch app if provided
                if (apkPath != null && packageName != null && activityName != null) {
                    println("📦 Installing APK...")
                    if (installApk(sdkPath, deviceSerial, apkPath)) {
                        println("✅ APK installed")

                        // Wait for installation
                        Thread.sleep(2000)

                        println("🚀 Launching app...")
                        if (launchApp(sdkPath, deviceSerial, packageName, activityName)) {
                            println("✅ App launched successfully")
                            SwingUtilities.invokeLater { onSuccess() }
                        } else {
                            SwingUtilities.invokeLater { onError("Failed to launch app") }
                        }
                    } else {
                        SwingUtilities.invokeLater { onError("Failed to install APK") }
                    }
                } else {
                    SwingUtilities.invokeLater { onSuccess() }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                SwingUtilities.invokeLater { onError(e.message ?: "Unknown error") }
            }
        }.start()
    }

    private fun findSdkPath(): String? {
        // First check if we installed it
        val installerPath = installer.getEmulatorPath()?.substringBeforeLast("/emulator")
        if (installerPath != null) return installerPath

        // Check environment variables
        System.getenv("ANDROID_HOME")?.let { return it }
        System.getenv("ANDROID_SDK_ROOT")?.let { return it }

        // Check common locations
        val homeDir = System.getProperty("user.home")
        val possiblePaths = listOf(
            "$homeDir/dinamoemulator",  // Our custom location
            "$homeDir/Library/Android/sdk",
            "$homeDir/Android/Sdk",
            "/opt/android-sdk"
        )

        return possiblePaths.firstOrNull { File(it).exists() }
    }

    private fun getEmulatorPath(sdkPath: String): String? {
        val emulatorExe = if (SystemInfo.isWindows) "emulator.exe" else "emulator"
        val emulatorPath = File(sdkPath, "emulator/$emulatorExe")
        return if (emulatorPath.exists()) emulatorPath.absolutePath else null
    }

    private fun waitForEmulatorBoot(sdkPath: String, timeoutSeconds: Int = 120): String? {
        val adbPath = getAdbPath(sdkPath) ?: return null
        val startTime = System.currentTimeMillis()

        // Wait for device to appear
        var deviceSerial: String? = null
        while (System.currentTimeMillis() - startTime < timeoutSeconds * 1000 / 2) {
            try {
                val process = ProcessBuilder(adbPath, "devices").start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()

                val devices = output.lines()
                    .filter { it.contains("emulator-") && it.contains("device") }
                    .map { it.split("\t")[0].trim() }

                if (devices.isNotEmpty()) {
                    deviceSerial = devices.last()
                    println("✅ Device found: $deviceSerial")
                    break
                }
            } catch (_: Exception) {
                // Continue waiting
            }
            Thread.sleep(2000)
        }

        if (deviceSerial == null) return null

        // Wait for boot completion
        println("⏳ Waiting for boot completion...")
        while (System.currentTimeMillis() - startTime < timeoutSeconds * 1000) {
            try {
                val process = ProcessBuilder(
                    adbPath, "-s", deviceSerial, "shell", "getprop", "sys.boot_completed"
                ).start()
                val output = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()

                if (output == "1") {
                    println("✅ Boot completed")
                    Thread.sleep(3000)
                    return deviceSerial
                }
            } catch (_: Exception) {
                // Continue waiting
            }
            Thread.sleep(2000)
        }

        return null
    }

    private fun installApk(sdkPath: String, deviceSerial: String, apkPath: String): Boolean {
        val adbPath = getAdbPath(sdkPath) ?: return false

        return try {
            val process = ProcessBuilder(
                adbPath, "-s", deviceSerial, "install", "-r", apkPath
            ).start()

            val exitCode = process.waitFor()
            exitCode == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun launchApp(
        sdkPath: String,
        deviceSerial: String,
        packageName: String,
        activityName: String
    ): Boolean {
        val adbPath = getAdbPath(sdkPath) ?: return false

        return try {
            val fullActivityName = if (activityName.startsWith(".")) {
                "$packageName$activityName"
            } else if (!activityName.contains(".")) {
                "$packageName.$activityName"
            } else {
                activityName
            }

            val process = ProcessBuilder(
                adbPath, "-s", deviceSerial, "shell", "am", "start",
                "-n", "$packageName/$fullActivityName"
            ).start()

            val exitCode = process.waitFor()
            exitCode == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun getAdbPath(sdkPath: String): String? {
        val adbExe = if (SystemInfo.isWindows) "adb.exe" else "adb"
        val adbPath = File(sdkPath, "platform-tools/$adbExe")
        return if (adbPath.exists()) adbPath.absolutePath else null
    }
}


