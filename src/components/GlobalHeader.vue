<template>
  <div id="globalHeader">
    <a-row :wrap="false" align="middle">
      <a-col flex="200px">
        <router-link to="/">
          <div class="title-bar">
            <div class="title">MuShan</div>
          </div>
        </router-link>
      </a-col>
      <a-col flex="auto">
        <div class="menu-wrapper">
          <a-menu
            v-model:selectedKeys="current"
            mode="horizontal"
            :items="items"
            @click="doMenuClick"
            class="nav-menu"
          />
        </div>
      </a-col>
      <!-- 用户信息展示栏 -->
      <a-col flex="200px">
        <div class="user-login-status">
          <div
            v-if="loginUserStore.loginUser.id"
            style="display: flex; align-items: center; justify-content: space-between"
          >
            <a-dropdown>
              <a-space class="user-info">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="48" />
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <router-link :to="`/user/profile/${loginUserStore.loginUser.id}`">
                      <UserOutlined />
                      个人信息
                    </router-link>
                  </a-menu-item>
                  <a-menu-item>
                    <router-link to="/my_space">
                      <PieChartOutlined />
                      图片空间
                    </router-link>
                  </a-menu-item>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <!-- 上传按钮 -->
            <a-button
              class="upload-button"
              type="primary"
              @click="goToUpload"
              style="margin-left: 12px"
              size="large"
            >
              上传
            </a-button>
          </div>
          <div v-else>
            <a-button class="login-button" type="primary" href="/user/login" size="large"
              >登录</a-button
            >
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import { HomeOutlined, PieChartOutlined, LogoutOutlined, UserOutlined, SettingOutlined, CompassOutlined, EditOutlined } from '@ant-design/icons-vue'
import { MenuProps, message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { userLogoutUsingPost } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/userLoginUserStore'

const loginUserStore = useLoginUserStore()

// 未经过滤的菜单项（已移除创建图片选项）
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/explore',
    icon: () => h(CompassOutlined),
    label: '探索',
    title: '探索',
    children: [
      {
        key: '/explore/pictures',
        label: '发现图片',
        title: '发现图片',
      },
      {
        key: '/explore/authors',
        label: '热门作者',
        title: '热门作者',
      },
    ],
  },
  {
    key: '/text_to_image',
    icon: () => h(EditOutlined),
    label: '创作',
    title: '创作',
  },
  // {
  //   key: '/add_picture',
  //   label: '创建图片',
  //   title: '创建图片',
  // },
  {
    key: 'admin',
    label: '管理',
    title: '管理',
    icon: () => h(SettingOutlined),
    children: [
      {
        key: '/admin/userManage',
        label: '用户管理',
        title: '用户管理',
      },
      {
        key: '/admin/pictureManage',
        label: '图片管理',
        title: '图片管理',
      },
      {
        key: '/admin/spaceManage',
        label: '空间管理',
        title: '空间管理',
      },
    ],
  },
]

// 根据权限过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    // 管理员才能看到 /admin 开头的菜单
    if (menu?.key === 'admin') {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
      // 过滤子菜单项
      menu.children = menu.children?.filter((child) => {
        if (child?.key?.startsWith('/admin')) {
          return loginUser.userRole === 'admin'
        }
        return true
      })
      return menu.children && menu.children.length > 0
    }

    if (menu?.key?.startsWith('/admin')) {
      const loginUser = loginUserStore.loginUser
      if (!loginUser || loginUser.userRole !== 'admin') {
        return false
      }
    }
    return true
  })
}

// 展示在菜单的路由数组
const items = computed(() => filterMenus(originItems))

const router = useRouter()
// 当前要高亮的菜单项
const current = ref<string[]>([])
// 监听路由变化，更新高亮菜单项
router.afterEach((to, from, next) => {
  current.value = [to.path]
})

// 路由跳转事件
const doMenuClick = ({ key }) => {
  router.push({
    path: key,
  })
}

// 用户注销
const doLogout = async () => {
  const res = await userLogoutUsingPost()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

// 跳转到上传页面
const goToUpload = () => {
  router.push('/add_picture')
}
</script>

<style scoped>
#globalHeader {
  max-width: 90vw;
  margin: 0 auto;
}

#globalHeader .title-bar {
  height: 56px;
  width: 100%;
  display: flex;
  align-items: center;
}

.title {
  color: black;
  font-size: 28px;
  font-family: 'LogoFont';
  display: flex;
  align-items: center;
  height: 100%;
}

.menu-wrapper {
  display: flex;
  justify-content: center;
  width: 100%;
  overflow: visible;
}

.nav-menu {
  font-size: 18px;
  border-bottom: none !important;
  width: 100%;
  justify-content: center;
}

/* 菜单项样式 */
.nav-menu :deep(.ant-menu-item) {
  color: #5d5d5d;
}

/* 高亮的菜单项样式 */
.nav-menu :deep(.ant-menu-item-selected) {
  color: black !important;
}

/* 高亮菜单项的底部横线 */
.nav-menu :deep(.ant-menu-item-selected::after) {
  border-bottom: 2px solid black !important;
}

/* 鼠标悬停时菜单项的底部横线 */
.nav-menu :deep(.ant-menu-item:hover::after) {
  border-bottom: 2px solid black !important;
}

/* 子菜单样式 */
.nav-menu :deep(.ant-menu-submenu-title) {
  color: #5d5d5d;
}

.nav-menu :deep(.ant-menu-submenu-title:hover) {
  color: black;
}


/* 添加这一条规则使子菜单在hover时也有黑色下划线 */
.nav-menu :deep(.ant-menu-submenu:hover::after) {
  border-bottom: 2px solid black !important;
}

/* 当子菜单项被悬停时，保持父级菜单项的黑色下划线 */
.nav-menu :deep(.ant-menu-submenu-open::after) {
  border-bottom: 2px solid black !important;
}

/* 子菜单选中时显示黑色下划线 */
.nav-menu :deep(.ant-menu-submenu-selected::after) {
  border-bottom: 2px solid black !important;
}

/* 子菜单标题选中时显示黑色下划线 */
.nav-menu :deep(.ant-menu-submenu-active::after) {
  border-bottom: 2px solid black !important;
}

.user-info {
  display: flex;
  align-items: center;
  justify-content: start;
}

.user-login-status {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.upload-button {
  background-color: #000000;
}

.upload-button:hover {
  background-color: #5d5d5d;
}

.login-button {
  background-color: #000000;
}
.login-button:hover {
  background-color: #5d5d5d;
}

@font-face {
  font-family: 'LogoFont';
  src: url('../assets/fonts/Logo1.woff2');
}

/* 添加媒体查询确保在小屏幕上也能正常显示 */
@media (max-width: 768px) {
  #globalHeader .title-bar {
    justify-content: flex-start;
  }

  .nav-menu {
    font-size: 16px;
  }

  #globalHeader {
    max-width: 95vw;
  }
}
</style>
