package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ApkUpdateManagerTest {

    @Test
    fun parseVersionCode_fromSemanticTag_returnsCorrectCode() {
        // v1.3.0 -> 130
        assertEquals(130, ApkUpdateManager.parseVersionCode("v1.3.0"))
        // v1.3.1 -> 131
        assertEquals(131, ApkUpdateManager.parseVersionCode("v1.3.1"))
        // 1.3.1 without 'v' -> 131
        assertEquals(131, ApkUpdateManager.parseVersionCode("1.3.1"))
        // v1.4.0 -> 140
        assertEquals(140, ApkUpdateManager.parseVersionCode("v1.4.0"))
        // v2.0.0 -> 200
        assertEquals(200, ApkUpdateManager.parseVersionCode("v2.0.0"))
        // Without 'v' prefix: 1.3.5 -> 135
        assertEquals(135, ApkUpdateManager.parseVersionCode("1.3.5"))
    }

    @Test
    fun parseVersionCode_fromReleaseBody_prioritizesExplicitVersionCode() {
        val bodyWithColon = "New features and performance improvements.\nversionCode: 135"
        assertEquals(135, ApkUpdateManager.parseVersionCode(tag = "v1.3.1", body = bodyWithColon))

        val bodyMarkdown = "### Release v1.3.1\n- **versionCode**: 131\n- **Version**: 1.3.1"
        assertEquals(131, ApkUpdateManager.parseVersionCode(tag = "v1.3.1", body = bodyMarkdown))

        val bodyWithEquals = "Bug fixes\nversionCode = 142"
        assertEquals(142, ApkUpdateManager.parseVersionCode(tag = "v1.3.0", body = bodyWithEquals))
    }

    @Test
    fun isNewerVersion_detectsNewerVersionCorrectly() {
        // Installed: 130 (v1.3.0), Release: 131 (v1.3.1) -> TRUE
        assertTrue(
            ApkUpdateManager.isNewerVersion(
                currentCode = 130,
                currentName = "1.3.0",
                latestCode = 131,
                latestName = "1.3.1"
            )
        )
        // 131 vs 132 -> TRUE
        assertTrue(
            ApkUpdateManager.isNewerVersion(
                currentCode = 131,
                currentName = "1.3.1",
                latestCode = 132,
                latestName = "1.3.2"
            )
        )
        // 131 vs 200 -> TRUE
        assertTrue(
            ApkUpdateManager.isNewerVersion(
                currentCode = 131,
                currentName = "1.3.1",
                latestCode = 200,
                latestName = "2.0.0"
            )
        )
    }

    @Test
    fun isNewerVersion_sameVersion_returnsFalse() {
        // 130 vs 130 -> FALSE
        assertFalse(
            ApkUpdateManager.isNewerVersion(
                currentCode = 130,
                currentName = "1.3.0",
                latestCode = 130,
                latestName = "1.3.0"
            )
        )
        // 131 vs 131 -> FALSE
        assertFalse(
            ApkUpdateManager.isNewerVersion(
                currentCode = 131,
                currentName = "1.3.1",
                latestCode = 131,
                latestName = "1.3.1"
            )
        )
    }

    @Test
    fun isNewerVersion_olderVersion_preventsDowngrade() {
        // 131 installed vs 130 on release -> FALSE
        assertFalse(
            ApkUpdateManager.isNewerVersion(
                currentCode = 131,
                currentName = "1.3.1",
                latestCode = 130,
                latestName = "1.3.0"
            )
        )
        // 130 installed vs 129 -> FALSE
        assertFalse(
            ApkUpdateManager.isNewerVersion(
                currentCode = 130,
                currentName = "1.3.0",
                latestCode = 129,
                latestName = "1.2.9"
            )
        )
    }

    @Test
    fun parseVersionCode_malformedTag_fallsBackGracefully() {
        assertEquals(130, ApkUpdateManager.parseVersionCode("invalid-tag-format", fallbackCode = 130))
        assertEquals(130, ApkUpdateManager.parseVersionCode("", fallbackCode = 130))
    }

    @Test
    fun parseVersionName_cleansTagPrefix() {
        assertEquals("1.3.0", ApkUpdateManager.parseVersionName("v1.3.0"))
        assertEquals("1.3.1", ApkUpdateManager.parseVersionName("V1.3.1"))
        assertEquals("1.3.1", ApkUpdateManager.parseVersionName("1.3.1"))
        assertEquals("1.4.0", ApkUpdateManager.parseVersionName("1.4.0"))
    }

    @Test
    fun parseReleaseJson_withCustomApkFilename_extractsActualBrowserDownloadUrl() {
        // Test that ANY arbitrary APK filename ending with .apk has its browser_download_url extracted
        val jsonWithCustomName = """
            {
              "tag_name": "v1.3.1",
              "name": "Aqeel Rider v1.3.1",
              "body": "New release features\nversionCode: 131",
              "assets": [
                {
                  "name": "release-checksums.txt",
                  "browser_download_url": "https://github.com/aqeelawan687-ux/Aqeel/releases/download/v1.3.1/checksums.txt"
                },
                {
                  "name": "AqeelRider_Custom_Build_v1.3.1.apk",
                  "browser_download_url": "https://github.com/aqeelawan687-ux/Aqeel/releases/download/v1.3.1/AqeelRider_Custom_Build_v1.3.1.apk"
                }
              ]
            }
        """.trimIndent()

        val info = ApkUpdateManager.parseReleaseJson(
            jsonStr = jsonWithCustomName,
            currentCode = 130,
            currentName = "1.3.0"
        )

        assertTrue(info.hasUpdate)
        assertEquals(131, info.latestVersionCode)
        assertEquals("1.3.1", info.latestVersionName)
        assertEquals("v1.3.1", info.releaseTagName)
        assertNull(info.errorMessage)
        // Verified: The actual browser_download_url of the custom APK asset ending in .apk is used!
        assertEquals(
            "https://github.com/aqeelawan687-ux/Aqeel/releases/download/v1.3.1/AqeelRider_Custom_Build_v1.3.1.apk",
            info.downloadUrl
        )
    }

    @Test
    fun parseReleaseJson_withStandardAppReleaseApk_extractsActualBrowserDownloadUrl() {
        val standardJson = """
            {
              "tag_name": "v1.3.1",
              "name": "Aqeel Rider Release v1.3.1",
              "body": "## Release Notes\nversionCode: 131",
              "assets": [
                {
                  "name": "app-release.apk",
                  "browser_download_url": "https://github.com/aqeelawan687-ux/Aqeel/releases/download/v1.3.1/app-release.apk"
                }
              ]
            }
        """.trimIndent()

        val info = ApkUpdateManager.parseReleaseJson(
            jsonStr = standardJson,
            currentCode = 130,
            currentName = "1.3.0"
        )

        assertTrue(info.hasUpdate)
        assertEquals(131, info.latestVersionCode)
        assertEquals("1.3.1", info.latestVersionName)
        assertEquals("v1.3.1", info.releaseTagName)
        assertEquals(
            "https://github.com/aqeelawan687-ux/Aqeel/releases/download/v1.3.1/app-release.apk",
            info.downloadUrl
        )
    }

    @Test
    fun parseReleaseJson_excludesDebugAndUnsignedApks() {
        val jsonWithDebugAndRelease = """
            {
              "tag_name": "v1.3.1",
              "name": "Aqeel Rider v1.3.1",
              "body": "versionCode: 131",
              "assets": [
                {
                  "name": "app-debug.apk",
                  "browser_download_url": "https://github.com/aqeelawan687-ux/Aqeel/releases/download/v1.3.1/app-debug.apk"
                },
                {
                  "name": "app-release-unsigned.apk",
                  "browser_download_url": "https://github.com/aqeelawan687-ux/Aqeel/releases/download/v1.3.1/app-release-unsigned.apk"
                },
                {
                  "name": "app-release.apk",
                  "browser_download_url": "https://github.com/aqeelawan687-ux/Aqeel/releases/download/v1.3.1/app-release.apk"
                }
              ]
            }
        """.trimIndent()

        val info = ApkUpdateManager.parseReleaseJson(
            jsonStr = jsonWithDebugAndRelease,
            currentCode = 130,
            currentName = "1.3.0"
        )

        assertTrue(info.hasUpdate)
        // Must select app-release.apk and NOT app-debug.apk or app-release-unsigned.apk
        assertEquals(
            "https://github.com/aqeelawan687-ux/Aqeel/releases/download/v1.3.1/app-release.apk",
            info.downloadUrl
        )
    }

    @Test
    fun parseReleaseJson_withNonApkAssets_fallsBackToPermanentDownloadUrl() {
        val jsonNoApk = """
            {
              "tag_name": "v1.3.1",
              "name": "Aqeel Rider Release v1.3.1",
              "body": "versionCode: 131",
              "assets": [
                {
                  "name": "source-code.zip",
                  "browser_download_url": "https://github.com/aqeelawan687-ux/Aqeel/releases/download/v1.3.1/source-code.zip"
                },
                {
                  "name": "checksums.sha256",
                  "browser_download_url": "https://github.com/aqeelawan687-ux/Aqeel/releases/download/v1.3.1/checksums.sha256"
                }
              ]
            }
        """.trimIndent()

        val info = ApkUpdateManager.parseReleaseJson(
            jsonStr = jsonNoApk,
            currentCode = 130,
            currentName = "1.3.0"
        )

        assertTrue(info.hasUpdate)
        assertEquals(ApkUpdateManager.PERMANENT_APK_DOWNLOAD_URL, info.downloadUrl)
    }

    @Test
    fun apiConstants_useCorrectOwnerAndRepository() {
        assertEquals("aqeelawan687-ux", ApkUpdateManager.GITHUB_REPO_OWNER)
        assertEquals("Awang", ApkUpdateManager.GITHUB_REPO_NAME)
        assertEquals(
            "https://api.github.com/repos/aqeelawan687-ux/Awang/releases/latest",
            ApkUpdateManager.GITHUB_RELEASE_API_URL
        )
        assertEquals(
            "https://github.com/aqeelawan687-ux/Awang/releases/latest/download/app-release.apk",
            ApkUpdateManager.PERMANENT_APK_DOWNLOAD_URL
        )
    }

    @Test
    fun isValidDownloadUrl_verifiesHttpsAndTrustedDomains() {
        assertTrue(ApkUpdateManager.isValidDownloadUrl("https://github.com/aqeelawan687-ux/Aqeel/releases/download/v1.3.1/app-release.apk"))
        assertTrue(ApkUpdateManager.isValidDownloadUrl("https://objects.githubusercontent.com/github-production-release-asset/app.apk"))
        assertFalse(ApkUpdateManager.isValidDownloadUrl("http://github.com/insecure/app.apk"))
        assertFalse(ApkUpdateManager.isValidDownloadUrl("https://malicious-site.com/app.apk"))
    }
}
