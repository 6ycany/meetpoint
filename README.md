# Claude Code 業務自動化テンプレート

Qiita記事「Claude Codeですべての日常業務を爆速化しよう！」の仕組みを
自分の作業環境として使うためのテンプレートリポジトリ。

---

## このリポジトリの構成

```
claude-work-template/          ← このリポジトリがモノレポのルートになる
│
├── new-project.sh             ← 新プロジェクト初期化スクリプト
│
├── tasks/                     ← 各業務プロジェクト
│   ├── 2026-04-05-提案書/      #   単発タスク（日付プレフィックス）
│   └── 毎月-経費精算/          #   月次タスク
│       └── 2026-04/           #     当月の作業メモ
│
├── snippets/                  ← 横断的に使い回すスクリプト集
│   └── gmail_search.py        #   例: Gmail検索ユーティリティ
│
├── docs/                      ← 調査メモ・参考資料
│
└── [テンプレートファイル群]
    ├── HANDOFF.md
    ├── CLAUDE.md
    ├── PLAN.md
    ├── SPEC.md
    ├── TODO.md
    └── KNOWLEDGE.md
```

---

## クイックスタート

### 1. このリポジトリをクローン or コピー

```bash
git clone <this-repo> ~/work
cd ~/work
chmod +x new-project.sh
```

### 2. 新プロジェクトを作成

```bash
# 単発タスク
./new-project.sh 提案書-ABC社

# 月次繰り返しタスク
./new-project.sh 経費精算 monthly

# 常時稼働エージェント
./new-project.sh メールリマインダー agent
```

### 3. Claude Code を起動して作業開始

```bash
cd tasks/2026-04-05-提案書-ABC社
# PLAN.md に音声ダンプ（やりたいことを全部書く）
claude
```

Claude Code に最初にこう伝える：
```
HANDOFF.md と KNOWLEDGE.md の最新エントリを読んで、現状を把握してください。
その後、TODO.md の未完了タスクの中から次に着手すべきものを提案してください。
```

---

## 4つのドキュメントの役割

| ファイル | タイミング | 内容 |
|----------|-----------|------|
| **PLAN.md** | 最初 | 音声ダンプ。考えを整理する前に全部書く |
| **SPEC.md** | Claudeと壁打ち後 | 仕様確定。スコープイン/アウトを明確に |
| **TODO.md** | 作業中 | タスク管理。コンテキストリセット後の再開地点 |
| **KNOWLEDGE.md** | 随時・セッション終了時 | ハマったこと・学び。新しいエントリは先頭に |
| **HANDOFF.md** | 毎回更新 | 引継ぎ資料。これを読めば現状がわかる地図 |
| **CLAUDE.md** | プロジェクト開始時 | Claude向けルール・ペルソナ定義 |

---

## MCP設定例（~/.claude/claude.json）

よく使うMCPサーバーの設定例：

```json
{
  "mcpServers": {
    "gmail": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-gmail"]
    },
    "google-calendar": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-google-calendar"]
    },
    "todoist": {
      "command": "npx",
      "args": ["-y", "mcp-server-todoist"],
      "env": {
        "TODOIST_API_TOKEN": "your-token-here"
      }
    }
  }
}
```

---

## CLAUDE.md グローバル設定（~/.claude/CLAUDE.md）

全プロジェクト共通のルールはホームディレクトリの CLAUDE.md に書く：

```markdown
## グローバルルール

- 作業者は音声入力を使うため、フィラー・誤字は正しく解釈すること
- 不明点は推測で進めず、必ず確認する
- セッション終了時はKNOWLEDGE.mdに作業サマリーを追記する
- 提案は作業前に提示し、承認を得てから実行する
```

---

## 参考

- [Claude Codeですべての日常業務を爆速化しよう！ - Qiita](https://qiita.com/minorun365/items/114f53def8cb0db60f47)
