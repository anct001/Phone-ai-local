# 帳票チェッカー (FormChecker)

Androidアプリで製造業の帳票を撮影し、AIで記入内容を自動検証します。

## 対応帳票

| 帳票種類 | 検証内容 |
|---------|---------|
| 加工指示書・ロット管理票 | ヘッダー必須項目、累計値整合性、日産数合計 |
| ライン作業日報 | 時間帯別生産数、合計値、担当者署名 |
| その他製造帳票 | 必須フィールド、数値整合性 |

## セットアップ

### 1. APIキーの設定

`local.properties.example` を `local.properties` にコピーして編集:

```
sdk.dir=/path/to/your/Android/sdk
CLAUDE_API_KEY=sk-ant-your-api-key-here
```

APIキーは [Anthropic Console](https://console.anthropic.com) から取得してください。

### 2. ビルド

```bash
./gradlew assembleDebug
```

### 3. インストール

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 使い方

1. アプリを起動
2. **「帳票を撮影する」** ボタンをタップ
3. 帳票全体が枠内に収まるよう撮影
4. AIが自動で各フィールドを解析
5. 結果画面で OK / エラー / 警告 / 未記入 を確認

## 検証ルール

- 必須フィールドの未記入チェック
- 日付フォーマットの確認
- 累計値の整合性（累計 = 前日累計 + 当日値）
- 合計欄と各行の合計の一致
- 不良数の異常検知（日産数の10%超で警告）
- 担当者・確認者の署名欄チェック

## 技術スタック

- Kotlin + CameraX
- Claude claude-sonnet-4-6 (Vision API)
- Material Design 3
- OkHttp + Coroutines
