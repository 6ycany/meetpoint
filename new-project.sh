#!/bin/bash
# new-project.sh — Claude Code プロジェクト初期化スクリプト
#
# 使い方:
#   ./new-project.sh <project-name> [task-type]
#
# task-type:
#   once     単発タスク（デフォルト）
#   monthly  月次繰り返しタスク
#   agent    常時稼働エージェント
#
# 例:
#   ./new-project.sh 経費精算 monthly
#   ./new-project.sh 提案書-ABC社 once

set -e

TEMPLATE_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_NAME="${1:-new-project}"
TASK_TYPE="${2:-once}"
TODAY=$(date +%Y-%m-%d)

# 月次タスクの場合はYYYY-MM-プレフィックスをつける
if [ "$TASK_TYPE" = "monthly" ]; then
  MONTH_PREFIX=$(date +%Y-%m)
  DIR_NAME="tasks/毎月-${PROJECT_NAME}"
  ENTRY_DIR="tasks/毎月-${PROJECT_NAME}/${MONTH_PREFIX}"
else
  DIR_NAME="tasks/${TODAY}-${PROJECT_NAME}"
  ENTRY_DIR="$DIR_NAME"
fi

echo "🚀 Creating project: ${PROJECT_NAME} (${TASK_TYPE})"
echo "   Directory: ${DIR_NAME}"

# ディレクトリ作成
mkdir -p "${DIR_NAME}"
if [ "$TASK_TYPE" = "monthly" ]; then
  mkdir -p "${ENTRY_DIR}"
fi
mkdir -p "${DIR_NAME}/data" "${DIR_NAME}/output" "${DIR_NAME}/scripts"

# テンプレートファイルをコピー（日付を差し替えて）
for f in HANDOFF.md CLAUDE.md PLAN.md SPEC.md TODO.md KNOWLEDGE.md; do
  sed "s/<!-- YYYY-MM-DD -->/${TODAY}/g" "${TEMPLATE_DIR}/${f}" > "${DIR_NAME}/${f}"
done

# HANDOFF.md のプロジェクト名を差し替え
sed -i "s/<!-- 例: 月次経費精算の半自動化 -->/${PROJECT_NAME}/g" "${DIR_NAME}/HANDOFF.md" 2>/dev/null || \
sed -i '' "s/<!-- 例: 月次経費精算の半自動化 -->/${PROJECT_NAME}/g" "${DIR_NAME}/HANDOFF.md"

# 月次タスクの場合、当月エントリ用のメモファイルを追加
if [ "$TASK_TYPE" = "monthly" ]; then
  MONTH_PREFIX=$(date +%Y-%m)
  cat > "${ENTRY_DIR}/memo.md" << EOF
# ${PROJECT_NAME} — ${MONTH_PREFIX} 作業メモ

作業日: ${TODAY}

## 今月の特記事項

-

## 完了確認

- [ ] 出力物の確認
- [ ] 提出・申請
EOF
fi

echo ""
echo "✅ Done! プロジェクトを初期化しました。"
echo ""
echo "次のステップ:"
echo "  1. cd ${DIR_NAME}"
echo "  2. PLAN.md に音声ダンプ（やりたいことを全部書く）"
echo "  3. claude を起動して「HANDOFF.mdを読んで、PLAN.mdの内容をもとにSPEC.mdをドラフトして」と伝える"
echo ""
