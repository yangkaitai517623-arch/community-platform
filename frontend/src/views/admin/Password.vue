<template>
  <div class="admin-page">
    <header class="admin-page-header">
      <div class="admin-page-title-block">
        <span class="admin-page-eyebrow">凭证 · CREDENTIALS</span>
        <h1 class="admin-page-title">修改密码</h1>
        <p class="admin-page-subtitle">定期修改你的登录凭证</p>
      </div>
    </header>

    <div class="password-grid">
      <aside class="security-aside">
        <div class="security-num">01</div>
        <h3 class="security-title">为什么要定期修改？</h3>
        <p class="security-text">作为管理员，你掌握着居民档案与服务订单的写入权限。定期更换密码是保护社区数据的最简单一步。</p>
        <ul class="tip-list">
          <li><span>·</span> 至少 6 位字符</li>
          <li><span>·</span> 建议字母 + 数字 + 符号</li>
          <li><span>·</span> 不要复用邮箱密码</li>
          <li><span>·</span> 修改后将立即生效</li>
        </ul>
      </aside>

      <div class="form-side admin-surface">
        <div class="form-pad">
          <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
            <el-form-item label="当前密码" prop="currentPassword">
              <el-input v-model="formData.currentPassword" type="password" show-password placeholder="请输入当前密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="formData.newPassword" type="password" show-password placeholder="6 - 20 位" />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="formData.confirmPassword" type="password" show-password placeholder="再输一次" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSubmit" :loading="loading">保存修改</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { updatePassword } from '@/api/admin'

const loading = ref(false)
const formRef = ref(null)

const formData = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== formData.newPassword) callback(new Error('两次输入的密码不一致'))
  else callback()
}

const formRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6 - 20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleReset = () => {
  formData.currentPassword = ''
  formData.newPassword = ''
  formData.confirmPassword = ''
  if (formRef.value) formRef.value.clearValidate()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await updatePassword({ oldPassword: formData.currentPassword, newPassword: formData.newPassword })
      ElMessage.success('密码修改成功')
      handleReset()
    } catch (error) {
      console.error('修改密码失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.password-grid {
  display: grid;
  grid-template-columns: 1fr 1.4fr;
  gap: 24px;
  max-width: 960px;
}

.security-aside {
  padding: 32px 28px;
  background: linear-gradient(180deg, var(--admin-paper-soft) 0%, var(--admin-paper) 100%);
  border: 1px solid var(--admin-line);
  border-radius: var(--r-md);
  display: flex;
  flex-direction: column;
}
.security-num {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--admin-stamp);
  letter-spacing: 2px;
  width: fit-content;
  padding: 2px 8px;
  border: 1px solid var(--admin-stamp-line);
  border-radius: var(--r-xs);
  margin-bottom: 16px;
}
.security-title {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 600;
  color: var(--admin-ink);
  line-height: 1.3;
  margin-bottom: 12px;
}
.security-text {
  font-size: 13px;
  color: var(--admin-muted);
  line-height: 1.7;
  margin-bottom: 20px;
}
.tip-list {
  list-style: none;
  border-top: 1px dashed var(--admin-line);
  padding-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.tip-list li {
  font-size: 13px;
  color: var(--admin-ink-soft);
  display: flex;
  gap: 8px;
}
.tip-list li span { color: var(--admin-stamp); font-weight: 600; }

.form-side { padding: 0; }
.form-pad { padding: 28px 32px; }

@media (max-width: 768px) {
  .password-grid { grid-template-columns: 1fr; }
}
</style>
