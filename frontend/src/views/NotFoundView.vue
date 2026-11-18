<template>
  <main class="not-found">
    <el-result icon="warning" title="404" sub-title="这个页面暂时不存在"
      ><template #extra
        ><RouterLink to="/overview"
          ><el-button type="primary">返回工作台</el-button></RouterLink
        ></template
      ></el-result
    >
  </main>
</template>
