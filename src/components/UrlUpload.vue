<template>
  <div class="url-picture-upload">
    <div class="upload-header">
      <h3>URL 上传</h3>
      <p class="upload-description">通过图片链接直接上传</p>
    </div>

    <div class="url-upload-form">
      <a-input-group compact class="url-input-group">
        <a-input
          v-model:value="fileUrl"
          placeholder="请输入图片 URL，如：https://example.com/image.jpg"
          class="url-input"
        />
        <a-button
          type="primary"
          :loading="loading"
          @click="handleUpload"
          class="upload-button"
        >
          {{ loading ? '上传中' : '上传' }}
        </a-button>
      </a-input-group>
    </div>

    <div v-if="picture?.url" class="preview-container">
      <div class="preview-header">
        <span>图片预览</span>
      </div>
      <div class="image-preview">
        <img :src="picture?.url" alt="Preview" />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { message } from 'ant-design-vue';
import type { UploadProps } from 'ant-design-vue';
import { uploadPictureByUrlUsingPost } from '@/api/pictureController'

function getBase64(img: Blob, callback: (base64Url: string) => void) {
  const reader = new FileReader();
  reader.addEventListener('load', () => callback(reader.result as string));
  reader.readAsDataURL(img);
}

const loading = ref<boolean>(false);

interface Props {
  picture?: API.PictureVO
  onSuccess?: (newPicture: API.PictureVO) => void
}
const props = defineProps<Props>()
const fileUrl = ref<string>()

/**
 * 上传
 */
const handleUpload = async () => {
  if (!fileUrl.value) {
    message.warning('请输入图片URL');
    return;
  }

  // 简单验证URL格式
  try {
    new URL(fileUrl.value);
  } catch (e) {
    message.error('请输入有效的URL地址');
    return;
  }

  loading.value = true
  try {
    const params: API.PictureUploadRequest = { fileUrl: fileUrl.value }
    if (props.picture) {
      params.id = props.picture.id
    }
    const res = await uploadPictureByUrlUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error) {
    message.error('图片上传失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.url-picture-upload {
  background: rgba(255, 255, 255, 0.85);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
}

.url-picture-upload:hover {
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.upload-header h3 {
  color: #2c3e50;
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}

.upload-description {
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
}

.url-upload-form {
  margin-bottom: 20px;
}

.url-input-group {
  display: flex !important;
  gap: 8px;
}

.url-input {
  flex: 1;
  border-radius: 8px !important;
}

.upload-button {
  border-radius: 8px !important;
  background: linear-gradient(135deg, #4096ff 0%, #52c41a 100%) !important;
  border: none !important;
  font-weight: 500;
  letter-spacing: 1px;
  box-shadow: 0 4px 12px rgba(64, 150, 255, 0.3);
}

.preview-container {
  margin-top: 20px;
}

.preview-header {
  font-size: 16px;
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.image-preview {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  background: rgba(245, 247, 250, 0.7);
  border-radius: 8px;
  border: 1px dashed #d9d9d9;
  padding: 16px;
}

.image-preview img {
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .url-picture-upload {
    padding: 16px;
  }

  .url-input-group {
    flex-direction: column;
  }

  .url-input,
  .upload-button {
    width: 100% !important;
  }

  .image-preview {
    min-height: 150px;
  }

  .image-preview img {
    max-height: 200px;
  }
}
</style>
