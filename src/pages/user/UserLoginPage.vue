<template>
  <div id="userLoginPage">
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <h1 class="login-title">木杉</h1>
          <p class="login-subtitle">用户登录</p>
        </div>
        <a-form :model="formState" name="basic" autocomplete="off" @finish="handleSubmit">
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input
              v-model:value="formState.userAccount"
              placeholder="请输入账号"
              size="large"
              class="login-input"
            >
              <template #prefix>
                <UserOutlined />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item
            name="userPassword"
            :rules="[
              { required: true, message: '请输入密码' },
              { min: 8, message: '密码长度不能小于8位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              placeholder="请输入密码"
              size="large"
              class="login-input"
            >
              <template #prefix>
                <LockOutlined />
              </template>
            </a-input-password>
          </a-form-item>

          <div class="tips">
            没有账号？
            <router-link to="/user/Register" class="register-link">
              点击注册
            </router-link>
          </div>

          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              block
              class="login-button"
            >
              登录
            </a-button>
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive } from 'vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userLoginUsingPost } from '@/api/userController'
import { useLoginUserStore } from '@/stores/userLoginUserStore'

interface FormState {
  username: string
  password: string
}

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const router = useRouter()
const loginUserStore = useLoginUserStore()


/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  const res = await userLoginUsingPost(values)
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({
      path: '/',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}


</script>

<style scoped>
#userLoginPage {
  min-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 400px;
}

.login-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  padding: 40px 30px;
  backdrop-filter: blur(10px);
  border: 1px solid #e0e0e0;
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #333333;
  margin: 0 0 5px 0;
}

.login-subtitle {
  font-size: 16px;
  color: #666;
  margin: 0;
}

.login-input {
  border-radius: 8px;
  border: 1px solid #ddd;
}

.login-input :deep(.ant-input) {
  border-radius: 8px;
  border-color: #ddd;
}

.login-input :deep(.ant-input:focus) {
  border-color: #333;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1);
}

.login-input :deep(.ant-input-prefix) {
  margin-right: 10px;
  color: #333;
}

.tips {
  text-align: center;
  margin-bottom: 20px;
  color: #666;
}

.register-link {
  color: #333;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s;
}

.register-link:hover {
  color: #000;
  text-decoration: underline;
}

.login-button {
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  margin-top: 10px;
  border: none;
  color: white;
  background: #2cccda;
}

.login-button:hover {
  transition: all 0.3s ease;
  background: #2cccdaaa;
}

:deep(.ant-form-item) {
  margin-bottom: 20px;
}

:deep(.ant-form-item-explain-error) {
  font-size: 13px;
}
</style>
