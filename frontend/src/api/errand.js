import api from './index'

// 管理员接口
export const getErrandList = (params) => api.get('/admin/errand', { params })
export const assignRunner = (id, runnerId) => api.put(`/admin/errand/${id}/assign`, null, { params: { runnerId } })
export const getRunnerList = () => api.get('/admin/users', { params: { role: 0, size: 100 } })

// 用户端接口
export const getUserErrandList = (params) => api.get('/errand-requests', { params })
export const createErrandRequest = (data) => api.post('/errand-requests', data)
export const updateErrandRequest = (id, data) => api.put(`/errand-requests/${id}`, data)
export const deleteErrandRequest = (id) => api.delete(`/errand-requests/${id}`)
export const acceptErrandRequest = (id) => api.put(`/errand-requests/${id}/accept`)
export const completeErrandRequest = (id) => api.put(`/errand-requests/${id}/complete`)
export const cancelErrandAccept = (id) => api.put(`/errand-requests/${id}/cancel-accept`)
