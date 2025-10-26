<template>
  <div class="picture-album-manager">
    <!-- 添加/编辑收藏夹表单 -->
    <div v-if="isModalVisible" class="modal-overlay" @click="handleCancel">
      <div class="modal-container" @click.stop>
        <div class="modal-header">
          <h2>{{ editingAlbum ? '编辑收藏夹' : '创建收藏夹' }}</h2>
          <button class="close-button" @click="handleCancel">×</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleOk">
            <div class="form-group">
              <label for="albumName">
                收藏夹名称
                <span class="required-star">*</span>
              </label>
              <input
                id="albumName"
                v-model="formState.name"
                type="text"
                class="form-input"
                :class="{ 'required-empty': isNameEmpty }"
                placeholder="请输入收藏夹名称"
                required
              />
            </div>

            <div class="form-group">
              <label for="albumDescription">描述</label>
              <textarea
                id="albumDescription"
                v-model="formState.description"
                class="form-textarea"
                placeholder="请输入收藏夹描述（可选）"
              ></textarea>
            </div>

            <div class="form-group switch-group">
              <span class="switch-label">是否设为公开收藏夹</span>
              <label class="switch">
                <input
                  v-model="formState.isPublic"
                  type="checkbox"
                  class="switch-input"
                />
                <span class="switch-slider"></span>
              </label>
            </div>

            <div class="modal-footer">
              <button type="button" class="cancel-button" @click="handleCancel">取消</button>
              <button type="submit" class="submit-button">
                {{ editingAlbum ? '保存' : '创建' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import {
  addPictureAlbumUsingPost,
  updatePictureAlbumUsingPost
} from '@/api/pictureAlbumController'
import type { API } from '@/api/typings'

// 定义组件属性
interface Props {
  onSuccess?: () => void
}

// 定义组件事件
interface Emits {
  (e: 'success'): void
}

const props = withDefaults(defineProps<Props>(), {
  onSuccess: () => {}
})

const emit = defineEmits<Emits>()

// 表单状态
const formState = reactive({
  name: '',
  description: '',
  isPublic: false
})

// 模态框可见性
const isModalVisible = ref(false)

// 正在编辑的收藏夹
const editingAlbum = ref<API.PictureAlbum | null>(null)

// 打开创建收藏夹模态框
const openCreateModal = () => {
  // 重置表单
  formState.name = ''
  formState.description = ''
  formState.isPublic = false
  editingAlbum.value = null
  isModalVisible.value = true
}

// 打开编辑收藏夹模态框
const openEditModal = (album: API.PictureAlbum) => {
  // 设置表单值
  formState.name = album.name || ''
  formState.description = album.description || ''
  formState.isPublic = album.isPublic === 1
  editingAlbum.value = album
  isModalVisible.value = true
}

// 处理确认按钮
const handleOk = async () => {
  try {
    if (editingAlbum.value) {
      // 编辑收藏夹
      const res = await updatePictureAlbumUsingPost({
        id: editingAlbum.value.id,
        name: formState.name,
        description: formState.description,
        isPublic: formState.isPublic ? 1 : 0
      })

      if (res.data.code === 0) {
        message.success('收藏夹更新成功')
        isModalVisible.value = false
        emit('success')
        if (props.onSuccess) props.onSuccess()
      } else {
        message.error(res.data.message || '更新失败')
      }
    } else {
      // 创建收藏夹
      const res = await addPictureAlbumUsingPost({
        name: formState.name,
        description: formState.description,
        isPublic: formState.isPublic ? 1 : 0
      })

      if (res.data.code === 0) {
        message.success('收藏夹创建成功')
        isModalVisible.value = false
        emit('success')
        if (props.onSuccess) props.onSuccess()
      } else {
        message.error(res.data.message || '创建失败')
      }
    }
  } catch (error: any) {
    message.error(error.message || '操作失败')
  }
}

// 处理取消按钮
const handleCancel = () => {
  isModalVisible.value = false
}

// 暴露方法给父组件
defineExpose({
  openCreateModal,
  openEditModal
})
</script>

<style scoped>
.picture-album-manager {
  display: inline-block;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999; /* 确保遮罩层在最上层 */
}

.modal-container {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  z-index: 9999; /* 确保模态框在遮罩层之上 */
}

.modal-header {
  padding: 25px 30px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #000;
  letter-spacing: -0.5px;
}

.close-button {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #666;
  transition: color 0.3s ease;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.close-button:hover {
  color: #000;
  background-color: #f5f5f5;
}

.modal-body {
  padding: 25px 30px;
}

.form-group {
  margin-bottom: 25px;
}

.form-group label {
  display: block;
  font-weight: 600;
  color: #333;
  font-size: 16px;
  margin-bottom: 8px;
}

.required-star {
  color: #ff4d4f;
  margin-left: 4px;
}

.form-input,
.form-textarea {
  width: 100%;
  border: 2px solid #f0f0f0;
  border-radius: 8px;
  transition: all 0.3s ease;
  padding: 12px 15px;
  font-size: 15px;
  box-sizing: border-box;
  font-family: inherit;
}

.form-input:hover,
.form-textarea:hover {
  border-color: #d9d9d9;
}

.form-input:focus,
.form-textarea:focus {
  border-color: #000;
  outline: none;
}

.form-textarea {
  min-height: 100px;
  resize: vertical;
}

.switch-group {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
}

.switch-label {
  font-weight: 600;
  color: #333;
  font-size: 16px;
  flex: 1;
}

.switch {
  position: relative;
  display: inline-block;
  width: 60px;
  height: 30px;
}

.switch-input {
  opacity: 0;
  width: 0;
  height: 0;
}

.switch-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #f0f0f0;
  transition: .3s;
  border-radius: 30px;
}

.switch-slider:before {
  position: absolute;
  content: "";
  height: 24px;
  width: 24px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .3s;
  border-radius: 50%;
}

.switch-input:checked + .switch-slider {
  background-color: #000;
}

.switch-input:checked + .switch-slider:before {
  transform: translateX(30px);
}

.switch-text {
  font-size: 14px;
  color: #666;
  min-width: 40px;
  text-align: right;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 10px;
}

.cancel-button,
.submit-button {
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-button {
  background-color: #f5f5f5;
  color: #333;
  border: 1px solid #ddd;
}

.cancel-button:hover {
  background-color: #e0e0e0;
  border-color: #ccc;
}

.submit-button {
  background-color: #000;
  color: white;
  border: 1px solid #000;
}

.submit-button:hover {
  background-color: #333;
  border-color: #333;
}
</style>
