<template>
  <div class="product-list-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span class="title">活動週邊商品庫存總覽</span>
          <el-button type="primary" @click="$router.push({ name: 'ProductAdd', params: { organizerId } })">
            + 新增週邊商品
          </el-button>
        </div>
      </template>

      <div class="toolbar-container">
        <div class="batch-actions">
          <transition name="el-fade-in">
            <div v-if="selectedProducts.length > 0" class="batch-buttons">
              <el-tag type="info" effect="dark">已選取 {{ selectedProducts.length }} 項商品</el-tag>
              <el-button type="success" size="small" @click="handleBatchStatus('Released')">批量上架</el-button>
              <el-button type="warning" size="small" @click="handleBatchStatus('Draft')">批量下架</el-button>
            </div>
          </transition>
        </div>
        <div class="search-box">
          <el-input
            v-model="searchQuery"
            placeholder="請輸入商品名稱或 ID 搜尋..."
            clearable
            style="width: 260px;"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </div>

      <el-table
        :data="filteredProductList"
        v-loading="loading"
        style="width: 100%" 
        border
        row-key="productId"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="expand">
          <template #default="props">
            <div class="variant-expand-wrapper">
              <h4 class="sub-title">【{{ props.row.productName }}】規格與庫存明細</h4>
              
              <el-table :data="props.row.variants" size="small" border stripe>
                <el-table-column label="廠商款式編號 (orgSkuNo)" prop="orgSkuNo" width="180" />
                <el-table-column label="商品顏色" prop="productColor" width="120">
                  <template #default="scope">
                    <el-tag size="small" effect="plain">{{ scope.row.productColor }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="商品尺寸" prop="productSize" width="100" />
                <el-table-column label="單價 (NTD)" width="120">
                  <template #default="scope">
                    <span class="price-text">${{ formatPrice(scope.row.unitPrice) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="剩餘庫存 (stockQty)" width="150">
                  <template #default="scope">
                    <el-badge 
                      :value="scope.row.stockQty" 
                      :type="scope.row.stockQty === 0 ? 'danger' : (scope.row.stockQty < 100 ? 'warning' : 'primary'                    
                      )"
                      :offset="[35,10]"                      
                    >
                      <span class="stock-text" :class="{ 'out-of-stock': scope.row.stockQty === 0 }">
                        {{ scope.row.stockQty === 0 ? '已售罄 (缺貨)' : scope.row.stockQty + ' 件' }}
                      </span>
                    </el-badge>
                  </template>
                </el-table-column>
                <el-table-column label="國際條碼" prop="barcode" width="180" />
                <el-table-column label="款式狀態">
                  <template #default="scope">
                    <el-tag :type="scope.row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
                      {{ scope.row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="商品 ID" prop="productId" width="90" align="center" />
        
        <el-table-column label="關聯活動主題" width="220">
          <template #default="scope">
            <el-icon><Calendar /></el-icon>
            <span style="margin-left: 8px; font-weight: bold;">{{ scope.row.theme?.title || '未關聯活動' }}</span>
          </template>
        </el-table-column>        

        <el-table-column label="商品分類" width="140">
          <template #default="scope">
            <el-tag type="info" effect="light">{{ scope.row.category?.categoryName || '預設分類' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="商品名稱" prop="productName" min-width="150" />


        <el-table-column type="selection" width="40" reserve-selection />
          <el-table-column label="發布狀態" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ translateStatus(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>


        <el-table-column label="建立日期" width="160" align="center">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" :fixed="isMobile ? false : 'right'" align="center">
          <template #default="scope">
            <el-button size="small" type="warning" @click="handleEdit(scope.row)">
              編輯
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">
              刪除
            </el-button>
                        <el-button size="small" type="success" @click="handleCopy(scope.row)">
              複製
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watchEffect } from 'vue'
import { Calendar, Search } from '@element-plus/icons-vue'
import axios from '@/plugins/axios' // 走封裝實例：自動帶 Bearer token + 401/403 處理
import { useRoute, useRouter } from 'vue-router'
import { useToast } from "@/composables/useToast.js";
import { useConfirm } from "@/composables/useConfirm.js";

const toast = useToast();
const { confirm } = useConfirm();

const route = useRoute()
const router = useRouter()

watchEffect(() => {
  console.log(route.params)
})

const organizerId = computed(() => route.params.organizerId)

const productList = ref([])
const loading = ref(false)
const searchQuery = ref('') 
const selectedProducts = ref([]) 

const form = ref({
  themeId: '',
  categoryId: '',
  productName: '',
  productSimDescription: '',
  productDescription: '',
  status: 'Released',
  images: [], 
  variants: [
    {
      orgSkuNo: '',
      productColor: '',
      productSize: '',
      unitPrice: 0,
      stockQty: 0,
      barcode: '',
      status: 'Released'
    }
  ]
})

// 【關鍵功能】利用 computed 進行前端即時搜尋過濾
const filteredProductList = computed(() => {
  if (!searchQuery.value.trim()) {
    return productList.value
  }
  const query = searchQuery.value.toLowerCase().trim()
  return productList.value.filter(item => {
    return (
      item.productName?.toLowerCase().includes(query) ||
      String(item.productId).includes(query)
    )
  })
})

// 頁面加載時取得所有商品與規格
const fetchProducts = async () => {
  loading.value = true
  try {
    const response = await axios.get(`/api/org/${organizerId.value}/admin/products`)
    // [fix by Jason] 保險過濾：後端已依 organizerId 篩選，前端再擋一層，
    // 只保留屬於當前組織的商品，避免任何情況下看到別組織的商品。
    // （organizerId 缺漏時不濾除，以免後端未帶欄位時整頁空白）
    const rows = Array.isArray(response.data) ? response.data : []
    productList.value = rows.filter(
      (p) => !p.organizerId || p.organizerId === organizerId.value
    )
  } catch (error) {
    console.error('取得商品列表失敗:', error)
    toast.error('無法取得商品列表')
  } finally {
    loading.value = false
  }
}

// 窄螢幕（<768）取消操作欄的 fixed="right"，避免固定欄永久佔住視窗把資料蓋住
const isMobile = ref(typeof window !== 'undefined' && window.innerWidth < 768)
const handleResize = () => { isMobile.value = window.innerWidth < 768 }

onMounted(() => {
  fetchProducts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

// 狀態標籤顏色分流
const getStatusType = (status) => {
  switch (status) {
    case 'Released': return 'success'
    case 'Draft': return 'info'
    case 'Discontinued': return 'warning'
    case 'OutOfStock': return 'danger'
    default: return ''
  }
}

// 狀態中文化
const translateStatus = (status) => {
  switch (status) {
    case 'Released': return '上架中'
    case 'Draft': return '草稿暫存'
    case 'Discontinued': return '已下架'
    case 'OutOfStock': return '缺貨中'
    default: return status
  }
}

// 價格與日期格式化工具
const formatPrice = (value) => value ? Number(value).toFixed(0) : '0'
const formatDate = (dateStr) => dateStr ? dateStr.replace('T', ' ').substring(0, 16) : ''

// 編輯
const handleEdit = (row) => {  
    router.push(`/org/${organizerId.value}/admin/products/${row.productId}/edit`) 
 }

 //複製
const handleCopy = (row) => {
  router.push(`/org/${organizerId.value}/admin/product-add?copyFrom=${row.productId}`)
}

//刪除
const handleDelete = async (row) => {
    const ok = await confirm({
        title: "確定要刪除嗎？",
        message: `商品「${row.productName}」以及所有規格都會被永久刪除！`,
        confirmText: "確認刪除",
        variant: "danger"
    })

    if (!ok) {
        return
    }
    try {
        await axios.delete(
            `/api/org/${organizerId.value}/admin/products/${row.productId}`
        )

        toast.success("商品已成功刪除。")

        //重新取得資料
        fetchProducts()

    } catch (error) {
        console.error(error)
        toast.error("請稍後再試。")
    }
}

// 當使用者勾選或取消勾選時觸發
const handleSelectionChange = (selection) => {
  selectedProducts.value = selection
}

// 處理批量修改狀態的函式
const handleBatchStatus = async (newStatus) => {
  if (selectedProducts.value.length === 0) {
    toast.error('請先勾選要操作的商品！')
    return
  }

  const productId = selectedProducts.value.map(item => item.productId)
  const statusText = newStatus === 'Released' ? '上架' : '下架'

  const ok = await confirm({
    title: '批量操作提示',
    message: `確定要將這 ${productId.length} 項商品批量變更狀態為【${statusText}】嗎？`,
    confirmText: '確定',
    variant: 'primary'
  })

  if (!ok) return

  try {
    // 發送請求給 Java 後端 API
    await axios.put(`/api/org/${organizerId.value}/admin/batch-status`, {
      productId: productId,
      status: newStatus === 'Released' ? "1" : "2"
    })

    await fetchProducts() // 重新整理列表
    selectedProducts.value = []
    toast.success(`成功批量${statusText} ${productId.length} 項商品！`)
    
  } catch (error) {
    console.error(error)
    toast.error(
      error.response?.data || '批量修改狀態失敗，請稍後再試。'
    )
  }
}


</script>

<style scoped>
.product-list-container {
  max-width: 1400px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

/* 窄螢幕：工具列改上下堆疊、搜尋框滿版 */
@media (max-width: 767.98px) {
  .toolbar-container {
    flex-direction: column;
    align-items: stretch;
  }
  .search-box,
  .search-box :deep(.el-input) {
    width: 100% !important;
  }
}
.title {
  font-size: 18px;
  font-weight: bold;
}
.variant-expand-wrapper {
  padding: 15px 40px;
  background-color: #fcfdfd;
}
.sub-title {
  margin-top: 0;
  margin-bottom: 12px;
  color: #606266;
  font-size: 14px;
}
.price-text {
  color: #f56c6c;
  font-weight: bold;
}
.stock-text {
  font-size: 13px;
  padding: 2px 6px;
}
.out-of-stock {
  color: #f56c6c;
  text-decoration: line-through;
}
</style>