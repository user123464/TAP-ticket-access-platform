<template>
  <el-container class="admin-container">
    <!-- <el-aside width="240px" class="admin-aside">
      <div class="logo">週邊商城管理系統</div>
      <el-menu
        router
        :default-active="$route.path"
        background-color="#e57346"
        text-color="#e2e8f0"
        active-text-color="#1e293b">
        <el-menu-item :index="`/org/${organizerId}/admin/products`">
          <el-icon><List /></el-icon>
          <span>商品總覽庫存</span>
        </el-menu-item>
        <el-menu-item :index="`/org/${organizerId}/admin/product-add`">
          <el-icon><Plus /></el-icon>
          <span>新增活動週邊</span>
        </el-menu-item>
      </el-menu>
    </el-aside> -->

    <el-container>
      <!-- <el-header class="admin-header">廠商後台管理端</el-header> -->
      <el-main>
        <router-view></router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { List, Plus } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { computed } from 'vue' // 引入 computed 確保響應式

const route = useRoute()

// 使用 computed 確保當路由參數改變時，這裡也會跟著連動
const organizerId = computed(() => route.params.organizerId)

console.log(route.params.organizerId)
</script>

<style scoped>
.admin-container {
  height: 100vh;
}

.admin-aside {
  background-color: #e57346;
  color: e2e8f0;
}

.admin-aside .el-menu {
  border-right: none !important;
  background: #e57346;

}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: left;
  /* 靠左 */
  padding-left: 25px;
  /* 左邊留一些空間 */
  font-weight: bold;
  background: #e57346;
  color: #e2e8f0;
}

.admin-header {
  line-height: 60px;
  text-align: center;
  padding-left: 2%;
  border-bottom: 1px solid #e2e8f0;
  background-color: #e2e8f0;
  color: #e57346;
}
</style>