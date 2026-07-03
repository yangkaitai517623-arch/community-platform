<template>
  <div class="admin-page">
    <header class="admin-page-header">
      <div class="admin-page-title-block">
        <span class="admin-page-eyebrow">订单档案 · GOODS ORDERS</span>
        <h1 class="admin-page-title">商品订单管理</h1>
        <p class="admin-page-subtitle">管理二手商品交易订单</p>
      </div>
      <span class="stamp-mark">{{ total }} 笔订单</span>
    </header>

    <div class="admin-toolbar">
      <el-input v-model="searchKeyword" placeholder="搜索订单编号" clearable @clear="handleSearch" @keyup.enter="handleSearch">
        <template #prefix><Search :size="16" class="ti" /></template>
      </el-input>
      <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleSearch">
        <el-option label="全部" value="" />
        <el-option label="待确认" :value="0" />
        <el-option label="已确认" :value="1" />
        <el-option label="已完成" :value="2" />
        <el-option label="已取消" :value="4" />
      </el-select>
      <span class="admin-toolbar-spacer"></span>
      <el-button @click="resetSearch">重置</el-button>
      <el-button type="primary" @click="handleSearch"><Search :size="14" /> &nbsp;搜索</el-button>
    </div>

    <div class="admin-surface">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="orderNo" label="订单编号" min-width="220">
          <template #default="{ row }">
            <span class="font-mono tabular order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="buyerId" label="买家" width="100">
          <template #default="{ row }">
            <span class="muted font-mono"># {{ row.buyerId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sellerId" label="卖家" width="100">
          <template #default="{ row }">
            <span class="muted font-mono"># {{ row.sellerId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120" align="right">
          <template #default="{ row }">
            <span class="admin-price">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
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
              <button v-if="row.status === 0" class="row-btn warn" @click="handleDelete(row)">取消</button>
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

    <el-dialog v-model="viewDialogVisible" title="订单详情" width="540px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单编号">
          <span class="font-mono">{{ currentRow.orderNo }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="买家 ID">{{ currentRow.buyerId }}</el-descriptions-item>
        <el-descriptions-item label="卖家 ID">{{ currentRow.sellerId }}</el-descriptions-item>
        <el-descriptions-item label="金额"><span class="admin-price">¥{{ currentRow.amount }}</span></el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusText(currentRow.status) }}</el-descriptions-item>
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

const getStatusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'success', 4: 'danger' }[s] || 'info')
const getStatusText = (s) => ({ 0: '待确认', 1: '已确认', 2: '已完成', 4: '已取消' }[s] || '未知')

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterStatus.value !== '' && filterStatus.value !== null) params.status = filterStatus.value
    const res = await api.get('/admin/goods-orders', { params })
    tableData.value = res.data?.data?.records || []
    total.value = res.data?.data?.total || 0
  } catch (e) { console.error(e) } finally { loading.value = false }
}

const handleSearch = () => { currentPage.value = 1; loadData() }
const resetSearch = () => { searchKeyword.value = ''; filterStatus.value = ''; currentPage.value = 1; loadData() }
const handleSizeChange = (v) => { pageSize.value = v; loadData() }
const handleCurrentChange = (v) => { currentPage.value = v; loadData() }
const handleView = (row) => { currentRow.value = row; viewDialogVisible.value = true }

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该订单吗？', '删除订单', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await api.delete(`/admin/goods-orders/${row.id}`)
    ElMessage.success('订单已取消')
    loadData()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

onMounted(() => loadData())
</script>

<style scoped>
.order-no { color: var(--admin-ink); font-weight: 500; }
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
