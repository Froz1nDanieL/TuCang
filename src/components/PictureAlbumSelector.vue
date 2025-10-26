<template>
  <a-modal
    v-model:open="modalVisible"
    title="选择收藏夹"
    :footer="null"
    @cancel="handleCancel"
    width="600px"
    class="album-select-modal"
  >
    <a-spin :spinning="loading">
      <div class="album-list-container">
        <a-row :gutter="[16, 16]">
          <!-- 创建新收藏夹选项 -->
          <a-col
            :span="8"
            class="album-col"
          >
            <div 
              class="album-card create-new"
              @click="openCreateAlbumModal"
            >
              <div class="album-cover">
                <div class="album-cover-placeholder">
                  <PlusOutlined />
                </div>
              </div>
              <div class="album-info">
                <div class="album-name">创建新的收藏夹</div>
              </div>
            </div>
          </a-col>
          
          <!-- 收藏夹列表 -->
          <a-col
            v-for="album in albumList"
            :key="album.id"
            :span="8"
            class="album-col"
          >
            <div 
              class="album-card"
              :class="{ active: selectedAlbumIds.includes(album.id) }"
              @click="toggleAlbumSelection(album)"
            >
              <div class="album-cover">
                <img 
                  v-if="albumRecentPictures && albumRecentPictures[album.id] && albumRecentPictures[album.id].length > 0"
                  :src="albumRecentPictures[album.id][0].thumbnailUrl ?? albumRecentPictures[album.id][0].url"
                  :alt="album.name"
                  class="album-cover-image"
                />
                <div v-else class="album-cover-placeholder">
                  <FolderOpenOutlined />
                </div>
              </div>
              <div class="album-info">
                <div class="album-name">{{ album.name }}</div>
              </div>
            </div>
          </a-col>
        </a-row>
      </div>
    </a-spin>
  </a-modal>
  
  <!-- 创建/编辑收藏夹组件 -->
  <PictureAlbumManager
    ref="albumManagerRef"
    @success="handleAlbumManagerSuccess"
  />
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { 
  message,
  Spin as ASpin,
  Row as ARow,
  Col as ACol
} from 'ant-design-vue'
import {
  FolderOpenOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import {
  listPictureAlbumsWithStatusUsingGet,
  addPictureToAlbumUsingPost,
  removePictureFromAlbumUsingPost
} from '@/api/pictureController'
import {
  listPicturesInAlbumByCursorUsingGet
} from '@/api/pictureAlbumController'
import PictureAlbumManager from '@/components/PictureAlbumManager.vue'

// 定义组件的属性
interface Props {
  visible: boolean
  pictureId?: number
}

// 定义组件的事件
interface Emits {
  (e: 'update:visible', visible: boolean): void
  (e: 'cancel'): void
  (e: 'albumUpdated'): void
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  pictureId: 0
})

const emit = defineEmits<Emits>()

// 模态框可见性
const modalVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// 加载状态
const loading = ref(false)

// 收藏夹列表
const albumList = ref<API.PictureAlbumVO[]>([])

// 选中的收藏夹ID列表
const selectedAlbumIds = ref<number[]>([])

// 存储每个收藏夹最近的图片
const albumRecentPictures = ref<Record<number, API.PictureVO[]>>({})

// 组件引用
const albumManagerRef = ref<InstanceType<typeof PictureAlbumManager> | null>(null)

// 获取包含图片收藏状态的收藏夹列表
const fetchAlbumListWithStatus = async (pictureId: number) => {
  try {
    loading.value = true
    const res = await listPictureAlbumsWithStatusUsingGet({ pictureId })
    
    if (res.data.code === 0) {
      albumList.value = res.data.data || []
      // 设置已选中的收藏夹
      selectedAlbumIds.value = albumList.value
        .filter(album => album.isPictureFavorited)
        .map(album => album.id as number)
      
      // 获取每个收藏夹最近的图片
      await Promise.all(albumList.value.map(album => fetchAlbumRecentPictures(album)))
    } else {
      message.error(res.data.message || '获取收藏夹列表失败')
    }
  } catch (error: any) {
    message.error(error.message || '获取收藏夹列表失败')
  } finally {
    loading.value = false
  }
}

// 获取收藏夹最近的图片
const fetchAlbumRecentPictures = async (album: API.PictureAlbumVO) => {
  if (!album.id) return
  
  try {
    const params = {
      albumId: album.id,
      pageSize: 1,
      sortField: 'createTime',
      sortOrder: 'descend'
    }
    
    const res = await listPicturesInAlbumByCursorUsingGet(params)
    
    if (res.data?.code === 0 && res.data?.data?.pictureList) {
      // 使用 Vue 的响应式系统更新数据
      albumRecentPictures.value = {
        ...albumRecentPictures.value,
        [album.id]: res.data.data.pictureList
      }
    }
  } catch (error) {
    console.error(`获取收藏夹 ${album.id} 的图片失败:`, error)
  }
}

// 切换收藏夹选择状态
const toggleAlbumSelection = async (album: API.PictureAlbumVO) => {
  const albumId = album.id as number
  const pictureId = props.pictureId
  const isSelected = selectedAlbumIds.value.includes(albumId)
  
  try {
    if (isSelected) {
      // 取消收藏
      const res = await removePictureFromAlbumUsingPost({ 
        pictureId: pictureId,
        albumId: albumId
      })
      
      if (res.data.code === 0) {
        // 从选中列表中移除
        const index = selectedAlbumIds.value.indexOf(albumId)
        if (index > -1) {
          selectedAlbumIds.value.splice(index, 1)
        }
        
        // 更新albumList中的状态
        const albumItem = albumList.value.find(item => item.id === albumId)
        if (albumItem) {
          albumItem.isPictureFavorited = false
          albumItem.pictureCount = Math.max(0, (albumItem.pictureCount || 0) - 1)
        }
        
        message.success('已取消收藏')
      } else {
        // 如果后端返回错误，检查是否是因为图片本来就不在收藏夹中
        if (res.data.message && res.data.message.includes('不存在')) {
          // 即使后端报错，也更新前端状态以保持一致性
          const index = selectedAlbumIds.value.indexOf(albumId)
          if (index > -1) {
            selectedAlbumIds.value.splice(index, 1)
          }
          
          const albumItem = albumList.value.find(item => item.id === albumId)
          if (albumItem) {
            albumItem.isPictureFavorited = false
          }
          
          message.success('已取消收藏')
        } else {
          message.error(res.data.message || '取消收藏失败')
        }
      }
    } else {
      // 添加收藏
      const res = await addPictureToAlbumUsingPost({ 
        pictureId: pictureId,
        albumId: albumId
      })
      
      if (res.data.code === 0) {
        // 添加到选中列表
        selectedAlbumIds.value.push(albumId)
        
        // 更新albumList中的状态
        const albumItem = albumList.value.find(item => item.id === albumId)
        if (albumItem) {
          albumItem.isPictureFavorited = true
          albumItem.pictureCount = (albumItem.pictureCount || 0) + 1
        }
        
        message.success('收藏成功')
      } else {
        // 如果后端返回错误，检查是否是因为图片已经收藏过了
        if (res.data.message && res.data.message.includes('已存在')) {
          // 即使后端报错，也更新前端状态以保持一致性
          if (!selectedAlbumIds.value.includes(albumId)) {
            selectedAlbumIds.value.push(albumId)
          }
          
          const albumItem = albumList.value.find(item => item.id === albumId)
          if (albumItem) {
            albumItem.isPictureFavorited = true
          }
          
          message.success('收藏成功')
        } else {
          message.error(res.data.message || '收藏失败')
        }
      }
    }
    
    // 重新获取该收藏夹的最近图片
    await fetchAlbumRecentPictures(album)
    
    // 通知父组件收藏状态已更新
    emit('albumUpdated')
  } catch (err) {
    console.error('收藏操作失败', err)
    message.error('操作失败')
  }
}

// 打开创建收藏夹模态框
const openCreateAlbumModal = () => {
  if (albumManagerRef.value) {
    albumManagerRef.value.openCreateModal()
  }
}

// 处理取消事件
const handleCancel = () => {
  emit('cancel')
}

// 处理收藏夹管理器成功事件
const handleAlbumManagerSuccess = async () => {
  // 刷新列表
  await fetchAlbumListWithStatus(props.pictureId)
}

// 监听 pictureId 变化，重新获取收藏夹列表
watch(() => props.pictureId, async (newPictureId) => {
  if (newPictureId && props.visible) {
    await fetchAlbumListWithStatus(newPictureId)
  }
}, { immediate: true })

// 监听 visible 变化
watch(() => props.visible, async (newVisible) => {
  if (newVisible && props.pictureId) {
    await fetchAlbumListWithStatus(props.pictureId)
  }
}, { immediate: true })
</script>

<script lang="ts">
export default {
  name: 'PictureAlbumSelector'
}
</script>

<style scoped>
/* 收藏夹相关样式 */
.album-list-container {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px;
  margin: -8px;
}

.album-col {
  margin-bottom: 16px;
}

.album-card {
  border: 1px solid #d9d9d9;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.album-card:hover {
  box-shadow: 0 0 8px #2cccda;
}

.album-card.active {
  border-color: #2cccdaaa;
  box-shadow: 0 0 0 4px #2cccdaaa;
}

.album-card.create-new {
  background-color: #fafafa;
}

.album-card.create-new:hover {
  background-color: #f0f0f0;
}

.album-cover {
  position: relative;
  padding-top: 100%; /* 1:1 宽高比 */
  background-color: #f5f5f5;
}

.album-cover-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #bfbfbf;
}

.album-cover-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.album-info {
  padding: 12px;
  flex: 1;
  display: flex;
  align-items: center;
}

.album-name {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

/* 弹窗样式 */
:deep(.album-select-modal .ant-modal-body) {
  padding: 16px;
}

:deep(.album-select-modal .ant-row) {
  margin: 0 -8px;
}

:deep(.album-select-modal .ant-col) {
  padding: 0 8px;
}
</style>