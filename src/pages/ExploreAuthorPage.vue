<template>
  <div id="exploreAuthorPage">
    <!-- 探索标题 -->
    <div class="explore-header">
      <h1 class="explore-title">热门作者排行榜</h1>
      <p class="explore-subtitle">发现平台中的优质创作者</p>
    </div>

    <!-- 切换按钮 -->
    <div class="profile-actions">
      <span
        class="action-link"
        :class="{ active: activeTab === 'hot' }"
        @click="switchTab('hot')"
      >
        社区活跃
      </span>
      <span
        class="action-link"
        :class="{ active: activeTab === 'recommended' }"
        @click="switchTab('recommended')"
      >
        最受欢迎
      </span>
    </div>

    <!-- 按钮下方的细线 -->
    <div class="divider"></div>

    <!-- 热门作者排行榜 -->
    <div class="section" v-show="activeTab === 'hot'">
      <div class="section-header">
        <h2 class="section-title">社区活跃作者</h2>
      </div>
      <div class="ranking-content">
        <a-list
          class="ranking-list"
          :data-source="hotAuthors"
          :split="false"
          item-layout="horizontal"
        >
          <template #renderItem="{ item, index }">
            <a-list-item class="ranking-item">
              <div class="ranking-item-content">
                <div class="ranking-number" :class="getRankClass(index)">
                  {{ index + 1 }}
                </div>
                <div class="author-avatar">
                  <a-avatar :src="item.userAvatar" :size="64" />
                </div>
                <div class="author-info">
                  <h3 class="author-name">{{ item.userName }}</h3>
                  <div class="author-stats">
                    <span class="stat-item">
                      <strong>{{ item.pictureCount }}</strong> 作品数
                    </span>
                  </div>
                </div>
              </div>
            </a-list-item>
          </template>
          <template #footer v-if="hotAuthors.length === 0">
            <a-empty description="暂无热门作者数据" />
          </template>
        </a-list>
      </div>
    </div>

    <!-- 推荐作者排行榜 -->
    <div class="section" v-show="activeTab === 'recommended'">
      <div class="section-header">
        <h2 class="section-title">最受欢迎作者</h2>
      </div>
      <div class="ranking-content">
        <a-list
          class="ranking-list"
          :data-source="recommendedAuthors"
          :split="false"
          item-layout="horizontal"
        >
          <template #renderItem="{ item, index }">
            <a-list-item class="ranking-item">
              <div class="ranking-item-content">
                <div class="ranking-number" :class="getRankClass(index)">
                  {{ index + 1 }}
                </div>
                <div class="author-avatar">
                  <a-avatar :src="item.userAvatar" :size="64" />
                </div>
                <div class="author-info">
                  <h3 class="author-name">{{ item.userName }}</h3>
                  <div class="author-stats">
                    <span class="stat-item">
                      <strong>{{ item.heatValue }}</strong> 热度值
                    </span>
                  </div>
                </div>
              </div>
            </a-list-item>
          </template>
          <template #footer v-if="recommendedAuthors.length === 0">
            <a-empty description="暂无推荐作者数据" />
          </template>
        </a-list>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { getActiveUserRankingUsingGet, getPopularUserRankingUsingGet } from '@/api/userController'

// 当前激活的标签页
const activeTab = ref('hot')

// 定义数据
const hotAuthors = ref<API.UserVO[]>([]) // 热门作者
const recommendedAuthors = ref<API.UserVO[]>([]) // 推荐作者

// 切换标签页
const switchTab = (tab: string) => {
  activeTab.value = tab
}

// 获取排名类名
const getRankClass = (index: number) => {
  if (index === 0) return 'first'
  if (index === 1) return 'second'
  if (index === 2) return 'third'
  return 'normal'
}

// 获取热门作者数据
const fetchHotAuthors = async () => {
  try {
    const res = await getActiveUserRankingUsingGet({
      pageSize: 10
    })
    if (res.data.code === 0 && res.data.data) {
      hotAuthors.value = res.data.data.userList || []
    }
  } catch (error: any) {
    console.error('获取热门作者失败:', error)
    message.error('获取热门作者失败')
  }
}

// 获取推荐作者数据
const fetchRecommendedAuthors = async () => {
  try {
    const res = await getPopularUserRankingUsingGet({
      pageSize: 10
    })
    if (res.data.code === 0 && res.data.data) {
      recommendedAuthors.value = res.data.data.userList || []
    }
  } catch (error: any) {
    console.error('获取推荐作者失败:', error)
    message.error('获取推荐作者失败')
  }
}

// 获取所有数据
const fetchData = async () => {
  await Promise.all([
    fetchHotAuthors(),
    fetchRecommendedAuthors()
  ])
}

// 页面加载时获取数据
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
#exploreAuthorPage {
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

.profile-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
}

.action-link {
  height: 40px;
  line-height: 40px;
  width: 100px;
  margin: 0 10px;
  border-radius: 20px;
  text-align: center;
  cursor: pointer;
  color: #000;
  font-size: 16px;
  transition: all 0.3s ease;
}

.action-link:hover,
.action-link.active {
  color: #ffffff;
  background-color: #5f5f5f;
}

.divider {
  width: 90vw;
  max-width: 90vw;
  margin: 0 auto 30px;
  border-top: 1px solid #d9d9d9;
}

.section {
  margin-bottom: 48px;
}

.section-header {
  margin-bottom: 24px;
  text-align: center;
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

.ranking-content {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.ranking-list {
  padding: 0;
}

.ranking-item {
  padding: 20px 0;
  border-bottom: 1px solid #f0f0f0;
}

.ranking-item:last-child {
  border-bottom: none;
}

.ranking-item-content {
  display: flex;
  align-items: center;
  width: 100%;
}

.ranking-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-right: 20px;
  flex-shrink: 0;
}

.ranking-number.first {
  background: #ffd700;
  color: #fff;
}

.ranking-number.second {
  background: #c0c0c0;
  color: #fff;
}

.ranking-number.third {
  background: #cd7f32;
  color: #fff;
}

.ranking-number.normal {
  background: #f0f0f0;
  color: #666;
}

.author-avatar {
  margin-right: 20px;
  flex-shrink: 0;
}

.author-info {
  flex: 1;
}

.author-name {
  font-size: 18px;
  font-weight: bold;
  margin: 0 0 10px 0;
  color: #333;
}

.author-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 10px;
}

.stat-item {
  font-size: 14px;
  color: #666;
}

.stat-item strong {
  color: #000;
}

@media (max-width: 768px) {
  .ranking-item-content {
    flex-wrap: wrap;
  }

  .author-info {
    margin: 15px 0;
    width: 100%;
  }

  .author-stats {
    gap: 10px;
  }

  .action-link {
    width: 80px;
    font-size: 14px;
  }
}
</style>