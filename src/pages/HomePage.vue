<template>
  <div id="homePage" ref="homePageRef">
    <!-- 搜索框 -->
    <div class="search-container">
      <div class="search-title">木杉图仓 — “探索图片的美”</div>
      <!-- 分类 -->
      <div class="category-bar">
        <div class="category-list">
          <div
            v-for="category in categoryListWithAll"
            :key="category.value"
            :class="['category-item', { active: selectedCategory === category.value }]"
            @click="selectCategory(category.value)"
          >
            {{ category.label }}
          </div>
        </div>
      </div>
      <div class="search-input-wrapper">
        <a-input
          placeholder="从海量图片中搜索"
          v-model:value="searchParams.searchText"
          size="large"
          @pressEnter="doSearch"
          class="search-input"
        >
          <template #suffix>
            <search-outlined @click="doSearch" class="search-icon" />
          </template>
        </a-input>
      </div>
      <!-- 标签 -->
      <div class="tag-bar">
        <div class="tag-list">
          <a-checkable-tag
            v-for="(tag, index) in tagList"
            :key="tag"
            v-model:checked="selectedTagList[index]"
            @change="doSearch"
            class="tag-item"
            :class="{ 'tag-item-checked': selectedTagList[index] }"
          >
            {{ tag }}
          </a-checkable-tag>
        </div>
      </div>
    </div>

    <!-- 图片列表 -->
    <PictureList1
      :dataList="dataList"
      :hasMore="hasMore"
      :loadingMore="loadingMore"
      @loadMore="handleLoadMore"
      @pictureClick="handlePictureClick"
      @pictureUpdate="handlePictureUpdate"
    />

    <!-- 加载指示器 -->
    <div class="loading-more-container" v-if="loadingMore">
      <div class="dot-loading-animation">
        <span class="dot"></span>
        <span class="dot"></span>
        <span class="dot"></span>
      </div>
    </div>
    <div class="no-more-container" v-else-if="!hasMore && dataList.length > 0">
      <span class="no-more-text">没有更多图片了</span>
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
          <PictureDetailPage :id="selectedPictureId" @picture-update="handlePictureUpdate" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref, watch, computed } from 'vue'
import { listPictureTagCategoryUsingGet, listPictureVoByCursorUsingPost } from '@/api/pictureController'
import { message } from 'ant-design-vue'
import PictureList1 from '@/components/PictureList1.vue'
import PictureDetailPage from '@/pages/user/PictureDetailPage.vue'
import { CloseOutlined, SearchOutlined } from '@ant-design/icons-vue'

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

// 处理图片更新事件
const handlePictureUpdate = (updatedPicture: API.PictureVO) => {
  // 更新列表中的对应图片数据
  const index = dataList.value.findIndex(p => p.id === updatedPicture.id);
  if (index !== -1) {
    dataList.value[index] = { ...dataList.value[index], ...updatedPicture };
  }
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
const dataList = ref<API.PictureVO[]>([])
const nextCursorId = ref<number | undefined>(undefined)
const hasMore = ref<boolean>(true)
const loadingMore = ref<boolean>(false)

// 搜索条件
const searchParams = reactive<API.PictureCursorQueryRequest>({
  pageSize: 16,
  sortField: 'id',
  sortOrder: 'descend',
})

// 获取数据
const fetchData = async (isLoadMore = false) => {
  // 如果正在加载中，则不重复发起请求
  if (loadingMore.value) {
    return
  }

  // 设置加载状态
  if (isLoadMore) {
    loadingMore.value = true
  }

  // 转换搜索参数
  const params = {
    ...searchParams,
    tags: [] as string[],
  }
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  // [true, false, false] => ['java']
  selectedTagList.value.forEach((useTag, index) => {
    if (useTag) {
      params.tags.push(tagList.value[index])
    }
  })

  // 添加游标ID（如果有）
  if (nextCursorId.value) {
    params.cursorId = nextCursorId.value
  }

  try {
    const res = await listPictureVoByCursorUsingPost(params)
    if (res.data.code === 0 && res.data.data) {
      const pictureList = res.data.data.pictureList ?? []

      // 如果是加载更多，追加数据；否则替换数据
      if (isLoadMore) {
        dataList.value = [...dataList.value, ...pictureList]
      } else {
        dataList.value = pictureList
      }

      // 更新游标和是否有更多数据
      nextCursorId.value = res.data.data.nextCursorId

      // 判断是否还有更多数据
      if (res.data.data.hasOwnProperty('hasNext')) {
        hasMore.value = res.data.data.hasMore ?? false
      } else {
        hasMore.value = pictureList.length >= (params.pageSize ?? 16)
      }
    } else {
      message.error('获取数据失败，' + res.data.message)
      hasMore.value = false
    }
  } catch (error) {
    console.error('获取数据失败:', error)
    message.error('获取数据失败')
    hasMore.value = false
  } finally {
    // 重置加载状态
    loadingMore.value = false
  }
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
  // 添加滚动事件监听器
  window.addEventListener('scroll', handleScroll)
})

// 组件卸载时移除滚动事件监听器和恢复页面滚动
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  // 确保页面滚动被恢复
  document.body.style.overflow = ''
})

// 搜索
const doSearch = () => {
  // 重置搜索条件
  searchParams.cursorId = undefined
  nextCursorId.value = undefined
  hasMore.value = true
  fetchData()
}

// 处理加载更多
const handleLoadMore = () => {
  // 只有在有更多数据且未在加载中时才触发加载
  if (hasMore.value && !loadingMore.value) {
    fetchData(true)
  }
}

// 处理滚动事件
let scrollTimer: number | null = null
const handleScroll = () => {
  // 防抖处理
  if (scrollTimer) {
    clearTimeout(scrollTimer)
  }

  scrollTimer = window.setTimeout(() => {
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
    const scrollHeight = document.documentElement.scrollHeight || document.body.scrollHeight
    const clientHeight = document.documentElement.clientHeight || window.innerHeight

    // 距离底部50px时加载更多
    if (scrollHeight - scrollTop - clientHeight < 30) {
      handleLoadMore()
    }
  }, 100)
}

// 标签和分类列表
const categoryList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const tagList = ref<string[]>([])
const selectedTagList = ref<boolean[]>([])

// 包含"全部"选项的分类列表
const categoryListWithAll = computed(() => {
  return [{ label: '全部', value: 'all' }, ...categoryList.value.map(category => ({ label: category, value: category }))]
})

// 选择分类
const selectCategory = (category: string) => {
  selectedCategory.value = category
  doSearch()
}

/**
 * 获取标签和分类选项
 * @param values
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagList.value = res.data.data.tagList ?? []
    categoryList.value = res.data.data.categoryList ?? []
    // 初始化标签选择状态
    selectedTagList.value = new Array(tagList.value.length).fill(false)
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}

onMounted(() => {
  getTagCategoryOptions()
})

</script>

<style scoped>
#homePage {
  max-width: 90vw;
  margin: 0 auto;
  padding: 20px 0;
}

/* 搜索区域样式 */
.search-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
  padding: 40px 20px;
}

.search-title {
  font-size: 2.2rem;
  font-weight: 700;
  margin-bottom: 20px;
  color: #000000;
  letter-spacing: 1px;
}

.search-input-wrapper {
  width: 100%;
  max-width: 700px;
  margin: 20px 0;
}

.search-input {
  background-color: #f5f5f5;
  border: none;
  border-radius: 15px;
  font-size: 16px;
  padding: 16px 20px;
  height: auto;
}

.search-input :deep(.ant-input) {
  background-color: #f5f5f5;
  border: none;
  font-size: 16px;
  padding: 0;
  height: 24px;
  line-height: 24px;
}

.search-input :deep(.ant-input:focus) {
  background-color: #f5f5f5;
  box-shadow: none;
  border: none;
}

.search-input:focus {
  background-color: #f5f5f5;
  box-shadow: none;
  border: none;
}

.search-icon {
  color: #8c8c8c;
  font-size: 20px;
  cursor: pointer;
}

.search-icon:hover {
  color: #000000;
}

.search-subtitle {
  margin: 14px 0;
  font-size: 0.95rem;
  color: #555555;
  font-weight: 400;
}

/* 分类区域样式 */
.category-bar {
  display: flex;
  flex-wrap: wrap;
  margin: 10px 0;
}

.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.category-item {
  padding: 6px 16px;
  background: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 20px;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.3s;
}

.category-item:hover {
  border-color: #d0d0d0;
}

.category-item.active {
  background: #000000;
  border-color: #000000;
  color: white;
}

/* 标签区域样式 */
.tag-bar {
  display: flex;
  flex-wrap: wrap;
  margin-top: 20px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-item {
  margin: 0;
  border-radius: 12px;
  padding: 3px 10px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(232, 232, 232, 0.7);
  font-size: 0.85rem;
  backdrop-filter: blur(5px);
  color: #333333 !important;
  transition: all 0.3s ease;
}

.tag-item:hover {
  border-color: #d0d0d0;
  color: #333333 !important;
}

.tag-item-checked {
  background: #000000 !important;
  border-color: #000000 !important;
  color: white !important;
}

.tag-item-checked:hover {
  background: #333333 !important;
  border-color: #333333 !important;
  color: white !important;
}

/* 筛选区域样式 */
.filter-container {
  background: #ffffff;
  margin-bottom: 30px;
  padding: 20px 0;
  border: none;
}

.no-more-container {
  text-align: center;
  padding: 20px;
  color: #999;
}

/* 加载更多动画样式 */
.loading-more-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 30px;
}

.dot-loading-animation {
  display: flex;
  justify-content: center;
  align-items: center;
}

.dot {
  width: 16px;
  height: 16px;
  margin: 0 4px;
  background-color: #000000;
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
    background-color: #000000;
  }
}

/* 图片详情遮罩样式 */
.picture-detail-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.7);
  z-index: 1000;
  backdrop-filter: blur(10px);
  overflow-y: auto;
  padding-top: 5vh;
}

.picture-detail-content {
  border-radius: 20px;
  background: white;
  width: 90vw;
  margin: 0 auto;
  overflow: hidden;
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
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
