<template>
  <div class="admin-page">
    <header class="admin-page-header">
      <div class="admin-page-title-block">
        <span class="admin-page-eyebrow">二手档案 · GOODS</span>
        <h1 class="admin-page-title">二手商品管理</h1>
        <p class="admin-page-subtitle">审核、上下架、查看居民发布的闲置商品</p>
      </div>
      <span class="stamp-mark">{{ total }} 件商品</span>
    </header>

    <div class="admin-toolbar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索商品标题"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      >
        <template #prefix><Search :size="16" class="ti" /></template>
      </el-input>
      <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleSearch">
        <el-option label="全部" value="" />
        <el-option label="待审核" value="pending" />
        <el-option label="在售" value="on_sale" />
        <el-option label="已售" value="sold" />
        <el-option label="下架" value="off_shelf" />
        <el-option label="审核不通过" value="rejected" />
      </el-select>
      <span class="admin-toolbar-spacer"></span>
      <el-button @click="resetSearch">重置</el-button>
      <el-button type="primary" @click="handleSearch">
        <Search :size="14" /> &nbsp;搜索
      </el-button>
    </div>

    <div class="admin-surface">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column label="商品" min-width="320">
          <template #default="{ row }">
            <div class="goods-cell">
              <div class="goods-thumb">
                <img v-if="row.image" :src="row.image" :alt="row.title" />
                <ImageOff v-else :size="20" />
              </div>
              <div class="goods-meta">
                <span class="goods-title">{{ row.title }}</span>
                <span class="goods-desc">{{ row.description }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sellerName" label="发布者" width="120">
          <template #default="{ row }">
            <span class="muted">{{ row.sellerName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sellingPrice" label="价格" width="110" align="right">
          <template #default="{ row }">
            <span class="admin-price">¥{{ row.sellingPrice?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="100">
          <template #default="{ row }">
            <span class="muted">{{ row.categoryName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发布时间" width="180">
          <template #default="{ row }">
            <span class="font-mono tabular muted">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
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

    <el-dialog v-model="dialogVisible" title="商品详情" width="640px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商品标题" :span="2">{{ currentRow.title }}</el-descriptions-item>
        <el-descriptions-item label="商品图片" :span="2">
          <img v-if="currentRow.images" :src="currentRow.images" class="detail-img" />
          <span v-else class="muted">暂无图片</span>
        </el-descriptions-item>
        <el-descriptions-item label="商品描述" :span="2">{{ currentRow.description }}</el-descriptions-item>
        <el-descriptions-item label="发布者">{{ currentRow.sellerName || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="价格"><span class="admin-price">¥{{ currentRow.sellingPrice }}</span></el-descriptions-item>
        <el-descriptions-item label="原价"><span class="font-mono">¥{{ currentRow.originalPrice }}</span></el-descriptions-item>
        <el-descriptions-item label="成色">{{ currentRow.conditionLevel }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ currentRow.categoryName || '未分类' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ getStatusText(currentRow.status) }}</el-descriptions-item>
        <el-descriptions-item label="发布时间" :span="2">{{ currentRow.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ImageOff } from 'lucide-vue-next'
import { getGoodsList, auditGoods, offShelfGoods, deleteGoods } from '@/api/goods'

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const filterStatus = ref('')
const dialogVisible = ref(false)
const currentRow = ref({})

const getStatusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'danger' }[s] || 'info')
const getStatusText = (s) => ({ 0: '待审核', 1: '在售', 2: '已售', 3: '下架', 4: '审核不通过' }[s] || '—')
const statusMap = { 'pending': 0, 'on_sale': 1, 'sold': 2, 'off_shelf': 3, 'rejected': 4 }

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterStatus.value) params.status = statusMap[filterStatus.value]
    const res = await getGoodsList(params)
    tableData.value = res.data?.data?.records || []
    total.value = res.data?.data?.total || 0
  } catch (error) {
    console.error('加载商品数据失败:', error)
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
    await ElMessageBox.confirm('确定要审核通过该商品吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'info'
    })
    await auditGoods(row.id, 1)
    row.status = 1
    ElMessage.success('审核通过')
    loadData()
  } catch (e) { if (e !== 'cancel') console.error('审核失败:', e) }
}

const handleOffShelf = async (row) => {
  try {
    await ElMessageBox.confirm('确定要下架该商品吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await offShelfGoods(row.id)
    ElMessage.success('下架成功')
    loadData()
  } catch (e) { if (e !== 'cancel') console.error('下架失败:', e) }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？删除后不可恢复', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await deleteGoods(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { if (e !== 'cancel') console.error('删除失败:', e) }
}

onMounted(() => loadData())
</script>

<style scoped>
.goods-cell { display: flex; gap: 12px; align-items: center; }
.goods-thumb {
  width: 56px;
  height: 56px;
  border-radius: var(--r-sm);
  background: var(--admin-paper-soft);
  border: 1px solid var(--admin-line);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--admin-faint);
  overflow: hidden;
  flex-shrink: 0;
}
.goods-thumb img { width: 100%; height: 100%; object-fit: cover; }
.goods-meta { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.goods-title {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 14px;
  color: var(--admin-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.goods-desc {
  font-size: 12px;
  color: var(--admin-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 240px;
}
.muted { color: var(--admin-muted); }
.font-mono { font-family: var(--font-mono); font-size: 13px; color: var(--admin-ink-soft); }
.detail-img { max-width: 240px; max-height: 240px; border-radius: var(--r-sm); border: 1px solid var(--admin-line); }
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
