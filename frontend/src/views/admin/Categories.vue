<template>
  <div class="admin-page">
    <header class="admin-page-header">
      <div class="admin-page-title-block">
        <span class="admin-page-eyebrow">分类档案 · CATEGORIES</span>
        <h1 class="admin-page-title">商品分类管理</h1>
        <p class="admin-page-subtitle">管理二手商品的分类信息</p>
      </div>
      <el-button type="primary" @click="handleAdd">
        <Plus :size="14" /> &nbsp;添加分类
      </el-button>
    </header>

    <div class="admin-surface">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="name" label="分类名称" min-width="200">
          <template #default="{ row }">
            <span class="cat-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="图标" width="100" align="center">
          <template #default="{ row }">
            <span class="cat-icon-tag">{{ row.icon || 'Folder' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="100" align="center">
          <template #default="{ row }">
            <span class="font-mono tabular">{{ row.sortOrder }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <button class="row-btn" @click="handleEdit(row)">编辑</button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="84px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="formData.name" placeholder="如「家具」「电子」" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="formData.icon" placeholder="lucide 图标名（选填）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from 'lucide-vue-next'
import { getCategoryList, createCategory, updateCategory, deleteCategory } from '@/api/category'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('添加分类')
const formRef = ref(null)

const formData = reactive({ id: null, name: '', icon: '', sortOrder: 0, status: 1 })
const formRules = { name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCategoryList()
    tableData.value = res.data?.data || []
    total.value = tableData.value.length
  } catch (error) {
    console.error('加载分类数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (val) => { pageSize.value = val; loadData() }
const handleCurrentChange = (val) => { currentPage.value = val; loadData() }

const resetForm = () => {
  formData.id = null
  formData.name = ''
  formData.icon = ''
  formData.sortOrder = 0
  formData.status = 1
}

const handleAdd = () => { dialogTitle.value = '添加分类'; resetForm(); dialogVisible.value = true }

const handleEdit = (row) => {
  dialogTitle.value = '编辑分类'
  formData.id = row.id
  formData.name = row.name
  formData.icon = row.icon
  formData.sortOrder = row.sortOrder
  formData.status = row.status
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该分类吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败:', error)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (formData.id) await updateCategory(formData)
      else await createCategory(formData)
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
    } catch (error) {
      console.error('保存失败:', error)
    } finally {
      submitLoading.value = false
    }
  })
}

onMounted(() => loadData())
</script>

<style scoped>
.cat-name {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  color: var(--admin-ink);
}
.cat-icon-tag {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--admin-muted);
  background: var(--admin-paper-soft);
  border: 1px solid var(--admin-line-soft);
  padding: 2px 8px;
  border-radius: var(--r-xs);
}
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
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 16px 4px 4px;
  border-top: 1px solid var(--admin-line-soft);
  margin-top: 8px;
}
</style>
