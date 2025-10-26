<template>
  <div id="userRegisterPage">
    <div class="register-container">
      <div class="register-card">
        <div class="register-header">
          <h1 class="register-title">木杉图仓</h1>
          <p class="register-subtitle">用户注册</p>
          <p class="register-desc">企业级智能协同云图库</p>
        </div>
        <a-form
          :model="formState"
          name="basic"
          autocomplete="off"
          @finish="handleSubmit"
        >
          <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
            <a-input
              v-model:value="formState.userAccount"
              placeholder="请输入账号"
              size="large"
              class="register-input"
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
              { min: 8, message: '密码不能小于 8 位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.userPassword"
              placeholder="请输入密码"
              size="large"
              class="register-input"
            >
              <template #prefix>
                <LockOutlined />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item
            name="checkPassword"
            :rules="[
              { required: true, message: '请输入确认密码' },
              { min: 8, message: '确认密码不能小于 8 位' },
            ]"
          >
            <a-input-password
              v-model:value="formState.checkPassword"
              placeholder="请输入确认密码"
              size="large"
              class="register-input"
            >
              <template #prefix>
                <LockOutlined />
              </template>
            </a-input-password>
          </a-form-item>

          <div class="tips">
            已有账号？
            <router-link to="/user/login" class="login-link">
              去登录
            </router-link>
          </div>

          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              block
              class="register-button"
            >
              注册
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
import { userRegisterUsingPost } from '@/api/userController'
import { useLoginUserStore } from '@/stores/userLoginUserStore'

interface FormState {
  userAccount: string
  userPassword: string
  checkPassword: string
}

const formState = reactive<FormState>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const router = useRouter()
const loginUserStore = useLoginUserStore()

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  // 判断两次输入的密码是否一致
  if (formState.userPassword !== formState.checkPassword) {
    message.error('二次输入的密码不一致')
    return
  }

  const res = await userRegisterUsingPost(values)
  // 注册成功，跳转到登录页面
  if (res.data.code === 0 && res.data.data) {
    message.success('注册成功')
    router.push({
      path: '/user/login',
      replace: true,
    })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
  min-height: 70vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.register-container {
  width: 100%;
  max-width: 400px;
}

.register-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  padding: 40px 30px;
  backdrop-filter: blur(10px);
  border: 1px solid #e0e0e0;
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.register-title {
  font-size: 28px;
  font-weight: 700;
  color: #333333;
  margin: 0 0 5px 0;
}

.register-subtitle {
  font-size: 16px;
  color: #666;
  margin: 0 0 10px 0;
}

.register-desc {
  font-size: 14px;
  color: #888;
  margin: 0;
}

.register-input {
  border-radius: 8px;
  border: 1px solid #ddd;
}

.register-input :deep(.ant-input) {
  border-radius: 8px;
  padding: 12px 15px;
  border-color: #ddd;
}

.register-input :deep(.ant-input:focus) {
  border-color: #333;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.1);
}

.register-input :deep(.ant-input-prefix) {
  margin-right: 10px;
  color: #333;
}

.tips {
  text-align: center;
  margin-bottom: 20px;
  color: #666;
}

.login-link {
  color: #333;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s;
}

.login-link:hover {
  color: #000;
  text-decoration: underline;
}

.register-button {
  border-radius: 8px;
  height: 45px;
  font-size: 16px;
  font-weight: 500;
  margin-top: 10px;
  background: #2cccda;
  border: none;
  color: white;
}

.register-button:hover {
  background: #2cccdaaa;
  transition: all 0.3s ease;
}

:deep(.ant-form-item) {
  margin-bottom: 20px;
}

:deep(.ant-form-item-explain-error) {
  font-size: 13px;
}
</style>
