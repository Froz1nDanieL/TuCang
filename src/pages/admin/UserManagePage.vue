<template>
  <div id="userManage">
    <!-- 搜索区域 -->
    <a-card style="margin-bottom: 20px;">
      <a-form layout="inline" :model="searchParams" @finish="doSearch">
        <a-row :gutter="[16, 16]" style="width: 100%;">
          <a-col :md="6" :sm="12" :xs="24">
            <a-form-item label="账号" style="width: 100%;">
              <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="12" :xs="24">
            <a-form-item label="用户名" style="width: 100%;">
              <a-input v-model:value="searchParams.userName" placeholder="输入用户名" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="12" :xs="24">
            <a-form-item style="width: 100%;">
              <a-button type="primary" html-type="submit">搜索</a-button>
              <a-button style="margin-left: 10px;" @click="resetSearch">重置</a-button>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 表格区域 -->
    <a-card>
      <template #title>
        <span>用户列表</span>
        <span style="float: right; font-size: 14px;">共 {{ total }} 条数据</span>
      </template>

      <a-table
        :columns="columns"
        :data-source="dataList"
        :pagination="pagination"
        :loading="loading"
        @change="doTableChange"
        style="margin-top: 16px;"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userAvatar'">
            <a-avatar :src="record.userAvatar" :size="64" shape="square" />
          </template>
          <template v-else-if="column.dataIndex === 'userRole'">
            <a-tag v-if="record.userRole === 'admin'" color="green">管理员</a-tag>
            <a-tag v-else color="blue">普通用户</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-popconfirm title="确定要删除这个用户吗?" @confirm="doDelete(record.id)">
                <a-button danger size="small">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { SmileOutlined, DownOutlined } from '@ant-design/icons-vue'
import { deleteUserUsingPost, listUserVoByPageUsingPost } from '@/api/userController'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import type { TablePaginationConfig } from 'ant-design-vue'

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
  },
  {
    title: '用户名',
    dataIndex: 'userName',
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    width: 100,
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
    width: 120,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 200,
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    fixed: 'right',
  },
]

// 数据
const dataList = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.UserQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'ascend',
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUserVoByPageUsingPost({
      ...searchParams,
    })
    if (res.data.data) {
      dataList.value = res.data.data.records ?? []
      total.value = res.data.data.total ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

// 重置搜索
const resetSearch = () => {
  searchParams.userAccount = undefined
  searchParams.userName = undefined
  searchParams.current = 1
  fetchData()
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})

// 分页参数
const pagination = computed<TablePaginationConfig>(() => {
  return {
    current: searchParams.current ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total) => `共 ${total} 条`,
    pageSizeOptions: ['10', '20', '50', '100'],
    showQuickJumper: true,
  }
})

// 表格变化处理
const doTableChange = (page: TablePaginationConfig) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 获取数据
const doSearch = () => {
  // 重置页码
  searchParams.current = 1
  fetchData()
}

// 删除数据
const doDelete = async (id: string) => {
  if (!id) {
    return
  }
  const res = await deleteUserUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}
</script>

<style scoped>
#userManage {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 64px);
}

.ant-card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
  border-radius: 8px;
}

:deep(.ant-card-head) {
  border-bottom: 1px solid #f0f0f0;
}

:deep(.ant-table-thead > tr > th) {
  background-color: #fafafa;
  font-weight: 600;
}

@media (max-width: 768px) {
  #userManage {
    padding: 12px;
  }

  .ant-card {
    margin-bottom: 12px;
  }
}
</style>
