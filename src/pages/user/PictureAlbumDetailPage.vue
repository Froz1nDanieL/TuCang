<template>
  <div id="pictureAlbumDetailPage" style="max-width: 90vw;margin: 0 auto;">
    <!-- 收藏夹头部信息 -->
    <div class="album-header">
      <div class="album-info">
        <h1 class="album-name">{{ album.name }}</h1>
        <p class="album-description">{{ album.description || '暂无简介' }}</p>
        <div class="album-actions" v-if="isOwner">
          <a-button type="text" @click="handleEditAlbum" class="edit-button">
            <template #icon>
              <EditOutlined />
            </template>
          </a-button>
        </div>
      </div>
    </div>

    <!-- 收藏夹元信息 -->
    <div class="album-meta">
      <div class="album-owner">
        <a-avatar :src="user.userAvatar" :size="40" />
        <span class="owner-name">{{ user.userName }}</span>
      </div>
      <div class="album-stats">
        <span class="picture-count">{{ album.pictureCount || 0 }} 张图片</span>
      </div>
    </div>

    <!-- 图片列表 -->
    <div class="picture-list-container">
      <PictureList1
        :data-list="pictureList"
        :has-more="hasMore"
        :loading-more="loadingMore"
        @load-more="loadMorePictures"
        @picture-update="handlePictureUpdate"
        @picture-click="handlePictureClick"
      />

      <!-- 加载指示器 -->
      <div class="loading-more-container" v-if="loadingMore">
        <div class="dot-loading-animation">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
      </div>
      <div class="no-more-container" v-else-if="!hasMore && pictureList.length > 0">
        <span class="no-more-text">没有更多图片了</span>
      </div>
    </div>

    <!-- 图片详情遮罩 -->
    <div v-if="showPictureDetail" class="picture-detail-overlay" @click="closePictureDetail">
      <div class="picture-detail-content" @click.stop>
        <div class="picture-detail-header">
          <a-button type="text" @click="closePictureDetail" class="close-button">
            <template #icon>
              <CloseOutlined />
            </template>
          </a-button>
        </div>
        <div class="picture-detail-body">
          <PictureDetailPage :id="selectedPictureId" />
        </div>
      </div>
    </div>

    <!-- 编辑收藏夹模态框 -->
    <PictureAlbumManager
      ref="albumManagerRef"
      @success="handleAlbumUpdateSuccess"
    />

    <!-- 空状态 -->
    <a-empty v-if="!loading && pictureList.length === 0" description="该收藏夹暂无图片" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getPictureAlbumByIdUsingGet,
  listPicturesInAlbumByCursorUsingGet
} from '@/api/pictureAlbumController'
import { getUserByIdUsingGet } from '@/api/userController'
import { CloseOutlined, EditOutlined } from '@ant-design/icons-vue'
import PictureList1 from '@/components/PictureList1.vue'
import PictureDetailPage from '@/pages/user/PictureDetailPage.vue'
import PictureAlbumManager from '@/components/PictureAlbumManager.vue'
import type * as API from '@/api/typings'
import { useLoginUserStore } from '@/stores/userLoginUserStore'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 收藏夹信息
const album = ref<API.PictureAlbum>({
  name: '',
  description: '',
  pictureCount: 0
})

// 用户信息
const user = ref<API.UserVO>({})

// 图片列表相关状态
const pictureList = ref<API.PictureVO[]>([])
const hasMore = ref(true)
const loading = ref(false)
const loadingMore = ref(false)
const showPictureDetail = ref(false)
const selectedPictureId = ref<number | string>('')

// 游标参数
const cursorParams = reactive({
  cursorId: 0,
  pageSize: 16
})

// 判断当前用户是否为收藏夹所有者
const isOwner = ref(false)

// 收藏夹管理器引用
const albumManagerRef = ref<InstanceType<typeof PictureAlbumManager> | null>(null)

// 获取收藏夹详情
const fetchAlbumDetail = async () => {
  try {
    loading.value = true
    const res = await getPictureAlbumByIdUsingGet({
      id: route.params.id as string
    })

    if (res.data.code === 0 && res.data.data) {
      album.value = res.data.data

      // 获取用户信息
      if (res.data.data.userId) {
        const userRes = await getUserByIdUsingGet({
          id: res.data.data.userId
        })

        if (userRes.data.code === 0 && userRes.data.data) {
          user.value = userRes.data.data

          // 判断当前用户是否为收藏夹所有者
          isOwner.value = loginUserStore.loginUser?.id === userRes.data.data.id
        }
      }
    } else {
      message.error(res.data.message || '获取收藏夹详情失败')
    }
  } catch (error: any) {
    message.error(error.message || '获取收藏夹详情失败')
  } finally {
    loading.value = false
  }
}

// 获取收藏夹中的图片列表（游标查询）
const fetchPicturesInAlbum = async (isLoadMore = false) => {
  if (loadingMore.value) {
    return
  }

  if (isLoadMore && !hasMore.value) {
    return
  }

  loadingMore.value = true

  try {
    const params: any = {
      albumId: route.params.id as string,
      pageSize: cursorParams.pageSize
    }

    // 如果是加载更多，使用游标ID
    if (isLoadMore) {
      params.cursorId = cursorParams.cursorId
    }

    const res = await listPicturesInAlbumByCursorUsingGet(params)

    if (res.data.code === 0 && res.data.data) {
      const pictureCursorData = res.data.data
      const pictureListData = pictureCursorData.pictureList ?? []

      // 如果是加载更多，追加数据；否则替换数据
      if (isLoadMore) {
        pictureList.value = [...pictureList.value, ...pictureListData]
      } else {
        pictureList.value = pictureListData
      }

      // 更新游标参数和是否有更多数据
      cursorParams.cursorId = pictureCursorData.nextCursorId || 0
      hasMore.value = pictureCursorData.hasMore || false
    } else {
      message.error('获取数据失败，' + (res.data.message || ''))
      hasMore.value = false
    }
  } catch (error: any) {
    console.error('获取数据失败:', error)
    message.error('获取数据失败')
    hasMore.value = false
  } finally {
    loadingMore.value = false
  }
}

// 加载更多图片
const loadMorePictures = () => {
  fetchPicturesInAlbum(true)
}

// 处理图片更新（点赞/收藏状态变化）
const handlePictureUpdate = (picture: API.PictureVO) => {
  // 更新列表中的图片状态
  const index = pictureList.value.findIndex(item => item.id === picture.id)
  if (index !== -1) {
    pictureList.value[index] = { ...pictureList.value[index], ...picture }
  }
}

// 处理图片点击事件
const handlePictureClick = (picture: API.PictureVO) => {
  selectedPictureId.value = picture.id || ''
  showPictureDetail.value = true
}

// 关闭图片详情遮罩
const closePictureDetail = () => {
  showPictureDetail.value = false
}

// 处理编辑收藏夹
const handleEditAlbum = () => {
  if (albumManagerRef.value && album.value) {
    albumManagerRef.value.openEditModal(album.value)
  }
}

// 处理收藏夹更新成功
const handleAlbumUpdateSuccess = () => {
  // 重新获取收藏夹详情
  fetchAlbumDetail()
}

// 监听遮罩显示状态，控制页面滚动
watch(showPictureDetail, (newVal) => {
  if (newVal) {
    // 禁止页面滚动
    document.body.style.overflow = 'hidden'
  } else {
    // 恢复页面滚动
    document.body.style.overflow = ''
  }
})

// 页面加载时获取数据
onMounted(() => {
  fetchAlbumDetail()
  fetchPicturesInAlbum()
})
</script>

<style scoped>
.album-info {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.album-name {
  font-size: 32px;
  font-weight: 600;
  color: #333;
}

.album-description {
  font-size: 16px;
  color: #666;
}

.edit-button {
  margin-top: 16px;
  margin-bottom: 16px;
  font-size: 18px;
  color: #000000;
}

.album-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.album-owner {
  display: flex;
  align-items: center;
  gap: 12px;
}

.owner-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.picture-count {
  font-size: 16px;
  color: #666;
}

.picture-list-container {
  margin-top: 24px;
}

.loading-more-container {
  text-align: center;
  padding: 20px;
}

.dot-loading-animation {
  display: inline-block;
}

.dot {
  width: 12px;
  height: 12px;
  margin: 0 4px;
  background-color: #1890ff;
  border-radius: 50%;
  display: inline-block;
  animation: dot-bouncing 1.5s infinite ease-in-out both;
}

.dot:nth-child(1) {
  animation-delay: -0.32s;
}

.dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes dot-bouncing {
  0%, 80%, 100% {
    transform: scale(0);
    background-color: #cccccc;
  }
  40% {
    transform: scale(1.0);
    background-color: #1890ff;
  }
}

.no-more-container {
  text-align: center;
  padding: 20px;
  color: #999;
}

/* 图片详情遮罩样式 */
.picture-detail-overlay {
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
  backdrop-filter: blur(5px);
}

.picture-detail-content {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-width: 90vw;
  max-height: 90vh;
}

.picture-detail-header {
  padding: 12px;
  background: #f5f5f5;
  display: flex;
  justify-content: flex-end;
}

.close-button {
  font-size: 20px;
  color: #666;
}

@media (max-width: 768px) {
  #pictureAlbumDetailPage {
    padding: 16px;
  }

  .album-meta {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .album-name {
    font-size: 24px;
  }
}
</style>
