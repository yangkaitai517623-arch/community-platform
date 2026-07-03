<template>
  <div class="admin-page">
    <header class="admin-page-header">
      <div class="admin-page-title-block">
        <span class="admin-page-eyebrow">居民档案 · USERS</span>
        <h1 class="admin-page-title">用户管理</h1>
        <p class="admin-page-subtitle">管理平台所有注册居民</p>
      </div>
      <span class="stamp-mark">{{ total }} 位居民</span>
    </header>

    <div class="admin-toolbar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户名 / 姓名 / 手机号"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      >
        <template #prefix><Search :size="16" class="ti" /></template>
      </el-input>
      <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleSearch">
        <el-option label="全部" value="" />
        <el-option label="活跃" :value="1" />
        <el-option label="未活跃" :value="0" />
      </el-select>
      <span class="admin-toolbar-spacer"></span>
      <el-button @click="resetSearch">重置</el-button>
      <el-button type="primary" @click="handleSearch">
        <Search :size="14" /> &nbsp;搜索
      </el-button>
    </div>

    <div class="admin-surface">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column label="居民信息" min-width="280">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="avatar-square">{{ row.realName?.charAt(0) || row.username?.charAt(0) || '?' }}</div>
              <div class="user-detail">
                <span class="user-name">{{ row.realName || row.username }}</span>
                <span class="user-meta tabular">{{ row.phone || row.email || '—' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="building" label="楼栋" width="100">
          <template #default="{ row }">
            <span class="font-mono tabular">{{ row.building || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="180">
          <template #default="{ row }">
            <span class="font-mono tabular muted">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '活跃' : '未活跃' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <button class="row-btn" @click="handleView(row)">查看</button>
              <button class="row-btn" @click="handleEdit(row)">编辑</button>
              <button
                class="row-btn"
                :class="row.status === 1 ? 'warn' : 'success'"
                @click="handleToggleStatus(row)"
              >{{ row.status === 1 ? '禁用' : '启用' }}</button>
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

    <el-dialog v-model="editDialogVisible" title="编辑居民档案" width="500px">
      <el-form :model="editForm" label-width="84px">
        <el-form-item label="姓名"><el-input v-model="editForm.realName" placeholder="请输入姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="editForm.phone" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="editForm.email" placeholder="请输入邮箱" /></el-form-item>
        <el-form-item label="楼栋"><el-input v-model="editForm.building" placeholder="如 3 栋" /></el-form-item>
        <el-form-item label="房间号"><el-input v-model="editForm.room" placeholder="如 502" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="居民档案详情" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ viewData.username }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ viewData.realName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ viewData.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ viewData.email || '—' }}</el-descriptions-item>
        <el-descriptions-item label="楼栋">{{ viewData.building || '—' }}</el-descriptions-item>
        <el-descriptions-item label="房间号">{{ viewData.room || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewData.status === 1 ? 'success' : 'info'">
            {{ viewData.status === 1 ? '活跃' : '未活跃' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ viewData.createdAt }}</el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="viewDialogVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from 'lucide-vue-next'
import { getUserList, updateUser } from '@/api/user'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const filterStatus = ref('')

const editDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const editForm = reactive({ id: null, realName: '', phone: '', email: '', building: '', room: '' })
const viewData = ref({})

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterStatus.value !== '' && filterStatus.value !== null) params.status = filterStatus.value
    const res = await getUserList(params)
    tableData.value = res.data?.data?.records || []
    total.value = res.data?.data?.total || 0
  } catch (error) {
    console.error('加载用户数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { currentPage.value = 1; loadData() }
const resetSearch = () => { searchKeyword.value = ''; filterStatus.value = ''; currentPage.value = 1; loadData() }
const handleSizeChange = (val) => { pageSize.value = val; loadData() }
const handleCurrentChange = (val) => { currentPage.value = val; loadData() }

const handleEdit = (row) => {
  editForm.id = row.id
  editForm.realName = row.realName || ''
  editForm.phone = row.phone || ''
  editForm.email = row.email || ''
  editForm.building = row.building || ''
  editForm.room = row.room || ''
  editDialogVisible.value = true
}

const handleEditSubmit = async () => {
  submitLoading.value = true
  try {
    await updateUser(editForm.id, editForm)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('更新失败')
  } finally {
    submitLoading.value = false
  }
}

const handleView = (row) => {
  viewData.value = { ...row }
  viewDialogVisible.value = true
}

const handleToggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该用户吗？`, '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await updateUser(row.id, { status: newStatus })
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(`${action}失败`)
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.user-cell { display: flex; align-items: center; gap: 12px; }
.avatar-square {
  width: 40px;
  height: 40px;
  border-radius: var(--r-sm);
  background: var(--admin-stamp-soft);
  color: var(--admin-stamp);
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.user-detail { display: flex; flex-direction: column; line-height: 1.3; }
.user-name {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 14px;
  color: var(--admin-ink);
}
.user-meta {
  font-size: 12px;
  color: var(--admin-muted);
  font-family: var(--font-mono);
}

.muted { color: var(--admin-muted); }
.font-mono { font-family: var(--font-mono); font-size: 13px; color: var(--admin-ink-soft); }

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
.row-btn:hover {
  background: var(--admin-stamp-soft);
  color: var(--admin-stamp);
  border-color: var(--admin-stamp-line);
}
.row-btn.warn:hover {
  background: var(--admin-amber-soft);
  color: var(--admin-amber);
  border-color: var(--admin-amber-soft);
}
.row-btn.success:hover {
  background: var(--admin-green-soft);
  color: var(--admin-green);
  border-color: var(--admin-green-soft);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 4px 4px;
  border-top: 1px solid var(--admin-line-soft);
  margin-top: 8px;
}

.ti { color: var(--admin-muted); }
</style>
