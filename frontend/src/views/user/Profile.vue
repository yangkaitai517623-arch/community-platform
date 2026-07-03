<template>
  <div class="u-page profile">
    <!-- Hero -->
    <section class="u-hero">
      <span class="u-hero-eyebrow">居民档案 · MEMBER</span>
      <h2 class="u-hero-title">{{ user?.realName || '欢迎回来' }}</h2>
      <p class="u-hero-sub">
        {{ user?.building || '—' }} · {{ user?.room || '房间未填写' }}
      </p>

      <div class="u-quick-actions">
        <button class="u-quick-tile" @click="$router.push('/errands')">
          <Bike class="icon" :size="22" />
          <span class="label">跑腿</span>
        </button>
        <button class="u-quick-tile" @click="$router.push('/repair')">
          <Wrench class="icon" :size="22" />
          <span class="label">检修</span>
        </button>
        <button class="u-quick-tile" @click="$router.push('/goods')">
          <Package class="icon" :size="22" />
          <span class="label">二手</span>
        </button>
        <button class="u-quick-tile" @click="$router.push('/forum')">
          <MessagesSquare class="icon" :size="22" />
          <span class="label">论坛</span>
        </button>
      </div>
    </section>

    <!-- 我的订单 -->
    <section class="orders-section">
      <div class="section-head">
        <span class="num">01</span>
        <h3>我的订单</h3>
        <span class="rule"></span>
      </div>

      <el-tabs v-model="activeTab" class="order-tabs">
        <el-tab-pane label="跑腿" name="errand">
          <div class="order-list" v-if="errandOrders.length > 0">
            <div class="order-row" v-for="item in errandOrders" :key="item.id">
              <div class="order-pin errand"></div>
              <div class="order-body">
                <h4>{{ item.title }}</h4>
                <p class="order-meta tabular">{{ item.createdAt }}</p>
                <div class="order-tags">
                  <el-tag v-if="item.userId === currentUserId" type="info" size="small">我发布的</el-tag>
                  <el-tag v-if="item.runnerId === currentUserId && [1, 2, 3].includes(item.status)" type="warning" size="small">我接的单</el-tag>
                </div>
              </div>
              <el-tag :type="getStatusType(item.status)" size="small">{{ getStatusText(item.status) }}</el-tag>
            </div>
          </div>
          <div v-else class="u-empty small">
            <div class="u-empty-icon"><Bike :size="22" /></div>
            <div class="u-empty-sub">暂无跑腿订单</div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="检修" name="repair">
          <div class="order-list" v-if="repairOrders.length > 0">
            <div class="order-row" v-for="item in repairOrders" :key="item.id">
              <div class="order-pin repair"></div>
              <div class="order-body">
                <h4>{{ item.title }}</h4>
                <p class="order-meta tabular">{{ item.createdAt }}</p>
                <div class="order-tags">
                  <el-tag v-if="item.userId === currentUserId" type="info" size="small">我发布的</el-tag>
                  <el-tag v-if="item.workerId === currentUserId && [1, 2, 3].includes(item.status)" type="warning" size="small">我接的单</el-tag>
                </div>
              </div>
              <el-tag :type="getStatusType(item.status)" size="small">{{ getStatusText(item.status) }}</el-tag>
            </div>
          </div>
          <div v-else class="u-empty small">
            <div class="u-empty-icon"><Wrench :size="22" /></div>
            <div class="u-empty-sub">暂无检修订单</div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="买入" name="buyer">
          <div class="order-list" v-if="buyerOrders.length > 0">
            <div class="order-row" v-for="item in buyerOrders" :key="item.id">
              <div class="order-pin goods"></div>
              <div class="order-body">
                <h4>订单 #{{ item.orderNo }}</h4>
                <p class="order-meta tabular">¥ {{ item.amount }} · {{ item.createdAt }}</p>
              </div>
              <div class="order-actions">
                <el-tag :type="getOrderStatusType(item.status)" size="small">{{ getOrderStatusText(item.status) }}</el-tag>
                <el-button
                  v-if="item.status === 1 && item.buyerId === currentUserId"
                  type="success"
                  size="small"
                  @click="handleBuyerConfirm(item)"
                >确认收货</el-button>
              </div>
            </div>
          </div>
          <div v-else class="u-empty small">
            <div class="u-empty-icon"><Package :size="22" /></div>
            <div class="u-empty-sub">暂无购买订单</div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="出售" name="seller">
          <div class="order-list" v-if="sellerOrders.length > 0">
            <div class="order-row" v-for="item in sellerOrders" :key="item.id">
              <div class="order-pin goods"></div>
              <div class="order-body">
                <h4>订单 #{{ item.orderNo }}</h4>
                <p class="order-meta tabular">¥ {{ item.amount }} · {{ item.createdAt }}</p>
              </div>
              <div class="order-actions">
                <el-tag :type="getOrderStatusType(item.status)" size="small">{{ getOrderStatusText(item.status) }}</el-tag>
                <el-button
                  v-if="item.status === 0 && item.sellerId === currentUserId"
                  type="primary"
                  size="small"
                  @click="handleSellerConfirm(item)"
                >确认订单</el-button>
              </div>
            </div>
          </div>
          <div v-else class="u-empty small">
            <div class="u-empty-icon"><Package :size="22" /></div>
            <div class="u-empty-sub">暂无出售订单</div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <!-- 个人资料 -->
    <section class="profile-section">
      <div class="section-head">
        <span class="num">02</span>
        <h3>编辑档案</h3>
        <span class="rule"></span>
      </div>

      <div class="profile-form-wrap">
        <el-form :model="form" label-width="84px" class="profile-form">
          <el-form-item label="用户名"><el-input v-model="form.username" disabled /></el-form-item>
          <el-form-item label="真实姓名"><el-input v-model="form.realName" placeholder="请输入真实姓名" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="form.phone" placeholder="请输入手机号" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="form.email" placeholder="请输入邮箱" /></el-form-item>
          <el-form-item label="楼栋"><el-input v-model="form.building" placeholder="如 3 栋" /></el-form-item>
          <el-form-item label="房间号"><el-input v-model="form.room" placeholder="如 502" /></el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSave" :loading="saving">保存修改</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bike, Wrench, Package, MessagesSquare } from 'lucide-vue-next'
import api from '@/api'

const userStore = useUserStore()
const user = ref(userStore.user)
const saving = ref(false)
const activeTab = ref('errand')
const errandOrders = ref([])
const repairOrders = ref([])
const buyerOrders = ref([])
const sellerOrders = ref([])
const currentUserId = ref(null)

const form = reactive({
  username: user.value?.username || '',
  realName: user.value?.realName || '',
  phone: user.value?.phone || '',
  email: user.value?.email || '',
  building: user.value?.building || '',
  room: user.value?.room || ''
})

const fetchCurrentUser = async () => {
  try {
    const res = await api.get('/user/profile')
    if (res.data.code === 200) currentUserId.value = res.data.data?.id
  } catch (e) {}
}

const fetchOrders = async () => {
  try {
    const [errandRes, repairRes, buyRes] = await Promise.all([
      api.get('/errand-requests/my'),
      api.get('/repair-requests/my'),
      api.get('/goods-orders/my')
    ])
    if (errandRes.data.code === 200) errandOrders.value = errandRes.data.data?.records || []
    if (repairRes.data.code === 200) repairOrders.value = repairRes.data.data?.records || []
    if (buyRes.data.code === 200) {
      buyerOrders.value = buyRes.data.data?.buyerOrders || []
      sellerOrders.value = buyRes.data.data?.sellerOrders || []
    }
  } catch (e) {}
}

const handleSave = async () => {
  saving.value = true
  try {
    const res = await api.put('/user/profile', form)
    if (res.data.code === 200) {
      ElMessage.success('保存成功')
      userStore.user = { ...userStore.user, ...form }
      localStorage.setItem('user', JSON.stringify(userStore.user))
    }
  } catch (e) { ElMessage.error('保存失败') }
  finally { saving.value = false }
}

const handleSellerConfirm = async (item) => {
  try {
    await ElMessageBox.confirm('确认该订单吗？确认后商品将标记为已售出', '确认订单', {
      confirmButtonText: '确认', cancelButtonText: '取消', type: 'info'
    })
    const res = await api.put(`/goods-orders/${item.id}/confirm`)
    if (res.data.code === 200) { ElMessage.success('订单已确认'); fetchOrders() }
    else ElMessage.error(res.data.message || '确认失败')
  } catch (e) { if (e !== 'cancel') ElMessage.error('确认失败') }
}

const handleBuyerConfirm = async (item) => {
  try {
    await ElMessageBox.confirm('确认已收到商品？', '确认收货', {
      confirmButtonText: '确认收货', cancelButtonText: '取消', type: 'success'
    })
    const res = await api.put(`/goods-orders/${item.id}/complete`)
    if (res.data.code === 200) { ElMessage.success('已确认收货'); fetchOrders() }
    else ElMessage.error(res.data.message || '确认失败')
  } catch (e) { if (e !== 'cancel') ElMessage.error('确认失败') }
}

const getStatusType = (s) => ({ 0: 'info', 1: 'warning', 2: 'primary', 3: 'success', 4: 'danger' }[s] || 'info')
const getStatusText = (s) => ({ 0: '待接单', 1: '已接单', 2: '进行中', 3: '已完成', 4: '已取消' }[s] || '未知')
const getOrderStatusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'success', 4: 'danger' }[s] || 'info')
const getOrderStatusText = (s) => ({ 0: '待确认', 1: '已确认', 2: '已完成', 4: '已取消' }[s] || '未知')

onMounted(() => { fetchCurrentUser(); fetchOrders() })
</script>

<style scoped>
.profile { display: flex; flex-direction: column; gap: 24px; }

.section-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.section-head .num {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  color: var(--user-brand);
  letter-spacing: 1px;
  padding: 2px 6px;
  border: 1px solid var(--user-brand-soft);
  border-radius: var(--r-xs);
}
.section-head h3 {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  color: var(--user-ink);
  letter-spacing: 0.3px;
}
.section-head .rule {
  flex: 1;
  height: 1px;
  background: var(--user-line);
}

.orders-section, .profile-section {
  background: var(--user-surface);
  border: 1px solid var(--user-line);
  border-radius: var(--r-lg);
  padding: 24px;
}

.order-list { display: flex; flex-direction: column; gap: 8px; }
.order-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  padding: 12px;
  background: var(--user-bg-soft);
  border-radius: var(--r-md);
  align-items: center;
}
.order-pin {
  width: 4px;
  align-self: stretch;
  border-radius: 2px;
}
.order-pin.errand { background: var(--pin-errand); }
.order-pin.repair { background: var(--pin-repair); }
.order-pin.goods  { background: var(--pin-goods); }
.order-body { min-width: 0; }
.order-body h4 {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 14px;
  color: var(--user-ink);
  margin-bottom: 2px;
}
.order-meta {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--user-faint);
}
.order-tags { display: flex; gap: 4px; margin-top: 6px; }
.order-actions { display: flex; flex-direction: column; gap: 6px; align-items: flex-end; }

.u-empty.small { padding: 28px 16px; }
.u-empty.small .u-empty-icon { width: 48px; height: 48px; margin-bottom: 12px; }

.profile-form { max-width: 480px; }

@media (min-width: 1024px) {
  .profile {
    display: grid;
    grid-template-columns: 1.4fr 1fr;
    gap: 24px;
  }
  .u-hero { grid-column: 1 / -1; }
}
</style>
