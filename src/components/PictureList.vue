<template>
  <div class="picture-list">
    <!-- 图片列表 -->
    <a-list
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }"
      :data-source="dataList"
      :loading="loading"
    >
      <template #renderItem="{ item: picture }">
        <a-list-item style="padding: 0">
          <!-- 单张图片 -->
          <a-card hoverable @click="doClickPicture(picture)" class="picture-card">
            <template #cover>
              <div class="image-container">
                <img
                  :alt="picture.name"
                  :src="picture.thumbnailUrl ?? picture.url"
                  class="picture-image"
                />
              </div>
            </template>
            <a-card-meta :title="picture.name" class="picture-meta">
              <template #description>
                <a-flex>
                  <a-tag color="green" class="picture-tag">
                    {{ picture.category ?? '默认' }}
                  </a-tag>
                  <a-tag v-for="tag in picture.tags" :key="tag" class="picture-tag">
                    {{ tag }}
                  </a-tag>
                </a-flex>
              </template>
            </a-card-meta>
            <template v-if="showOp" #actions>
              <SearchOutlined @click="(e) => doSearch(picture, e)" />
              <EditOutlined v-if="canEdit" @click="(e) => doEdit(picture, e)" />
              <DeleteOutlined v-if="canDelete" @click="(e) => doDelete(picture, e)" />
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import {
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  ShareAltOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { ref } from 'vue'
import { deletePictureUsingPost } from '@/api/pictureController'

interface Props {
  dataList?: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  canEdit?: boolean
  canDelete?: boolean
  onReload?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  canEdit: false,
  canDelete: false,
})

const router = useRouter()
// 跳转至图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

// 搜索
const doSearch = (picture, e) => {
  // 阻止冒泡
  e.stopPropagation()
  // 打开新的页面
  window.open(`/search_picture?pictureId=${picture.id}`)
}

// 编辑
const doEdit = (picture, e) => {
  // 阻止冒泡
  e.stopPropagation()
  // 跳转时一定要携带 spaceId
  router.push({
    path: '/add_picture',
    query: {
      id: picture.id,
      spaceId: picture.spaceId,
    },
  })
}

// 删除数据
const doDelete = async (picture, e) => {
  // 阻止冒泡
  e.stopPropagation()
  const id = picture.id
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    props.onReload?.()
  } else {
    message.error('删除失败')
  }
}
</script>

<style scoped>
.picture-card {
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.picture-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.image-container {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  height: 180px;
  background-color: #f5f5f5;
}

.picture-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.picture-card:hover .picture-image {
  transform: scale(1.05);
}

.picture-meta {
  padding: 12px 0;
}

.picture-tag {
  border-radius: 6px;
  font-size: 12px;
  margin-bottom: 4px;
}

/* 操作按钮样式 */
.picture-card :deep(.ant-card-actions) {
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}
</style>
