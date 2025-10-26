<template>
  <div id="explorePage">
    <!-- 探索标题 -->
    <div class="explore-header">
      <h1 class="explore-title">发现图片</h1>
      <p class="explore-subtitle">探索精彩视觉作品，发现你喜爱的图片</p>
    </div>

    <!-- 每日精选图集 -->
    <div class="section">
      <div class="section-header">
        <h2 class="section-title">每日精选</h2>
        <p class="section-description">每天为用户精心挑选的优质收藏夹</p>
      </div>
      <div class="album-content">
        <!-- 收藏夹列表 -->
        <PictureAlbumList
          :album-list="featuredAlbums"
          :album-recent-pictures="albumRecentPictures"
          @view-album="goToAlbumDetail"
        />

        <!-- 空状态 -->
        <a-empty v-if="!loadingFeatured && featuredAlbums.length === 0" description="暂无精选收藏夹" />
      </div>
    </div>

    <!-- 为你推荐图集 -->
    <div class="section">
      <div class="section-header">
        <h2 class="section-title">为你推荐</h2>
        <p class="section-description">根据您的兴趣为您推荐的收藏夹</p>
      </div>
      <div class="album-content">
        <!-- 收藏夹列表 -->
        <PictureAlbumList
          :album-list="recommendedAlbums"
          :album-recent-pictures="albumRecentPictures"
          @view-album="goToAlbumDetail"
        />

        <!-- 空状态 -->
        <a-empty v-if="!loadingRecommended && recommendedAlbums.length === 0" description="暂无推荐收藏夹" />
      </div>
    </div>

    <!-- 热门图集 -->
    <div class="section">
      <div class="section-header">
        <h2 class="section-title">热门图片</h2>
        <p class="section-description">当前最受欢迎的图片</p>
      </div>
      <PictureList1
        :dataList="popularPictures"
        :hasMore="false"
        :loadingMore="loadingPopular"
        @pictureClick="handlePictureClick"
      />

      <!-- 热门收藏夹（在热门图片下展示） -->
      <div class="album-content">
        <PictureAlbumList
          :album-list="hotAlbums"
          :album-recent-pictures="albumRecentPictures"
          @view-album="goToAlbumDetail"
        />

        <!-- 空状态 -->
        <a-empty v-if="!loadingHot && hotAlbums.length === 0" description="暂无热门收藏夹" />
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import PictureList1 from '@/components/PictureList1.vue'
import PictureAlbumList from '@/components/PictureAlbumList.vue'
import PictureDetailPage from '@/pages/user/PictureDetailPage.vue'
import { CloseOutlined, FolderOpenOutlined, PictureOutlined } from '@ant-design/icons-vue'
import type * as API from '@/api/typings'
import {
  getFeaturedAlbumsUsingGet,
  getRecommendedAlbumsUsingGet,
  listPicturesInAlbumUsingGet,
  listSystemHotPictureAlbumsUsingGet,
  listPicturesInAlbumByCursorUsingGet
} from '@/api/pictureAlbumController'

const router = useRouter()

// 图片详情遮罩相关
const showPictureDetail = ref(false)
const selectedPictureId = ref<number | string>('')

// 处理图片点击事件
const handlePictureClick = (picture: API.PictureVO) => {
  selectedPictureId.value = picture.id || ''
  showPictureDetail.value = true
}

// 关闭图片详情遮罩
const closePictureDetail = () => {
  showPictureDetail.value = false
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

// 定义数据
const featuredAlbums = ref<API.PictureAlbum[]>([]) // 每日精选收藏夹
const recommendedAlbums = ref<API.PictureAlbum[]>([]) // 为你推荐收藏夹
const recommendedPictures = ref<API.PictureVO[]>([]) // 推荐图片
const popularPictures = ref<API.PictureVO[]>([]) // 热门图片
const hotAlbums = ref<API.PictureAlbum[]>([]) // 热门收藏夹
const albumRecentPictures = ref<Record<number, API.PictureVO[]>>({}) // 存储每个收藏夹最近的图片

// 加载状态
const loadingFeatured = ref<boolean>(false)
const loadingRecommended = ref<boolean>(false)
const loadingPopular = ref<boolean>(false)
const loadingHot = ref<boolean>(false) // 热门收藏夹加载状态

// 跳转到收藏夹详情
const goToAlbumDetail = (album: API.PictureAlbum) => {
  router.push(`/user/album/${album.id}`)
}

// 获取每日精选收藏夹
const fetchFeaturedAlbums = async () => {
  loadingFeatured.value = true
  try {
    const res = await getFeaturedAlbumsUsingGet({
      limit: 10
    })
    if (res.data?.code === 0 && res.data?.data) {
      featuredAlbums.value = res.data.data

      // 获取每个收藏夹的最近图片
      await Promise.all(featuredAlbums.value.map(album => fetchAlbumRecentPictures(album)))
    } else {
      throw new Error(res.data?.message || '获取每日精选收藏夹失败')
    }
  } catch (error: any) {
    console.error('获取每日精选收藏夹失败:', error)
    message.error('获取每日精选收藏夹失败')
  } finally {
    loadingFeatured.value = false
  }
}

// 获取为你推荐收藏夹
const fetchRecommendedAlbums = async () => {
  loadingRecommended.value = true
  try {
    const res = await getRecommendedAlbumsUsingGet({
      limit: 10
    })
    if (res.data?.code === 0 && res.data?.data) {
      recommendedAlbums.value = res.data.data

      // 获取每个收藏夹的最近图片
      await Promise.all(recommendedAlbums.value.map(album => fetchAlbumRecentPictures(album)))
    } else {
      throw new Error(res.data?.message || '获取推荐收藏夹失败')
    }
  } catch (error: any) {
    console.error('获取推荐收藏夹失败:', error)
    message.error('获取推荐收藏夹失败')
  } finally {
    loadingRecommended.value = false
  }
}

// 获取热门收藏夹
const fetchHotAlbums = async () => {
  loadingHot.value = true
  try {
    const res = await listSystemHotPictureAlbumsUsingGet({
      limit: 10
    })
    if (res.data?.code === 0 && res.data?.data) {
      hotAlbums.value = res.data.data

      // 获取每个收藏夹的最近图片
      await Promise.all(hotAlbums.value.map(album => fetchAlbumRecentPictures(album)))
    } else {
      throw new Error(res.data?.message || '获取热门收藏夹失败')
    }
  } catch (error: any) {
    console.error('获取热门收藏夹失败:', error)
    message.error('获取热门收藏夹失败')
  } finally {
    loadingHot.value = false
  }
}

// 获取收藏夹最近的图片
const fetchAlbumRecentPictures = async (album: API.PictureAlbum) => {
  if (!album.id) return

  try {
    // 使用支持userId参数的API来获取其他用户的收藏夹图片
    const params: API.listPicturesInAlbumByCursorUsingGETParams = {
      albumId: album.id,
      pageSize: 3,
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

// 获取所有数据
const fetchData = async () => {
  await Promise.all([
    fetchFeaturedAlbums(),
    fetchRecommendedAlbums(),
    fetchHotAlbums()
  ])
}

// 页面加载时获取数据
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#explorePage {
  max-width: 90vw;
  margin: 0 auto;
  padding: 24px 0;
}

.explore-header {
  text-align: center;
  margin-bottom: 32px;
}

.explore-title {
  font-size: 32px;
  font-weight: bold;
  color: #000;
  margin-bottom: 8px;
}

.explore-subtitle {
  font-size: 16px;
  color: #333;
  margin: 0;
}

.section {
  margin-bottom: 48px;
}

.section-header {
  margin-bottom: 24px;
}

.section-title {
  font-size: 24px;
  font-weight: bold;
  color: #000;
  margin-bottom: 8px;
}

.section-description {
  font-size: 16px;
  color: #333;
  margin: 0;
}

/* 收藏夹列表样式 */
.album-content {
  margin-top: 20px;
}

.album-item {
  transition: all 0.3s ease;
  cursor: pointer;
  padding: 12px;
  border-radius: 8px;
}

.album-item:hover {
  background-color: #f5f5f5;
}

.album-layout {
  display: flex;
  height: 200px;
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
}

.album-main-image {
  flex: 1;
  padding-right: 8px;
}

.album-sub-images {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding-left: 8px;
}

.album-image-container {
  width: 100%;
  height: 100%;
}

.album-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.album-image.small {
  height: 50%;
}

.album-image-container.small {
  flex: 1;
}

.album-image-container.small:first-child {
  margin-bottom: 8px;
}

.album-image-placeholder {
  background-color: #e6f7ff;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  transition: all 0.3s ease;
}

.album-image-placeholder.small {
  flex: 1;
}

.album-image-placeholder.small:first-child {
  margin-bottom: 8px;
}

.album-item:hover .album-image-placeholder {
  opacity: 0.7;
  background-color: #bae7ff;
}

.album-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.album-name {
  font-size: 16px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 8px;
}

.album-stats {
  display: flex;
  align-items: flex-end;
  font-size: 14px;
  color: #666;
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
}

.picture-detail-content {
  background: #fff;
  border-radius: 8px;
  width: 90%;
  max-width: 1200px;
  max-height: 90vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.picture-detail-header {
  display: flex;
  justify-content: flex-end;
  padding: 12px;
}

.close-button {
  font-size: 20px;
  color: #333;
}

.picture-detail-body {
  flex: 1;
  overflow-y: auto;
}
</style>
