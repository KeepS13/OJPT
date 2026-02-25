<script setup lang="ts" generic="T">
import { ElTable, ElTableColumn, ElPagination, ElEmpty } from 'element-plus'

interface Column {
  prop?: string
  label: string
  width?: number | string
  minWidth?: number | string
  fixed?: 'left' | 'right' | boolean
  align?: 'left' | 'center' | 'right'
  sortable?: boolean
  slot?: string
}

interface Pagination {
  page: number
  size: number
  total: number
  pageSizes?: number[]
}

interface Props {
  data: T[]
  columns: Column[]
  loading?: boolean
  pagination?: Pagination | null
  emptyText?: string
  rowKey?: string
  stripe?: boolean
  border?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  pagination: null,
  emptyText: '暂无数据',
  rowKey: 'id',
  stripe: false,
  border: false,
})

const emit = defineEmits<{
  'page-change': [page: number]
  'size-change': [size: number]
  'row-click': [row: T]
}>()

const handlePageChange = (page: number) => {
  emit('page-change', page)
}

const handleSizeChange = (size: number) => {
  emit('size-change', size)
}

const handleRowClick = (row: T) => {
  emit('row-click', row)
}
</script>

<template>
  <div class="data-table">
    <el-table
      :data="data"
      v-loading="loading"
      :row-key="rowKey"
      :stripe="stripe"
      :border="border"
      style="width: 100%"
      @row-click="handleRowClick"
    >
      <template v-for="col in columns" :key="col.prop || col.slot">
        <el-table-column
          v-if="col.slot"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :fixed="col.fixed"
          :align="col.align"
          :sortable="col.sortable"
        >
          <template #default="scope">
            <slot :name="col.slot" v-bind="scope"></slot>
          </template>
        </el-table-column>
        <el-table-column
          v-else
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :fixed="col.fixed"
          :align="col.align"
          :sortable="col.sortable"
        />
      </template>
      
      <template #empty>
        <el-empty :description="emptyText" />
      </template>
    </el-table>

    <div v-if="pagination" class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="pagination.pageSizes || [10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.data-table {
  width: 100%;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
