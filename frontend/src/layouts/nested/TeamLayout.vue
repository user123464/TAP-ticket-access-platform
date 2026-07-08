<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import PageTabsLayout from '@/layouts/nested/PageTabsLayout.vue';

const route = useRoute();
const orgId = computed(() => route.params.organizerId);

const tabs = computed(() => [
  { to: `/org/${orgId.value}/team/members`, label: '成員列表與邀請', icon: 'bi-person-lines-fill' },
  { to: `/org/${orgId.value}/team/roles`, label: '角色與權限設定', icon: 'bi-shield-lock-fill' }
]);
const backTo = computed(() => `/org/${orgId.value}/themes`);
</script>

<template>
  <PageTabsLayout
    title="團隊管理"
    title-icon="bi-people-fill text-primary"
    :back-to="backTo"
    back-text="返回控制台"
    :tabs="tabs"
  />
</template>
