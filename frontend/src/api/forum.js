import api from './index'

export const getForumList = (params) => api.get('/admin/forum', { params })
export const auditPost = (id, status = 1) => api.put(`/admin/forum/${id}/audit`, null, { params: { status } })
export const offShelfPost = (id) => api.put(`/admin/forum/${id}/audit`, null, { params: { status: 2 } })
export const deletePost = (id) => api.delete(`/admin/forum/${id}`)
