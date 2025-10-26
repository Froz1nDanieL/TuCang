<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>
      <a-layout class="layout-content">
        <GlobalSider ref="globalSiderRef" class="sider" />
        <a-layout-content class="content">
          <router-view />
        </a-layout-content>
      </a-layout>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from "@/components/GlobalHeader.vue";
import GlobalSider from '@/components/GlobalSider.vue'


</script>

<style scoped>
#basicLayout .header {
  height: 72px;
  background: #fff;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
}

#basicLayout .layout-content {
  background: #fff;
  padding-top: 10vh;
}

#basicLayout .sider {
  background: #fff;
  border-right: 0.5px solid #eee;
  padding-top: 20px;
}

#basicLayout .content {
  background-color: #fff;
}


/* 修复侧边栏折叠时的显示问题 */
:deep(.ant-layout-sider-zero-width-trigger) {
  top: 64px; /* 与 header 高度一致 */
}
</style>
