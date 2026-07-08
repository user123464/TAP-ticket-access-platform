<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import PageTabsLayout from '@/layouts/nested/PageTabsLayout.vue';

const route = useRoute();
const isB2b = computed(() => route.path.startsWith('/org'));

const tabs = computed(() => [
  { to: isB2b.value ? '/org/settings/profile' : '/settings/profile', label: '個人資料', icon: 'bi-person-circle' },
  { to: isB2b.value ? '/org/settings/account' : '/settings/account', label: '帳號與安全性', icon: 'bi-shield-lock' }
]);
const backTo = computed(() => (isB2b.value ? '/org/select' : '/'));
const backText = computed(() => (isB2b.value ? '返回組織選擇' : '返回首頁'));
</script>

<template>
  <PageTabsLayout
    title="個人設定"
    title-icon="bi-gear-fill text-primary"
    :back-to="backTo"
    :back-text="backText"
    :tabs="tabs"
    max-width="600px"
  />
</template>
