import * as yup from 'yup'

/**
 * 用户相关验证规则
 */
export const userSchema = yup.object({
  username: yup
    .string()
    .required('用户名必填')
    .min(2, '用户名至少2个字符')
    .max(20, '用户名最多20个字符')
    .matches(/^[\u4e00-\u9fa5a-zA-Z0-9_\s\u00b2\u00b3\u2070-\u2079\u2080-\u2089\u00b9\u00b0\u00aa\u00ba\u02b0-\u02ff\u0300-\u036f\u1d00-\u1dff\u1da0-\u1dff]+$/, '用户名格式不正确'),

  email: yup
    .string()
    .email('邮箱格式不正确')
    .nullable(),

  phone: yup
    .string()
    .matches(/^1\d{10}$/, '手机号格式不正确（1开头的11位数字）')
    .nullable(),

  password: yup
    .string()
    .min(8, '密码至少8个字符')
    .matches(/^(?=.*[A-Za-z])(?=.*\d)/, '密码必须包含字母和数字'),

  oldPassword: yup
    .string()
    .required('原密码必填'),

  newPassword: yup
    .string()
    .required('新密码必填')
    .min(8, '密码至少8个字符')
    .matches(/^(?=.*[A-Za-z])(?=.*\d)/, '密码必须包含字母和数字')
    .test('different', '新密码不能与原密码相同', function(value) {
      return value !== this.parent.oldPassword
    }),
})

/**
 * 角色相关验证规则
 */
export const roleSchema = yup.object({
  code: yup
    .string()
    .required('角色编码必填')
    .matches(/^[A-Z_]+$/, '角色编码只能包含大写字母和下划线')
    .max(64, '角色编码最多64个字符'),

  name: yup
    .string()
    .required('角色名称必填')
    .max(128, '角色名称最多128个字符'),

  description: yup
    .string()
    .max(255, '描述最多255个字符')
    .nullable(),

  level: yup
    .number()
    .min(0, '层级不能小于0')
    .max(1000, '层级不能大于1000')
    .integer('层级必须是整数')
    .nullable(),
})

/**
 * 权限相关验证规则
 */
export const permissionSchema = yup.object({
  resource: yup
    .string()
    .required('资源标识必填')
    .max(128, '资源标识最多128个字符'),

  action: yup
    .string()
    .required('操作动作必填')
    .max(64, '操作动作最多64个字符'),

  description: yup
    .string()
    .max(255, '描述最多255个字符')
    .nullable(),

  conditionJson: yup
    .string()
    .nullable(),
})

/**
 * 学校相关验证规则
 */
export const schoolSchema = yup.object({
  name: yup
    .string()
    .required('学校名称必填')
    .max(255, '学校名称最多255个字符'),

  contact: yup
    .string()
    .max(128, '联系方式最多128个字符')
    .nullable(),

  status: yup
    .number()
    .oneOf([0, 1, 2], '状态值必须是0、1或2')
    .nullable(),
})

/**
 * 用户更新验证规则（部分字段可选）
 */
export const userUpdateSchema = yup.object({
  email: yup
    .string()
    .email('邮箱格式不正确')
    .nullable(),

  phone: yup
    .string()
    .matches(/^1\d{10}$/, '手机号格式不正确')
    .nullable(),

  studentNo: yup
    .string()
    .max(64, '学号最多64个字符')
    .nullable(),
})

/**
 * 角色更新验证规则（部分字段可选）
 */
export const roleUpdateSchema = yup.object({
  name: yup
    .string()
    .max(128, '角色名称最多128个字符')
    .nullable(),

  description: yup
    .string()
    .max(255, '描述最多255个字符')
    .nullable(),

  level: yup
    .number()
    .min(0, '层级不能小于0')
    .max(1000, '层级不能大于1000')
    .integer('层级必须是整数')
    .nullable(),
})

/**
 * 权限更新验证规则（部分字段可选）
 */
export const permissionUpdateSchema = yup.object({
  description: yup
    .string()
    .max(255, '描述最多255个字符')
    .nullable(),

  conditionJson: yup
    .string()
    .nullable(),
})

/**
 * 学校更新验证规则（部分字段可选）
 */
export const schoolUpdateSchema = yup.object({
  name: yup
    .string()
    .max(255, '学校名称最多255个字符')
    .nullable(),

  contact: yup
    .string()
    .max(128, '联系方式最多128个字符')
    .nullable(),

  status: yup
    .number()
    .oneOf([0, 1, 2], '状态值必须是0、1或2')
    .nullable(),
})
