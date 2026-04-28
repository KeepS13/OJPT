export type RoleType = 'USER' | 'ADMIN'

export const normalizeRoleType = (
  roleType?: string | null,
  roles?: readonly string[] | null,
): RoleType => {
  const hasAdminRole = roles?.some((role) => role?.toUpperCase() === 'ADMIN') ?? false
  if (hasAdminRole || roleType?.toUpperCase() === 'ADMIN') {
    return 'ADMIN'
  }
  return 'USER'
}

export const normalizeRoles = (
  roleType?: string | null,
  roles?: readonly string[] | null,
): RoleType[] => {
  return [normalizeRoleType(roleType, roles)]
}
