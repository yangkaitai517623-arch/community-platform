import api from './index'

export const getCommentList = (params) => api.get('/admin/comments', { params })
export const auditComment = (id, status) => api.put(`/admin/comments/${id}/audit`, null, { params: { status } })
export const deleteComment = (id) => api.delete(`/admin/comments/${id}`)
