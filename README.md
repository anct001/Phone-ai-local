# 帳票チェッカー (FormChecker)

Androidアプリで製造業の帳票を撮影し、AIで記入内容を自動検証します。

**2つの解析エンジンに対応:**
- 🧠 **オンデバイスAI (Gemma 3n)** — 完全オフライン・APIキー不要（既定）
- ☁ **クラウドAI (Claude)** — 高精度（手書きに強い・要APIキー）

エンジンはアプリ内「エンジン切替」でいつでも変更できます。

## 対応帳票

| 帳票種類 | 検証内容 |
|---------|---------|
| 加工指示書・ロット管理票 | ヘッダー必須項目、累計値整合性、日産数合計 |
| ライン作業日報 | 時間帯別生産数、合計値、担当者署名 |
| その他製造帳票 | 必須フィールド、数値整合性 |

## オンデバイスAI (Gemma) のセットアップ

オンデバイスエンジンはモデルファイルが大きい(~1.5–3GB)ためAPKには同梱しません。
ユーザーが端末にモデルを取り込みます。

1. Gemma 3n の画像対応モデル(`.task` 形式)を入手
   （[Google AI Edge / LiteRT のモデルページ](https://ai.google.dev/edge/litert) や
   Hugging Face の対応モデルから。ライセンス同意が必要な場合があります）
2. 端末本体ストレージに保存
3. アプリ →「🧠 AIモデルを管理」→「取り込む」→ ファイルを選択
4. 取り込み完了後、撮影するとオフラインで解析されます

> 推奨スペック: RAM 6–8GB 以上。手書きが多い帳票は精度が下がるため、
> 重要な検証はクラウドAI(Claude)併用を推奨します。

開発時に adb で直接モデルを置く場合:
```bash
adb push gemma-3n.task /sdcard/Download/
# その後アプリの「取り込む」で /Download から選択
```

## クラウドAI (Claude) のセットアップ

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
- **MediaPipe LLM Inference (Gemma 3n / on-device vision)**
- Claude claude-sonnet-4-6 (Vision API / cloud)
- Material Design 3
- OkHttp + Gson + Coroutines

## アーキテクチャ

```
CameraActivity → 画像
                  ↓
        ModelManager.createAnalyzer()   ← エンジン選択 (SharedPreferences)
              ┌────────┴─────────┐
   LocalGemmaAnalyzer        ClaudeApiService
   (MediaPipe, offline)      (Anthropic API)
              └────────┬─────────┘
            FormAnalyzer interface
                  ↓
        FormAnalysisResult → ResultActivity
```
