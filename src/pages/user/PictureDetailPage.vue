<template>
  <div id="pictureDetailPage">
    <!-- 顶部信息栏 -->
    <div class="top-bar">
      <a-row align="middle" justify="space-between">
        <a-col>
          <div class="author-info">
            <a-avatar :size="48" :src="picture.user?.userAvatar" />
            <span class="author-name">{{ picture.user?.userName }}</span>
          </div>
        </a-col>
        <a-col>
          <div class="action-info">
            <span class="like-count">{{ picture.likeCount || 0 }} 点赞</span>
            <a-button
              :type="picture.isLiked ? 'primary' : 'default'"
              @click="toggleLike"
              class="like-button"
            >
              <template #icon>
                <HeartOutlined v-if="!picture.isLiked" />
                <HeartFilled v-else />
              </template>
              {{ picture.isLiked ? '已点赞' : '点赞' }}
            </a-button>
          </div>
        </a-col>
      </a-row>
    </div>

    <!-- 图片展示区 -->
    <div class="image-container">
      <a-image
        :height="600"
        style="max-height: 600px; object-fit: contain"
        :src="picture.url"
        alt="图片预览"
      />
    </div>

    <!-- 图片信息区 -->
    <div class="image-info-container">
      <div class="image-details">
        <div class="title-container">
          <h1 class="image-title">{{ picture.name ?? '未命名' }}</h1>
          <div class="action-buttons">
            <a-button type="default" size="large" @click="showInfoOverlay" class="action-button info-button">
              图片信息
            </a-button>
            <a-button type="primary" size="large" @click="doDownload" class="action-button">
              免费下载
              <template #icon>
                <DownloadOutlined />
              </template>
            </a-button>
            <a-button
              v-if="canEdit"
              type="default"
              size="large"
              @click="doEdit"
              class="action-button"
            >
              <template #icon>
                <EditOutlined />
              </template>
              编辑
            </a-button>
            <a-button
              v-if="canEdit"
              size="large"
              @click="doDelete"
              class="action-button delete-button"
            >
              <template #icon>
                <DeleteOutlined />
              </template>
              删除
            </a-button>
          </div>
        </div>
        <p class="image-description">{{ picture.introduction ?? '暂无简介' }}</p>

        <div class="image-meta">
          <div class="meta-item">
            <span class="meta-label">分类:</span>
            <a-tag class="meta-value">{{ picture.category ?? '默认' }}</a-tag>
          </div>

          <div class="meta-item">
            <span class="meta-label">标签:</span>
            <div class="tags-container">
              <a-tag v-for="tag in picture.tags" :key="tag" class="tag-item">
                {{ tag }}
              </a-tag>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片信息遮罩 -->
    <div v-if="showInfo" class="info-overlay" @click="hideInfoOverlay">
      <div class="info-content" @click.stop>
        <div class="info-header">
          <h2>图片信息</h2>
          <a-button class="close-button" @click="hideInfoOverlay">
            <template #icon>
              <CloseOutlined />
            </template>
          </a-button>
        </div>

        <div class="info-body">
          <div class="info-item">
            <span class="info-label">格式:</span>
            <span class="info-value">{{ picture.picFormat ?? '-' }}</span>
          </div>

          <div class="info-item">
            <span class="info-label">尺寸:</span>
            <span class="info-value">{{ picture.picWidth ?? '-' }} × {{ picture.picHeight ?? '-' }}</span>
          </div>

          <div class="info-item">
            <span class="info-label">宽高比:</span>
            <span class="info-value">{{ picture.picScale ?? '-' }}</span>
          </div>

          <div class="info-item">
            <span class="info-label">大小:</span>
            <span class="info-value">{{ formatSize(picture.picSize) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, reactive } from 'vue'
import { 
  deletePictureUsingPost, 
  getPictureVoByIdUsingGet, 
  likePictureUsingPost
} from '@/api/pictureController'
import { message } from 'ant-design-vue'
import { downloadImage, formatSize } from '../../utils'
import { useLoginUserStore } from '@/stores/userLoginUserStore'
import router from '@/router'
import {
  EditOutlined,
  DeleteOutlined,
  DownloadOutlined,
  HeartOutlined,
  HeartFilled,
  CloseOutlined
} from '@ant-design/icons-vue'

const props = defineProps<{
  id: string | number
}>()

const picture = ref<API.PictureVO>({})
const showInfo = ref(false) // 控制信息遮罩显示

// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
      // 确保收藏和点赞状态属性存在
      picture.value.isLiked = picture.value.isLiked ?? false
    } else {
      message.error('获取图片详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败：' + e.message)
  }
}

const loginUserStore = useLoginUserStore()
// 是否具有编辑权限
const canEdit = computed(() => {
  const loginUser = loginUserStore.loginUser;
  // 未登录不可编辑
  if (!loginUser.id) {
    return false
  }
  // 仅本人或管理员可编辑
  const user = picture.value.user || {}
  return loginUser.id === user.id || loginUser.userRole === 'admin'
})

// 编辑
const doEdit = () => {
  router.push('/add_picture?id=' + picture.value.id)
}

// 删除
const doDelete = async () => {
  const id = picture.value.id
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    router.push('/picture')
  } else {
    message.error('删除失败')
  }
}

// 处理下载
const doDownload = () => {
  downloadImage(picture.value.url)
}

// 显示信息遮罩
const showInfoOverlay = () => {
  showInfo.value = true
}

// 隐藏信息遮罩
const hideInfoOverlay = () => {
  showInfo.value = false
}

// 切换点赞状态
const toggleLike = async () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    return
  }

  try {
    const res = await likePictureUsingPost({
      pictureId: picture.value.id as any
    })

    if (res.data.code === 0) {
      // 更新图片的点赞状态
      picture.value.isLiked = !picture.value.isLiked
      // 更新图片的点赞数
      if (picture.value.isLiked) {
        picture.value.likeCount = (picture.value.likeCount || 0) + 1
      } else {
        picture.value.likeCount = Math.max(0, (picture.value.likeCount || 0) - 1)
      }
      message.success(picture.value.isLiked ? '点赞成功' : '取消点赞成功')
      
      // 通知父组件更新数据
      emit('picture-update', picture.value)
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch (e: any) {
    message.error('操作失败: ' + e.message)
  }
}

// 添加事件发射器
const emit = defineEmits(['pictureUpdate'])

onMounted(() => {
  fetchPictureDetail()
})
</script>

<style scoped>
#pictureDetailPage {
  max-width: 90vw;
  margin: 0 auto;
  padding: 3vh 3vw;
  width: 90vw;
  background-color: #fff;
  color: #000;
  position: relative;
}

.top-bar {
  padding: 16px 0;
  border-bottom: 1px solid #e0e0e0;
  margin-bottom: 24px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-name {
  font-weight: 500;
  font-size: 18px;
  color: #000;
}

.action-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.like-count {
  font-size: 16px;
  color: #333;
}

.like-button {
  background: #000;
  border: 1px solid #000;
  color: #fff;
}

.like-button:hover {
  background: #333;
  border-color: #333;
  color: #fff;
}

.image-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  max-height: 60vh;
  margin-bottom: 24px;
}

.image-info-container {
  padding: 24px 0;
}

.title-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.image-title {
  font-size: 24px;
  font-weight: bold;
  margin: 0;
  color: #000;
  flex: 1;
}

.image-description {
  color: #333;
  font-size: 16px;
  line-height: 1.6;
  margin: 12px 0 24px 0;
}

.image-meta {
  margin-bottom: 32px;
}

.meta-item {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.meta-label {
  font-weight: 500;
  width: 60px;
  color: #000;
  font-size: 14px;
}

.meta-value {
  margin-left: 12px;
  color: #333;
  font-size: 14px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-left: 12px;
}

.tag-item {
  margin: 0;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  color: #000;
}

.action-buttons {
  display: flex;
  flex-direction: row;
  gap: 12px;
  align-items: center;
}

.action-button {
  height: 40px;
  font-size: 14px;
  background: #000;
  border: 1px solid #000;
  color: #fff;
  display: flex;
  align-items: center;
  padding: 0 16px;
}

.info-button {
  background: #fff;
  border: 1px solid #000;
  color: #000;
}

.action-button:hover {
  background: #333;
  border-color: #333;
  color: #fff;
}

.info-button:hover {
  background: #f5f5f5;
  border-color: #000;
  color: #000;
}

.action-button:active {
  background: #000;
  border-color: #000;
  color: #fff;
}

.delete-button {
  background: #fff;
  border: 1px solid #000;
  color: #000;
}

.delete-button:hover {
  background: #f5f5f5;
  border-color: #000;
  color: #000;
}

.delete-button:active {
  background: #e0e0e0;
  border-color: #000;
  color: #000;
}

/* 图片信息遮罩样式 */
.info-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.info-content {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e0e0e0;
}

.info-header h2 {
  margin: 0;
  color: #000;
  font-size: 20px;
}

.close-button {
  border: none;
  background: none;
  font-size: 20px;
  cursor: pointer;
  color: #333;
}

.info-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
}

.info-label {
  font-weight: 500;
  color: #000;
  margin-bottom: 4px;
  font-size: 14px;
}

.info-value {
  color: #333;
  font-size: 16px;
}

.info-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.info-tags .tag-item {
  margin: 0;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  color: #000;
}
</style>