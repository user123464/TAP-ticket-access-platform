<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import PageTabsLayout from '@/layouts/nested/PageTabsLayout.vue';

const route = useRoute();
const orgId = computed(() => route.params.organizerId);

const tabs = computed(() => [
  { to: `/org/${orgId.value}/settings/profile`, label: '組織基本資料與 KYC', icon: 'bi-file-earmark-person-fill' },
  { to: `/org/${orgId.value}/settings/contracts`, label: '合約與線上簽署', icon: 'bi-file-earmark-text-fill' },
  { to: `/org/${orgId.value}/settings/subscription`, label: '訂閱方案與加值服務', icon: 'bi-gem' }
]);
const backTo = computed(() => `/org/${orgId.value}/themes`);
</script>

<template>
  <PageTabsLayout
    title="組織設定"
    title-icon="bi-building-fill text-primary"
    :back-to="backTo"
    back-text="返回控制台"
    :tabs="tabs"
  />
</template>
