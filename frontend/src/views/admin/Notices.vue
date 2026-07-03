<template>
  <div class="admin-page">
    <header class="admin-page-header">
      <div class="admin-page-title-block">
        <span class="admin-page-eyebrow">通告档案 · NOTICES</span>
        <h1 class="admin-page-title">通知管理</h1>
        <p class="admin-page-subtitle">发布、查看系统通知</p>
      </div>
      <el-button type="primary" @click="handleCreate">
        <Plus :size="14" /> &nbsp;发送通知
      </el-button>
    </header>

    <div class="admin-toolbar">
      <el-input v-model="searchKeyword" placeholder="搜索通知标题" clearable @clear="handleSearch" @keyup.enter="handleSearch">
        <template #prefix><Search :size="16" class="ti" /></template>
      </el-input>
      <el-select v-model="filterType" placeholder="类型" clearable @change="handleSearch">
        <el-option label="全部" value="" />
        <el-option label="系统通知" value="system" />
        <el-option label="订单通知" value="order" />
        <el-option label="活动通知" value="activity" />
      </el-select>
      <span class="admin-toolbar-spacer"></span>
      <el-button @click="resetSearch">重置</el-button>
      <el-button type="primary" @click="handleSearch"><Search :size="14" /> &nbsp;搜索</el-button>
    </div>

    <div class="admin-surface">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <span class="row-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="220">
          <template #default="{ row }">
            <span class="content-cell">{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120" align="center">
          <template #default="{ row }">
            <span class="type-chip">{{ getTypeText(row.type) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="receiver" label="接收者" width="140">
          <template #default="{ row }">
            <span class="muted">{{ getReceiverText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="isRead" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isRead ? 'success' : 'warning'">
              {{ row.isRead ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">
            <span class="font-mono tabular muted">{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <button class="row-btn warn" @click="handleDelete(row)">删除</button>
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

    <el-dialog v-model="dialogVisible" title="发送通知" width="600px">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="通知标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入通知标题" />
        </el-form-item>
        <el-form-item label="通知类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择" style="width: 100%">
            <el-option label="系统通知" value="system" />
            <el-option label="订单通知" value="order" />
            <el-option label="活动通知" value="activity" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收者">
          <el-input v-model="formData.receiver" placeholder="留空则发送给全部居民（输入用户 ID）" />
        </el-form-item>
        <el-form-item label="通知内容" prop="content">
          <el-input v-model="formData.content" type="textarea" :rows="5" placeholder="请输入通知内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from 'lucide-vue-next'
import { getNoticeList, createNotice, deleteNotice } from '@/api/notice'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const filterType = ref('')
const dialogVisible = ref(false)
const formRef = ref(null)

const formData = reactive({ title: '', type: 'system', receiver: '', content: '' })
const formRules = {
  title: [{ required: true, message: '请输入通知标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择通知类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入通知内容', trigger: 'blur' }]
}

const getTypeText = (type) => ({ 1: '系统通知', 2: '订单通知', 3: '活动通知' }[type] || type)
const typeMap = { system: 1, order: 2, activity: 3 }
const getReceiverText = (row) => row.receiverName || row.receiver || (row.userId ? `用户ID: ${row.userId}` : '全部居民')
const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterType.value) params.type = typeMap[filterType.value]
    const res = await getNoticeList(params)
    tableData.value = res.data?.data?.records || []
    total.value = res.data?.data?.total || 0
  } catch (error) {
    console.error('加载通知数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { currentPage.value = 1; loadData() }
const resetSearch = () => { searchKeyword.value = ''; filterType.value = ''; currentPage.value = 1; loadData() }
const handleSizeChange = (v) => { pageSize.value = v; loadData() }
const handleCurrentChange = (v) => { currentPage.value = v; loadData() }

const resetForm = () => {
  formData.title = ''
  formData.type = 'system'
  formData.receiver = ''
  formData.content = ''
}

const handleCreate = () => { resetForm(); dialogVisible.value = true }

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该通知吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await deleteNotice(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { if (e !== 'cancel') console.error('删除失败:', e) }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const sendData = {
        title: formData.title,
        content: formData.content,
        type: typeMap[formData.type] || 1,
        userId: formData.receiver ? parseInt(formData.receiver) : null
      }
      await createNotice(sendData)
      ElMessage.success('发送成功')
      dialogVisible.value = false
      loadData()
    } catch (error) {
      console.error('发送失败:', error)
    } finally {
      submitLoading.value = false
    }
  })
}

onMounted(() => loadData())
</script>

<style scoped>
.row-title { font-family: var(--font-display); font-weight: 600; font-size: 14px; color: var(--admin-ink); }
.content-cell {
  font-size: 13px;
  color: var(--admin-ink-soft);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.type-chip {
  display: inline-block;
  padding: 2px 8px;
  font-size: 11px;
  background: var(--admin-blue-soft);
  border: 1px solid var(--admin-blue-soft);
  border-radius: var(--r-xs);
  color: var(--admin-blue);
  font-weight: 500;
}
.muted { color: var(--admin-muted); font-size: 13px; }
.font-mono { font-family: var(--font-mono); font-size: 13px; }
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
