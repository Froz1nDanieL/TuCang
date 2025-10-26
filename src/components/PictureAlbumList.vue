<template>
  <div class="album-list">
    <a-row :gutter="[16, 16]">
      <a-col
        v-for="album in albumList"
        :key="album.id"
        :xs="24"
        :sm="12"
        :md="8"
      >
        <div class="album-item" @click="viewAlbum(album)">
          <div class="album-layout">
            <!-- 左侧大图 -->
            <div class="album-main-image">
              <div
                v-if="albumRecentPictures[album.id] && albumRecentPictures[album.id].length > 0"
                class="album-image-container"
              >
                <img
                  :src="albumRecentPictures[album.id][0].thumbnailUrl"
                  alt="最近收藏图片"
                  class="album-image"
                />
              </div>
              <div v-else class="album-image-placeholder">
                <FolderOpenOutlined style="font-size: 32px; color: #1890ff" />
              </div>
            </div>

            <!-- 右侧小图 -->
            <div class="album-sub-images">
              <template
                v-for="index in 2"
                :key="index"
              >
                <div
                  v-if="albumRecentPictures[album.id] && albumRecentPictures[album.id].length > index"
                  class="album-image-container small"
                >
                  <img
                    :src="albumRecentPictures[album.id][index].url"
                    alt="最近收藏图片"
                    class="album-image"
                  />
                </div>
                <div v-else class="album-image-placeholder small"></div>
              </template>
            </div>
          </div>

          <div class="album-info">
            <div class="album-name">
              {{ album.name }}
              <LockOutlined v-if="album.isPublic === 0" class="private-icon" />
            </div>
            <div class="album-stats">
              <PictureOutlined />
              <span style="padding-left: 10px" class="album-count">{{ album.pictureCount || 0 }}</span>
            </div>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { FolderOpenOutlined, PictureOutlined, LockOutlined } from '@ant-design/icons-vue'
import type * as API from '@/api/typings'
import { ref } from 'vue'

const props = defineProps<{
  albumList: API.PictureAlbum[],
  albumRecentPictures: Record<number, API.PictureVO[]>
}>()

const emit = defineEmits<{
  (e: 'viewAlbum', album: API.PictureAlbum): void
}>()

const viewAlbum = (album: API.PictureAlbum) => {
  emit('viewAlbum', album)
}
</script>

<style scoped>
.album-item {
  transition: all 0.3s ease;
  cursor: pointer;
  padding: 12px;
}

.album-layout {
  display: flex;
  height: 280px;
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

.album-sub-images .album-image-container.small {
  height: calc(50% - 4px);
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

.private-icon {
  margin-left: 8px;
  color: #666;
  font-size: 14px;
}

.album-stats {
  display: flex;
  align-items: flex-end;
  font-size: 14px;
  color: #666;
}
</style>
