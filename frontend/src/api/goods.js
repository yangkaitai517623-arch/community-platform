import api from './index'

export const getGoodsList = (params) => api.get('/admin/goods', { params })
export const auditGoods = (id, status) => api.put(`/admin/goods/${id}/audit`, null, { params: { status } })
export const offShelfGoods = (id) => api.put(`/admin/goods/${id}/status`, null, { params: { status: 3 } })
export const deleteGoods = (id) => api.delete(`/admin/goods/${id}`)
