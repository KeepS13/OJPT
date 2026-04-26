import * as yup from 'yup'

export const userSchema = yup.object({
  username: yup
    .string()
    .required('用户名必填')
    .min(2, '用户名至少 2 个字符')
    .max(20, '用户名最多 20 个字符')
    .matches(/^[\u4e00-\u9fa5a-zA-Z0-9_\s\u00b2\u00b3\u2070-\u2079\u2080-\u2089\u00b9\u00b0\u00aa\u00ba\u02b0-\u02ff\u0300-\u036f\u1d00-\u1dff\u1da0-\u1dff]+$/, '用户名格式不正确'),

  email: yup
    .string()
    .email('邮箱格式不正确')
    .nullable(),

  phone: yup
    .string()
    .matches(/^1\d{10}$/, '手机号格式不正确')
    .nullable(),

  password: yup
    .string()
    .min(8, '密码至少 8 个字符')
    .matches(/^(?=.*[A-Za-z])(?=.*\d)/, '密码必须包含字母和数字'),

  oldPassword: yup
    .string()
    .required('原密码必填'),

  newPassword: yup
    .string()
    .required('新密码必填')
    .min(8, '密码至少 8 个字符')
    .matches(/^(?=.*[A-Za-z])(?=.*\d)/, '密码必须包含字母和数字')
    .test('different', '新密码不能与原密码相同', function (value) {
      return value !== this.parent.oldPassword
    }),
})

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
    .max(64, '学号最多 64 个字符')
    .nullable(),
})
