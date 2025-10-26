<template>
  <div id="globalSider">
    <a-layout-sider
      v-if="loginUserStore.loginUser.id && shouldShowSider"
      width="200"
      breakpoint="lg"
      collapsed-width="0"
    >
      <a-menu
        v-model:selectedKeys="current"
        mode="inline"
        :items="menuItems"
        @click="doMenuClick"
      />
    </a-layout-sider>
  </div>
</template>
<script lang="ts" setup>
import { computed, h, ref, watchEffect } from 'vue'
import { PictureOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/userLoginUserStore'
import { SPACE_TYPE_ENUM, SPACE_ROLE_ENUM } from '@/constants/space'
import { listMyTeamSpaceUsingPost } from '@/api/spaceUserController'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController'

const loginUserStore = useLoginUserStore()
const route = useRoute()

// 判断是否应该显示侧边栏
const shouldShowSider = computed(() => {
  // 只有在/my_space或/space/*路径下才显示侧边栏
  return route.path === '/my_space' || route.path.startsWith('/space/') || route.path.startsWith('/add_space')
})

// 固定的菜单列表
const fixedMenuItems = [
  {
    key: '/my_space',
    label: '我的空间',
    icon: () => h(UserOutlined),
  },
  {
    key: '/add_space?type=' + SPACE_TYPE_ENUM.TEAM,
    label: '创建团队',
    icon: () => h(TeamOutlined),
  },
]

const teamSpaceList = ref<API.SpaceUserVO[]>([])

const menuItems = computed(() => {
  // 如果用户没有团队空间，则只展示固定菜单
  if (teamSpaceList.value.length < 1) {
    return fixedMenuItems
  }
  // 如果用户有团队空间，则展示固定菜单和团队空间菜单
  // 展示团队空间分组
  const teamSpaceSubMenus = teamSpaceList.value.map((spaceUser) => {
    const space = spaceUser.space
    // 判断是否为管理员
    const isAdmin = spaceUser.spaceRole === SPACE_ROLE_ENUM.ADMIN
    return {
      key: '/space/' + spaceUser.spaceId,
      label: space?.spaceName,
      // 如果是管理员，设置字体加粗
      style: isAdmin ? { fontWeight: 'bold' } : {}
    }
  })
  const teamSpaceMenuGroup = {
    type: 'group',
    label: '我的团队',
    key: 'teamSpace', // 添加key值，确保删除后能刷新
    children: teamSpaceSubMenus,
  }
  return [...fixedMenuItems, teamSpaceMenuGroup]
})

// 加载团队空间列表
const fetchTeamSpaceList = async () => {
  const res = await listMyTeamSpaceUsingPost()
  if (res.data.code === 0 && res.data.data) {
    // 校验空间是否被删除
    const validTeamSpaces = []
    for (const spaceUser of res.data.data) {
      if (spaceUser.spaceId) {
        try {
          const spaceRes = await getSpaceVoByIdUsingGet({ id: spaceUser.spaceId })
          // 只有当空间存在时才添加到列表中
          if (spaceRes.data.code === 0 && spaceRes.data.data) {
            validTeamSpaces.push(spaceUser)
          }
        } catch (e) {
          // 如果获取空间信息失败，说明空间可能已被删除，跳过该空间
          console.warn(`Space with id ${spaceUser.spaceId} not found or deleted`)
        }
      }
    }
    teamSpaceList.value = validTeamSpaces
  } else {
    message.error('加载我的团队空间失败，' + res.data.message)
  }
}

/**
 * 监听变量，改变时触发数据的重新加载
 */
watchEffect(() => {
  // 登录才加载，且只有在需要显示侧边栏时才加载
  if (loginUserStore.loginUser.id && shouldShowSider.value) {
    fetchTeamSpaceList()
  }
})

const router = useRouter()
// 当前要高亮的菜单项
const current = ref<string[]>([])
// 监听路由变化，更新高亮菜单项
router.afterEach((to, from, next) => {
  current.value = [to.path]
})

// 路由跳转事件
const doMenuClick = ({ key }) => {
  router.push(key)
}
</script>

<style scoped>
#globalSider .ant-layout-sider {
  background: none;
}
</style>
