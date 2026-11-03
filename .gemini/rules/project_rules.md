# StrongAlarm 開発ルール

## 1. プロジェクト概要
「絶対に起きられる」をコンセプトにした高信頼性Androidアラームアプリ。
二度寝防止（起床確認）、アクション解除（計算・シェイク・バーコードスキャン）、段階的音量増加、放置時リセット＆再鳴動などの機能を備える。

## 2. 技術スタック & バージョン方針
- **言語**: Kotlin (2.3+)
- **UI**: Jetpack Compose + Material 3 (Compose BOM 2026.03.01+)
- **ターゲット**: compileSdk 35 / targetSdk 35 / minSdk 26 (Android 8.0 Oreo以上)
- **非同期処理**: Kotlin Coroutines & StateFlow / SharedFlow
- **アーキテクチャ**: MVVM + Repository パターン
- **ローカル永続化**: Room Database (アラーム設定・履歴) / Preferences DataStore (アプリ設定)
- **アラーム制御**: `AlarmManager` (Exact Alarm) + `ForegroundService` (Media Player / AudioAttributes.USAGE_ALARM)

## 3. ディレクトリ構成
```
com.terusan.strongalarm/
├── data/           # Room Database, Entity, Dao, DataStore, Repository
├── domain/         # ビジネスロジック・ユースケース・モデル定義
├── service/        # AlarmManager用Receiver, ForegroundService, BootReceiver
├── mission/        # 解除アクション (計算, シェイク, バーコードスキャン等)
├── ui/             # Jetpack Compose UI
│   ├── main/       # アラーム一覧・管理画面
│   ├── edit/       # アラーム作成・編集画面
│   ├── ringing/    # アラーム鳴動・アクション解除画面
│   ├── theme/      # Compose テーマ・カラー・タイポグラフィ
│   └── components/ # 共通UIコンポーネント
└── util/           # 音量制御, バイブレーション, 権限ヘルパー
```

## 4. Android固有の品質・堅牢性ルール
1. **OS制限・Dozeモード対策**:
   - `AlarmManager.setExactAndAllowWhileIdle()` を使用し、省電力モードでも確実に鳴動させる。
   - 画面オフ／ロック時でも鳴動画面を表示できるよう `USE_FULL_SCREEN_INTENT` を適用する。
   - 通知権限 (`POST_NOTIFICATIONS`)、正確なアラーム権限 (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`) を適切にハンドリングする。
2. **消音・再鳴動ロジック**:
   - アクション実行中は近所迷惑防止のため音声を一時停止（消音）する。
   - ユーザー操作が一定時間途切れた場合は、アクション状態をリセットし最大音量で再鳴動させる。
3. **端末再起動対策**:
   - `RECEIVE_BOOT_COMPLETED` で再起動時に登録済みアラームを再スケジュールする。

## 5. コーディング & Git 規約
- **コメント**: ソースコード内のコメントは日本語でわかりやすく記述する。
- **Git Author**: コミット時は必ず `Terusan-61 <terukuma1106@gmail.com>` を使用する。
- **コミットメッセージ**: `feat:`, `fix:`, `refactor:`, `style:`, `test:`, `chore:` などのプレフィックスを使用する。
- **検証**: 機能追加・変更時は `./gradlew assembleDebug` でビルドが成功することを確認する。
