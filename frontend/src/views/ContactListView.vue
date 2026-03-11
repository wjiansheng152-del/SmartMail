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
            <el-select v-model="contactSortBy" placeholder="排序字段" style="width: 100px">
              <el-option label="ID" value="id" />
              <el-option label="邮箱" value="email" />
              <el-option label="姓名" value="name" />
              <el-option label="手机" value="mobile" />
            </el-select>
            <el-select v-model="contactSortOrder" style="width: 90px">
              <el-option label="升序" value="asc" />
              <el-option label="降序" value="desc" />
            </el-select>
            <el-select v-model="contactSearchField" placeholder="搜索字段" clearable style="width: 100px">
              <el-option label="ID" value="id" />
              <el-option label="邮箱" value="email" />
              <el-option label="姓名" value="name" />
              <el-option label="手机" value="mobile" />
            </el-select>
            <el-input
              v-model="contactSearchKeyword"
              placeholder="搜索"
              clearable
              style="width: 160px"
            />
            <el-button type="primary" @click="showAddContact">新建客户</el-button>
            <el-button
              :disabled="selectedContacts.length === 0"
              @click="showBatchAddToGroup"
            >
              加入分组
            </el-button>
          </div>
          <el-table
            ref="contactTableRef"
            :data="filteredContactList"
            v-loading="loading"
            style="margin-top: 16px"
            @selection-change="onContactSelectionChange"
          >
            <el-table-column type="selection" width="48" />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="email" label="邮箱" />
            <el-table-column prop="name" label="姓名" />
            <el-table-column prop="mobile" label="手机" />
            <el-table-column label="操作" width="280">
              <template #default="{ row }">
                <el-button link type="primary" @click="showEditContact(row)">编辑</el-button>
                <el-button link type="primary" @click="showAddToGroup(row)">加入分组</el-button>
                <el-button
                  v-if="filterGroupId != null"
                  link
                  type="warning"
                  @click="handleRemoveFromGroup(row)"
                >
                  移出分组
                </el-button>
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
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button link type="primary" @click="showGroupEdit(row)">编辑</el-button>
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
    <!-- 新建/编辑客户 -->
    <el-dialog v-model="contactDialogVisible" :title="contactEditId == null ? '新建客户' : '编辑客户'">
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
    <!-- 加入分组（单条或批量） -->
    <el-dialog v-model="addGroupVisible" title="加入分组" width="400px">
      <el-form label-width="80px">
        <el-form-item label="选择分组">
          <el-select v-model="addGroupGroupId" placeholder="请选择分组" style="width: 100%">
            <el-option
              v-for="g in groupList"
              :key="g.id"
              :label="g.name"
              :value="g.id"
            />
          </el-select>
        </el-form-item>
        <p v-if="addGroupContactIds.length > 0" class="add-group-tip">
          {{ addGroupContactIds.length === 1 ? '将该客户加入所选分组。' : `将选中的 ${addGroupContactIds.length} 个客户加入所选分组。` }}
        </p>
      </el-form>
      <template #footer>
        <el-button @click="addGroupVisible = false">取消</el-button>
        <el-button type="primary" :disabled="addGroupGroupId == null" @click="submitAddToGroup">
          确定
        </el-button>
      </template>
    </el-dialog>
    <!-- 分组编辑：批量加入/移出客户 -->
    <el-dialog
      v-model="groupEditVisible"
      :title="editGroup ? `管理分组：${editGroup.name}` : '管理分组'"
      width="720px"
      @open="onGroupEditOpen"
    >
      <div v-if="editGroup" class="group-edit-content">
        <h4 class="group-edit-section">当前成员</h4>
        <div class="group-edit-search">
          <span class="group-edit-sort-label">搜索：</span>
          <el-select v-model="groupEditMembersSearchField" placeholder="字段" size="small" clearable style="width: 90px">
            <el-option label="ID" value="id" />
            <el-option label="邮箱" value="email" />
            <el-option label="姓名" value="name" />
            <el-option label="手机" value="mobile" />
          </el-select>
          <el-input
            v-model="groupEditMembersSearchKeyword"
            placeholder="输入关键词"
            clearable
            size="small"
            style="width: 140px"
          />
        </div>
        <div class="group-edit-sort">
          <span class="group-edit-sort-label">排序：</span>
          <el-select v-model="groupEditMembersSortBy" placeholder="字段" size="small" style="width: 100px">
            <el-option label="ID" value="id" />
            <el-option label="邮箱" value="email" />
            <el-option label="姓名" value="name" />
            <el-option label="手机" value="mobile" />
          </el-select>
          <el-select v-model="groupEditMembersSortOrder" size="small" style="width: 80px">
            <el-option label="升序" value="asc" />
            <el-option label="降序" value="desc" />
          </el-select>
        </div>
        <div class="group-edit-toolbar">
          <el-button
            type="warning"
            :disabled="groupEditMembersSelected.length === 0"
            @click="handleBatchRemoveFromGroup"
          >
            批量移出
          </el-button>
        </div>
        <el-table
          ref="groupMembersTableRef"
          :data="filteredGroupEditMembers"
          v-loading="groupEditMembersLoading"
          max-height="200"
          @selection-change="onGroupMembersSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column prop="name" label="姓名" />
          <el-table-column prop="mobile" label="手机" />
        </el-table>
        <h4 class="group-edit-section">添加客户到分组</h4>
        <div class="group-edit-search">
          <span class="group-edit-sort-label">搜索：</span>
          <el-select v-model="groupEditAddSearchField" placeholder="字段" size="small" clearable style="width: 90px">
            <el-option label="ID" value="id" />
            <el-option label="邮箱" value="email" />
            <el-option label="姓名" value="name" />
            <el-option label="手机" value="mobile" />
          </el-select>
          <el-input
            v-model="groupEditAddSearchKeyword"
            placeholder="输入关键词"
            clearable
            size="small"
            style="width: 140px"
          />
        </div>
        <div class="group-edit-sort">
          <span class="group-edit-sort-label">排序：</span>
          <el-select v-model="groupEditAddSortBy" placeholder="字段" size="small" style="width: 100px">
            <el-option label="ID" value="id" />
            <el-option label="邮箱" value="email" />
            <el-option label="姓名" value="name" />
            <el-option label="手机" value="mobile" />
          </el-select>
          <el-select v-model="groupEditAddSortOrder" size="small" style="width: 80px">
            <el-option label="升序" value="asc" />
            <el-option label="降序" value="desc" />
          </el-select>
        </div>
        <div class="group-edit-toolbar">
          <el-button
            type="primary"
            :disabled="groupEditAddSelected.length === 0"
            @click="handleBatchAddToEditGroup"
          >
            加入此分组
          </el-button>
        </div>
        <el-table
          ref="groupAddTableRef"
          :data="filteredGroupEditAddList"
          v-loading="groupEditAddLoading"
          max-height="200"
          @selection-change="onGroupAddSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column prop="name" label="姓名" />
          <el-table-column prop="mobile" label="手机" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="groupEditVisible = false">关闭</el-button>
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
import { ref, computed, watch, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { TableInstance } from 'element-plus'
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
const contactSortBy = ref<'id' | 'email' | 'name' | 'mobile'>('id')
const contactSortOrder = ref<'asc' | 'desc'>('desc')
const contactSearchField = ref<'id' | 'email' | 'name' | 'mobile' | ''>('')
const contactSearchKeyword = ref('')
const contactDialogVisible = ref(false)
const groupDialogVisible = ref(false)
const contactFormRef = ref<FormInstance>()
const groupFormRef = ref<FormInstance>()
const contactTableRef = ref<TableInstance>()
const contactEditId = ref<number | null>(null)
const selectedContacts = ref<Contact[]>([])
const addGroupVisible = ref(false)
const addGroupGroupId = ref<number | null>(null)
const addGroupContactIds = ref<number[]>([])
const unsubEmail = ref('')
const checkResult = ref<boolean | null>(null)
const blacklist = ref<{ email: string; source?: string }[]>([])

// 分组编辑弹窗
const groupEditVisible = ref(false)
const editGroup = ref<ContactGroup | null>(null)
const groupEditMembers = ref<Contact[]>([])
const groupEditMembersLoading = ref(false)
const groupEditMembersSelected = ref<Contact[]>([])
const groupEditAddList = ref<Contact[]>([])
const groupEditAddLoading = ref(false)
const groupEditAddSelected = ref<Contact[]>([])
const groupMembersTableRef = ref<TableInstance>()
const groupAddTableRef = ref<TableInstance>()
const groupEditMembersSortBy = ref<'id' | 'email' | 'name' | 'mobile'>('id')
const groupEditMembersSortOrder = ref<'asc' | 'desc'>('asc')
const groupEditAddSortBy = ref<'id' | 'email' | 'name' | 'mobile'>('id')
const groupEditAddSortOrder = ref<'asc' | 'desc'>('asc')
const groupEditMembersSearchField = ref<'id' | 'email' | 'name' | 'mobile' | ''>('')
const groupEditMembersSearchKeyword = ref('')
const groupEditAddSearchField = ref<'id' | 'email' | 'name' | 'mobile' | ''>('')
const groupEditAddSearchKeyword = ref('')

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

/** 按字段与顺序对客户列表排序（前端排序） */
function sortContacts(
  list: Contact[],
  by: 'id' | 'email' | 'name' | 'mobile',
  order: 'asc' | 'desc'
): Contact[] {
  return [...list].sort((a, b) => {
    let va: string | number | undefined = by === 'id' ? a.id : a[by] ?? ''
    let vb: string | number | undefined = by === 'id' ? b.id : b[by] ?? ''
    if (typeof va === 'string') va = va.toLowerCase()
    if (typeof vb === 'string') vb = vb.toLowerCase()
    if (va === undefined || va === null) va = ''
    if (vb === undefined || vb === null) vb = ''
    if (va < vb) return order === 'asc' ? -1 : 1
    if (va > vb) return order === 'asc' ? 1 : -1
    return 0
  })
}

const sortedContactList = computed(() =>
  sortContacts(contactList.value, contactSortBy.value, contactSortOrder.value)
)
const sortedGroupEditMembers = computed(() =>
  sortContacts(groupEditMembers.value, groupEditMembersSortBy.value, groupEditMembersSortOrder.value)
)
const sortedGroupEditAddList = computed(() =>
  sortContacts(groupEditAddList.value, groupEditAddSortBy.value, groupEditAddSortOrder.value)
)

/** 按选定字段与关键词过滤客户列表（关键词为空或未选字段时不过滤） */
function filterContacts(
  list: Contact[],
  field: 'id' | 'email' | 'name' | 'mobile' | '',
  keyword: string
): Contact[] {
  const k = keyword.trim().toLowerCase()
  if (!k) return list
  if (!field) {
    return list.filter((c) => {
      const idStr = String(c.id ?? '').toLowerCase()
      const email = (c.email ?? '').toLowerCase()
      const name = (c.name ?? '').toLowerCase()
      const mobile = (c.mobile ?? '').toLowerCase()
      return idStr.includes(k) || email.includes(k) || name.includes(k) || mobile.includes(k)
    })
  }
  return list.filter((c) => {
    const v = field === 'id' ? c.id : c[field]
    const s = String(v ?? '').toLowerCase()
    return s.includes(k)
  })
}

const filteredContactList = computed(() =>
  filterContacts(
    sortedContactList.value,
    contactSearchField.value,
    contactSearchKeyword.value
  )
)
const filteredGroupEditMembers = computed(() =>
  filterContacts(
    sortedGroupEditMembers.value,
    groupEditMembersSearchField.value,
    groupEditMembersSearchKeyword.value
  )
)
const filteredGroupEditAddList = computed(() =>
  filterContacts(
    sortedGroupEditAddList.value,
    groupEditAddSearchField.value,
    groupEditAddSearchKeyword.value
  )
)

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
  contactEditId.value = null
  contactForm.value = { email: '', name: '', mobile: '' }
  contactDialogVisible.value = true
}

function showEditContact(row: Contact) {
  contactEditId.value = row.id
  contactForm.value = {
    email: row.email,
    name: row.name ?? '',
    mobile: row.mobile ?? '',
  }
  contactDialogVisible.value = true
}

async function submitContact() {
  await contactFormRef.value?.validate(async (valid) => {
    if (!valid) return
    if (contactEditId.value != null) {
      await contactApi.updateContact(contactEditId.value, {
        email: contactForm.value.email,
        name: contactForm.value.name || undefined,
        mobile: contactForm.value.mobile || undefined,
      })
      ElMessage.success('更新成功')
    } else {
      await contactApi.createContact(contactForm.value)
      ElMessage.success('创建成功')
    }
    contactDialogVisible.value = false
    contactEditId.value = null
    loadContacts()
  })
}

function onContactSelectionChange(rows: Contact[]) {
  selectedContacts.value = rows
}

function showAddToGroup(row: Contact) {
  addGroupContactIds.value = [row.id]
  addGroupGroupId.value = groupList.value[0]?.id ?? null
  addGroupVisible.value = true
}

function showBatchAddToGroup() {
  if (selectedContacts.value.length === 0) {
    ElMessage.warning('请先选择客户')
    return
  }
  addGroupContactIds.value = selectedContacts.value.map((c) => c.id)
  addGroupGroupId.value = groupList.value[0]?.id ?? null
  addGroupVisible.value = true
}

async function submitAddToGroup() {
  if (addGroupGroupId.value == null) {
    ElMessage.warning('请选择分组')
    return
  }
  if (addGroupContactIds.value.length === 0) return
  if (addGroupContactIds.value.length === 1) {
    await contactApi.addMemberToGroup(addGroupGroupId.value, addGroupContactIds.value[0])
  } else {
    await contactApi.addMemberToGroupBatch(addGroupGroupId.value, addGroupContactIds.value)
  }
  ElMessage.success('已加入分组')
  addGroupVisible.value = false
  contactTableRef.value?.clearSelection()
  loadContacts()
}

async function handleRemoveFromGroup(row: Contact) {
  if (filterGroupId.value == null) return
  await ElMessageBox.confirm('确定将该客户移出当前分组？', '提示', { type: 'warning' })
  await contactApi.removeMemberFromGroup(filterGroupId.value, row.id)
  ElMessage.success('已移出分组')
  loadContacts()
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

function showGroupEdit(row: ContactGroup) {
  editGroup.value = { ...row }
  groupEditVisible.value = true
}

async function onGroupEditOpen() {
  if (!editGroup.value) return
  groupEditMembersSelected.value = []
  groupEditAddSelected.value = []
  groupMembersTableRef.value?.clearSelection()
  groupAddTableRef.value?.clearSelection()
  await loadGroupEditMembers()
  await loadGroupEditAddList()
  // 仅显示未加入当前分组的客户，已在此分组中的不显示
  const memberIds = new Set(groupEditMembers.value.map((m) => m.id))
  groupEditAddList.value = groupEditAddList.value.filter((c) => !memberIds.has(c.id))
}

async function loadGroupEditMembers() {
  if (!editGroup.value) return
  groupEditMembersLoading.value = true
  try {
    const res = await contactApi.getContactPage({
      page: 1,
      size: 100,
      groupId: editGroup.value.id,
    })
    groupEditMembers.value = res.data?.records ?? []
  } finally {
    groupEditMembersLoading.value = false
  }
}

async function loadGroupEditAddList() {
  groupEditAddLoading.value = true
  try {
    const res = await contactApi.getContactPage({ page: 1, size: 100 })
    groupEditAddList.value = res.data?.records ?? []
  } finally {
    groupEditAddLoading.value = false
  }
}

function onGroupMembersSelectionChange(rows: Contact[]) {
  groupEditMembersSelected.value = rows
}

function onGroupAddSelectionChange(rows: Contact[]) {
  groupEditAddSelected.value = rows
}

async function handleBatchRemoveFromGroup() {
  if (!editGroup.value || groupEditMembersSelected.value.length === 0) return
  await ElMessageBox.confirm(
    `确定将选中的 ${groupEditMembersSelected.value.length} 个客户移出该分组？`,
    '提示',
    { type: 'warning' }
  )
  await contactApi.removeMemberFromGroupBatch(
    editGroup.value.id,
    groupEditMembersSelected.value.map((c) => c.id)
  )
  ElMessage.success('已移出分组')
  groupEditMembersSelected.value = []
  groupMembersTableRef.value?.clearSelection()
  await loadGroupEditMembers()
  // 重新加载并过滤「添加客户到分组」列表，使被移出的客户重新出现
  await loadGroupEditAddList()
  const memberIds = new Set(groupEditMembers.value.map((m) => m.id))
  groupEditAddList.value = groupEditAddList.value.filter((c) => !memberIds.has(c.id))
}

async function handleBatchAddToEditGroup() {
  if (!editGroup.value || groupEditAddSelected.value.length === 0) return
  await contactApi.addMemberToGroupBatch(
    editGroup.value.id,
    groupEditAddSelected.value.map((c) => c.id)
  )
  ElMessage.success('已加入分组')
  groupEditAddSelected.value = []
  groupAddTableRef.value?.clearSelection()
  await loadGroupEditMembers()
  // 从「添加客户到分组」列表中移除已加入的客户
  const memberIds = new Set(groupEditMembers.value.map((m) => m.id))
  groupEditAddList.value = groupEditAddList.value.filter((c) => !memberIds.has(c.id))
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
.add-group-tip {
  margin: 0;
  color: #666;
  font-size: 13px;
}
.group-edit-content {
  padding: 0;
}
.group-edit-sort,
.group-edit-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.group-edit-sort-label {
  font-size: 13px;
  color: #606266;
}
.group-edit-section {
  margin: 16px 0 8px;
  font-size: 14px;
  color: #1a1a2e;
}
.group-edit-section:first-child {
  margin-top: 0;
}
.group-edit-toolbar {
  margin-bottom: 8px;
}
</style>
