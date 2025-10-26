<template>
  <div id="textToImagePage">
    <h1 class="page-title">木杉AI</h1>
    <p class="page-subtitle">输入文字描述，让 AI 为你生成图片</p>

    <div class="main-container">
      <!-- 左侧输入区域 -->
      <div class="input-section">
        <a-textarea
          v-model:value="inputText"
          placeholder="请输入图片描述，例如：一只在海边看日落的橘猫..."
          :auto-size="{ minRows: 4, maxRows: 8 }"
          :disabled="isLoading"
          class="prompt-input"
        />

        <!-- 高级参数设置 -->
        <div class="advanced-settings">
          <div class="settings-buttons">
            <div class="setting-button" @click="showImageSizeModal = true">
              <div class="setting-button-title">图片尺寸</div>
              <div class="setting-button-value">{{ imageSizeMap[imageSize] || imageSize }}</div>
            </div>
            <div class="setting-button" @click="showImageCountModal = true">
              <div class="setting-button-title">生成数量</div>
              <div class="setting-button-value">{{ imageCount }} 张</div>
            </div>
            <div class="setting-button">
              <div class="setting-button-title">种子</div>
              <div class="setting-button-value">
                <a-input-number
                  v-model:value="seed"
                  placeholder="1-99999999"
                  :min="0"
                  :max="999999999"
                  size="small"
                  class="inline-input-number"
                  :controls="false"
                />
              </div>
            </div>
            <div class="setting-button" @click="showNegativePromptModal = true">
              <div class="setting-button-title">反向提示词</div>
              <div class="setting-button-value">{{ negativePrompt || '未设置' }}</div>
            </div>
          </div>
        </div>

        <div class="action-buttons">
          <a-button
            type="primary"
            @click="handleSend"
            :loading="isLoading"
            :disabled="!inputText.trim() || isLoading"
            class="generate-button"
          >
            {{ isLoading ? '生成中...' : '生成图片' }}
          </a-button>
          <a-button @click="showHistoryModal = true" class="history-button">
            历史记录
          </a-button>
        </div>
      </div>

      <!-- 右侧展示区域 -->
      <div class="output-section">
        <div v-if="generatedImages.length > 0" class="image-gallery">
          <div
            v-for="(image, index) in generatedImages"
            :key="index"
            class="image-item"
          >
            <a-image 
              :src="image" 
              alt="AI生成图片" 
              class="generated-image"
              :preview="true"
            />
          </div>
          <!-- 如果生成的图片少于4张，用空位填充以保持四宫格布局 -->
          <div
            v-for="index in (4 - generatedImages.length)"
            :key="`empty-${index}`"
            class="image-item empty"
          ></div>
        </div>
        <div v-else class="placeholder">
          <div class="placeholder-content">
            <div class="placeholder-icon">🎨</div>
            <p>输入描述并点击生成按钮，AI将为你创作图片</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片尺寸设置模态框 -->
    <div v-if="showImageSizeModal" class="custom-modal-overlay" @click="showImageSizeModal = false">
      <div class="custom-modal" @click.stop>
        <div class="custom-modal-header">
          <h3>图片尺寸</h3>
          <span class="custom-modal-close" @click="showImageSizeModal = false">&times;</span>
        </div>
        <div class="custom-modal-body">
          <div class="size-options">
            <div
              v-for="(label, size) in imageSizeMap"
              :key="size"
              class="size-option"
              :class="{ active: imageSize === size }"
              @click="imageSize = size"
            >
              {{ label }}
            </div>
          </div>
        </div>
        <div class="custom-modal-footer">
          <button class="modal-confirm-button" @click="showImageSizeModal = false">确定</button>
        </div>
      </div>
    </div>

    <!-- 生成数量设置模态框 -->
    <div v-if="showImageCountModal" class="custom-modal-overlay" @click="showImageCountModal = false">
      <div class="custom-modal" @click.stop>
        <div class="custom-modal-header">
          <h3>生成数量</h3>
          <span class="custom-modal-close" @click="showImageCountModal = false">&times;</span>
        </div>
        <div class="custom-modal-body">
          <div class="slider-container">
            <a-slider v-model:value="imageCount" :min="1" :max="4" />
            <div class="slider-value">{{ imageCount }} 张</div>
          </div>
        </div>
        <div class="custom-modal-footer">
          <button class="modal-confirm-button" @click="showImageCountModal = false">确定</button>
        </div>
      </div>
    </div>

    <!-- 反向提示词设置模态框 -->
    <div v-if="showNegativePromptModal" class="custom-modal-overlay" @click="showNegativePromptModal = false">
      <div class="custom-modal" @click.stop>
        <div class="custom-modal-header">
          <h3>反向提示词</h3>
          <span class="custom-modal-close" @click="showNegativePromptModal = false">&times;</span>
        </div>
        <div class="custom-modal-body">
          <a-textarea
            v-model:value="negativePrompt"
            placeholder="请输入不希望图片中出现的内容，例如：模糊、低质量、文字..."
            :auto-size="{ minRows: 3, maxRows: 5 }"
            class="custom-textarea"
          />
        </div>
        <div class="custom-modal-footer">
          <button class="modal-confirm-button" @click="showNegativePromptModal = false">确定</button>
        </div>
      </div>
    </div>

    <!-- 历史记录模态框 -->
    <div v-if="showHistoryModal" class="custom-modal-overlay" @click="showHistoryModal = false">
      <div class="custom-modal history-modal" @click.stop>
        <div class="custom-modal-header">
          <h3>AI生成历史</h3>
          <span class="custom-modal-close" @click="showHistoryModal = false">&times;</span>
        </div>
        <div class="custom-modal-body history-modal-body">
          <div v-if="historyLoading" class="history-loading">
            加载中...
          </div>
          <div v-else-if="aiHistoryList.length === 0" class="history-empty">
            暂无历史记录
          </div>
          <div v-else class="history-list">
            <div 
              v-for="(history, index) in aiHistoryList" 
              :key="index" 
              class="history-item"
            >
              <div class="history-prompt">{{ history.prompt }}</div>
              <div class="history-images">
                <div 
                  v-for="(image, imgIndex) in (history.imageUrlList || []).slice(0, 4)" 
                  :key="imgIndex"
                  class="history-image-item"
                >
                  <a-image 
                    :src="image" 
                    class="history-image"
                    :preview="true"
                  />
                </div>
                <!-- 如果图片少于4张，用空位填充 -->
                <div
                  v-for="i in Math.max(0, 4 - (history.imageUrlList || []).slice(0, 4).length)"
                  :key="`empty-${i}`"
                  class="history-image-item empty"
                ></div>
              </div>
              <div class="history-time">{{ formatTime(history.createTime) }}</div>
            </div>
          </div>
        </div>
        <div class="custom-modal-footer">
          <button class="modal-confirm-button" @click="showHistoryModal = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { createTextToImageTaskUsingPost, getTextToImageTaskUsingGet } from '@/api/pictureController'
import { listUserAiGenHistoriesUsingGet } from '@/api/aiGenHistoryController'

// 输入文本
const inputText = ref('')

// 图片尺寸映射
const imageSizeMap = {
  '512*512': '512×512',
  '768*768': '768×768',
  '1024*1024': '1024×1024',
  '1280*720': '1280×720',
  '720*1280': '720×1280',
  '1440*1440': '1440×1440'
}

// 图片尺寸
const imageSize = ref('512*512')

// 图片数量
const imageCount = ref(1)

// 种子参数
const seed = ref<number | null>(null)

// 反向提示词
const negativePrompt = ref('')

// 加载状态
const isLoading = ref(false)

// 生成的图片
const generatedImages = ref<string[]>([])

// 控制各个设置项的显示状态
const showImageSizeModal = ref(false)
const showImageCountModal = ref(false)
// 移除种子模态框控制变量
const showNegativePromptModal = ref(false)

// 历史记录模态框控制
const showHistoryModal = ref(false)
const historyLoading = ref(false)
const aiHistoryList = ref<API.AiGenHistoryVO[]>([])

// 任务ID
const taskId = ref<string | null>(null)

// 轮询定时器
let pollingTimer: number | null = null

// 格式化时间
const formatTime = (timeStr: string | undefined) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN')
}

// 获取历史记录
const fetchAiHistory = async () => {
  if (historyLoading.value) return
  historyLoading.value = true
  try {
    const res = await listUserAiGenHistoriesUsingGet()
    if (res.data.code === 0 && res.data.data) {
      aiHistoryList.value = res.data.data
    } else {
      message.error(res.data.message || '获取历史记录失败')
    }
  } catch (error: any) {
    console.error('获取历史记录失败:', error)
    message.error('获取历史记录失败: ' + (error.message || '未知错误'))
  } finally {
    historyLoading.value = false
  }
}

// 监听历史记录模态框打开事件
const onHistoryModalOpen = () => {
  fetchAiHistory()
}

// 监听历史记录模态框关闭事件
const onHistoryModalClose = () => {
  // 可以在这里添加清理逻辑
}

// 当历史记录模态框打开时获取数据
const watchHistoryModal = () => {
  const unwatch = watch(showHistoryModal, (newVal) => {
    if (newVal) {
      onHistoryModalOpen()
    } else {
      onHistoryModalClose()
    }
  })
  return unwatch
}

// 轮询任务状态
const pollTaskStatus = async () => {
  if (!taskId.value) return

  try {
    const res = await getTextToImageTaskUsingGet({ taskId: taskId.value })
    if (res.data.code === 0 && res.data.data) {
      const taskResult = res.data.data.output
      if (taskResult && taskResult.taskStatus === 'SUCCEEDED') {
        // 任务成功，显示生成的图片
        if (taskResult.results && taskResult.results.length > 0) {
          const imageUrls = taskResult.results.map(result => result.url)
          generatedImages.value = imageUrls
        }
        clearPolling()
      } else if (taskResult && taskResult.taskStatus === 'FAILED') {
        // 任务失败
        message.error('图片生成失败')
        clearPolling()
      }
      // 其他状态继续轮询
    }
  } catch (error: any) {
    console.error('轮询任务状态失败:', error)
    message.error('轮询任务状态失败：' + error.message)
    clearPolling()
  }
}

// 清理轮询
const clearPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
  taskId.value = null
  isLoading.value = false
}

// 发送消息
const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text || isLoading.value) return

  // 清空之前的图片
  generatedImages.value = []

  // 设置加载状态
  isLoading.value = true

  try {
    // 调用后端API创建文生图任务
    const res = await createTextToImageTaskUsingPost({
      prompt: text,
      size: imageSize.value,
      n: imageCount.value,
      negativePrompt: negativePrompt.value || undefined,
      seed: seed.value !== null ? seed.value : undefined,
      promptExtend: true,
      watermark: false
    })

    if (res.data.code === 0 && res.data.data) {
      // 获取任务ID并开始轮询
      const requestId = res.data.data.output?.taskId || null

      if (requestId) {
        taskId.value = requestId
        // 开始轮询任务状态
        pollingTimer = window.setInterval(pollTaskStatus, 3000)
      } else {
        console.error('后端返回数据:', res.data.data)
        message.error('任务创建失败，未返回任务ID')
        isLoading.value = false
      }
    } else {
      message.error(res.data.message || '任务创建失败')
      isLoading.value = false
    }
  } catch (error: any) {
    console.error('任务创建失败:', error)
    message.error(error.message || '任务创建失败')
    isLoading.value = false
  }
}

// 监听历史记录模态框
watchHistoryModal()
</script>

<style scoped>
#textToImagePage {
  max-width: 90vw;
  margin: 0 auto;
  padding: 20px 0;
}

.page-title {
  font-size: 2rem;
  font-weight: 700;
  text-align: center;
  margin-bottom: 10px;
  color: #000;
}

.page-subtitle {
  font-size: 1rem;
  text-align: center;
  margin-bottom: 30px;
  color: #666;
}

.main-container {
  display: flex;
  gap: 30px;
  min-height: 70vh;
  align-items: flex-start;
}

.input-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 500px;
}

.prompt-input {
  border-radius: 12px !important;
  border: 2px solid #f0f0f0 !important;
}

.prompt-input:focus {
  border-color: #000 !important;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1) !important;
}

.advanced-settings {
  background-color: #f5f5f5;
  border-radius: 12px;
  padding: 16px;
}

.settings-buttons {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.setting-button {
  background-color: #f8f8f8;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid #f0f0f0;
  min-height: 80px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.setting-button:hover {
  border-color: #000;
  background-color: #f0f0f0;
}

.setting-button-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.setting-button-value {
  font-size: 16px;
  font-weight: 600;
  color: #000;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.inline-input-number {
  width: 100%;
  border: 1px solid #d9d9d9 !important;
}

.inline-input-number:focus {
  border-color: #000 !important;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1) !important;
}

.action-buttons {
  display: flex;
  gap: 12px;
}

.generate-button {
  color: white;
  background: #2cccda;
  border-radius: 15px;
  border: none;
  padding: 10px 24px;
  font-size: 16px;
  height: auto;
  flex: 1;
}

.generate-button:hover {
  background: #2cccdaaa;
}

.history-button {
  color: #333;
  background: #f5f5f5;
  border-radius: 15px;
  border: none;
  padding: 10px 24px;
  font-size: 16px;
  height: auto;
  flex: 1;
}

.history-button:hover {
  background: #e0e0e0;
}

.output-section {
  flex: 1;
  border: none;
  border-radius: 0;
  min-height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: transparent;
  align-self: flex-start;
}

.placeholder {
  text-align: center;
  color: #999;
}

.placeholder-content {
  padding: 20px;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.image-gallery {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 20px;
  padding: 0;
  width: 100%;
  height: auto;
  max-width: 600px;
  max-height: 600px;
}

.image-item {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 12px;
}

.image-item.empty {
  background-color: #f0f0f0;
  border: 2px dashed #ddd;
}

.generated-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 自定义模态框样式 */
.custom-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.custom-modal {
  background-color: #fff;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.custom-modal.history-modal {
  max-width: 800px;
  max-height: 80vh;
}

.custom-modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.custom-modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.custom-modal-close {
  font-size: 28px;
  color: #999;
  cursor: pointer;
  transition: color 0.3s ease;
}

.custom-modal-close:hover {
  color: #333;
}

.custom-modal-body {
  padding: 24px;
  flex: 1;
  overflow-y: auto;
}

.history-modal-body {
  padding: 16px;
}

.history-loading, .history-empty {
  text-align: center;
  padding: 20px;
  color: #666;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.history-item {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 16px;
  background-color: #fafafa;
}

.history-prompt {
  font-size: 14px;
  color: #333;
  margin-bottom: 12px;
  word-break: break-all;
}

.history-images {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}

.history-image-item {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 8px;
}

.history-image-item.empty {
  background-color: #f0f0f0;
  border: 1px dashed #ddd;
}

.history-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 4px;
}

.history-time {
  font-size: 12px;
  color: #999;
  text-align: right;
}

.size-options {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.size-option {
  padding: 16px;
  border: 2px solid #f0f0f0;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.size-option:hover {
  border-color: #000;
  background-color: #f8f8f8;
}

.size-option.active {
  border-color: #000;
  background-color: #000;
  color: #fff;
}

.slider-container {
  padding: 10px 0;
}

.slider-value {
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  margin-top: 20px;
  color: #000;
}

.custom-textarea {
  border-radius: 12px !important;
  border: 2px solid #f0f0f0 !important;
}

.custom-textarea:focus {
  border-color: #000 !important;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1) !important;
}

.custom-input-number {
  width: 100%;
  border-radius: 12px !important;
  border: 2px solid #f0f0f0 !important;
}

.custom-input-number:focus {
  border-color: #000 !important;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1) !important;
}

.seed-info {
  margin-top: 16px;
}

.seed-info p {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}

.random-button {
  width: 100%;
  background-color: #f0f0f0;
  border-color: #f0f0f0;
  color: #333;
}

.random-button:hover {
  background-color: #e0e0e0;
  border-color: #e0e0e0;
}

.custom-modal-footer {
  padding: 20px 24px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
}

.modal-confirm-button {
  padding: 12px 30px;
  background-color: #000;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.modal-confirm-button:hover {
  background-color: #333;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-container {
    flex-direction: column;
  }

  .input-section {
    max-width: 100%;
  }

  .settings-buttons {
    grid-template-columns: 1fr;
  }

  .action-buttons {
    flex-direction: column;
  }

  .output-section {
    min-height: 400px;
    margin-top: 30px;
  }

  .image-gallery {
    max-width: 100%;
    max-height: 400px;
  }

  .custom-modal.history-modal {
    max-width: 90%;
  }

  .history-images {
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: repeat(2, 1fr);
  }
}
</style>