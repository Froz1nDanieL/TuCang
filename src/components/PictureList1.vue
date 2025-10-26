<template>
  <div class="picture-list">
    <Waterfall
      :list="dataList"
      :has-around-gutter="false"
      :lazyload="true"
      :animationDuration="300"
      :gutter="16"
      :breakpoints="{
        4000: { rowPerView: 3 }, // 窗口宽度 ≤4000px 时应用
        800: { rowPerView: 2 }, // 窗口宽度 ≤800px 时应用
        500: { rowPerView: 1 }, // 窗口宽度 ≤500px 时应用
      }"
      :auto-resize="true"
      @scrollReachBottom="handleScrollToBottom"
    >
      <template #item="{ item: picture, index }">
        <div class="picture-item" @click.stop="doClickPicture(picture)">
          <div class="picture-overlay">
            <div class="picture-info-top">
              <div class="user-info" @click.stop="goToUserProfile(picture.user?.id)">
                <a-avatar
                  :src="
                    picture.user?.userAvatar ??
                    'https://gw.alipayobjects.com/zos/rmsportal/BiazfanxmamNRoxxVxka.png'
                  "
                  :size="48"
                />
                <span class="user-name">{{ picture.user?.userName ?? '未知用户' }}</span>
              </div>
              <div class="actions">
                <a-button
                  class="action-btn"
                  size="middle"
                  :type="picture.isLiked ? 'primary' : 'default'"
                  @click.stop="handleLikePicture(picture)"
                  :data-liked="picture.isLiked"
                >
                  <template #icon>
                    <HeartOutlined />
                  </template>
                </a-button>
                <a-button
                  class="action-btn"
                  size="middle"
                  :type="picture.isFavorited ? 'primary' : 'default'"
                  @click.stop="handleFavoritePicture(picture)"
                  :data-favorited="picture.isFavorited"
                >
                  <template #icon>
                    <StarOutlined />
                  </template>
                </a-button>
              </div>
            </div>
            <div class="picture-info-bottom">
              <div class="picture-name">{{ picture.name }}</div>
              <div class="picture-meta">
                <a-tag class="category" color="white">
                  {{ picture.category ?? '默认' }}
                </a-tag>
                <a-tag class="tag" v-for="tag in picture.tags" :key="tag">
                  {{ tag }}
                </a-tag>
              </div>
            </div>
          </div>
          <img
            :alt="picture.name"
            :src="picture.thumbnailUrl ?? picture.url"
            class="picture-image"
          />
        </div>
      </template>
    </Waterfall>

    <!-- 选择收藏夹的模态框 -->
    <PictureAlbumSelector
      v-model:visible="albumModalVisible"
      :picture-id="currentPicture?.id"
      @cancel="handleSelectAlbumCancel"
      @album-updated="handleAlbumUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Avatar as AAvatar,
  Button as AButton,
  message
} from 'ant-design-vue'
import {
  HeartOutlined,
  StarOutlined
} from '@ant-design/icons-vue'
import { Waterfall } from 'vue-waterfall-plugin-next'
import 'vue-waterfall-plugin-next/dist/style.css'
import {
  likePictureUsingPost
} from '@/api/pictureController'
import PictureAlbumSelector from '@/components/PictureAlbumSelector.vue'

interface Props {
  dataList?: any[]
  hasMore?: boolean
  loadingMore?: boolean
}

interface Emits {
  (e: 'loadMore'): void
  (e: 'pictureUpdate', picture: any): void
  (e: 'pictureClick', picture: any): void
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  hasMore: true,
  loadingMore: false,
})

const emit = defineEmits<Emits>()

const router = useRouter()
// 跳转至图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  emit('pictureClick', picture)
}

// 跳转至用户个人主页
const goToUserProfile = (userId: number | undefined) => {
  if (userId) {
    router.push(`/user/profile/${userId}`)
  }
}

// 处理滚动到底部事件
const handleScrollToBottom = () => {
  // 只有在有更多数据且未在加载中时才触发加载
  if (props.hasMore && !props.loadingMore) {
    emit('loadMore')
  }
}

// 点赞图片
const handleLikePicture = async (picture: API.PictureVO) => {
  try {
    const res = await likePictureUsingPost({
      pictureId: picture.id as any
    })
    if (res.data.code === 0) {
      // 更新图片的点赞状态
      picture.isLiked = !picture.isLiked
      if (picture.isLiked) {
        picture.likeCount = (picture.likeCount || 0) + 1
      } else {
        picture.likeCount = Math.max(0, (picture.likeCount || 0) - 1)
      }
      message.success(picture.isLiked ? '点赞成功' : '取消点赞')
      emit('pictureUpdate', picture)
    } else {
      message.error(res.data.message || '操作失败')
    }
  } catch (err) {
    console.error('点赞操作失败', err)
    message.error('操作失败')
  }
}

// 收藏夹相关状态
const albumModalVisible = ref(false)
const currentPicture = ref<API.PictureVO | null>(null)

// 收藏图片 - 打开选择收藏夹模态框
const handleFavoritePicture = async (picture: API.PictureVO) => {
  // 记录当前操作的图片
  currentPicture.value = picture

  // 显示选择收藏夹模态框
  albumModalVisible.value = true
}

// 选择收藏夹取消
const handleSelectAlbumCancel = () => {
  albumModalVisible.value = false
  currentPicture.value = null
}

// 处理收藏夹更新事件
const handleAlbumUpdated = () => {
  // 如果有当前图片，更新其收藏状态
  if (currentPicture.value) {
    // 切换收藏状态
    currentPicture.value.isFavorited = !currentPicture.value.isFavorited;
    // 重新获取图片详情以更新收藏状态
    emit('pictureUpdate', currentPicture.value)
    // 重置当前图片
    currentPicture.value = null
  }
}
</script>

<style scoped>
.picture-list {
  overflow: hidden;
  position: relative;
}

.picture-item {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.picture-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 1;
}

.picture-item:hover .picture-overlay {
  opacity: 1;
}

.picture-info-top {
  width: 100%;
  padding: 20px;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.5), transparent);
  transform: translateY(-100%);
  transition: transform 0.3s ease;
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 2;
  box-sizing: border-box;
}

.picture-item:hover .picture-info-top {
  transform: translateY(0);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  color: white;
  font-size: 20px;
  font-weight: bold;
}

.actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  border: none;
  background: none;
  color: white;
  font-size: 20px;
  height: 40px;
  width: 40px;
}

.action-btn:hover {
  background: rgba(0, 0, 0, 0.5);
  color: white;
}

.action-btn[data-liked="true"] {
  background: white;
  color: #ff3434;
}

.action-btn[data-favorited="true"] {
  background: white;
  color: black;
}

.picture-info-bottom {
  width: 100%;
  padding: 16px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.5), transparent);
  transform: translateY(100%);
  transition: transform 0.3s ease;
  position: absolute;
  bottom: 0;
  left: 0;
  z-index: 2;
  box-sizing: border-box;
}

.picture-item:hover .picture-info-bottom {
  transform: translateY(0);
}

.picture-name {
  color: white;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category {
  color: black;
}

.tag {
  color: white;
}

.picture-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.picture-meta :deep(.ant-tag) {
  margin-inline-end: 0;
  font-size: 14px;
}

.picture-image {
  width: 100%;
  height: auto;
}
</style>
