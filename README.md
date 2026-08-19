# Aqeel Rider (عقیل رائڈر) 🛵💰

An Android application designed for rider daily earnings tracking, customer debts (qarza) management, parcel delivery bill tracking, and date-wise financial reporting.

---

## ✨ Features (خصوصیات)

- 📊 **Dashboard & Date-wise Earnings (تاریخ وار آمدن)**:
  - View total ride earnings categorized by each date.
  - Quick daily summaries for rides completed and total earnings.
- 👥 **Customer Folder & Debts Management (کسٹمرز اور قرضہ جات)**:
  - Add customers with debt details, track payment dates, and days unpaid.
  - Track individual ride earnings per customer.
  - "Bill Paid" (رقم ادا کریں) quick action to clear debts and keep customer ledger updated.
  - Record complete payment history for each customer.
- 📦 **Parcel & Delivery Ledger (سامان / پارسل)**:
  - Track delivery charges, sender & receiver info, and payment statuses.
- 🔄 **GitHub-Based Auto-Update System (آٹو اپڈیٹ سسٹم)**:
  - Automatically checks GitHub Releases for new updates with intelligent 6-hour throttling.
  - Compares `versionCode` authoritatively to prevent downgrades.
  - In-app download with live progress bar and size percentage.
  - Seamless Android package installation via `FileProvider`.
  - Manual "Check for Updates" button in Settings.
- 📄 **PDF Reports & Share (رپورٹس)**:
  - Generate PDF financial reports and share directly via WhatsApp or export.
- 🌙 **Dark / Light Theme & Privacy Toggle**:
  - Toggle Dark/Light UI theme.
  - Hide amounts option (`Rs. ****`) for visual privacy.

---

## 🛠️ Built With (ٹیکنالوجی)

- **Language**: Kotlin 100%
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Database**: Room Database with KSP
- **Architecture**: MVVM (Model-View-ViewModel) + StateFlow
- **Networking**: OkHttp & GitHub Releases REST API
- **Export & PDF**: Android Printing Framework & Canvas PDF Generation

---

## 🚀 How to Publish an App Update via GitHub

This repository includes a GitHub Actions workflow (`.github/workflows/release.yml`) that automatically builds and signs the release APK and publishes a new GitHub Release.

### 1. Set Up GitHub Repository Secrets

Go to your GitHub repository -> **Settings** -> **Secrets and variables** -> **Actions** -> **New repository secret**:

| Secret Name | Description | Example / Note |
|---|---|---|
| `KEYSTORE_BASE64` | Base64-encoded string of your `.jks` release keystore file | `base64 -w 0 my-upload-key.jks` |
| `STORE_PASSWORD` | Keystore password | Your keystore password |
| `KEY_ALIAS` | Key alias in the keystore | e.g. `upload` or your alias |
| `KEY_PASSWORD` | Key password | Your key password |

*(Note: The release APK must be signed with the SAME release key as the currently installed production APK so that Android updates the app without signature mismatch errors.)*

### 2. Steps to Release a New Version

1. **Bump the Version in `app/build.gradle.kts`**:
   ```kotlin
   defaultConfig {
       versionCode = 131      // Increment versionCode (e.g. 130 -> 131)
       versionName = "1.3.1"  // Increment versionName (e.g. "1.3.0" -> "1.3.1")
   }
   ```

2. **Commit and Push to GitHub**:
   ```bash
   git add app/build.gradle.kts
   git commit -m "Bump version to v1.3.1 (131)"
   git push origin main
   ```

3. **Create and Push a Release Tag**:
   ```bash
   git tag v1.3.1
   git push origin v1.3.1
   ```

4. **Automatic GitHub Release**:
   - The GitHub Actions workflow will trigger automatically on tag push (`v*`).
   - It will build the signed APK (`app-release.apk`) and attach it to the new GitHub Release `v1.3.1`.
   - The in-app auto-update system in **Aqeel Rider** will detect the new release, display the update prompt to riders, download the APK, and launch the installer.

---

## 📱 How to Run in Android Studio

1. Clone or download this GitHub repository.
2. Open the project in **Android Studio** (Ladybug or newer recommended).
3. Let Gradle sync and resolve dependencies automatically.
4. Run on an Android Emulator or connected device (Android 7.0+ / API 24+).

---

## 📱 License & Author

Developed for **Aqeel Rider** management.
