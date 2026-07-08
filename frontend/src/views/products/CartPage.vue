<template>
  <div class="cart-page-container">
    <h2 class="page-title">您的購物車清單</h2>

    <div v-if="cartStore.cartItems.length > 0" class="cart-layout">
      
      <div class="cart-left">
        <el-card class="box-card" shadow="never">
          <div class="cart-table-wrapper">
            <el-table :data="cartStore.cartItems" style="width: 100%">
              <el-table-column label="商品資訊" min-width="220">
                <template #default="scope">
                 <div class="cart-prod-info"> 
                    <img 
                      :src=" scope.row.mainImage || 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'" 
                      alt="商品圖" 
                      class="cart-prod-img" 
                      @error="(e) => { e.target.src = 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png' }"
                    />
                    <div class="cart-prod-detail">
                      <span class="prod-name">{{ scope.row.productName }}</span>
                      <div class="prod-tags">
                        <el-tag size="small" type="info" effect="plain">顏色: {{ scope.row.productColor }}</el-tag>
                        <el-tag size="small" type="info" effect="plain" style="margin-left: 5px;">尺寸: {{ scope.row.productSize }}</el-tag>
                      </div>
                    </div>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="單價" width="100" align="center">
                <template #default="scope">
                  <span class="price-text">NT$ {{ scope.row.unitPrice }}</span>
                </template>
              </el-table-column>

              <el-table-column label="數量" width="160" align="center">
                <template #default="scope">
                  <el-input-number 
                    v-model="scope.row.quantity" 
                    :min="1" 
                    :max="scope.row.stockQty" 
                    size="small"
                    @change="(val) => handleQtyChange(scope.row.variantId, val)"
                  />
                </template>
              </el-table-column>

              <el-table-column label="小計" width="120" align="center">
                <template #default="scope">
                  <span class="price-text subtotal">NT$ {{ scope.row.unitPrice * scope.row.quantity }}</span>
                </template>
              </el-table-column>

              <el-table-column label="操作" width="80" align="center">
                <template #default="scope">
                  <el-button 
                    type="danger" 
                    link 
                    @click="cartStore.removeFromCart(scope.row.variantId)"
                  >
                    刪除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="cart-toolbar">
            <el-button type="info" link @click="clearCartConfirm">清空購物車</el-button>
            <el-button type="primary" link @click="goContinueShopping"><i class="bi bi-bag-plus me-1"></i>繼續選購週邊</el-button>
          </div>
        </el-card>
      </div>

      <div class="cart-right">
        <el-card class="summary-card" shadow="never">
          <h3 class="summary-title">訂單金額結算</h3>
          <div class="summary-row">
            <span>商品總件數</span>
            <span>{{ cartStore.totalCount }} 件</span>
          </div>
          <div class="summary-row">
            <span>商品小計</span>
            <span>NT$ {{ cartStore.totalAmount }}</span>
          </div>
          <div class="summary-row">
            <span>運費</span>
            <span class="free-shipping">NT$ 0</span>
          </div>
          <el-divider />
          <div class="summary-row total">
            <span>應付總金額</span>
            <span class="total-price">NT$ {{ cartStore.totalAmount }}</span>
          </div>
        </el-card>

        <el-card class="checkout-card" shadow="never" style="margin-top: 20px;">
          <!-- <h3 class="summary-title">📦 填寫物流與收件資訊</h3> -->
          <el-form :model="orderForm" :rules="orderRules" ref="orderFormRef" label-position="top">
            <!-- <el-form-item label="收件人真實姓名" prop="contactName">
              <el-input v-model="orderForm.contactName" placeholder="請輸入收件人姓名" />
            </el-form-item>
            <el-form-item label="行動電話" prop="contactPhone">
              <el-input v-model="orderForm.contactPhone" placeholder="如: 0912345678" />
            </el-form-item>
            <el-form-item label="收件電子郵件" prop="contactEmail">
              <el-input v-model="orderForm.contactEmail" placeholder="用於接收商城出貨通知" />
            </el-form-item>
            <el-form-item label="寄送地址" prop="shippingAddress">
              <el-input v-model="orderForm.shippingAddress" type="textarea" :rows="2" placeholder="請輸入完整的縣市、街道寄送地址" />
            </el-form-item>
            <el-form-item label="付款方式">
              <el-radio-group v-model="orderForm.paymentMethod">
                <el-radio value="CREDIT_CARD">線上刷卡</el-radio>
                <el-radio value="ATM">ATM 轉帳</el-radio>
              </el-radio-group>
            </el-form-item> -->

            <el-button 
              type="success" 
              size="large" 
              class="checkout-submit-btn" 
              :loading="checkingOut"
              @click="handleCheckout()">
              確認送出商城訂單
            </el-button>

          </el-form>
        </el-card>
      </div>

    </div>

    <div v-else class="empty-cart-view">
      <el-empty description="您的購物車內目前沒有任何週邊商品喔！">
        <el-button type="primary" size="large" @click="$router.push('/shop/home')">
          前往週邊選購大廳 <i class="bi bi-arrow-right"></i>
        </el-button>
      </el-empty>
    </div>

    <el-dialog
      v-model="receiptVisible"
      title="感謝您的訂購！訂單已成立"
      width="500px"
      :close-on-click-modal="false"
      :show-close="false"
      center
    >
      <div class="receipt-content">
        <el-result type="success" title="訂單扣款成功" sub-title="我們將盡快為您安排出貨。">
        </el-result>
        
        <!-- <div class="receipt-details">
          <p><strong>商城訂單編號：</strong> <el-tag type="warning">{{ generatedOrderId }}</el-tag></p>
          <p><strong>訂單聯絡人：</strong> {{ orderForm.contactName }}</p>
          <p><strong>付款總額：</strong> <span class="receipt-price">NT$ {{ receiptAmount }}</span></p>
          <p class="receipt-tip">提示：此訂單已同步扣除 SQL Server 中的商品庫存欄位 (stock_qty).</p>
        </div> -->
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="closeReceipt">確認並返回商城</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>

import { useCartStore } from '@/stores/cart'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from "@/composables/useToast.js";
import { useConfirm } from "@/composables/useConfirm.js";

const router = useRouter()
const cartStore = useCartStore()
const toast = useToast();
const { confirm } = useConfirm();

const orderFormRef = ref(null)
const checkingOut = ref(false)
const receiptVisible = ref(false)

// 模擬生成的收據資料存放
const generatedOrderId = ref('')
const receiptAmount = ref(0)


onMounted(async () => {
  await cartStore.fetchRemoteCart()
  console.log(cartStore.cartItems)

})

// 結帳表單欄位
// const orderForm = ref({
//   contactName: '',
//   contactPhone: '',
//   contactEmail: '',
//   shippingAddress: '',
//   paymentMethod: 'CREDIT_CARD'
// })

// 表單驗證規則
// const orderRules = {
//   contactName: [{ required: true, message: '收件人姓名為必填欄位', trigger: 'blur' }],
//   contactPhone: [{ required: true, message: '請輸入收件人行動電話', trigger: 'blur' }],
//   contactEmail: [{ required: true, type: 'email', message: '請輸入正確的 Email 格式', trigger: 'blur' }],
//   shippingAddress: [{ required: true, message: '請輸入完整的收件地址以利物流配送', trigger: 'blur' }]
// }

// 變更購物車內商品數量時即時同步更新 localStorage
const handleQtyChange = (variantId, newQty) => {
  cartStore.updateQuantity(variantId, newQty)
}

const goContinueShopping = () => {
    const themeId = localStorage.getItem("currentThemeId")

    if (themeId) {
        router.push(`/shop/theme/${themeId}`)
    } else {
        router.push('/shop/home')
    }
}

const handleCheckout = async () => {

    if (cartStore.cartItems.length > 0) {
      checkingOut.value = true
      router.push({ name: 'PaymentMerch'})
    } else {
      toast.error('請重新送出訂單')
    }
  }



// 清空購物車確認防呆
const clearCartConfirm = async () => {
  const ok = await confirm({
    title: '提示',
    message: '確定要清空購物車內的所有活動週邊商品嗎？',
    confirmText: '確定清空',
    cancelText: '取消',
    variant: 'danger'
  });
  if (ok) {
    cartStore.clearCart()
    toast.info('購物車已清空')
  }
}

// 關閉收據彈窗，並徹底清空 LocalStorage
const closeReceipt = () => {
  receiptVisible.value = false
  cartStore.clearCart() // 清空購物車
  router.push('/shop/home') // 引導消費者回到大廳
}
</script>

<style scoped>
.cart-page-container {
  max-width: 1200px;
  margin: 0 auto;
}
.page-title {
  font-size: 22px;
  color: #333;
  margin-bottom: 25px;
}
.cart-layout {
  display: flex;
  gap: 30px;
  align-items: flex-start;
}
.cart-left {
  flex: 1.6;
}
.cart-right {
  flex: 1;
}
.cart-prod-info {
  display: flex;
  align-items: center;
}
.cart-prod-img {
  width: 70px;
  height: 70px;
  border-radius: 4px;
  object-fit: cover;
  border: 1px solid #eee;
  margin-right: 15px;
}
.cart-prod-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.prod-name {
  font-size: 14px;
  font-weight: bold;
  color: #333;
}
.prod-tags {
  margin-top: 2px;
}
.price-text {
  font-size: 14px;
  color: #444;
  font-weight: 500;
}
.price-text.subtotal {
  color: #f56c6c;
  font-weight: bold;
}
.cart-toolbar {
  display: flex;
  justify-content: space-between;
  padding: 15px 10px 5px 10px;
}
.summary-title {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 15px;
  color: #333;
  border-left: 3px solid #409eff;
  padding-left: 8px;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}
.free-shipping {
  color: #67c23a;
  font-weight: bold;
}
.summary-row.total {
  font-size: 16px;
  color: #222;
  font-weight: bold;
}
.total-price {
  color: #f56c6c;
  font-size: 20px;
  font-weight: 900;
}
.checkout-submit-btn {
  width: 100%;
  margin-top: 15px;
  height: 45px !important;
  font-size: 15px !important;
  font-weight: bold !important;
}
.empty-cart-view {
  background: white;
  padding: 60px 0;
  background-color: #fff;
  border-radius: 8px;
}
.receipt-details {
  background-color: #fcfbf7;
  padding: 20px;
  border-radius: 6px;
  border: 1px dashed #e6a23c;
  margin-top: -10px;
}
.receipt-details p {
  margin: 8px 0;
  font-size: 14px;
  color: #444;
}
.receipt-price {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}
.receipt-tip {
  font-size: 12px !important;
  color: #909399 !important;
  margin-top: 15px !important;
  border-top: 1px solid #eee;
  padding-top: 10px;
}

@media (max-width: 991.98px) {
  .cart-page-container {
    padding: 12px;
  }
  .cart-layout {
    flex-direction: column;
    align-items: stretch;
    gap: 20px;
  }
  .cart-left, .cart-right {
    width: 100%;
    flex: none;
  }
  .cart-table-wrapper {
    width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  .cart-table-wrapper :deep(.el-table) {
    min-width: 650px;
  }
  .cart-toolbar {
    flex-direction: column;
    align-items: center;
    gap: 12px;
  }
}
</style>