# 動画コンパク太 (UIedVideoCompacter)

> ⚠️ **注意**: このプロジェクトは完全にAIによって生成・コーディングされたものです。

<div align="center">

[![Android](https://img.shields.io/badge/Android-31%2B-green?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-purple?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

動画を簡単に圧縮して、スマホの容量を節約

[README in English](#english-version)

</div>

---

## ✨ 特徴

- 🎬 **ファイルサイズ指定圧縮** - 元動画の10%〜100%の範囲で目標サイズを柔軟に指定可能
- 📉 **自動解像度調整 (Adaptive Resolution)** - 低ビットレート時に自動で解像度を下げ、ブロックノイズを防ぎます
- 📊 **バッチ処理** - 複数の動画をキューに追加して一括圧縮
- 🎨 **モダンなUI** - Material Design 3 + Jetpack Compose
- 🔔 **バックグラウンド処理** - WorkManagerを使用した堅牢な非同期圧縮
- 📱 **Android 31+対応** - 最新のAndroid機能を活用

## 📦 技術スタック

| カテゴリ | 技術 |
|----------|------|
| 言語 | Kotlin (2.0.21) |
| UI | Jetpack Compose (Material 3) |
| ビデオ処理 | Media3 (Transformer, Effect) |
| バックグラウンド処理 | WorkManager |
| 画像ローディング | Coil |
| 状態管理 | ViewModel + DataStore |
| 非同期処理 | Kotlin Coroutines + Flow |

## 🔑 必要な権限

アプリを正常に動作させるために、以下の権限を使用します：

- **通知 (POST_NOTIFICATIONS)**: 圧縮の進行状況や完了をバックグラウンドで通知するために使用します。
- **メディアアクセス (READ_MEDIA_VIDEO / READ_EXTERNAL_STORAGE)**: デバイス内の動画ファイルを選択・圧縮するために必要です（Android 13未満はREAD_EXTERNAL_STORAGEを使用）。
- **フォアグラウンドサービス (FOREGROUND_SERVICE / MEDIA_PROCESSING)**: アプリを閉じても圧縮処理を継続するために使用します。

## 🎯 圧縮ロジックについて

本アプリは固定プリセットではなく、**「元のファイルサイズの何％にするか」** という直感的な指定方法を採用しています。

1. **目標サイズの計算**: `元ファイルサイズ × 指定％` で目標サイズを決定
2. **ビットレートの算出**: 目標サイズと動画時間から必要なビットレートを逆算
3. **画質保護 (Adaptive Resolution)**: 算出されたビットレートが解像度に対して低すぎる場合（Bits Per Pixel < 0.05）、画質崩壊を防ぐために自動的に解像度をダウンサイズします（例：1080p → 720p）。

## ⬇️ ダウンロード

### APKファイル

[![Latest Release](https://img.shields.io/badge/Download-Latest%20Release-green?logo=github)](https://github.com/Aero123421/doga-compressor/releases/latest)

最新のAPKは [GitHub Releases](https://github.com/Aero123421/doga-compressor/releases) からダウンロードできます。

> ⚠️ **注意**: APKは署名されていません。インストールには不明なアプリのインストールを許可する必要があり、更新の度にアンインストールが必要になる場合があります。

## 🚀 クイックスタート

### 環境要件

- Android Studio Hedgehog (2023.1.1) 以上
- **JDK 17** (AGP 8.x要件のため)
- Android SDK 36 (Compile SDK) / 31 (Min SDK)

### ビルド手順

```bash
# リポジトリのクローン (ディレクトリ名を指定)
git clone https://github.com/Aero123421/doga-compressor.git UIedvideocompacter
cd UIedvideocompacter

# Gradle同期
./gradlew build

# APKをインストール
./gradlew installDebug
```

## 📱 画面構成

### ライブラリ
端末内の動画を一覧表示、ソート、検索

### プレビュー
選択した動画の再生確認、目標圧縮率（％）の設定

### 圧縮キュー
圧縮待ちの動画一覧

### 実行中タスク (Progress)
現在進行中の圧縮処理の監視

### 圧縮結果 (Result)
圧縮済み動画の閲覧、共有、元動画との比較

### 設定
アプリの設定（並列実行数など）

## 📁 プロジェクト構成

```
app/src/main/java/com/example/uiedvideocompacter/
├── MainActivity.kt                 # アプリのエントリーポイント
├── data/
│   ├── manager/
│   │   ├── CompressionEngine.kt     # 圧縮エンジン (Media3 Transformer)
│   │   └── CompressionWorker.kt     # WorkManager Worker
│   ├── model/
│   │   ├── VideoItem.kt             # 動画データモデル
│   │   └── SearchSuggestionTags.kt  # 検索タグ
│   ├── repository/
│   │   └── VideoRepository.kt       # 動画リポジトリ
│   └── store/
│       ├── QueueStore.kt            # キュー状態管理
│       ├── ResultStore.kt           # 結果状態管理
│       └── UserPreferences.kt       # ユーザー設定
├── ui/
│   ├── components/                  # 共通コンポーネント
│   ├── navigation/
│   │   ├── AppNavHost.kt            # ナビゲーション定義
│   │   └── Screen.kt                # 画面定義
│   ├── screens/
│   │   ├── library/                 # ライブラリ画面
│   │   ├── preview/                 # プレビュー・設定画面
│   │   ├── queue/                   # キュー画面
│   │   ├── progress/                # 実行中タスク画面
│   │   ├── result/                  # 結果画面
│   │   ├── settings/                # 設定画面
│   │   └── onboarding/              # オンボーディング画面
│   └── theme/                       # テーマ設定
```

## 🤝 コントリビューション

コントリビューションを歓迎します！

1. Forkする
2. ブランチを作成 (`git checkout -b feature/AmazingFeature`)
3. コミットする (`git commit -m 'Add some AmazingFeature'`)
4. プッシュする (`git push origin feature/AmazingFeature`)
5. Pull Requestを作成する

## 📄 ライセンス

MIT License - 詳細は [LICENSE](LICENSE) ファイルを参照してください

---

<div align="center">

Made with ❤️ by Aero123421

</div>

---

## English Version

# UIedVideoCompacter (動画コンパク太)

> ⚠️ **Notice**: This project is entirely AI-generated and coded.

<div align="center">

[![Android](https://img.shields.io/badge/Android-31%2B-green?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-purple?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

Easily compress videos and save your phone storage

</div>

---

## ✨ Features

- 🎬 **Percentage-based Compression** - Flexibly set target file size from 10% to 100%
- 📉 **Adaptive Resolution** - Automatically downscales resolution to prevent quality loss when bitrate is too low
- 📊 **Batch Processing** - Add multiple videos to queue for batch compression
- 🎨 **Modern UI** - Material Design 3 + Jetpack Compose
- 🔔 **Background Processing** - Robust asynchronous compression with WorkManager
- 📱 **Android 31+ Support** - Leveraging latest Android features

## 🎯 Compression Logic

Instead of fixed presets, this app uses a **Target Percentage** approach:

1. **Target Calculation**: `Original Size * Percentage` = Target Size
2. **Bitrate Calculation**: Calculates required bitrate based on target size and duration.
3. **Quality Protection**: If the calculated bitrate is too low for the current resolution (Bits Per Pixel < 0.05), the app automatically downscales the video (e.g., 1080p -> 720p) to avoid blocky artifacts.

## ⬇️ Download

### APK Files

[![Latest Release](https://img.shields.io/badge/Download-Latest%20Release-green?logo=github)](https://github.com/Aero123421/doga-compressor/releases/latest)

Download the latest APK from [GitHub Releases](https://github.com/Aero123421/doga-compressor/releases).

> ⚠️ **Notice**: APKs are unsigned. You may need to allow installation from unknown sources and uninstall previous versions to update.

## 🔑 Permissions

This app requires the following permissions to function correctly:

- **Notifications (POST_NOTIFICATIONS)**: Used to show compression progress and completion status in the background.
- **Media Access (READ_MEDIA_VIDEO / READ_EXTERNAL_STORAGE)**: Required to select and compress video files from your device.
- **Foreground Service (FOREGROUND_SERVICE / MEDIA_PROCESSING)**: Allows the app to continue compressing videos even when the app is closed.

## 🚀 Quick Start

### Requirements

- Android Studio Hedgehog (2023.1.1) or higher
- **JDK 17** (Required for AGP 8.x)
- Android SDK 36

### Build

```bash
# Clone the repository (specifying directory name)
git clone https://github.com/Aero123421/doga-compressor.git UIedvideocompacter
cd UIedvideocompacter

# Build with Gradle
./gradlew build

# Install APK
./gradlew installDebug
```

## 🛠️ Tech Stack

- **Language**: Kotlin (2.0.21)
- **UI**: Jetpack Compose (Material 3)
- **Video**: Media3 (Transformer, Effect)
- **Background**: WorkManager
- **State**: ViewModel + DataStore

## 📝 License

MIT License - see [LICENSE](LICENSE) for details
