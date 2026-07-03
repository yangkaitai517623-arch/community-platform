import api from './index'

export const getRepairOrderList = (params) => api.get('/admin/repair-orders', { params })
