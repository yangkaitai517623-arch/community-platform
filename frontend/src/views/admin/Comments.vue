<template>
  <div class="admin-page">
    <header class="admin-page-header">
      <div class="admin-page-title-block">
        <span class="admin-page-eyebrow">评论档案 · COMMENTS</span>
        <h1 class="admin-page-title">评论管理</h1>
        <p class="admin-page-subtitle">管理论坛帖子评论</p>
      </div>
      <span class="stamp-mark">{{ total }} 条评论</span>
    </header>

    <div class="admin-toolbar">
      <el-input v-model="searchKeyword" placeholder="搜索评论内容" clearable @clear="handleSearch" @keyup.enter="handleSearch">
        <template #prefix><Search :size="16" class="ti" /></template>
      </el-input>
      <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleSearch">
        <el-option label="全部" value="" />
        <el-option label="审核中" value="pending" />
        <el-option label="已发布" value="published" />
      </el-select>
      <span class="admin-toolbar-spacer"></span>
      <el-button @click="resetSearch">重置</el-button>
      <el-button type="primary" @click="handleSearch"><Search :size="14" /> &nbsp;搜索</el-button>
    </div>

    <div class="admin-surface">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="content" label="评论内容" min-width="280">
          <template #default="{ row }">
            <div class="cmt-content">"{{ row.content }}"</div>
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="评论者" width="120">
          <template #default="{ row }"><span class="muted">{{ row.userName || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="postTitle" label="所属帖子" width="200">
          <template #default="{ row }"><span class="muted">{{ row.postTitle }}</span></template>
        </el-table-column>
        <el-table-column prop="likeCount" label="点赞" width="80" align="right">
          <template #default="{ row }"><span class="font-mono tabular">{{ row.likeCount || 0 }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">
            <span class="font-mono tabular muted">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <button v-if="row.status === 'pending' || row.status === 0" class="row-btn success" @click="handleAudit(row)">审核</button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from 'lucide-vue-next'
import { getCommentList, auditComment, deleteComment } from '@/api/comment'

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const filterStatus = ref('')

const getStatusType = (s) => ({ 0: 'warning', 1: 'success', 'pending': 'warning', 'published': 'success' }[s] || 'info')
const getStatusText = (s) => ({ 0: '审核中', 1: '已发布', 'pending': '审核中', 'published': '已发布' }[s] || '—')
const statusMap = { 'pending': 0, 'published': 1 }

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterStatus.value) params.status = statusMap[filterStatus.value]
    const res = await getCommentList(params)
    tableData.value = res.data?.data?.records || []
    total.value = res.data?.data?.total || 0
  } catch (error) {
    console.error('加载评论数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { currentPage.value = 1; loadData() }
const resetSearch = () => { searchKeyword.value = ''; filterStatus.value = ''; currentPage.value = 1; loadData() }
const handleSizeChange = (v) => { pageSize.value = v; loadData() }
const handleCurrentChange = (v) => { currentPage.value = v; loadData() }

const handleAudit = async (row) => {
  try {
    await ElMessageBox.confirm('确定要审核通过该评论吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'info'
    })
    await auditComment(row.id)
    ElMessage.success('审核通过')
    loadData()
  } catch (e) { if (e !== 'cancel') console.error('审核失败:', e) }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该评论吗？删除后不可恢复', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await deleteComment(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { if (e !== 'cancel') console.error('删除失败:', e) }
}

onMounted(() => loadData())
</script>

<style scoped>
.cmt-content {
  font-family: var(--font-display);
  color: var(--admin-ink);
  font-style: italic;
  font-size: 14px;
  line-height: 1.6;
  padding-left: 12px;
  border-left: 2px solid var(--admin-stamp-line);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.muted { color: var(--admin-muted); font-size: 13px; }
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
