import api from './index'

export const getErrandOrderList = (params) => api.get('/admin/errand-orders', { params })
