export const VECTOR_STATUS = Object.freeze({
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  SUCCESS: 'SUCCESS',
  FAILED: 'FAILED'
})

const presentations = {
  [VECTOR_STATUS.PENDING]: { text: '待向量化', tagType: 'info' },
  [VECTOR_STATUS.PROCESSING]: { text: '向量化中', tagType: 'warning' },
  [VECTOR_STATUS.SUCCESS]: { text: '已索引', tagType: 'success' },
  [VECTOR_STATUS.FAILED]: { text: '索引失败', tagType: 'danger' }
}

export const getVectorStatusPresentation = status =>
  presentations[status] || { text: status, tagType: 'info' }

export const isVectorStatusInProgress = status =>
  status === VECTOR_STATUS.PENDING || status === VECTOR_STATUS.PROCESSING
