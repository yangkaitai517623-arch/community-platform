<template>
  <div class="u-page">
    <header class="u-header">
      <div>
        <h2 class="u-title">二手市集</h2>
        <p class="u-subtitle">让闲置在邻里间继续被需要</p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.push('/profile')">
          <List :size="14" /> &nbsp;我的订单
        </el-button>
        <el-button type="primary" @click="showPublish = true">
          <Plus :size="14" /> &nbsp;发布商品
        </el-button>
      </div>
    </header>

    <div v-if="goods.length > 0" class="u-grid">
      <article class="u-goods-card" v-for="item in goods" :key="item.id">
        <div class="u-goods-img" @click="previewImage(item.images || item.image)">
          <img v-if="item.images || item.image" :src="item.images || item.image" :alt="item.title" loading="lazy" @error="onImgError" />
          <div v-else class="u-goods-img-fallback">
            <ImageOff :size="20" />
            <span>暂无图片</span>
          </div>
          <span v-if="item.conditionLevel" class="u-goods-condition">{{ item.conditionLevel }}</span>
        </div>
        <div class="u-goods-info">
          <h3 class="u-goods-title">{{ item.title }}</h3>
          <div class="u-goods-price-row">
            <span class="u-price">¥ {{ item.sellingPrice }}</span>
            <span v-if="item.originalPrice" class="u-price-orig">¥ {{ item.originalPrice }}</span>
          </div>
          <div class="u-goods-foot">
            <span>{{ item.sellerName || '邻居' }}</span>
            <div class="card-actions">
              <el-button
                v-if="item.status === 1 && item.sellerId !== currentUserId"
                type="primary"
                size="small"
                @click="handleBuy(item)"
              >买下</el-button>
              <el-tag v-if="item.sellerId === currentUserId" type="info" size="small">我的</el-tag>
              <el-tag v-if="item.status === 2" type="success" size="small">已售</el-tag>
            </div>
          </div>
        </div>
      </article>
    </div>

    <div v-else class="u-empty">
      <div class="u-empty-icon"><Package :size="28" /></div>
      <div class="u-empty-text">市集还很安静</div>
      <div class="u-empty-sub">第一件二手会出现在这里</div>
      <el-button type="primary" @click="showPublish = true">发布我的闲置</el-button>
    </div>

    <!-- 图片预览 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="80%" top="5vh">
      <div class="img-preview-wrap">
        <img :src="previewImageUrl" />
      </div>
    </el-dialog>

    <!-- 发布商品 -->
    <el-dialog v-model="showPublish" title="发布二手商品" width="540px">
      <el-form :model="publishForm" label-width="84px">
        <el-form-item label="商品名称">
          <el-input v-model="publishForm.title" placeholder="如：九成新自行车" />
        </el-form-item>
        <el-form-item label="商品图片">
          <input
            type="file"
            ref="fileInput"
            accept="image/*"
            style="display: none"
            @change="handleFileSelect"
          />
          <div class="upload-frame" @click="$refs.fileInput.click()">
            <img v-if="publishForm.images" :src="publishForm.images" class="uploaded-image" />
            <div v-else class="upload-placeholder">
              <ImageUp :size="22" />
              <span>点击上传商品图</span>
            </div>
          </div>
          <div class="upload-tip">支持 jpg、png 格式，5MB 以内</div>
        </el-form-item>
        <el-form-item label="商品分类">
          <el-select v-model="publishForm.categoryId" placeholder="请选择">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="publishForm.description" type="textarea" :rows="3" placeholder="新旧程度、有无瑕疵、为什么转让" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="publishForm.originalPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="售价">
          <el-input-number v-model="publishForm.sellingPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="成色">
          <el-select v-model="publishForm.conditionLevel" placeholder="请选择">
            <el-option label="全新" value="全新" />
            <el-option label="九成新" value="九成新" />
            <el-option label="八成新" value="八成新" />
            <el-option label="七成新" value="七成新" />
            <el-option label="六成新以下" value="六成新以下" />
          </el-select>
        </el-form-item>

        <div class="ai-row">
          <el-button @click="handleAiEstimate" :loading="aiEstimating">
            <Sparkles :size="14" /> &nbsp;AI 智能估价
          </el-button>
        </div>

        <div v-if="aiEstimateResult" class="u-ai-card">
          <div class="u-ai-card-icon"><Sparkles :size="16" /></div>
          <div class="u-ai-card-body">
            <div class="u-ai-card-title">AI 估价：¥ {{ aiEstimateResult.estimatedPrice }}</div>
            <div class="u-ai-card-text">
              建议区间 ¥ {{ aiEstimateResult.priceRangeMin }} - ¥ {{ aiEstimateResult.priceRangeMax }} ·
              已为你应用估价
            </div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showPublish = false">取消</el-button>
        <el-button type="primary" @click="handlePublish" :loading="publishing">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Sparkles, ImageOff, ImageUp, Package, List } from 'lucide-vue-next'
import api from '@/api'

const goods = ref([])
const categories = ref([])
const showPublish = ref(false)
const publishing = ref(false)
const currentUserId = ref(null)
const fileInput = ref(null)
const previewVisible = ref(false)
const previewImageUrl = ref('')

const publishForm = reactive({
  title: '', description: '', categoryId: null,
  originalPrice: 0, sellingPrice: 0, conditionLevel: '九成新', images: ''
})

const handleFileSelect = (event) => {
  const file = event.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { ElMessage.error('只能上传图片文件'); return }
  if (file.size > 5 * 1024 * 1024) { ElMessage.error('图片大小不能超过 5MB'); return }
  const reader = new FileReader()
  reader.onload = (e) => {
    publishForm.images = e.target.result
    ElMessage.success('图片已选择')
  }
  reader.readAsDataURL(file)
  event.target.value = ''
}

const aiEstimating = ref(false)
const aiEstimateResult = ref(null)

const handleAiEstimate = async () => {
  if (!publishForm.title || !publishForm.originalPrice) {
    ElMessage.warning('请先填写商品名称和原价')
    return
  }
  aiEstimating.value = true
  try {
    const res = await api.post('/ai/estimate-price', {
      goodsName: publishForm.title,
      condition: publishForm.conditionLevel,
      originalPrice: publishForm.originalPrice
    })
    if (res.data.code === 200) {
      aiEstimateResult.value = res.data.data
      if (aiEstimateResult.value.estimatedPrice) {
        publishForm.sellingPrice = aiEstimateResult.value.estimatedPrice
      }
      if (aiEstimateResult.value.optimizedDescription) {
        publishForm.description = aiEstimateResult.value.optimizedDescription
      }
      ElMessage.success('AI 估价完成')
    }
  } catch (e) { ElMessage.error('AI 估价失败') }
  finally { aiEstimating.value = false }
}

const fetchCurrentUser = async () => {
  try {
    const res = await api.get('/user/profile')
    if (res.data.code === 200) currentUserId.value = res.data.data?.id
  } catch (e) {}
}

const onImgError = (event) => {
  const wrap = event.target.parentElement
  event.target.remove()
  wrap.querySelector('.u-goods-img-fallback')?.remove()
  const fallback = document.createElement('div')
  fallback.className = 'u-goods-img-fallback'
  fallback.innerHTML = '<span>图片加载失败</span>'
  wrap.appendChild(fallback)
}

const previewImage = (url) => {
  if (url) { previewImageUrl.value = url; previewVisible.value = true }
}

const handleBuy = async (item) => {
  try {
    await ElMessageBox.confirm(`确定买下「${item.title}」吗？\n¥${item.sellingPrice}`, '确认购买', {
      confirmButtonText: '确认购买', cancelButtonText: '再想想', type: 'info'
    })
    const res = await api.post('/goods-orders', { goodsId: item.id, amount: item.sellingPrice })
    if (res.data.code === 200) { ElMessage.success('已下单，等待卖家确认'); fetchGoods() }
    else ElMessage.error(res.data.message || '购买失败')
  } catch (e) { if (e !== 'cancel') ElMessage.error('购买失败') }
}

const fetchGoods = async () => {
  try {
    const res = await api.get('/goods', { params: { page: 1, size: 20, status: 1 } })
    if (res.data.code === 200) goods.value = res.data.data?.records || []
  } catch (e) {}
}

const fetchCategories = async () => {
  try {
    const res = await api.get('/categories')
    if (res.data.code === 200) categories.value = res.data.data || []
  } catch (e) {}
}

const handlePublish = async () => {
  publishing.value = true
  try {
    const res = await api.post('/goods', publishForm)
    if (res.data.code === 200) {
      ElMessage.success('发布成功，等待审核')
      showPublish.value = false
      Object.assign(publishForm, {
        title: '', description: '', categoryId: null,
        originalPrice: 0, sellingPrice: 0, conditionLevel: '九成新', images: ''
      })
      aiEstimateResult.value = null
      fetchGoods()
    } else {
      ElMessage.error(res.data.message || '发布失败')
    }
  } catch (e) {
    ElMessage.error('发布失败：' + (e.message || '未知错误'))
  } finally {
    publishing.value = false
  }
}

onMounted(() => {
  fetchCurrentUser()
  fetchGoods()
  fetchCategories()
})
</script>

<style scoped>
.u-header { flex-wrap: wrap; gap: 12px; }
.header-actions { display: flex; gap: 8px; }
.card-actions { display: flex; gap: 6px; align-items: center; }

.img-preview-wrap { text-align: center; }
.img-preview-wrap img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: var(--r-md);
  border: 1px solid var(--user-line);
}

.upload-frame {
  width: 140px;
  height: 140px;
  border: 1.5px dashed var(--user-line);
  border-radius: var(--r-md);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--user-bg-soft);
  overflow: hidden;
  transition: all var(--d-fast) var(--ease-standard);
}
.upload-frame:hover { border-color: var(--user-brand); background: var(--user-brand-soft); }
.uploaded-image { width: 100%; height: 100%; object-fit: cover; }
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--user-muted);
  font-size: 12px;
}
.upload-tip {
  font-size: 11px;
  color: var(--user-faint);
  margin-top: 6px;
}

.ai-row { padding-left: 84px; margin-top: 8px; margin-bottom: 8px; }
</style>
