<template>
  <div class="admin-page">
    <header class="admin-page-header">
      <div class="admin-page-title-block">
        <span class="admin-page-eyebrow">检修档案 · REPAIR</span>
        <h1 class="admin-page-title">检修需求管理</h1>
        <p class="admin-page-subtitle">管理居民提交的检修需求与师傅分配</p>
      </div>
      <span class="stamp-mark">{{ total }} 条需求</span>
    </header>

    <div class="admin-toolbar">
      <el-input v-model="searchKeyword" placeholder="搜索报修内容" clearable @clear="handleSearch" @keyup.enter="handleSearch">
        <template #prefix><Search :size="16" class="ti" /></template>
      </el-input>
      <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleSearch">
        <el-option label="全部" value="" />
        <el-option label="待接单" value="pending" />
        <el-option label="已接单" value="accepted" />
        <el-option label="维修中" value="repairing" />
        <el-option label="已完成" value="completed" />
      </el-select>
      <el-select v-model="filterPriority" placeholder="紧急度" clearable @change="handleSearch">
        <el-option label="全部" value="" />
        <el-option label="紧急" value="1" />
        <el-option label="一般" value="2" />
        <el-option label="不急" value="3" />
      </el-select>
      <span class="admin-toolbar-spacer"></span>
      <el-button @click="resetSearch">重置</el-button>
      <el-button type="primary" @click="handleSearch"><Search :size="14" /> &nbsp;搜索</el-button>
    </div>

    <div class="admin-surface">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column label="需求" min-width="280">
          <template #default="{ row }">
            <div class="repair-cell">
              <span class="repair-title">{{ row.title }}</span>
              <span class="repair-desc">{{ row.description }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="居民" width="100">
          <template #default="{ row }"><span class="muted font-mono"># {{ row.userId }}</span></template>
        </el-table-column>
        <el-table-column prop="repairType" label="类型" width="110">
          <template #default="{ row }"><span class="muted">{{ row.repairType || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="urgency" label="紧急度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getUrgencyType(row.urgency)">{{ getUrgencyText(row.urgency) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170">
          <template #default="{ row }">
            <span class="font-mono tabular muted">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <button v-if="row.status === 0" class="row-btn primary" @click="handleAssign(row)">分配师傅</button>
              <button v-if="row.status === 1 || row.status === 2" class="row-btn warn" @click="handleAssign(row)">更换师傅</button>
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
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <el-dialog v-model="assignDialogVisible" title="分配维修师傅" width="420px">
      <el-form :model="assignForm" label-width="84px">
        <el-form-item label="选择师傅">
          <el-select v-model="assignForm.masterId" placeholder="请选择维修师傅" style="width: 100%">
            <el-option
              v-for="item in masterList"
              :key="item.id"
              :label="`${item.realName || item.username}（ID: ${item.id}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="检修需求详情" width="640px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="需求标题" :span="2">{{ currentRow.title }}</el-descriptions-item>
        <el-descriptions-item label="需求描述" :span="2">{{ currentRow.description }}</el-descriptions-item>
        <el-descriptions-item label="居民 ID">{{ currentRow.userId }}</el-descriptions-item>
        <el-descriptions-item label="报修类型">{{ currentRow.repairType }}</el-descriptions-item>
        <el-descriptions-item label="紧急度">{{ getUrgencyText(currentRow.urgency) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusText(currentRow.status) }}</el-descriptions-item>
        <el-descriptions-item label="维修地址">{{ currentRow.location }}</el-descriptions-item>
        <el-descriptions-item label="维修师傅">{{ currentRow.workerId ? '师傅 ID: ' + currentRow.workerId : '待接单' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">{{ currentRow.createdAt }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="currentRow.status === 0 || currentRow.status === 1 || currentRow.status === 2" class="dialog-foot-actions">
        <el-button :type="currentRow.status === 0 ? 'primary' : 'warning'" @click="handleAssign(currentRow)">
          {{ currentRow.status === 0 ? '分配师傅' : '更换师傅' }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from 'lucide-vue-next'
import { getRepairList, assignMaster, getMasterList } from '@/api/repair'
import api from '@/api'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const filterStatus = ref('')
const filterPriority = ref('')
const assignDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const currentRow = ref({})
const masterList = ref([])

const assignForm = reactive({ repairId: null, masterId: null })

const getUrgencyType = (u) => ({ 1: 'danger', 2: 'warning', 3: 'info' }[u] || 'info')
const getUrgencyText = (u) => ({ 1: '紧急', 2: '一般', 3: '不急' }[u] || '—')
const getStatusType = (s) => ({ 0: 'warning', 1: 'info', 2: 'primary', 3: 'success', 4: 'danger' }[s] || 'info')
const getStatusText = (s) => ({ 0: '待接单', 1: '已接单', 2: '维修中', 3: '已完成', 4: '已取消' }[s] || '—')
const statusMap = { 'pending': 0, 'accepted': 1, 'repairing': 2, 'completed': 3 }

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterStatus.value) params.status = statusMap[filterStatus.value]
    if (filterPriority.value) params.urgency = filterPriority.value
    const res = await getRepairList(params)
    tableData.value = res.data?.data?.records || []
    total.value = res.data?.data?.total || 0
  } catch (error) {
    console.error('加载检修需求数据失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMasters = async () => {
  try {
    const res = await getMasterList()
    masterList.value = res.data?.data?.records || []
  } catch (error) { console.error('加载师傅列表失败:', error) }
}

const handleSearch = () => { currentPage.value = 1; loadData() }
const resetSearch = () => {
  searchKeyword.value = ''
  filterStatus.value = ''
  filterPriority.value = ''
  currentPage.value = 1
  loadData()
}
const handleSizeChange = (v) => { pageSize.value = v; loadData() }
const handleCurrentChange = (v) => { currentPage.value = v; loadData() }

const handleAssign = (row) => {
  assignForm.repairId = row.id
  assignForm.masterId = row.workerId || null
  assignDialogVisible.value = true
}

const handleAssignSubmit = async () => {
  if (!assignForm.masterId) {
    ElMessage.warning('请选择维修师傅')
    return
  }
  submitLoading.value = true
  try {
    const res = await assignMaster(assignForm.repairId, assignForm.masterId)
    if (res.data.code === 200) {
      ElMessage.success('分配成功')
      assignDialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.data.message || '分配失败')
    }
  } catch (error) {
    console.error('分配失败:', error)
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入删除原因（将通知发布者）', '删除需求', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      inputPlaceholder: '请输入删除原因...'
    })
    await api.delete(`/admin/repair/${row.id}`, { params: { reason } })
    ElMessage.success('删除成功，已通知发布者')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleView = (row) => { currentRow.value = row; viewDialogVisible.value = true }

onMounted(() => { loadData(); loadMasters() })
</script>

<style scoped>
.repair-cell { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.repair-title { font-family: var(--font-display); font-weight: 600; font-size: 14px; color: var(--admin-ink); }
.repair-desc {
  font-size: 12px;
  color: var(--admin-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 360px;
}
.muted { color: var(--admin-muted); }
.font-mono { font-family: var(--font-mono); font-size: 13px; }
.row-actions { display: flex; gap: 4px; flex-wrap: wrap; }
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
.row-btn.primary:hover { background: var(--admin-stamp-soft); color: var(--admin-stamp); border-color: var(--admin-stamp-line); }
.row-btn.warn:hover { background: var(--admin-amber-soft); color: var(--admin-amber); border-color: var(--admin-amber-soft); }
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 4px 4px;
  border-top: 1px solid var(--admin-line-soft);
  margin-top: 8px;
}
.ti { color: var(--admin-muted); }
.dialog-foot-actions { margin-top: 16px; text-align: right; }
</style>
