<template>
  <div class="admin-page">
    <header class="admin-page-header">
      <div class="admin-page-title-block">
        <span class="admin-page-eyebrow">权限档案 · ADMINS</span>
        <h1 class="admin-page-title">管理员管理</h1>
        <p class="admin-page-subtitle">管理系统管理员账号</p>
      </div>
      <el-button v-if="userStore.isSuperAdmin" type="primary" @click="handleAdd">
        <Plus :size="14" /> &nbsp;添加管理员
      </el-button>
    </header>

    <div class="admin-surface">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column label="档案" min-width="280">
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
        <el-table-column label="角色" width="140">
          <template #default="{ row }">
            <el-tag :type="row.role === 2 ? 'danger' : 'primary'">
              {{ row.role === 2 ? '超级管理员' : '管理员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="任命时间" width="180">
          <template #default="{ row }">
            <span class="font-mono tabular muted">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="userStore.isSuperAdmin" label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <button class="row-btn" @click="handleEdit(row)">编辑</button>
              <button class="row-btn warn" @click="handleDelete(row)" :disabled="row.role === 2">移除</button>
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

    <el-dialog v-model="addDialogVisible" title="任命新管理员" width="500px">
      <p class="dialog-tip">从普通居民中选择一位提升为管理员</p>
      <el-form label-width="100px">
        <el-form-item label="选择居民">
          <el-select v-model="selectedUserId" placeholder="请选择居民" filterable style="width: 100%">
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="user.realName + ' (' + user.username + ')'"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分配角色">
          <el-select v-model="selectedRole" placeholder="请选择" style="width: 100%">
            <el-option label="管理员" :value="1" />
            <el-option label="超级管理员" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddSubmit" :loading="submitLoading" :disabled="!selectedUserId">确认任命</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑管理员" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="用户名"><el-input v-model="editForm.username" disabled /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="editForm.realName" placeholder="请输入姓名" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="editForm.phone" placeholder="请输入手机号" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="editForm.email" placeholder="请输入邮箱" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" placeholder="请选择角色">
            <el-option label="管理员" :value="1" />
            <el-option label="超级管理员" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from 'lucide-vue-next'
import { getAdminList, updateAdmin } from '@/api/admin'
import { useUserStore } from '@/store/user'
import api from '@/api'

const loading = ref(false)
const userStore = useUserStore()
const submitLoading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const addDialogVisible = ref(false)
const editDialogVisible = ref(false)

const userList = ref([])
const selectedUserId = ref(null)
const selectedRole = ref(1)

const editForm = reactive({ id: null, username: '', realName: '', phone: '', email: '', role: 1 })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAdminList({ page: currentPage.value, size: pageSize.value })
    tableData.value = res.data?.data?.records || []
    total.value = res.data?.data?.total || 0
  } catch (error) {
    console.error('加载管理员数据失败:', error)
  } finally {
    loading.value = false
  }
}

const loadUserList = async () => {
  try {
    const res = await api.get('/admin/users', { params: { page: 1, size: 100 } })
    userList.value = res.data?.data?.records || []
  } catch (error) { console.error('加载用户列表失败:', error) }
}

const handleSizeChange = (val) => { pageSize.value = val; loadData() }
const handleCurrentChange = (val) => { currentPage.value = val; loadData() }

const handleAdd = () => {
  selectedUserId.value = null
  selectedRole.value = 1
  loadUserList()
  addDialogVisible.value = true
}

const handleAddSubmit = async () => {
  if (!selectedUserId.value) {
    ElMessage.warning('请选择居民')
    return
  }
  submitLoading.value = true
  try {
    await api.put(`/admin/users/${selectedUserId.value}/role`, null, { params: { role: selectedRole.value } })
    ElMessage.success('任命成功')
    addDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('任命失败')
  } finally {
    submitLoading.value = false
  }
}

const handleEdit = (row) => {
  editForm.id = row.id
  editForm.username = row.username || ''
  editForm.realName = row.realName || ''
  editForm.phone = row.phone || ''
  editForm.email = row.email || ''
  editForm.role = row.role || 1
  editDialogVisible.value = true
}

const handleEditSubmit = async () => {
  submitLoading.value = true
  try {
    await updateAdmin(editForm.id, editForm)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('更新失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row) => {
  if (row.role === 2) {
    ElMessage.warning('不能移除超级管理员')
    return
  }
  try {
    await ElMessageBox.confirm('确定要移除该管理员吗？移除后将恢复为普通居民', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await api.put(`/admin/users/${row.id}/role`, null, { params: { role: 0 } })
    ElMessage.success('已恢复为普通居民')
    loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('操作失败')
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
.user-name { font-family: var(--font-display); font-weight: 600; font-size: 14px; color: var(--admin-ink); }
.user-meta { font-size: 12px; color: var(--admin-muted); font-family: var(--font-mono); }
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
.row-btn:hover { background: var(--admin-stamp-soft); color: var(--admin-stamp); border-color: var(--admin-stamp-line); }
.row-btn.warn:hover { background: var(--admin-amber-soft); color: var(--admin-amber); border-color: var(--admin-amber-soft); }
.row-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 4px 4px;
  border-top: 1px solid var(--admin-line-soft);
  margin-top: 8px;
}
.dialog-tip {
  background: var(--admin-amber-soft);
  border-left: 3px solid var(--admin-amber);
  color: var(--admin-ink-soft);
  font-size: 13px;
  padding: 12px 16px;
  border-radius: var(--r-xs);
  margin-bottom: 20px;
}
</style>
