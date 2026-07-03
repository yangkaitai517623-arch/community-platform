import api from './index'

// 管理员接口
export const getRepairList = (params) => api.get('/admin/repair', { params })
export const assignMaster = (id, workerId) => api.put(`/admin/repair/${id}/assign`, null, { params: { workerId } })
// 师傅应该是专职人员(role=3)，不是普通用户
export const getMasterList = () => api.get('/admin/users', { params: { role: 3, size: 100 } })

// 用户端接口
export const getUserRepairList = (params) => api.get('/repair-requests', { params })
export const createRepairRequest = (data) => api.post('/repair-requests', data)
export const updateRepairRequest = (id, data) => api.put(`/repair-requests/${id}`, data)
export const deleteRepairRequest = (id) => api.delete(`/repair-requests/${id}`)
export const acceptRepairRequest = (id) => api.put(`/repair-requests/${id}/accept`)
export const completeRepairRequest = (id) => api.put(`/repair-requests/${id}/complete`)
export const cancelRepairAccept = (id) => api.put(`/repair-requests/${id}/cancel-accept`)
