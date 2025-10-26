<template>
  <div id="addPictureBatchPage">
    <div class="batch-page-container">
      <h2 class="page-title">批量创建图片</h2>
      <a-form layout="vertical" :model="formData" @finish="handleSubmit" class="batch-form">
        <a-form-item label="关键词" name="searchText">
          <a-input v-model:value="formData.searchText" placeholder="请输入关键词" class="form-input" />
        </a-form-item>
        <a-form-item label="抓取数量" name="count">
          <a-input-number
            v-model:value="formData.count"
            placeholder="请输入数量"
            style="min-width: 180px"
            :min="1"
            :max="30"
            allow-clear
            class="form-input"
          />
        </a-form-item>
        <a-form-item label="名称前缀" name="namePrefix">
          <a-input v-model:value="formData.namePrefix" placeholder="请输入名称前缀，会自动补充序号" class="form-input" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading" class="submit-button">
            执行任务
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { uploadPictureByBatchUsingPost } from '@/api/pictureController'
import router from '@/router'
import { message } from 'ant-design-vue'

const formData = reactive<API.PictureUploadByBatchRequest>({
  count: 10,
})
const loading = ref(false)

const handleSubmit = async (values: any) => {
  loading.value = true;
  const res = await uploadPictureByBatchUsingPost({
    ...formData,
  })
  if (res.data.code === 0 && res.data.data) {
    message.success(`创建成功，共 ${res.data.data} 条`)
    router.push({
      path: '/',
    })
  } else {
    message.error('创建失败，' + res.data.message)
  }
  loading.value = false;
}
</script>

<style scoped>
#addPictureBatchPage {
  display: flex;
  justify-content: center;
  padding: 40px 20px;
}

.batch-page-container {
  width: 100%;
  max-width: 500px;
  background: #ffffff;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.page-title {
  text-align: center;
  margin-bottom: 24px;
  color: #000000;
  font-size: 24px;
  font-weight: bold;
}

.batch-form {
  margin-top: 20px;
}

.form-input {
  border-radius: 8px;
  padding: 8px 12px;
}

.submit-button {
  background-color: #2cccda;
  border-radius: 8px;
  height: 40px;
  font-size: 16px;
  margin-top: 10px;
}

.submit-button:hover {
  background-color: #2cccdaaa;
}
</style>
