<template>
  <div class="contact-list">
    <h2 class="page-title">客户与分组</h2>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="客户列表" name="contacts">
        <el-card>
          <div class="toolbar">
            <el-select v-model="filterGroupId" placeholder="按分组筛选" clearable style="width: 200px">
              <el-option
                v-for="g in groupList"
                :key="g.id"
                :label="g.name"
                :value="g.id"
              />
            </el-select>
            <el-button type="primary" @click="showAddContact">新建客户</el-button>
          </div>
          <el-table :data="contactList" v-loading="loading" style="margin-top: 16px">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="mobile" label="手机" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="danger" @click="handleDeleteContact(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px"
            @current-change="loadContacts"
            @size-change="loadContacts"
          />
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="分组管理" name="groups">
        <el-card>
          <el-button type="primary" @click="showAddGroup">新建分组</el-button>
          <el-table :data="groupList" style="margin-top: 16px">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="ruleType" label="规则类型" width="120" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="danger" @click="handleDeleteGroup(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="退订与黑名单" name="unsub">
        <el-card>
          <el-form inline>
            <el-form-item label="邮箱">
              <el-input v-model="unsubEmail" placeholder="输入邮箱" style="width: 220px" />
            </el-form-item>
            <el-form-item>
              <el-button @click="checkUnsub">检查退订</el-button>
              <el-button type="warning" @click="addUnsub">添加退订</el-button>
              <el-button @click="checkBlack">检查黑名单</el-button>
              <el-button type="danger" @click="addBlack">添加黑名单</el-button>
            </el-form-item>
          </el-form>
          <p v-if="checkResult !== null" class="check-result">检查结果: {{ checkResult ? '是' : '否' }}</p>
          <h4 style="margin-top: 24px">黑名单列表</h4>
          <el-table :data="blacklist" v-loading="blackLoading" style="margin-top: 8px">
            <el-table-column prop="email" label="邮箱" />
            <el-table-column prop="source" label="来源" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
    <!-- 新建客户 -->
    <el-dialog v-model="contactDialogVisible" title="新建客户">
      <el-form ref="contactFormRef" :model="contactForm" :rules="contactRules" label-width="80px">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="contactForm.email" placeholder="必填" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="contactForm.name" />
        </el-form-item>
        <el-form-item label="手机" prop="mobile">
          <el-input v-model="contactForm.mobile" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitContact">确定</el-button>
      </template>
    </el-dialog>
    <!-- 新建分组 -->
    <el-dialog v-model="groupDialogVisible" title="新建分组">
      <el-form ref="groupFormRef" :model="groupForm" :rules="groupRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="分组名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGroup">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as contactApi from '@/api/contact'
import type { Contact, ContactGroup } from '@/types/entity'

const activeTab = ref('contacts')
const groupList = ref<ContactGroup[]>([])
const contactList = ref<Contact[]>([])
const loading = ref(false)
const blackLoading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filterGroupId = ref<number | undefined>()
const contactDialogVisible = ref(false)
const groupDialogVisible = ref(false)
const contactFormRef = ref<FormInstance>()
const groupFormRef = ref<FormInstance>()
const unsubEmail = ref('')
const checkResult = ref<boolean | null>(null)
const blacklist = ref<{ email: string; source?: string }[]>([])

const contactForm = ref({ email: '', name: '', mobile: '' })
const contactRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
}
const groupForm = ref({ name: '' })
const groupRules: FormRules = {
  name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }],
}

async function loadGroups() {
  const res = await contactApi.getGroupList()
  groupList.value = Array.isArray(res.data) ? res.data : []
}

async function loadContacts() {
  loading.value = true
  try {
    const res = await contactApi.getContactPage({
      page: page.value,
      size: pageSize.value,
      groupId: filterGroupId.value,
    })
    const data = res.data
    contactList.value = data?.records ?? []
    total.value = data?.total ?? 0
  } finally {
    loading.value = false
  }
}

watch([filterGroupId], () => {
  page.value = 1
  loadContacts()
})

function showAddContact() {
  contactForm.value = { email: '', name: '', mobile: '' }
  contactDialogVisible.value = true
}

async function submitContact() {
  await contactFormRef.value?.validate(async (valid) => {
    if (!valid) return
    await contactApi.createContact(contactForm.value)
    ElMessage.success('创建成功')
    contactDialogVisible.value = false
    loadContacts()
  })
}

async function handleDeleteContact(row: Contact) {
  await ElMessageBox.confirm('确定删除该客户？', '提示', { type: 'warning' })
  await contactApi.deleteContact(row.id)
  ElMessage.success('已删除')
  loadContacts()
}

function showAddGroup() {
  groupForm.value = { name: '' }
  groupDialogVisible.value = true
}

async function submitGroup() {
  await groupFormRef.value?.validate(async (valid) => {
    if (!valid) return
    await contactApi.createGroup({ name: groupForm.value.name, ruleType: 'static' })
    ElMessage.success('创建成功')
    groupDialogVisible.value = false
    loadGroups()
  })
}

async function handleDeleteGroup(row: ContactGroup) {
  await ElMessageBox.confirm('确定删除该分组？', '提示', { type: 'warning' })
  await contactApi.deleteGroup(row.id)
  ElMessage.success('已删除')
  loadGroups()
}

async function checkUnsub() {
  if (!unsubEmail.value) {
    ElMessage.warning('请输入邮箱')
    return
  }
  const res = await contactApi.checkUnsubscribe(unsubEmail.value)
  checkResult.value = res.data === true
}

async function addUnsub() {
  if (!unsubEmail.value) {
    ElMessage.warning('请输入邮箱')
    return
  }
  await contactApi.addUnsubscribe(unsubEmail.value)
  ElMessage.success('已添加退订')
  checkResult.value = null
}

async function checkBlack() {
  if (!unsubEmail.value) {
    ElMessage.warning('请输入邮箱')
    return
  }
  const res = await contactApi.checkBlacklist(unsubEmail.value)
  checkResult.value = res.data === true
}

async function addBlack() {
  if (!unsubEmail.value) {
    ElMessage.warning('请输入邮箱')
    return
  }
  await contactApi.addBlacklist(unsubEmail.value)
  ElMessage.success('已添加黑名单')
  loadBlacklist()
  checkResult.value = null
}

async function loadBlacklist() {
  blackLoading.value = true
  try {
    const res = await contactApi.getBlacklistList()
    blacklist.value = Array.isArray(res.data) ? res.data : []
  } finally {
    blackLoading.value = false
  }
}

onMounted(() => {
  loadGroups()
  loadContacts()
})
watch(activeTab, (tab) => {
  if (tab === 'unsub') loadBlacklist()
})
</script>

<style scoped>
.contact-list {
  max-width: 1200px;
}
.page-title {
  margin: 0 0 20px;
  font-size: 20px;
  color: #1a1a2e;
}
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}
.check-result {
  margin-top: 12px;
  color: #666;
}
</style>
