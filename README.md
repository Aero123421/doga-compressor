# 動画コンパク太 (UIedVideoCompacter)

<div align="center">

![Android](https://img.shields.io/badge/Android-31%2B-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6-purple?logo=jetpackcompose)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

動画を簡単に圧縮して、スマホの容量を節約

[README in English](#english-version)

</div>

---

## ✨ 特徴

- 🎬 **複数の圧縮プリセット** - 高画質から極小まで5つのプリセット
- 📊 **バッチ処理** - 複数の動画をキューに追加して一括圧縮
- 🎨 **モダンなUI** - Material Design 3 + Jetpack Compose
- 🔔 **バックグラウンド処理** - WorkManagerを使用した非同期圧縮
- 📱 **Android 31+対応** - 最新のAndroid機能を活用

## 📦 技術スタック

| カテゴリ | 技術 |
|----------|------|
| 言語 | Kotlin |
| UI | Jetpack Compose (Material 3) |
| ビデオ処理 | Media3 (ExoPlayer, Transformer, Effect) |
| バックグラウンド処理 | WorkManager |
| 状態管理 | ViewModel + DataStore |
| 依存性注入 | なし (シンプルな構成) |
| 非同期処理 | Kotlin Coroutines + Flow |

## 🎯 圧縮プリセット

| プリセット | ビットレート | 解像度 | 用途 |
|-----------|-------------|--------|------|
| 高画質 | 1.5GB/時 | 1080p | 画質を優先 |
| バランス | 800MB/時 | 1080p | 一般的な用途 (推奨) |
| 軽量 | 470MB/時 | 1080p | 共有向き |
| 小サイズ | 350MB/時 | 720p | SNS投稿向き |
| 極小 | 200MB/時 | 480p | 確認用 |

## 🚀 クイックスタート

### 環境要件

- Android Studio Hedgehog (2023.1.1) 以上
- JDK 11 以上
- Android SDK 36

### ビルド手順

```bash
# リポジトリのクローン
git clone <repository-url>
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
選択した動画の圧縮プレビュー、プリセット選択

### 圧縮キュー
圧縮待ちの動画一覧、プリセット変更

### 実行中タスク
現在進行中の圧縮処理の監視

### 圧縮結果
圧縮済み動画の閲覧、共有、元動画との比較

### 設定
アプリの設定、プリセットのカスタマイズ

## 📁 プロジェクト構成

```
app/src/main/java/com/example/uiedvideocompacter/
├── MainActivity.kt                 # アプリのエントリーポイント
├── data/
│   ├── manager/
│   │   ├── CompressionEngine.kt     # 圧縮エンジン
│   │   └── CompressionWorker.kt     # WorkManager Worker
│   ├── model/
│   │   ├── CompressionPreset.kt    # プリセットデータモデル
│   │   ├── VideoItem.kt           # 動画データモデル
│   │   └── SearchSuggestionTags.kt # 検索タグ
│   ├── repository/
│   │   └── VideoRepository.kt      # 動画リポジトリ
│   └── store/
│       ├── QueueStore.kt          # キュー状態管理
│       ├── ResultStore.kt         # 結果状態管理
│       └── UserPreferences.kt     # ユーザー設定
├── ui/
│   ├── components/
│   │   └── VideoThumbnail.kt      # 動画サムネイル
│   ├── navigation/
│   │   ├── AppNavHost.kt          # ナビゲーション
│   │   └── Screen.kt              # 画面定義
│   ├── screens/
│   │   ├── library/               # ライブラリ画面
│   │   ├── preview/               # プレビュー画面
│   │   ├── queue/                  # キュー画面
│   │   ├── active/                 # 実行中タスク画面
│   │   ├── results/               # 結果画面
│   │   ├── settings/               # 設定画面
│   │   └── onboarding/            # オンボーディング画面
│   └── theme/                      # テーマ設定
```

## 🛠️ 主要ライブラリ

```kotlin
// Jetpack Compose
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.ui)
implementation(libs.androidx.compose.material3)

// Media3
implementation(libs.androidx.media3.exoplayer)
implementation(libs.androidx.media3.transformer)
implementation(libs.androidx.media3.effect)

// その他
implementation(libs.androidx.work.runtime.ktx)
implementation(libs.androidx.navigation.compose)
implementation(libs.io.coil.compose)
```

## 📝 開発計画

- [ ] カスタムプリセットの作成・保存
- [ ] クラウドストレージ連携
- [ ] 圧縮履歴の統計表示
- [ ] トラックの選択的な削除
- [ ] GIF変換機能
- [ ] Dark Modeの最適化

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

Made with ❤️ by [Your Name]

</div>

---

## English Version

# UIedVideoCompacter (動画コンパク太)

<div align="center">

![Android](https://img.shields.io/badge/Android-31%2B-green?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue?logo=kotlin)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6-purple?logo=jetpackcompose)

Easily compress videos and save your phone storage

</div>

---

## ✨ Features

- 🎬 **Multiple Compression Presets** - 5 presets from high quality to ultra small
- 📊 **Batch Processing** - Add multiple videos to queue for batch compression
- 🎨 **Modern UI** - Material Design 3 + Jetpack Compose
- 🔔 **Background Processing** - Asynchronous compression with WorkManager
- 📱 **Android 31+ Support** - Leveraging latest Android features

## 🚀 Quick Start

### Requirements

- Android Studio Hedgehog (2023.1.1) or higher
- JDK 11 or higher
- Android SDK 36

### Build

```bash
# Clone the repository
git clone <repository-url>
cd UIedvideocompacter

# Build with Gradle
./gradlew build

# Install APK
./gradlew installDebug
```

## 📂 Project Structure

```
app/src/main/java/com/example/uiedvideocompacter/
├── MainActivity.kt                 # App entry point
├── data/                            # Data layer
├── ui/
│   ├── components/                  # Reusable components
│   ├── navigation/                  # Navigation setup
│   ├── screens/                     # Screen implementations
│   └── theme/                       # App theming
```

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Video**: Media3 (ExoPlayer, Transformer, Effect)
- **Background**: WorkManager
- **State**: ViewModel + DataStore

## 📝 License

MIT License - see [LICENSE](LICENSE) for details
