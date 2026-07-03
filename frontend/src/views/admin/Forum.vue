<template>
  <div class="admin-page">
    <header class="admin-page-header">
      <div class="admin-page-title-block">
        <span class="admin-page-eyebrow">论坛档案 · FORUM</span>
        <h1 class="admin-page-title">资讯论坛管理</h1>
        <p class="admin-page-subtitle">审核、下架、查看居民帖子</p>
      </div>
      <span class="stamp-mark">{{ total }} 条帖子</span>
    </header>

    <div class="admin-toolbar">
      <el-input v-model="searchKeyword" placeholder="搜索帖子标题" clearable @clear="handleSearch" @keyup.enter="handleSearch">
        <template #prefix><Search :size="16" class="ti" /></template>
      </el-input>
      <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleSearch">
        <el-option label="全部" value="" />
        <el-option label="审核中" value="pending" />
        <el-option label="已发布" value="published" />
        <el-option label="已下架" value="off_shelf" />
      </el-select>
      <span class="admin-toolbar-spacer"></span>
      <el-button @click="resetSearch">重置</el-button>
      <el-button type="primary" @click="handleSearch"><Search :size="14" /> &nbsp;搜索</el-button>
    </div>

    <div class="admin-surface">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="280">
          <template #default="{ row }">
            <span class="row-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userId" label="发布者" width="100">
          <template #default="{ row }"><span class="muted font-mono"># {{ row.userId }}</span></template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="90" align="right">
          <template #default="{ row }"><span class="font-mono tabular">{{ row.viewCount || 0 }}</span></template>
        </el-table-column>
        <el-table-column prop="commentCount" label="评论" width="90" align="right">
          <template #default="{ row }"><span class="font-mono tabular">{{ row.commentCount || 0 }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发布时间" width="180">
          <template #default="{ row }">
            <span class="font-mono tabular muted">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <button v-if="row.status === 0" class="row-btn success" @click="handleAudit(row)">审核</button>
              <button v-if="row.status === 1" class="row-btn warn" @click="handleOffShelf(row)">下架</button>
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

    <el-dialog v-model="dialogVisible" title="帖子详情" width="720px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="帖子标题" :span="2">{{ currentRow.title }}</el-descriptions-item>
        <el-descriptions-item label="发布者 ID">{{ currentRow.userId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusText(currentRow.status) }}</el-descriptions-item>
        <el-descriptions-item label="浏览数">{{ currentRow.viewCount }}</el-descriptions-item>
        <el-descriptions-item label="评论数">{{ currentRow.commentCount }}</el-descriptions-item>
        <el-descriptions-item label="发布时间" :span="2">{{ currentRow.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="帖子内容" :span="2">
          <div class="post-content">{{ currentRow.content }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from 'lucide-vue-next'
import { getForumList, auditPost, offShelfPost, deletePost } from '@/api/forum'

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const filterStatus = ref('')
const dialogVisible = ref(false)
const currentRow = ref({})

const getStatusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info' }[s] || 'info')
const getStatusText = (s) => ({ 0: '审核中', 1: '已发布', 2: '已下架' }[s] || '—')
const statusMap = { 'pending': 0, 'published': 1, 'off_shelf': 2 }

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterStatus.value) params.status = statusMap[filterStatus.value]
    const res = await getForumList(params)
    tableData.value = res.data?.data?.records || []
    total.value = res.data?.data?.total || 0
  } catch (error) {
    console.error('加载论坛数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { currentPage.value = 1; loadData() }
const resetSearch = () => { searchKeyword.value = ''; filterStatus.value = ''; currentPage.value = 1; loadData() }
const handleSizeChange = (v) => { pageSize.value = v; loadData() }
const handleCurrentChange = (v) => { currentPage.value = v; loadData() }
const handleView = (row) => { currentRow.value = row; dialogVisible.value = true }

const handleAudit = async (row) => {
  try {
    await ElMessageBox.confirm('确定要审核通过该帖子吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'info'
    })
    await auditPost(row.id)
    row.status = 1
    ElMessage.success('审核通过')
    loadData()
  } catch (e) { if (e !== 'cancel') console.error('审核失败:', e) }
}

const handleOffShelf = async (row) => {
  try {
    await ElMessageBox.confirm('确定要下架该帖子吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await offShelfPost(row.id)
    row.status = 2
    ElMessage.success('下架成功')
    loadData()
  } catch (e) { if (e !== 'cancel') console.error('下架失败:', e) }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该帖子吗？删除后不可恢复', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await deletePost(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { if (e !== 'cancel') console.error('删除失败:', e) }
}

onMounted(() => loadData())
</script>

<style scoped>
.row-title { font-family: var(--font-display); font-weight: 600; font-size: 14px; color: var(--admin-ink); }
.muted { color: var(--admin-muted); }
.font-mono { font-family: var(--font-mono); font-size: 13px; }
.post-content {
  max-height: 240px;
  overflow-y: auto;
  line-height: 1.7;
  color: var(--admin-ink-soft);
  white-space: pre-wrap;
  padding: 8px;
}
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
.row-btn.warn:hover { background: var(--admin-amber-soft); color: var(--admin-amber); border-color: var(--admin-amber-soft); }
.row-btn.success:hover { background: var(--admin-green-soft); color: var(--admin-green); border-color: var(--admin-green-soft); }
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 4px 4px;
  border-top: 1px solid var(--admin-line-soft);
  margin-top: 8px;
}
.ti { color: var(--admin-muted); }
</style>
