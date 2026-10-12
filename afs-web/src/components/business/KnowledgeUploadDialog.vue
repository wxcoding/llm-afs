<template>
  <el-dialog :model-value="visible" title="上传文档" width="600px" @update:model-value="$emit('update:visible', $event)">
    <div class="upload-tip">
      <el-alert type="info" :closable="false">
        <template #title>
          支持的文件格式：PDF、Word（.doc/.docx）、Markdown（.md）、Excel（.xls/.xlsx）、文本文件（.txt）
        </template>
      </el-alert>
    </div>
    <el-form :model="uploadForm" label-width="80px" style="margin-top: 16px">
      <el-form-item label="上传文件">
        <el-upload
          ref="uploadRef"
          class="upload-component"
          :auto-upload="false"
          :limit="10"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :file-list="fileList"
          accept=".pdf,.doc,.docx,.md,.markdown,.txt,.xls,.xlsx"
          multiple
        >
          <el-button type="primary">选择文件</el-button>
          <template #tip>
            <div class="el-upload__tip">支持批量上传，单个文件不超过10MB</div>
          </template>
        </el-upload>
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="uploadForm.category" placeholder="不选择则自动分类" clearable style="width: 100%">
          <el-option label="自动分类" value="" />
          <el-option
            v-for="option in categoryOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="$emit('upload')" :loading="uploading">
        上传 {{ fileList.length > 0 ? `(${fileList.length}个文件)` : '' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  visible: { type: Boolean, default: false },
  uploading: { type: Boolean, default: false },
  uploadForm: { type: Object, required: true },
  fileList: { type: Array, default: () => [] },
  categoryOptions: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:visible', 'upload', 'file-change', 'file-remove'])

const uploadRef = ref(null)

const handleFileChange = (file, files) => {
  emit('file-change', file, files)
}

const handleFileRemove = (file, files) => {
  emit('file-remove', file, files)
}
</script>

<style scoped>
.upload-tip {
  margin-bottom: 0;
}

.upload-component {
  width: 100%;
}
</style>
