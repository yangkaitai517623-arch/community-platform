import api from './index'

export const getCategoryList = () => api.get('/admin/categories')
export const createCategory = (data) => api.post('/admin/categories', data)
export const updateCategory = (data) => api.put(`/admin/categories/${data.id}`, data)
export const deleteCategory = (id) => api.delete(`/admin/categories/${id}`)
