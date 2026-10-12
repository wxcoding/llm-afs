<template>
  <el-dialog :model-value="visible" :title="isEdit ? '编辑知识' : '新增知识'" width="600px" @update:model-value="$emit('update:visible', $event)">
    <el-form :model="form" label-width="80px">
      <el-form-item label="标题">
        <el-input v-model="form.title" placeholder="请输入知识标题" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
          <el-option
            v-for="option in categoryOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="内容">
        <el-input v-model="form.content" type="textarea" :rows="10" placeholder="请输入知识内容" />
      </el-form-item>
      <el-form-item label="提交方式">
        <el-radio-group v-model="submitModeValue">
          <el-radio value="direct">直接保存</el-radio>
          <el-radio value="audit">提交审核</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="$emit('submit')" :loading="submitting">
        {{ submitModeValue === 'audit' ? '提交审核' : '确定' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  isEdit: { type: Boolean, default: false },
  form: { type: Object, required: true },
  submitMode: { type: String, default: 'direct' },
  submitting: { type: Boolean, default: false },
  categoryOptions: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:visible', 'update:submitMode', 'submit'])

const submitModeValue = computed({
  get: () => props.submitMode,
  set: (val) => emit('update:submitMode', val)
})
</script>
