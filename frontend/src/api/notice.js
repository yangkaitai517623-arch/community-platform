import api from './index'

export const getNoticeList = (params) => api.get('/admin/notices', { params })
export const createNotice = (data) => api.post('/admin/notices', data)
export const deleteNotice = (id) => api.delete(`/admin/notices/${id}`)
