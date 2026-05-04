import { ref } from 'vue'

export const usePagination = (defaultPageSize = 10) => {
  const currentPage = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)
  
  const totalPages = ref(0)
  
  const setTotal = (value) => {
    total.value = value
    totalPages.value = Math.ceil(value / pageSize.value)
  }
  
  const changePage = (page) => {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page
    }
  }
  
  const changePageSize = (size) => {
    pageSize.value = size
    totalPages.value = Math.ceil(total.value / size)
    if (currentPage.value > totalPages.value) {
      currentPage.value = Math.max(1, totalPages.value)
    }
  }
  
  const reset = () => {
    currentPage.value = 1
    total.value = 0
    totalPages.value = 0
  }
  
  return {
    currentPage,
    pageSize,
    total,
    totalPages,
    setTotal,
    changePage,
    changePageSize,
    reset
  }
}
