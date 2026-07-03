import api from './index'

export const getGoodsOrderList = (params) => api.get('/admin/goods-orders', { params })
export const getGoodsOrder = (id) => api.get(`/admin/goods-orders/${id}`)
export const updateGoodsOrderStatus = (id, status) => api.put(`/admin/goods-orders/${id}/status`, null, { params: { status } })
