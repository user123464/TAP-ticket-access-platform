<script setup>
/**
 * ContractEditor.vue — Markdown 合約編輯器（md-editor-v3 深色版）
 *
 * 公版合約範本編輯 + 即時預覽。v-model 綁定 .md 內容。
 * P2 通知模板也可複用本元件。
 */
import { MdEditor } from "md-editor-v3";
import "md-editor-v3/lib/style.css";

const content = defineModel({ type: String, default: "" });

defineProps({
  height: {
    type: String,
    default: "560px",
  },
  // 唯讀檢視模式（詳情頁用）
  readonly: {
    type: Boolean,
    default: false,
  },
});

// 精簡工具列：合約編輯用不到圖片上傳/任務清單等
const toolbars = [
  "bold", "italic", "strikeThrough", "title", "-",
  "unorderedList", "orderedList", "quote", "table", "link", "-",
  "revoke", "next", "-", "preview", "fullscreen",
];
</script>

<template>
  <MdEditor
    v-model="content"
    theme="dark"
    :style="{ height }"
    :toolbars="toolbars"
    :read-only="readonly"
    :preview="true"
    :footers="['markdownTotal']"
    placeholder="輸入合約條款（Markdown 格式）..."
  />
</template>

<style scoped>
/* 編輯器邊框融入深色卡片 */
:deep(.md-editor) {
  border-color: var(--tap-border);
  border-radius: 0.5rem;
}
</style>
