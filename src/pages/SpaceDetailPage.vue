<template>
  <div id="spaceDetailPage">
    <!-- 空间信息 -->
    <div class="space-header">
      <div class="space-title">
        <h2>{{ space.spaceName }}（{{ SPACE_TYPE_MAP[space.spaceType] }}）</h2>
      </div>
      <div class="space-actions">
        <a-space size="middle">
          <a-button
            v-if="canUploadPicture"
            type="primary"
            :href="`/add_picture?spaceId=${id}`"
            target="_blank"
            class="action-button"
          >
            + 创建图片
          </a-button>
          <a-button
            v-if="space.spaceType === SPACE_TYPE_ENUM.TEAM && canManageSpaceUser"
            type="primary"
            ghost
            :icon="h(TeamOutlined)"
            :href="`/spaceUserManage/${id}`"
            target="_blank"
            class="action-button"
          >
            成员管理
          </a-button>
          <a-button
            v-if="canManageSpaceUser"
            type="primary"
            ghost
            :icon="h(BarChartOutlined)"
            :href="`/space_analyze?spaceId=${id}`"
            target="_blank"
            class="action-button"
          >
            空间分析
          </a-button>
          <a-button 
            v-if="canEditPicture" 
            :icon="h(EditOutlined)" 
            @click="doBatchEdit"
            class="action-button"
          >
            批量编辑
          </a-button>
          <a-tooltip
            :title="`占用空间 ${formatSize(space.totalSize)} / ${formatSize(space.maxSize)}`"
          >
            <a-progress
              type="circle"
              :size="42"
              :percent="((space.totalSize * 100) / space.maxSize).toFixed(1)"
              class="space-progress"
            />
          </a-tooltip>
        </a-space>
      </div>
    </div>
    
    <div class="divider"></div>
    
    <!-- 搜索表单 -->
    <div class="search-section">
      <PictureSearchForm :onSearch="onSearch" />
    </div>
    
    <div class="content-section">
      <!-- 图片列表 -->
      <PictureList
        :dataList="dataList"
        :loading="loading"
        :showOp="true"
        :canEdit="canEditPicture"
        :canDelete="canDeletePicture"
        :onReload="fetchData"
      />
    </div>
    
    <!-- 分页 -->
    <div class="pagination-section">
      <a-pagination
        v-model:current="searchParams.current"
        v-model:pageSize="searchParams.pageSize"
        :total="total"
        @change="onPageChange"
        show-less-items
      />
    </div>
    
    <BatchEditPictureModel
      ref="batchEditPictureModalRef"
      :spaceId="id"
      :pictureList="dataList"
      :onSuccess="onBatchEditPictureSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'

import PictureList from '@/components/PictureList.vue'
import { BarChartOutlined, EditOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController'
import { SPACE_PERMISSION_ENUM, SPACE_TYPE_ENUM, SPACE_TYPE_MAP } from '@/constants/space'
import { listPictureVoByPageUsingPost } from '@/api/pictureController'
import { formatSize } from '@/utils'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import BatchEditPictureModel from '@/components/BatchEditPictureModel.vue'

interface Props {
  id: string | number
}

const props = defineProps<Props>()
const space = ref<API.SpaceVO>({})
const router = useRouter()
const route = useRoute()

// 获取全局侧边栏组件实例
const globalSider = ref(null)

// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (space.value.permissionList ?? []).includes(permission)
  })
}

// 定义权限检查
const canManageSpaceUser = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

// -------- 获取空间详情 --------
const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({
      id: props.id,
    })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
    } else {
      message.error('获取空间详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取空间详情失败：' + e.message)
  }
}

onMounted(() => {
  fetchSpaceDetail()
})

// --------- 获取图片列表 --------

// 定义数据
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

// 搜索条件
const searchParams = ref<API.PictureQueryRequest>({
  current: 1,
  pageSize: 12,
  sortField: 'createTime',
  sortOrder: 'descend',
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    spaceId: props.id,
    ...searchParams.value,
  }
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
})

// 分页参数
const onPageChange = (page: number, pageSize: number) => {
  searchParams.value.current = page
  searchParams.value.pageSize = pageSize
  fetchData()
}

// 搜索
const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  console.log('new', newSearchParams)

  searchParams.value = {
    ...searchParams.value,
    ...newSearchParams,
    current: 1,
  }
  console.log('searchparams', searchParams.value)
  fetchData()
}


// ---- 批量编辑图片 -----
const batchEditPictureModalRef = ref()

// 批量编辑图片成功
const onBatchEditPictureSuccess = () => {
  fetchData()
}

// 打开批量编辑图片弹窗
const doBatchEdit = () => {
  if (batchEditPictureModalRef.value) {
    batchEditPictureModalRef.value.openModal()
  }
}

// 空间 id 改变时，必须重新获取数据
watch(
  () => props.id,
  (newSpaceId) => {
    fetchSpaceDetail()
    fetchData()
  },
)
</script>

<style scoped>
#spaceDetailPage {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.space-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 20px;
}

.space-title h2 {
  margin: 0;
  font-size: 1.8rem;
  font-weight: 600;
  color: #000;
}

.space-actions {
  display: flex;
  align-items: center;
}

.action-button {
  border-radius: 8px;
  font-size: 14px;
}

.action-button:hover {
  transform: translateY(-2px);
  transition: transform 0.2s ease;
}

.space-progress :deep(.ant-progress-text) {
  font-size: 12px !important;
}

.divider {
  height: 1px;
  background-color: #f0f0f0;
  margin: 20px 0;
}

.search-section {
  margin-bottom: 30px;
}

.search-section :deep(.ant-form) {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 12px;
}

.content-section {
  margin-bottom: 30px;
}

.pagination-section {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .space-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .space-actions {
    width: 100%;
    justify-content: center;
  }
  
  #spaceDetailPage {
    padding: 15px;
  }
  
  .space-title h2 {
    font-size: 1.5rem;
  }
}
</style>
