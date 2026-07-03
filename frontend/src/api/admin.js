import api from './index'

export const getAdminList = (params) => api.get('/admin/admins', { params })
export const createAdmin = (data) => api.post('/admin/admins', data)
export const updateAdmin = (id, data) => api.put(`/admin/admins/${id}`, data)
export const deleteAdmin = (id) => api.delete(`/admin/admins/${id}`)
export const updateUserRole = (id, role) => api.put(`/admin/users/${id}/role`, null, { params: { role } })
export const updatePassword = (data) => api.put('/user/password', data)
