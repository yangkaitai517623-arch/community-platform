<template>
  <div class="u-page">
    <header class="u-header">
      <div>
        <h2 class="u-title">跑腿需求</h2>
        <p class="u-subtitle">让邻居顺路捎一手</p>
      </div>
      <el-button type="primary" @click="showPublish = true">
        <Plus :size="14" /> &nbsp;发布需求
      </el-button>
    </header>

    <div class="filter-bar">
      <el-radio-group v-model="filterStatus" size="small">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="pending">待接单</el-radio-button>
        <el-radio-button value="active">进行中</el-radio-button>
        <el-radio-button value="completed">已完成</el-radio-button>
      </el-radio-group>
    </div>

    <div class="u-card-list" v-if="requests.length > 0">
      <article class="u-card" v-for="item in requests" :key="item.id">
        <span class="u-pin errand"></span>
        <div class="card-tags">
          <el-tag :type="getUrgencyType(item.urgency)" size="small">{{ getUrgencyText(item.urgency) }}</el-tag>
          <el-tag :type="getStatusType(item.status)" size="small">{{ getStatusText(item.status) }}</el-tag>
          <span class="type-chip">{{ item.errandType }}</span>
        </div>
        <h3 class="u-card-title">{{ item.title }}</h3>
        <p class="u-card-desc">{{ item.description }}</p>
        <div class="addr-block" v-if="item.pickupAddress || item.deliveryAddress">
          <div v-if="item.pickupAddress" class="addr-row">
            <span class="addr-label">取</span>
            <span>{{ item.pickupAddress }}</span>
          </div>
          <div v-if="item.deliveryAddress" class="addr-row">
            <span class="addr-label send">送</span>
            <span>{{ item.deliveryAddress }}</span>
          </div>
        </div>
        <div class="u-card-meta">
          <span class="item"><UserRound :size="12" /> {{ item.publisherName || '邻居' }}</span>
          <span class="item"><Clock :size="12" /> {{ item.createdAt }}</span>
        </div>
        <div class="u-card-foot">
          <span class="u-price">¥ {{ item.reward }}</span>
          <div class="action-buttons">
            <el-button v-if="item.status === 0 && item.userId !== currentUserId" type="primary" size="small" @click="handleAccept(item)">
              我接单
            </el-button>
            <el-button v-if="(item.status === 1 || item.status === 2) && item.userId === currentUserId" type="success" size="small" @click="handleComplete(item)">
              确认完成
            </el-button>
            <el-button v-if="item.runnerId === currentUserId && (item.status === 1 || item.status === 2)" type="warning" size="small" @click="handleCancelAccept(item)">
              取消接单
            </el-button>
            <el-button v-if="item.status === 3 && item.userId === currentUserId && item.orderStatus !== 2" type="primary" size="small" @click="openReview(item)">
              评价服务
            </el-button>
            <el-tag v-if="item.status === 3 && item.orderStatus === 2" type="success" size="small">
              已评价 {{ item.orderRating }} 星
            </el-tag>
            <template v-if="item.userId === currentUserId && item.status === 0">
              <el-button size="small" @click="handleEdit(item)">编辑</el-button>
              <el-button type="danger" size="small" @click="handleDelete(item)">删除</el-button>
            </template>
            <el-tag v-if="item.userId === currentUserId" type="info" size="small">我发布的</el-tag>
            <el-tag v-if="item.runnerId === currentUserId && [1, 2, 3].includes(item.status)" type="warning" size="small">我接的单</el-tag>
          </div>
        </div>
      </article>
    </div>

    <div v-else class="u-empty">
      <div class="u-empty-icon"><Bike :size="28" /></div>
      <div class="u-empty-text">还没有跑腿需求</div>
      <div class="u-empty-sub">第一个发布的人最酷</div>
      <el-button type="primary" @click="showPublish = true">发布我的需求</el-button>
    </div>

    <!-- 发布需求 -->
    <el-dialog v-model="showPublish" title="发布跑腿需求" width="520px">
      <el-form :model="publishForm" label-width="84px">
        <el-form-item label="需求标题">
          <el-input v-model="publishForm.title" placeholder="如：帮取楼下快递" />
        </el-form-item>
        <el-form-item label="详细描述">
          <el-input
            v-model="publishForm.description"
            type="textarea"
            :rows="3"
            placeholder="描述你的需求，AI 将自动判断类型与紧急度"
          />
        </el-form-item>

        <div class="ai-row">
          <el-button @click="handleAiAnalyze" :loading="aiAnalyzing">
            <Sparkles :size="14" /> &nbsp;AI 智能分析
          </el-button>
        </div>

        <div v-if="aiResult" class="u-ai-card">
          <div class="u-ai-card-icon"><Sparkles :size="16" /></div>
          <div class="u-ai-card-body">
            <div class="u-ai-card-title">AI 已为你建议</div>
            <div class="u-ai-card-text">
              类型：{{ aiResult.serviceType || '—' }} ·
              紧急度：{{ aiResult.urgency === 'high' ? '紧急' : aiResult.urgency === 'medium' ? '一般' : '不急' }}
            </div>
          </div>
        </div>

        <el-form-item label="跑腿类型">
          <el-select v-model="publishForm.errandType" placeholder="请选择">
            <el-option label="代买" value="代买" />
            <el-option label="代取" value="代取" />
            <el-option label="代送" value="代送" />
            <el-option label="排队" value="排队" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="取货地址">
          <el-input v-model="publishForm.pickupAddress" placeholder="请输入取货地址" />
        </el-form-item>
        <el-form-item label="送达地址">
          <el-input v-model="publishForm.deliveryAddress" placeholder="请输入送达地址" />
        </el-form-item>
        <el-form-item label="紧急度">
          <el-radio-group v-model="publishForm.urgency">
            <el-radio :value="1">紧急</el-radio>
            <el-radio :value="2">一般</el-radio>
            <el-radio :value="3">不急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="报酬">
          <el-input-number v-model="publishForm.reward" :min="0" :step="1" />
          <span class="hint">作为对邻居帮忙的小小心意</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublish = false">取消</el-button>
        <el-button type="primary" @click="handlePublish" :loading="publishing">发布</el-button>
      </template>
    </el-dialog>

    <!-- 编辑需求 -->
    <el-dialog v-model="showEdit" title="编辑跑腿需求" width="520px">
      <el-form :model="editForm" label-width="84px">
        <el-form-item label="需求标题"><el-input v-model="editForm.title" /></el-form-item>
        <el-form-item label="跑腿类型">
          <el-select v-model="editForm.errandType">
            <el-option label="代买" value="代买" />
            <el-option label="代取" value="代取" />
            <el-option label="代送" value="代送" />
            <el-option label="排队" value="排队" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细描述"><el-input v-model="editForm.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="取货地址"><el-input v-model="editForm.pickupAddress" /></el-form-item>
        <el-form-item label="送达地址"><el-input v-model="editForm.deliveryAddress" /></el-form-item>
        <el-form-item label="紧急度">
          <el-radio-group v-model="editForm.urgency">
            <el-radio :value="1">紧急</el-radio>
            <el-radio :value="2">一般</el-radio>
            <el-radio :value="3">不急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="报酬"><el-input-number v-model="editForm.reward" :min="0" :step="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit" :loading="editing">保存</el-button>
      </template>
    </el-dialog>

    <!-- 评价订单 -->
    <el-dialog v-model="showReview" title="评价跑腿服务" width="420px">
      <el-form :model="reviewForm" label-width="72px">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" />
        </el-form-item>
        <el-form-item label="评价">
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="说说本次跑腿服务体验"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReview = false">取消</el-button>
        <el-button type="primary" @click="submitReview" :loading="reviewing">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Bike, UserRound, Clock, Sparkles } from 'lucide-vue-next'
import api from '@/api'

const filterStatus = ref('all')
const requests = ref([])
const showPublish = ref(false)
const showEdit = ref(false)
const showReview = ref(false)
const publishing = ref(false)
const editing = ref(false)
const reviewing = ref(false)
const currentUserId = ref(null)
const editingId = ref(null)
const reviewTargetId = ref(null)

const publishForm = reactive({
  title: '', description: '', errandType: '',
  pickupAddress: '', deliveryAddress: '', urgency: 2, reward: 0
})

const editForm = reactive({
  title: '', description: '', errandType: '',
  pickupAddress: '', deliveryAddress: '', urgency: 2, reward: 0
})

const reviewForm = reactive({ rating: 5, comment: '' })

const fetchCurrentUser = async () => {
  try {
    const res = await api.get('/user/profile')
    if (res.data.code === 200) currentUserId.value = res.data.data?.id
  } catch (e) {}
}

const handleAccept = async (item) => {
  try {
    await ElMessageBox.confirm('确认接下这一单吗？', '提示', {
      confirmButtonText: '确定接单', cancelButtonText: '再想想', type: 'info'
    })
    const res = await api.put(`/errand-requests/${item.id}/accept`)
    if (res.data.code === 200) { ElMessage.success('接单成功'); fetchRequests() }
    else ElMessage.error(res.data.message || '接单失败')
  } catch (e) { if (e !== 'cancel') ElMessage.error('接单失败') }
}

const handleComplete = async (item) => {
  try {
    await ElMessageBox.confirm('标记为已完成？', '提示', {
      confirmButtonText: '确认完成', cancelButtonText: '取消', type: 'success'
    })
    const res = await api.put(`/errand-requests/${item.id}/complete`)
    if (res.data.code === 200) { ElMessage.success('订单已完成'); fetchRequests() }
    else ElMessage.error(res.data.message || '操作失败')
  } catch (e) { if (e !== 'cancel') ElMessage.error('操作失败') }
}

const openReview = (item) => {
  reviewTargetId.value = item.id
  reviewForm.rating = item.orderRating || 5
  reviewForm.comment = item.orderComment || ''
  showReview.value = true
}

const submitReview = async () => {
  if (!reviewForm.rating) {
    ElMessage.warning('请先选择评分')
    return
  }
  reviewing.value = true
  try {
    const res = await api.put(`/errand-requests/${reviewTargetId.value}/review`, reviewForm)
    if (res.data.code === 200) {
      ElMessage.success('评价成功')
      showReview.value = false
      fetchRequests()
    } else {
      ElMessage.error(res.data.message || '评价失败')
    }
  } catch (e) {
    ElMessage.error('评价失败')
  } finally {
    reviewing.value = false
  }
}

const handleCancelAccept = async (item) => {
  try {
    await ElMessageBox.confirm('临时有事无法处理？取消后该需求会回退为待接单。', '取消接单', {
      confirmButtonText: '确认取消', cancelButtonText: '再想想', type: 'warning'
    })
    const res = await api.put(`/errand-requests/${item.id}/cancel-accept`)
    if (res.data.code === 200) { ElMessage.success('已取消接单'); fetchRequests() }
    else ElMessage.error(res.data.message || '取消失败')
  } catch (e) { if (e !== 'cancel') ElMessage.error('取消失败') }
}

const handleEdit = (item) => {
  editingId.value = item.id
  Object.assign(editForm, {
    title: item.title, description: item.description, errandType: item.errandType,
    pickupAddress: item.pickupAddress, deliveryAddress: item.deliveryAddress,
    urgency: item.urgency, reward: item.reward
  })
  showEdit.value = true
}

const handleEditSubmit = async () => {
  editing.value = true
  try {
    const res = await api.put(`/errand-requests/${editingId.value}`, editForm)
    if (res.data.code === 200) {
      ElMessage.success('修改成功')
      showEdit.value = false
      fetchRequests()
    } else ElMessage.error(res.data.message || '修改失败')
  } catch (e) { ElMessage.error('修改失败') }
  finally { editing.value = false }
}

const handleDelete = async (item) => {
  try {
    await ElMessageBox.confirm('确定要删除该需求吗？删除后无法恢复', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    const res = await api.delete(`/errand-requests/${item.id}`)
    if (res.data.code === 200) { ElMessage.success('删除成功'); fetchRequests() }
    else ElMessage.error(res.data.message || '删除失败')
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

const fetchRequests = async () => {
  try {
    const params = { page: 1, size: 20 }
    if (filterStatus.value !== 'all') params.scope = filterStatus.value
    const res = await api.get('/errand-requests', { params })
    if (res.data.code === 200) requests.value = res.data.data?.records || []
  } catch (e) {}
}

const aiAnalyzing = ref(false)
const aiResult = ref(null)

const handleAiAnalyze = async () => {
  if (!publishForm.description) {
    ElMessage.warning('请先输入需求描述')
    return
  }
  aiAnalyzing.value = true
  try {
    const res = await api.post('/ai/classify', { description: publishForm.description, type: 'errand' })
    if (res.data.code === 200) {
      aiResult.value = res.data.data
      if (aiResult.value.serviceType) {
        const typeMap = { '代买': '代买', '代取': '代取', '代送': '代送', '跑腿': '代买' }
        for (const [key, value] of Object.entries(typeMap)) {
          if (aiResult.value.serviceType.includes(key)) { publishForm.errandType = value; break }
        }
      }
      if (aiResult.value.urgency) {
        const urgencyMap = { 'high': 1, 'medium': 2, 'low': 3 }
        publishForm.urgency = urgencyMap[aiResult.value.urgency] || 2
      }
      ElMessage.success('AI 分析完成')
    }
  } catch (e) { ElMessage.error('AI 分析失败') }
  finally { aiAnalyzing.value = false }
}

const handlePublish = async () => {
  publishing.value = true
  try {
    const res = await api.post('/errand-requests', publishForm)
    if (res.data.code === 200) {
      ElMessage.success('发布成功')
      showPublish.value = false
      aiResult.value = null
      Object.assign(publishForm, { title: '', description: '', errandType: '', pickupAddress: '', deliveryAddress: '', urgency: 2, reward: 0 })
      fetchRequests()
    }
  } catch (e) { ElMessage.error('发布失败') }
  finally { publishing.value = false }
}

const getUrgencyType = (u) => ({ 1: 'danger', 2: 'warning', 3: 'info' }[u] || 'info')
const getUrgencyText = (u) => ({ 1: '紧急', 2: '一般', 3: '不急' }[u] || '一般')
const getStatusType = (s) => ({ 0: 'info', 1: 'warning', 2: 'primary', 3: 'success', 4: 'danger' }[s] || 'info')
const getStatusText = (s) => ({ 0: '待接单', 1: '已接单', 2: '进行中', 3: '已完成', 4: '已取消' }[s] || '未知')

watch(filterStatus, () => fetchRequests())
onMounted(() => { fetchCurrentUser(); fetchRequests() })
</script>

<style scoped>
.filter-bar { margin-bottom: 16px; }

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}
.type-chip {
  display: inline-block;
  padding: 2px 10px;
  font-size: 12px;
  border: 1px solid var(--user-line);
  background: var(--user-bg-soft);
  color: var(--user-ink-soft);
  border-radius: var(--r-pill);
  font-weight: 500;
}

.addr-block {
  margin: 12px 0;
  padding: 10px 12px;
  background: var(--user-bg-soft);
  border-radius: var(--r-md);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.addr-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--user-ink-soft);
}
.addr-label {
  width: 22px;
  height: 22px;
  border-radius: var(--r-xs);
  background: var(--pin-errand);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-family: var(--font-display);
}
.addr-label.send { background: var(--user-brand); }

.action-buttons { display: flex; gap: 6px; align-items: center; flex-wrap: wrap; }

.ai-row { margin-bottom: 16px; padding-left: 84px; }

.hint { margin-left: 12px; font-size: 12px; color: var(--user-faint); }
</style>
