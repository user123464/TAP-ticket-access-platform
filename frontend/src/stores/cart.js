import axios from '@/plugins/axios'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const useCartStore = defineStore('cart', () => {
  const cartItems = ref([])

  //登入後儲存在____(localStorage)
  const userId = computed(() => localStorage.getItem('user_id'))

  // 從後端資料庫撈取專屬購物車
  async function fetchRemoteCart() {
    if (!userId.value) return

    try {
      const res = await axios.get(`/shop/cart`)
      console.log('後端原始購物車資料 res.data：', res.data)
    
      // 將後端傳回的資料對應轉換為前端 Table 用的格式
      cartItems.value = res.data.map(item => ({
        productId: item.productId,
        themeId: item.themeId,
        variantId: item.variantId,
        productName: item.productName,
        mainImage: item.mainImage || '',
        orgSkuNo: item.orgSkuNo,
        productColor: item.productColor,
        productSize: item.productSize,
        unitPrice: item.unitPrice,
        stockQty: item.stockQty,
        quantity: item.quantity,
        
      }))        
    } catch (error) {
      console.error('撈取專屬購物車失敗', error)
    }
    }

  // 加入商品到購物車 (與後端同步)
  async function addToCart(variantId, quantity) {
    if (!userId.value) {
      console.warn('請先登入會員')
      return { ok: false, reason: 'unauthenticated' }
    }
    try {
      await axios.post(`/shop/cart/add`, {
        variantId: variantId,
        quantity: quantity
      })
      // 寫入成功後重撈，確保資料與資料庫完全一致
      await fetchRemoteCart()
      return { ok: true }
    } catch (error) {
      console.error('加入購物車失敗', error)
      return { ok: false, reason: 'error', error }
    }
  }



  // 變更數量時，同步打 API 告知後端
  async function updateQuantity(variantId, newQty) {
    if (!userId.value) return

    const item = cartItems.value.find(i => i.variantId === variantId)
    if (item) {
      try {
        item.quantity = newQty
        // 呼叫後端更新該變體數量的 API
        await axios.post(`/shop/cart/update`, {
          variantId,
          quantity: newQty
        })
        //重新同步確保正確性
        await fetchRemoteCart()
      } catch (error) {
        console.error('更新購物車數量失敗', error)
      }
    }
  }

  // 刪除購物車項目
  async function removeFromCart(variantId) {
    try {
      cartItems.value = cartItems.value.filter(i => i.variantId !== variantId)
      await axios.delete(`/shop/cart/remove/${variantId}`)
    } catch (error) {
      console.error('刪除購物車商品失敗', error)
    }
  }

  async function clearCart() {
    const requests = cartItems.value.map(item =>
    axios.delete(`/shop/cart/remove/${item.variantId}`));
  await Promise.all(requests);
  cartItems.value = [];
  }

  //補上CartPage.vue需要的計算屬性(總數量與金額)
  const totalCount = computed(() => {
    return cartItems.value.reduce((sum, item) => sum + item.quantity, 0)
  })

  const totalAmount = computed(() => {
    return cartItems.value.reduce((sum, item) => sum + (item.unitPrice * item.quantity), 0)
  })

  return {
    cartItems,
    totalCount,
    totalAmount,
    fetchRemoteCart,
    addToCart,
    updateQuantity,
    removeFromCart,
    clearCart
  }
})