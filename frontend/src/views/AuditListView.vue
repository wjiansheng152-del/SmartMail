<template>
  <div class="audit-list">
    <h2 class="page-title">审计日志</h2>
    <el-card>
      <el-form inline>
        <el-form-item label="用户ID">
          <el-input v-model="userId" placeholder="可选" type="number" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" style="margin-top: 16px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="action" label="操作" width="120" />
        <el-table-column prop="resource" label="资源" width="120" />
        <el-table-column prop="resourceId" label="资源ID" width="100" />
        <el-table-column prop="detail" label="详情" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="180" />
      </el-table>
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px"
        @current-change="loadList"
        @size-change="loadList"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import type { AuditLog } from '@/types/entity'
import type { IPage } from '@/types/api'

const list = ref<AuditLog[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const userId = ref<number | undefined>()

async function loadList() {
  loading.value = true
  try {
    const res = await request.get<AuditLog[] | IPage<AuditLog>>('/audit/log/list', {
      params: { page: page.value, size: pageSize.value, userId: userId.value },
    })
    const data = res.data
    if (Array.isArray(data)) {
      list.value = data
      total.value = data.length
    } else if (data && 'records' in data) {
      list.value = data.records ?? []
      total.value = data.total ?? 0
    } else {
      list.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.audit-list {
  max-width: 1200px;
}
.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  color: #1a1a2e;
}
</style>
