<template>
  <div class="admin-page">
    <header class="admin-page-header">
      <div class="admin-page-title-block">
        <span class="admin-page-eyebrow">检修订单 · REPAIR ORDERS</span>
        <h1 class="admin-page-title">检修订单管理</h1>
        <p class="admin-page-subtitle">查看已接单、维修中、已完成和已评价的检修服务订单</p>
      </div>
      <span class="stamp-mark">{{ total }} 笔订单</span>
    </header>

    <div class="admin-toolbar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索需求标题"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      >
        <template #prefix><Search :size="16" class="ti" /></template>
      </el-input>
      <el-select v-model="filterStatus" placeholder="需求状态" clearable @change="handleSearch">
        <el-option label="全部" value="" />
        <el-option label="已接单" :value="1" />
        <el-option label="维修中" :value="2" />
        <el-option label="已完成" :value="3" />
      </el-select>
      <span class="admin-toolbar-spacer"></span>
      <el-button @click="resetSearch">重置</el-button>
      <el-button type="primary" @click="handleSearch"><Search :size="14" /> &nbsp;搜索</el-button>
    </div>

    <div class="admin-surface">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="title" label="需求" min-width="220">
          <template #default="{ row }">
            <span class="row-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="repairType" label="类型" width="120">
          <template #default="{ row }"><span class="muted">{{ row.repairType || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="location" label="维修地址" min-width="170">
          <template #default="{ row }"><span class="muted">{{ row.location || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="需求状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusType(row.orderStatus)">{{ getOrderStatusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="服务评价" width="170">
          <template #default="{ row }">
            <el-rate
              v-if="row.orderRating"
              :model-value="row.orderRating"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value}"
            />
            <span v-else class="muted">未评价</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">
            <span class="font-mono tabular muted">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <button class="row-btn" @click="handleView(row)">查看</button>
              <button class="row-btn warn" @click="handleDelete(row)">删除</button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog v-model="viewDialogVisible" title="检修订单详情" width="620px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="需求标题">{{ currentRow.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ currentRow.repairType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ currentRow.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ currentRow.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="需求状态">{{ getStatusText(currentRow.status) }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">{{ getOrderStatusText(currentRow.orderStatus) }}</el-descriptions-item>
        <el-descriptions-item label="紧急度">{{ getUrgencyText(currentRow.urgency) }}</el-descriptions-item>
        <el-descriptions-item label="服务评分">
          <el-rate
            v-if="currentRow.orderRating"
            :model-value="currentRow.orderRating"
            disabled
            show-score
            text-color="#ff9900"
            score-template="{value}"
          />
          <span v-else>未评价</span>
        </el-descriptions-item>
        <el-descriptions-item label="评价内容">{{ currentRow.orderComment || '暂无评价' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentRow.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from 'lucide-vue-next'
import api from '@/api'

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const filterStatus = ref('')
const viewDialogVisible = ref(false)
const currentRow = ref({})

const getStatusType = (s) => ({ 1: 'warning', 2: 'primary', 3: 'success' }[s] || 'info')
const getStatusText = (s) => ({ 1: '已接单', 2: '维修中', 3: '已完成' }[s] || '-')
const getOrderStatusType = (s) => ({ 0: 'primary', 1: 'success', 2: 'success' }[s] || 'info')
const getOrderStatusText = (s) => ({ 0: '进行中', 1: '已完成待评价', 2: '已评价' }[s] || '未生成')
const getUrgencyText = (u) => ({ 1: '紧急', 2: '一般', 3: '不急' }[u] || '-')

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterStatus.value !== '' && filterStatus.value !== null) params.status = filterStatus.value
    const res = await api.get('/admin/repair-orders', { params })
    tableData.value = res.data?.data?.records || []
    total.value = res.data?.data?.total || 0
  } catch (e) {
    console.error(e)
    ElMessage.error('加载检修订单失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { currentPage.value = 1; loadData() }
const resetSearch = () => { searchKeyword.value = ''; filterStatus.value = ''; currentPage.value = 1; loadData() }
const handleSizeChange = (v) => { pageSize.value = v; loadData() }
const handleCurrentChange = (v) => { currentPage.value = v; loadData() }
const handleView = (row) => { currentRow.value = row; viewDialogVisible.value = true }

const handleDelete = async (row) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入删除原因，将通知相关人员', '删除订单', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      inputPlaceholder: '请输入删除原因...'
    })
    await api.delete(`/admin/repair/${row.id}`, { params: { reason } })
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.row-title { font-family: var(--font-display); font-weight: 600; font-size: 14px; color: var(--admin-ink); }
.muted { color: var(--admin-muted); }
.font-mono { font-family: var(--font-mono); font-size: 13px; }
.row-actions { display: flex; gap: 4px; }
.row-btn {
  background: transparent;
  border: 1px solid transparent;
  color: var(--admin-ink-soft);
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: var(--r-xs);
  cursor: pointer;
  transition: all var(--d-fast) var(--ease-standard);
  font-family: inherit;
}
.row-btn:hover { background: var(--admin-stamp-soft); color: var(--admin-stamp); border-color: var(--admin-stamp-line); }
.row-btn.warn:hover { background: var(--admin-amber-soft); color: var(--admin-amber); border-color: var(--admin-amber-soft); }
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 4px 4px;
  border-top: 1px solid var(--admin-line-soft);
  margin-top: 8px;
}
.ti { color: var(--admin-muted); }
</style>
