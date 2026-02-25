// 学员相关类型定义

// 班级VO
export interface ClassVO {
  id: string
  departmentId: string
  departmentName: string
  schoolId: string
  schoolName: string
  name: string
  year?: string
  teacherId: string
  teacherName: string
  merk?: string
  joinStatus?: 'PENDING' | 'APPROVED' | 'REJECTED' // 学员加入状态
  joinType?: 'APPLY' | 'INVITE' // 加入类型
  joinAt?: string
  createdAt: string
  updatedAt: string
}

// 班级成员VO
export interface ClassMemberVO {
  userId: string
  username: string
  email: string
  avatar?: string
  studentNo?: string
  joinAt: string
  joinType: 'APPLY' | 'INVITE'
}

// 申请加入响应
export interface ApplyResponse {
  message: string
}

// 退出班级响应
export interface QuitResponse {
  message: string
}


