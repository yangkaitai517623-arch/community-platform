<template>
  <div class="u-page">
    <header class="u-header">
      <div>
        <h2 class="u-title">社区论坛</h2>
        <p class="u-subtitle">分享你看到的、想到的、遇到的</p>
      </div>
      <el-button type="primary" @click="showPublish = true">
        <PenLine :size="14" /> &nbsp;发个帖
      </el-button>
    </header>

    <div class="u-card-list" v-if="posts.length > 0">
      <article class="u-card post" v-for="item in posts" :key="item.id">
        <span class="u-pin forum"></span>
        <div class="post-head">
          <div class="avatar">{{ getAuthorInitial(item) }}</div>
          <div class="post-meta">
            <span class="author">{{ getAuthorName(item) }}</span>
            <span class="time tabular">{{ item.createdAt }}</span>
          </div>
        </div>
        <h3 class="u-card-title post-title">{{ item.title }}</h3>
        <p class="post-content">{{ item.content }}</p>
        <div class="post-foot">
          <span class="stat"><Eye :size="13" /> {{ item.viewCount || 0 }}</span>
          <button class="stat action" @click="openComments(item)">
            <MessageCircle :size="13" /> {{ item.commentCount || 0 }} 评论
          </button>
          <button class="stat action like-action" :class="{ liked: item.liked }" @click="handleLike(item)">
            <Heart :size="13" :fill="item.liked ? 'currentColor' : 'none'" /> {{ item.likeCount || 0 }}
          </button>
        </div>
      </article>
    </div>

    <div v-else class="u-empty">
      <div class="u-empty-icon"><MessagesSquare :size="28" /></div>
      <div class="u-empty-text">论坛里还没人发帖</div>
      <div class="u-empty-sub">第一个发帖的人会被记住</div>
      <el-button type="primary" @click="showPublish = true">写第一条</el-button>
    </div>

    <el-dialog v-model="showPublish" title="发布帖子" width="540px">
      <el-form :model="publishForm" label-width="84px">
        <el-form-item label="帖子标题">
          <el-input v-model="publishForm.title" placeholder="一句话说明你想说什么" />
        </el-form-item>
        <el-form-item label="正文内容">
          <el-input v-model="publishForm.content" type="textarea" :rows="7" placeholder="把你想分享的写在这里..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublish = false">取消</el-button>
        <el-button type="primary" @click="handlePublish" :loading="publishing">发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showComments" :title="currentPost?.title || '评论'" width="620px">
      <div class="comment-list" v-loading="commentLoading">
        <div v-if="comments.length > 0" class="comment-item" v-for="comment in comments" :key="comment.id">
          <div class="comment-avatar">{{ comment.userName?.[0] || '邻' }}</div>
          <div class="comment-main">
            <div class="comment-head">
              <span class="comment-author">{{ comment.userName || `用户 #${comment.userId}` }}</span>
              <span class="comment-time tabular">{{ comment.createdAt }}</span>
            </div>
            <p class="comment-content">{{ comment.content }}</p>
          </div>
        </div>
        <div v-else class="comment-empty">还没有评论</div>
      </div>
      <div class="comment-editor">
        <el-input v-model="commentContent" type="textarea" :rows="3" placeholder="写下你的评论..." />
      </div>
      <template #footer>
        <el-button @click="showComments = false">关闭</el-button>
        <el-button type="primary" @click="submitComment" :loading="commentSubmitting">发布评论</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { PenLine, Eye, MessageCircle, Heart, MessagesSquare } from 'lucide-vue-next'
import api from '@/api'

const posts = ref([])
const showPublish = ref(false)
const publishing = ref(false)
const showComments = ref(false)
const commentLoading = ref(false)
const commentSubmitting = ref(false)
const comments = ref([])
const currentPost = ref(null)
const commentContent = ref('')

const publishForm = reactive({ title: '', content: '' })

const getAuthorName = (post) => post?.authorName || (post?.userId ? `用户 #${post.userId}` : '未知用户')
const getAuthorInitial = (post) => {
  const name = getAuthorName(post)
  return name?.[0] || '?'
}

const fetchPosts = async () => {
  try {
    const res = await api.get('/forum/posts', { params: { page: 1, size: 20 } })
    if (res.data.code === 200) posts.value = res.data.data?.records || []
  } catch (e) {}
}

const handlePublish = async () => {
  if (!publishForm.title || !publishForm.content) {
    ElMessage.warning('标题和内容都不能为空')
    return
  }
  publishing.value = true
  try {
    const res = await api.post('/forum/posts', publishForm)
    if (res.data.code === 200) {
      ElMessage.success('发布成功')
      showPublish.value = false
      publishForm.title = ''
      publishForm.content = ''
      fetchPosts()
    }
  } catch (e) { ElMessage.error('发布失败') }
  finally { publishing.value = false }
}

const openComments = async (post) => {
  currentPost.value = post
  showComments.value = true
  commentContent.value = ''
  await increaseView(post)
  await fetchComments(post.id)
}

const increaseView = async (post) => {
  try {
    const res = await api.put(`/forum/posts/${post.id}/view`)
    if (res.data.code === 200) post.viewCount = res.data.data
  } catch (e) {}
}

const fetchComments = async (postId) => {
  commentLoading.value = true
  try {
    const res = await api.get(`/forum/posts/${postId}/comments`)
    if (res.data.code === 200) {
      comments.value = res.data.data || []
      if (currentPost.value) currentPost.value.commentCount = comments.value.length
    }
  } catch (e) {
    ElMessage.error('评论加载失败')
  } finally {
    commentLoading.value = false
  }
}

const handleLike = async (post) => {
  try {
    const res = await api.put(`/forum/posts/${post.id}/like`)
    if (res.data.code === 200) {
      post.likeCount = res.data.data?.likeCount ?? post.likeCount
      post.liked = !!res.data.data?.liked
    }
  } catch (e) {
    ElMessage.error('点赞失败')
  }
}

const submitComment = async () => {
  if (!currentPost.value) return
  if (!commentContent.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  commentSubmitting.value = true
  try {
    const res = await api.post('/forum/comments', {
      postId: currentPost.value.id,
      content: commentContent.value.trim()
    })
    if (res.data.code === 200) {
      ElMessage.success('评论成功')
      commentContent.value = ''
      currentPost.value.commentCount = (currentPost.value.commentCount || 0) + 1
      await fetchComments(currentPost.value.id)
    }
  } catch (e) {
    ElMessage.error('评论失败')
  } finally {
    commentSubmitting.value = false
  }
}

onMounted(() => fetchPosts())
</script>

<style scoped>
.u-card.post { padding: 22px; }
.post-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--pin-forum);
  color: #fff;
  font-family: var(--font-display);
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}
.post-meta { display: flex; flex-direction: column; line-height: 1.3; }
.author { font-weight: 600; font-size: 14px; color: var(--user-ink); }
.time { font-size: 11px; color: var(--user-faint); font-family: var(--font-mono); }

.post-title {
  font-size: 18px;
  margin-bottom: 8px;
}
.post-content {
  font-size: 14px;
  color: var(--user-ink-soft);
  line-height: 1.7;
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.post-foot {
  display: flex;
  gap: 20px;
  padding-top: 12px;
  border-top: 1px dashed var(--user-line-soft);
  font-size: 12px;
  color: var(--user-muted);
}
.stat { display: inline-flex; align-items: center; gap: 4px; }
.stat.action {
  border: 0;
  background: transparent;
  padding: 0;
  color: var(--user-muted);
  cursor: pointer;
  font: inherit;
}
.stat.action:hover { color: var(--pin-forum); }
.like-action.liked {
  color: #e5484d;
}
.like-action.liked:hover {
  color: #d9363e;
}
.comment-list {
  max-height: 360px;
  overflow-y: auto;
  padding-right: 4px;
}
.comment-item {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--user-line-soft);
}
.comment-avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--pin-forum);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.comment-main { flex: 1; min-width: 0; }
.comment-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
}
.comment-author { font-weight: 600; color: var(--user-ink); font-size: 13px; }
.comment-time { color: var(--user-faint); font-size: 11px; font-family: var(--font-mono); }
.comment-content { margin: 0; color: var(--user-ink-soft); line-height: 1.6; font-size: 14px; }
.comment-empty {
  padding: 28px 0;
  text-align: center;
  color: var(--user-muted);
}
.comment-editor {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--user-line-soft);
}
</style>
