<template>
  <div class="product-form-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span class="title">{{ pageTitle }}</span>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="keyin-form">
        
        <div class="form-grid-2">
          <el-form-item label="活動主題" prop="themeId">
            <el-select v-model="form.themeId" placeholder="請選擇活動" style="width: 100%">
              <el-option v-for="item in themeOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="商品類型" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="請選擇商品主分類" style="width: 100%">
              <el-option v-for="item in categoryOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item prop="productName" class="hide-official-star">
          <template #label>
            <div class="label-with-counter">
              <span><span class="required-star">*</span>商品名稱</span>
              <span class="counter-badge" :class="{ 'over-limit': form.productName.length > 49 }">
                {{ form.productName.length }} / 50 字
              </span>
            </div>
          </template>
          <el-input 
            v-model="form.productName" 
            type="textarea" 
            :rows="1" 
            placeholder="請輸入商品名稱" 
            maxlength="50"
          />
        </el-form-item>

        <el-form-item prop="productSimDescription">
          <template #label >
            <div class="label-with-counter">
              <span style="padding-left: 10px;">商品摘要</span>
              <span class="counter-badge" :class="{ 'over-limit': form.productSimDescription.length > 99 }">
                {{ form.productSimDescription.length }} / 100 字
              </span>
            </div>
          </template>
          <el-input 
            v-model="form.productSimDescription" 
            type="textarea" 
            :rows="1" 
            placeholder="請輸入簡短摘要..." 
            maxlength="100"
          />
        </el-form-item>

        <el-form-item prop="productDescription">
          <template #label>
            <div class="label-with-counter">
              <span style="padding-left: 10px;">商品詳細描述介紹</span>
            </div>
          </template>
          <el-input 
            v-model="form.productDescription" 
            type="textarea" 
            :rows="2" 
            placeholder="請詳細輸入衣物材質、洗滌說明、退換貨條款等豐富內容..." 
          />
        </el-form-item>

        <div class="image-upload-section">
          <span class="section-sub-title" >商品相簿 (最多 6 張)</span>
          
          <div class="photo-grid">
            <div v-for="(url, index) in form.images" :key="'uploaded-'+index" class="photo-box preview-box">
              <img :src="url" class="preview-img" alt="商品相片">
              <button type="button" class="remove-btn" @click="removeUploadedImage(index)">×</button>
              <div class="cloud-tag">Cloudinary 已同步</div>
            </div>

            <div v-if="isCloudUploading" class="photo-box upload-loading-box" v-loading="true">
              <span class="loading-text">上傳雲端中...</span>
            </div>

            <div v-if="form.images.length < 6 && !isCloudUploading" class="photo-box upload-btn-box" @click="triggerFileInput">
              <div class="upload-content">
                <span class="icon">+</span>
                <span>上傳圖片</span>
              </div>
              <input 
                type="file" 
                ref="fileInputRef" 
                class="hidden-input" 
                accept="image/*" 
                multiple 
                @change="handleFileToCloudinary" 
              />
            </div>
          </div>
        </div>

        <el-divider />

        <div class="variants-section">
          <div class="variants-header">
            <span class="section-sub-title" style="padding-left: 10px;">商品規格款式與各別庫存明細</span>
            <el-button type="success" size="small" @click="addVariant">
              + 增加一筆尺寸/顏色規格
            </el-button>
          </div>

          <el-table :data="form.variants" style="width: 100%" border class="variant-table">
            <el-table-column label="廠商款式編號" min-width="150">
              <template #default="scope">
                <el-input v-model="scope.row.orgSkuNo" placeholder="如: TSHIRT-BK" size="small" />
              </template>
            </el-table-column>

            <el-table-column label="商品顏色" width="130">
              <template #default="scope">
                <el-input v-model="scope.row.productColor" placeholder="如: 白色" size="small" />
              </template>
            </el-table-column>

            <el-table-column label="商品尺寸" width="110">
              <template #default="scope">
                <el-input v-model="scope.row.productSize" placeholder="如: XL / FREE" size="small" />
              </template>
            </el-table-column>

              <el-table-column width="150">
              <template #header>
                <span class="required-star">*</span> 銷售單價 (NTD)
              </template>
              <template #default="scope">
                <el-form-item 
                  :prop="'variants.' + scope.$index + '.unitPrice'" 
                  :rules="[{ required: true, message: '請輸入價格', trigger: 'blur' }]"
                  class="table-form-item"
                >
                  <el-input-number 
                    v-model="scope.row.unitPrice" 
                    :min="0" 
                    :precision="0" 
                    :controls="false" 
                    style="width:100%" 
                    size="small" 
                  />
                </el-form-item>
              </template>
            </el-table-column>

            <el-table-column width="150">
              <template #header>
                <span class="required-star">*</span> 初始入庫數量
              </template>
              <template #default="scope">
                <el-form-item 
                  :prop="'variants.' + scope.$index + '.stockQty'" 
                  :rules="[{ required: true, message: '請輸入庫存', trigger: 'blur' }]"
                  class="table-form-item"
                >
                  <el-input-number 
                    v-model="scope.row.stockQty" 
                    :min="0" 
                    style="width:100%" 
                    size="small" 
                  />
                </el-form-item>
              </template>
            </el-table-column>

            <el-table-column label="國際條碼" min-width="140">
              <template #default="scope">
                <el-input v-model="scope.row.barcode" placeholder="選填條碼" size="small" />
              </template>
            </el-table-column>

            <el-table-column label="操作" width="70" align="center">
              <template #default="scope">
                <el-button type="danger" link :disabled="form.variants.length === 1" @click="removeVariant(scope.$index)">
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="form-actions">
          <el-button type="primary" size="large" class="submit-btn" :loading="submitting" @click="submitForm(formRef)">
            {{ submitBtnText }}
          </el-button>
          <el-button size="large" @click="handleCancel(formRef)">取消並返回</el-button>
        </div>

      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useToast } from "@/composables/useToast.js";
import { useConfirm } from "@/composables/useConfirm.js";
import axios from '@/plugins/axios'
import freeAxios from 'axios'

const toast = useToast();
const { confirm } = useConfirm();

const route = useRoute()
const router = useRouter()

// 路由參數分析
const organizerId = computed(() => route.params.organizerId)
const productId = computed(() => route.params.productId) // 修改模式會拿到這個 ID
const copyFromId = computed(() => route.query.copyFrom)   // 複製模式會拿到這個 Query



// 💡 判定當前頁面模式
const isEditMode = computed(() => {
  return productId.value != null
})
const isCopyMode = computed(() => !!copyFromId.value)

// 💡 動態計算 UI 顯示文字
const pageTitle = computed(() => {
  if (isEditMode.value) return '修改商品資訊'
  if (isCopyMode.value) return '複製並建立新商品'
  return '新增商品'
})

const submitBtnText = computed(() =>
  isEditMode.value
    ? '確認修改並更新商品'
    : '驗證欄位並確認儲存入庫'
)

// Cloudinary 配置
const CLOUDINARY_URL = 'https://api.cloudinary.com/v1_1/dw6mnxj0a/image/upload'
const UPLOAD_PRESET = 'project_tap_preset' 
const formRef = ref(null)
const fileInputRef = ref(null)
const submitting = ref(false)
const isCloudUploading = ref(false)

const themeOptions = ref([])
const categoryOptions = ref([])

// 表單初始結構
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

const rules = {
  themeId: [{ required: true, message: '請選擇關聯的活動主題', trigger: 'change' }],
  categoryId: [{ required: true, message: '請選擇商品主分類', trigger: 'change' }],
  productName: [{ required: true, message: '商品名稱為必填欄位', trigger: 'blur' }],
  unitPrice: [{ required: true, message: '單價為必填欄位', trigger: 'change' }],
  stockQty: [{ required: true, message: '庫存數量為必填欄位', trigger: 'change' }]
}

onMounted(async () => {
  // 1. 載入下拉式選單基礎資料
  try {
    const themeRes = await axios.get(`/api/org/${organizerId.value}/admin/themes/options`)
    themeOptions.value = themeRes.data.map(t => ({ id: t.id, label: t.label }))

    const catRes = await axios.get(`/api/org/${organizerId.value}/admin/categories/options`)
    categoryOptions.value = catRes.data.map(c => ({ id: c.id, label: c.label }))
  } catch (error) {
    console.error('載入基礎選單失敗:', error)
    toast.error('無法連線後端載入活動與分類選單。')
  }

  // 2. 💡 重點：如果為「修改」或「複製」模式，則非同步去後端撈取既存商品詳情
  const targetId = productId.value || copyFromId.value
  if (targetId) {
    try {
      // 假設你的 Java 後端查詢單一商品詳情 API 為：GET /products/{id}
      const res = await axios.get(`/api/org/${organizerId.value}/admin/products/${targetId}`)

      console.log("🔥 Java 後端回傳的原始全部資料 res.data:", res.data)

      const remoteData = res.data.data || res.data // 依據後端回傳格式微調

      console.log("整理後的商品詳情 remoteData 物件:", remoteData)
      
        if (remoteData) {
        // 填入基礎欄位
        form.value.themeId = remoteData.themeId
        form.value.categoryId = remoteData.categoryId
        form.value.productName = isCopyMode.value ? remoteData.productName + ' (複製)' : remoteData.productName
        form.value.productSimDescription = remoteData.productSimDescription || ''
        form.value.productDescription = remoteData.productDescription || ''
        form.value.status = remoteData.status || 'Draft'
        form.value.images = remoteData.images ? [...remoteData.images] : []

        const backendVariants = remoteData.variants || remoteData.productVariants || remoteData.product_variants || []
        console.log("檢查規格陣列 backendVariants:", backendVariants)
        
        // 處理規格 (Variants)
        if (remoteData.variants && remoteData.variants.length > 0) {
          form.value.variants = remoteData.variants.map(v => ({       
            ...(isEditMode.value ? { id: v.variantId } : {}),
            orgSkuNo: v.orgSkuNo,
            productColor: v.productColor,
            productSize: v.productSize,
            unitPrice: v.unitPrice,
            stockQty: v.stockQty, // 複製時通常保留初始數量，亦可改為 0
            barcode: v.barcode || '',
            status: v.status || 'Released'
          }))
        }
      }
    } catch (error) {
      console.error('載入商品既存資料失敗:', error)
      toast.error('找不到該商品的既存資料，無法進行修改或複製。')
    }
  }
})

// Cloudinary 圖片上傳邏輯維持不變
const triggerFileInput = () => { fileInputRef.value.click() }
const handleFileToCloudinary = async (event) => {
  const files = Array.from(event.target.files)
  if (form.value.images.length + files.length > 6) {
    toast.error('商品相簿最多只能容納 6 張圖片！')
    return
  }
  isCloudUploading.value = true
  for (const file of files) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('upload_preset', UPLOAD_PRESET)
    try {
      const response = await freeAxios.post(CLOUDINARY_URL, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      const secureUrl = response.data.secure_url
      form.value.images.push(secureUrl)
    } catch (error) {
      console.error('上傳 Cloudinary 失敗:', error)
      toast.error(`圖片 [${file.name}] 上傳雲端失敗。`)
    }
  }
  isCloudUploading.value = false
  event.target.value = ''
}

const removeUploadedImage = (index) => { form.value.images.splice(index, 1) }

const addVariant = () => {
  form.value.variants.push({
    orgSkuNo: '', productColor: '', productSize: '', unitPrice: 0, stockQty: 0, barcode: '', status: 'Released'
  })
}
const removeVariant = (index) => { form.value.variants.splice(index, 1) }

// 💡 提交表單（自動分流：PUT 修改 或 POST 新增）
const submitForm = async (formEl) => {
  if (!formEl) return

  if (form.value.productSimDescription.length > 100) {
    toast.error('商品描述(簡)已超越 100 字上限！')
    return
  }
  if (form.value.productName.length > 50) {
    toast.error('商品名稱已超越 50 字上限！')
    return
  }

  await formEl.validate(async (valid) => {
    if (valid) {
      const hasEmpty = form.value.variants.some(v => !v.orgSkuNo || !v.productColor || !v.productSize)
      const isZero = form.value.variants.some(z => z.unitPrice === 0)
      if(isZero){
        toast.error("單價不可為0");
        return
      }
      
      if (hasEmpty) {
        const ok = await confirm({
          title: "請確認免填 廠商編號、顏色 或 尺寸！",
          message: "是否確定要提交？",
          confirmText: "確定",
          variant: "primary"
        });
        if (!ok) {
          toast.info("已取消儲存");
          return;
        }
        submitting.value = true
        try {
          if (isEditMode.value) {
            // ── 【修改模式】：發送 PUT 到 /products/{id} ──
            await axios.put(`/api/org/${organizerId.value}/admin/products/${productId.value}`, form.value)
            toast.success('商品資訊已成功修改並同步至資料庫！')
          } else {
            // ── 【新增/複製模式】：發送 POST 到 /products ──
            await axios.post(`/api/org/${organizerId.value}/admin/products`, form.value)
            toast.success('新商品（含 Cloudinary 圖片連結）已成功存入 SQL Server！')
          }
          // 成功後跳轉回商品列表頁
          router.push(
            {
              name: "ProductList",
              params: {
                // id: organizerId
                organizerId: organizerId.value
              }
            }
          );
        } catch (error) {
          console.error('儲存失敗:', error)
          toast.error(error.response?.data || '後端回傳格式錯誤，儲存失敗。')
        } finally {
          submitting.value = false
        }
        return;
      }

      submitting.value = true
      try {
        if (isEditMode.value) {
          // ── 【修改模式】：發送 PUT 到 /products/{id} ──
          await axios.put(`/api/org/${organizerId.value}/admin/products/${productId.value}`, form.value)
          toast.success('商品資訊已成功修改並同步至資料庫！')
        } else {
          // ── 【新增/複製模式】：發送 POST 到 /products ──
          await axios.post(`/api/org/${organizerId.value}/admin/products`, form.value)
          toast.success('新商品（含 Cloudinary 圖片連結）已成功存入 SQL Server！')
        }
        // 成功後跳轉回商品列表頁
        router.push(
          {
            name: "ProductList",
            params: {
              // id: organizerId
              organizerId: organizerId.value
            }
          }
        );
      } catch (error) {
        console.error('儲存失敗:', error)
        toast.error(error.response?.data || '後端回傳格式錯誤，儲存失敗。')
      } finally {
        submitting.value = false
      }
    } else {
      toast.error('請檢查上方紅字必填欄位！')
    }

  })
}

// 取消按鈕邏輯
const handleCancel = (formEl) => {
    if (formEl) {
        formEl.resetFields();
    }

    router.push({
        name: "ProductList",
        params: {
          // id: organizerId
                        organizerId: organizerId.value
                      }
    });
};
</script>

<style scoped>
/* 保持你原本非常漂亮的樣式不變 */
.product-form-container { max-width: 1200px; margin: 0 auto; }
.title { font-size: 18px; font-weight: bold; color: #303133; }
.form-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.required-star { color: #f56c6c; margin-right: 4px; font-weight: bold; }
:deep(.hide-official-star .el-form-item__label-wrap::before),
:deep(.hide-official-star .el-form-item__label::before) { display: none !important; }
.label-with-counter { display: flex; justify-content: space-between; width: 100%; }
.counter-badge { font-size: 12px; color: #909399; background-color: #f4f4f5; padding: 0px 8px; border-radius: 10px; }
.counter-badge.over-limit { color: #fff; background-color: #f56c6c; font-weight: bold; }
.image-upload-section { background-color: #fcfcfc; border: 1px solid #e4e7ed; border-radius: 6px; padding: 12px 20px; margin: 25px 0; }
.section-sub-title { display: block; font-size: 14px; font-weight: bold; color: #606266; margin-bottom: 5px; }
.photo-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; max-width: 650px; }
.photo-box { aspect-ratio: 1; border-radius: 4px; position: relative; box-sizing: border-box; }
.upload-btn-box { border: 2px dashed #c0c4cc; display: flex; justify-content: center; align-items: center; cursor: pointer; background-color: #fff; transition: all 0.3s; }
.upload-btn-box:hover { border-color: #409eff; background-color: #f5f7fa; }
.upload-content { display: flex; flex-direction: column; align-items: center; font-size: 11px; color: #909399; }
.upload-content .icon { font-size: 24px; font-weight: 300; margin-bottom: 0px; }
.hidden-input { display: none; }
.preview-box { border: 1px solid #dcdfe6; background-color: #f5f7fa; }
.preview-img { width: 100%; height: 100%; object-fit: cover; }
.remove-btn { position: absolute; top: -6px; right: -6px; width: 20px; height: 20px; border-radius: 50%; background-color: rgba(245, 108, 108, 0.9); color: white; border: none; font-size: 14px; cursor: pointer; display: flex; justify-content: center; align-items: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.cloud-tag { position: absolute; bottom: 0; left: 0; right: 0; background-color: rgba(103, 194, 58, 0.85); color: white; font-size: 9px; text-align: center; padding: 1px 0; }
.upload-loading-box { border: 1px solid #e4e7ed; display: flex; justify-content: center; align-items: center; background: #fafafa; }
.loading-text { font-size: 11px; color: #409eff; }
.variants-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.variant-table :deep(.el-table__cell) {padding: 4px 0 !important;}

.variant-table :deep(.el-input-number),
.variant-table :deep(.el-input) {margin: 0;vertical-align: middle;}
.form-actions { margin-top: 16px; display: flex; gap: 15px; }
.submit-btn { flex: 1; }
.table-form-item { margin-bottom: 0 !important;}
.table-form-item :deep(.el-form-item__error) {  position: absolute;  top: 100%;  z-index: 1;  font-size: 11px;  padding-top: 0px;}

/* 窄螢幕：兩欄表單收單欄、相片格線減欄、操作鈕堆疊，避免窄版變形 */
@media (max-width: 767.98px) {
  .form-grid-2 { grid-template-columns: 1fr; gap: 12px; }
  .photo-grid { grid-template-columns: repeat(3, 1fr); }
  .variants-header { flex-wrap: wrap; gap: 8px; }
  .form-actions { flex-direction: column; }
}
</style>