<template>
  <div class="user-profile-page">
    <div class="profile-container">
      <div class="profile-header">
        <a-avatar :size="140" :src="userInfo.userAvatar" class="user-avatar" />
        <h1 class="user-name">{{ userInfo.userName }}</h1>
        <p class="user-profile">{{ userInfo.userProfile || '这个用户很懒，还没有介绍自己' }}</p>
        <a-button v-if="isCurrentUser" type="primary" @click="showEditModal" class="edit-button"><EditOutlined />编辑个人信息</a-button>
      </div>

      <!-- 新增的按钮 -->
      <div class="profile-actions">
        <span
          class="action-link"
          :class="{ active: activeTab === 'gallery' }"
          @click="loadUserPictures"
        >
          画廊
        </span>
        <span
          class="action-link"
          :class="{ active: activeTab === 'liked' }"
          @click="loadLikedPictures"
        >
          点赞
        </span>
        <span
          class="action-link"
          :class="{ active: activeTab === 'album' }"
          @click="loadAlbums"
        >
          收藏
        </span>
      </div>

      <!-- 按钮下方的细线 -->
      <div class="divider"></div>

      <!-- 图片展示区域 (画廊/点赞) -->
      <div class="picture-content" v-show="activeTab === 'gallery' || activeTab === 'liked'">
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

      <!-- 收藏夹展示区域 -->
      <div class="album-content" v-show="activeTab === 'album'">
        <div class="album-header" v-show="isCurrentUser">
          <h3>我的收藏夹</h3>
          <a-button type="primary" @click="showCreateAlbumModal">
            <template #icon>
              <PlusOutlined />
            </template>
            创建收藏夹
          </a-button>
        </div>

        <!-- 收藏夹列表 -->
        <PictureAlbumList
          :album-list="albumList"
          :album-recent-pictures="albumRecentPictures"
          @view-album="viewAlbum"
        />

        <!-- 空状态 -->
        <a-empty v-if="!albumLoading && albumList.length === 0" description="暂无收藏夹">
          <a-button type="primary" @click="showCreateAlbumModal">创建收藏夹</a-button>
        </a-empty>
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

      <!-- 编辑个人信息弹窗 -->
      <div v-if="editModalVisible" class="modal-overlay" @click="handleEditCancel">
        <div class="modal-container" @click.stop>
          <div class="modal-header">
            <h2>编辑个人信息</h2>
            <button class="close-button" @click="handleEditCancel">×</button>
          </div>
          <div class="modal-body">
            <a-form :model="formState" layout="vertical">
              <div class="form-group">
                <label for="userAvatar">头像</label>
                <div class="avatar-upload-container">
                  <a-upload
                    name="avatar"
                    list-type="picture-card"
                    class="avatar-uploader"
                    :show-upload-list="false"
                    :before-upload="beforeUpload"
                    @change="handleAvatarChange"
                  >
                    <img v-if="formState.userAvatar" :src="formState.userAvatar" alt="avatar" class="avatar-preview" />
                    <div v-else class="avatar-upload-placeholder">
                      <div class="ant-upload-text">上传头像</div>
                    </div>
                  </a-upload>
                </div>
              </div>

              <div class="form-group">
                <label for="userName">
                  昵称
                  <span class="required-star">*</span>
                </label>
                <a-input
                  id="userName"
                  v-model:value="formState.userName"
                  class="form-input"
                  :class="{ 'required-empty': !formState.userName }"
                />
              </div>

              <div class="form-group">
                <label for="userProfile">个人简介</label>
                <a-textarea
                  id="userProfile"
                  v-model:value="formState.userProfile"
                  class="form-textarea"
                  :rows="4"
                />
              </div>

              <div class="modal-footer">
                <button type="button" class="cancel-button" @click="handleEditCancel">取消</button>
                <button
                  type="button"
                  class="submit-button"
                  :disabled="confirmLoading"
                  @click="handleEditOk"
                >
                  {{ confirmLoading ? '保存中...' : '保存' }}
                </button>
              </div>
            </a-form>
          </div>
        </div>
      </div>

      <!-- 创建/编辑收藏夹组件 -->
      <PictureAlbumManager
        ref="albumManagerRef"
        @success="handleAlbumManagerSuccess"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/userLoginUserStore'
import { updateUserUsingPost, getUserVoByIdUsingGet } from '@/api/userController'
import {
  listUserLikedPicturesUsingPost,
  listPictureVoByCursorUsingPost
} from '@/api/pictureController'
import {
  listMyPictureAlbumsUsingGet,
  addPictureAlbumUsingPost,
  updatePictureAlbumUsingPost,
  listPicturesInAlbumUsingGet,
  listUserPublicAlbumsUsingGet, listPicturesInAlbumByCursorUsingGet
} from '@/api/pictureAlbumController'
import PictureList1 from '@/components/PictureList1.vue'
import PictureAlbumList from '@/components/PictureAlbumList.vue'
import PictureDetailPage from '@/pages/user/PictureDetailPage.vue'
import PictureAlbumManager from '@/components/PictureAlbumManager.vue'
import { EditOutlined, CloseOutlined, PlusOutlined, FolderOpenOutlined, PictureOutlined, LockOutlined } from '@ant-design/icons-vue'
import type * as API from '@/api/typings'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 收藏夹管理器组件引用
const albumManagerRef = ref()

// 获取用户ID（可能是当前用户或其他用户）
const userId = ref<string>()  // 修改为字符串类型以避免大整数精度丢失
const isCurrentUser = ref<boolean>(false)
const loadingUser = ref<boolean>(true) // 添加用户加载状态
const userLoadError = ref<boolean>(false) // 添加用户加载错误状态

// 监听路由参数变化
watch(
  () => route.params.id,
  async (newId) => {
    loadingUser.value = true
    userLoadError.value = false
    if (newId) {
      // 使用字符串形式处理大整数ID，避免精度丢失
      const idStr = String(newId);
      if (/^\d+$/.test(idStr)) {
        // 验证是否为有效的数字字符串
        userId.value = idStr
        isCurrentUser.value = userId.value === (loginUserStore.loginUser.id?.toString() || '')
      } else {
        // ID无效
        userId.value = undefined
        isCurrentUser.value = false
        userLoadError.value = true
        loadingUser.value = false
        return
      }
    } else {
      // 如果没有id参数，则显示当前用户信息
      userId.value = loginUserStore.loginUser.id?.toString()
      isCurrentUser.value = true
    }

    // 重新加载用户信息
    const success = await loadUserInfo()
    // 重新加载图片数据
    if (success && userId.value) {
      await loadUserPictures()
    }
    loadingUser.value = false
  },
  { immediate: true }
)

// 编辑弹窗相关状态
const editModalVisible = ref(false)
const confirmLoading = ref(false)

// 图片展示相关状态
const activeTab = ref('gallery') // 当前激活的标签页
const pictureList = ref<API.PictureVO[]>([])
const hasMore = ref(true)
const loadingMore = ref(false)
const nextCursorId = ref<number | undefined>(undefined)
const showPictureDetail = ref(false)
const selectedPictureId = ref<number | string>('')

// 收藏夹相关状态
const albumList = ref<API.PictureAlbum[]>([])
const albumLoading = ref(false)
const albumRecentPictures = ref<Record<number, API.PictureVO[]>>({}) // 存储每个收藏夹最近的图片

// 表单数据
const formState = reactive({
  userAvatar: '',
  userName: '',
  userProfile: ''
})

// 用户信息
const userInfo = ref<API.UserVO>({
  id: undefined,
  userAvatar: '',
  userName: '',
  userProfile: '',
  userRole: 'user'
})

// 加载用户信息
const loadUserInfo = async () => {
  if (!userId.value) {
    message.error('用户ID不能为空')
    userLoadError.value = true
    return false
  }

  try {
    // 使用字符串形式的ID调用API
    const res = await getUserVoByIdUsingGet({ id: userId.value })
    if (res.data.code === 0 && res.data.data) {
      // 直接使用返回的 UserVO 对象
      userInfo.value = res.data.data;

      // 如果是当前用户，更新表单数据
      if (isCurrentUser.value) {
        formState.userAvatar = userInfo.value.userAvatar || ''
        formState.userName = userInfo.value.userName || ''
        formState.userProfile = userInfo.value.userProfile || ''
      }
      userLoadError.value = false
      return true // 返回成功状态
    } else {
      // 根据错误码提供更具体的错误信息
      userLoadError.value = true
      switch (res.data.code) {
        case 404:
          message.error('用户不存在')
          break
        case 401:
          message.error('无权限访问该用户信息')
          break
        case 400:
          message.error('请求参数错误')
          break
        default:
          message.error('获取用户信息失败: ' + (res.data.message || '未知错误'))
      }
      return false // 返回失败状态
    }
  } catch (error: any) {
    console.error('获取用户信息失败:', error)
    userLoadError.value = true
    message.error('网络错误，无法获取用户信息')
    return false // 返回失败状态
  }
}

// 加载用户上传的图片
const loadUserPictures = async () => {
  activeTab.value = 'gallery'
  nextCursorId.value = undefined
  hasMore.value = true
  pictureList.value = []
  await fetchUserPictures()
}

// 加载点赞的图片
const loadLikedPictures = async () => {
  activeTab.value = 'liked'
  nextCursorId.value = undefined
  hasMore.value = true
  pictureList.value = []
  await fetchLikedPictures()
}

// 获取用户上传的图片数据
const fetchUserPictures = async (isLoadMore = false) => {
  if (loadingMore.value || (isLoadMore && !hasMore.value) || !userId.value) {
    return
  }

  loadingMore.value = true

  try {
    const params: API.PictureCursorQueryRequest = {
      pageSize: 16,
      sortField: 'id',
      sortOrder: 'descend',
      userId: userId.value
    }

    if (nextCursorId.value) {
      params.cursorId = nextCursorId.value
    }

    // 使用游标方式获取用户上传的图片
    const res = await listPictureVoByCursorUsingPost(params)

    if (res.data.code === 0 && res.data.data) {
      const pictureListData = res.data.data.pictureList ?? []

      // 如果是加载更多，追加数据；否则替换数据
      if (isLoadMore) {
        pictureList.value = [...pictureList.value, ...pictureListData]
      } else {
        pictureList.value = pictureListData
      }

      // 更新游标和是否有更多数据
      nextCursorId.value = res.data.data.nextCursorId

      // 判断是否还有更多数据
      if (res.data.data.hasOwnProperty('hasMore')) {
        hasMore.value = res.data.data.hasMore ?? false
      } else {
        hasMore.value = pictureListData.length >= (params.pageSize ?? 16)
      }
    } else {
      message.error('获取数据失败，' + (res.data.message || ''))
      hasMore.value = false
    }
  } catch (error: any) {
    console.error('获取数据失败:', error)
    message.error('获取数据失败，可能是网络问题或服务器错误')
    hasMore.value = false
  } finally {
    loadingMore.value = false
  }
}

// 获取点赞的图片数据
const fetchLikedPictures = async (isLoadMore = false) => {
  if (loadingMore.value || (isLoadMore && !hasMore.value) || !userId.value) {
    return
  }

  loadingMore.value = true

  try {
    const params: API.PictureCursorQueryRequest = {
      pageSize: 16,
      sortField: 'id',
      sortOrder: 'descend'
    }

    if (nextCursorId.value) {
      params.cursorId = nextCursorId.value
    }

    // 传入用户ID参数
    const res = await listUserLikedPicturesUsingPost({ userId: userId.value }, params)
    if (res.data.code === 0 && res.data.data) {
      const pictureListData = res.data.data.pictureList ?? []

      // 如果是加载更多，追加数据；否则替换数据
      if (isLoadMore) {
        pictureList.value = [...pictureList.value, ...pictureListData]
      } else {
        pictureList.value = pictureListData
      }

      // 更新游标和是否有更多数据
      nextCursorId.value = res.data.data.nextCursorId

      // 判断是否还有更多数据
      if (res.data.data.hasOwnProperty('hasNext')) {
        hasMore.value = res.data.data.hasMore ?? false
      } else {
        hasMore.value = pictureListData.length >= (params.pageSize ?? 16)
      }
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
  if (activeTab.value === 'gallery') {
    fetchUserPictures(true)
  } else if (activeTab.value === 'liked') {
    fetchLikedPictures(true)
  }
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
  if (picture.id) {
    selectedPictureId.value = picture.id
    showPictureDetail.value = true
  }
}

// 关闭图片详情
const closePictureDetail = () => {
  showPictureDetail.value = false
}

// 显示编辑弹窗
const showEditModal = () => {
  // 只有当前用户才能编辑个人信息
  if (!isCurrentUser.value) {
    message.error('您无法编辑其他用户的个人信息')
    return
  }

  // 重置表单数据为当前用户信息
  formState.userAvatar = userInfo.value.userAvatar || ''
  formState.userName = userInfo.value.userName || ''
  formState.userProfile = userInfo.value.userProfile || ''
  editModalVisible.value = true
}

// 处理头像上传
const handleAvatarChange = (info: any) => {
  if (info.file.status === 'uploading') {
    return
  }
  if (info.file.status === 'done') {
    // 模拟头像上传成功，实际项目中需要调用上传接口
    formState.userAvatar = URL.createObjectURL(info.file.originFileObj)
  }
  if (info.file.status === 'error') {
    message.error('上传头像失败')
  }
}

// 上传前检查
const beforeUpload = (file: any) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isJpgOrPng) {
    message.error('只能上传 JPG/PNG 格式的图片!')
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('图片大小不能超过 2MB!')
  }
  return isJpgOrPng && isLt2M
}

// 处理编辑确认
const handleEditOk = async () => {
  // 只有当前用户才能编辑个人信息
  if (!isCurrentUser.value) {
    message.error('您无法编辑其他用户的个人信息')
    return
  }

  confirmLoading.value = true
  try {
    // 调用更新用户信息接口
    const res = await updateUserUsingPost({
      id: loginUserStore.loginUser.id,
      userAvatar: formState.userAvatar,
      userName: formState.userName,
      userProfile: formState.userProfile
    })

    if (res.data.code === 0) {
      message.success('更新成功')

      // 更新本地存储的用户信息
      loginUserStore.setLoginUser({
        ...loginUserStore.loginUser,
        userAvatar: formState.userAvatar,
        userName: formState.userName,
        userProfile: formState.userProfile
      })

      // 更新页面显示的用户信息
      userInfo.value = {
        ...userInfo.value,
        userAvatar: formState.userAvatar,
        userName: formState.userName,
        userProfile: formState.userProfile
      }

      editModalVisible.value = false
    } else {
      message.error('更新失败: ' + res.data.message)
    }
  } catch (error: any) {
    message.error('更新失败: ' + error.message)
  } finally {
    confirmLoading.value = false
  }
}

// 处理编辑取消
const handleEditCancel = () => {
  editModalVisible.value = false
}

// 加载收藏夹
const loadAlbums = async () => {
  activeTab.value = 'album'
  try {
    albumLoading.value = true
    // 根据是否为当前用户调用不同的接口
    let res
    if (isCurrentUser.value) {
      res = await listMyPictureAlbumsUsingGet()
    } else {
      // 使用listUserPublicAlbumsUsingGet获取其他用户的公开收藏夹
      res = await listUserPublicAlbumsUsingGet({ userId: userId.value })
    }

    if (res.data.code === 0) {
      albumList.value = res.data.data || []

      // 获取每个收藏夹最近的三张图片
      await Promise.all(albumList.value.map(album => fetchAlbumRecentPictures(album)))
    } else {
      message.error(res.data.message || '获取收藏夹列表失败')
    }
  } catch (error: any) {
    message.error(error.message || '获取收藏夹列表失败')
  } finally {
    albumLoading.value = false
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

// 处理收藏夹管理器成功事件
const handleAlbumManagerSuccess = () => {
  loadAlbums(); // 重新加载收藏夹列表
}

// 显示创建收藏夹弹窗
const showCreateAlbumModal = () => {
  // 只有当前用户才能创建收藏夹
  if (!isCurrentUser.value) {
    message.error('您无法创建收藏夹')
    return
  }

  // 调用PictureAlbumManager组件方法
  albumManagerRef.value?.openCreateModal()
}

// 显示编辑收藏夹模态框
const showEditAlbumModal = (album: API.PictureAlbum) => {
  // 调用PictureAlbumManager组件方法
  albumManagerRef.value?.openEditModal(album)
}

// 查看收藏夹详情
const viewAlbum = (album: API.PictureAlbum) => {
  if (album.id) {
    router.push(`/user/album/${album.id}`)
  }
}

onMounted(async () => {
  loadingUser.value = true
  userLoadError.value = false
  // 组件挂载时根据路由参数初始化
  if (route.params.id) {
    // 使用字符串形式处理大整数ID，避免精度丢失
    const idStr = String(route.params.id);
    if (/^\d+$/.test(idStr)) {
      // 验证是否为有效的数字字符串
      userId.value = idStr
      isCurrentUser.value = userId.value === (loginUserStore.loginUser.id?.toString() || '')
    } else {
      // ID无效
      userId.value = undefined
      isCurrentUser.value = false
      userLoadError.value = true
      loadingUser.value = false
      return
    }
  } else {
    // 如果没有id参数，则显示当前用户信息
    userId.value = loginUserStore.loginUser.id?.toString()
    isCurrentUser.value = true
  }

  // 加载用户信息
  const success = await loadUserInfo()
  // 加载用户图片
  if (success && userId.value) {
    await loadUserPictures()
  }
  loadingUser.value = false
})
</script>

<style scoped>
.user-profile-page {
  max-width: 90vw;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.profile-container {
  max-width: 90vw;
  width: 100%;
}

.profile-header {
  text-align: center;
}

.user-avatar {
  margin-bottom: 20px;
}

.user-name {
  font-size: 60px;
  font-weight: 700;
}

.user-profile {
  font-size: 16px;
  margin-bottom: 30px;
  line-height: 1.6;
  min-height: 24px;
}

.edit-button {
  border: none;
  border-radius: 20px;
  font-size: 16px;
  height: 50px;
  background: #2cccda;
}

.edit-button:hover {
  background: #2cccdaaa;
}

.avatar-uploader > .ant-upload {
  width: 128px;
  height: 128px;
}

.ant-upload-text {
  margin-top: 8px;
  color: #666;
}

.action-link {
  height: 40px;
  line-height: 40px;
  width: 6vw;
  margin-right: 3vw;
  border-radius: 20px;
  text-align: center;
  cursor: pointer;
  color: #000;
  font-size: 20px;
  transition: all 0.3s ease;
}

.action-link:hover,
.action-link.active {
  color: #ffffff;
  background-color: #5f5f5f;
}

.profile-actions {
  display: flex;
  justify-content: left;
  align-items: center;
  margin-top: 10vh;
  margin-bottom: 1vh;
}

.divider {
  width: 90vw;
  max-width: 90vw;
  margin: 0 auto;
  border-top: 1px solid #d9d9d9;
}

.picture-content {
  margin-top: 20px;
  min-height: 500px;
}

.album-content {
  margin-top: 20px;
  min-height: 500px;
}

.album-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.album-header h3 {
  margin: 0;
}

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

.no-more-container {
  text-align: center;
  padding: 20px;
  color: #999;
}

.avatar-upload-container {
  display: flex;
  justify-content: center;
}

.avatar-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.avatar-upload-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background-color: #f5f5f5;
}

.avatar-uploader :deep(.ant-upload) {
  width: 128px;
  height: 128px;
  border-radius: 8px;
  border: 2px dashed #d9d9d9;
  background-color: #fafafa;
  transition: all 0.2s ease-in-out;
}

.avatar-uploader :deep(.ant-upload:hover) {
  border-color: #000;
}

.modal-overlay {
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

.modal-container {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  padding: 25px 30px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #000;
  letter-spacing: -0.5px;
}

.close-button {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #666;
  transition: color 0.3s ease;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.close-button:hover {
  color: #000;
  background-color: #f5f5f5;
}

.modal-body {
  padding: 25px 30px;
}

.form-group {
  margin-bottom: 25px;
}

.form-group label {
  display: block;
  font-weight: 600;
  color: #333;
  font-size: 16px;
  margin-bottom: 8px;
}

.required-star {
  color: #ff4d4f;
  margin-left: 4px;
}

.form-input,
.form-textarea {
  width: 100%;
  border: 2px solid #f0f0f0;
  border-radius: 8px;
  transition: all 0.3s ease;
  padding: 12px 15px;
  font-size: 15px;
  box-sizing: border-box;
  font-family: inherit;
}

.form-input:hover,
.form-textarea:hover {
  border-color: #d9d9d9;
}

.form-input:focus,
.form-textarea:focus {
  border-color: #000;
  outline: none;
}

.form-textarea {
  min-height: 100px;
  resize: vertical;
}

.required-empty {
  border-color: #ff4d4f !important;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 10px;
}

.cancel-button,
.submit-button {
  padding: 12px 24px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.cancel-button {
  background-color: #f5f5f5;
  color: #333;
  border: 1px solid #ddd;
}

.cancel-button:hover {
  background-color: #e0e0e0;
  border-color: #ccc;
}

.submit-button {
  background-color: #000;
  color: white;
  border: 1px solid #000;
}

.submit-button:hover {
  background-color: #333;
  border-color: #333;
}

.submit-button:disabled {
  background-color: #d9d9d9;
  border-color: #d9d9d9;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .action-link {
    width: 15vw;
    margin-right: 2vw;
    font-size: 16px;
  }

  .user-name {
    font-size: 36px;
  }

  .modal-container {
    width: 95%;
    margin: 0 10px;
  }
}
</style>
