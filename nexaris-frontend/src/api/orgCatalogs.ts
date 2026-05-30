import { DEFAULT_ACCESS_RULE_ROLES } from '@/utils/roles'

export interface OrgCatalogs {
  accessRuleRoles: string[]
  nodeTypes: string[]
  linkCategories: string[]
  announcementSeverities: string[]
}

const STORAGE_KEY = 'nexaris_org_catalogs'

const DEFAULT_CATALOGS: OrgCatalogs = {
  accessRuleRoles: [...DEFAULT_ACCESS_RULE_ROLES],
  nodeTypes: ['ORGANIZATION', 'DIVISION', 'DEPARTMENT', 'TEAM', 'UNIT'],
  linkCategories: ['GENERAL', 'TOOLS', 'DOCS', 'HR', 'FINANCE', 'IT'],
  announcementSeverities: ['INFO', 'WARNING', 'CRITICAL', 'MAINTENANCE', 'INCIDENT'],
}

function uniqUpper(values: string[]) {
  return [...new Set(values.map(v => v.trim().toUpperCase()).filter(Boolean))]
}

function sanitize(catalogs: Partial<OrgCatalogs> | null | undefined): OrgCatalogs {
  return {
    accessRuleRoles: uniqUpper(catalogs?.accessRuleRoles ?? DEFAULT_CATALOGS.accessRuleRoles),
    nodeTypes: uniqUpper(catalogs?.nodeTypes ?? DEFAULT_CATALOGS.nodeTypes),
    linkCategories: uniqUpper(catalogs?.linkCategories ?? DEFAULT_CATALOGS.linkCategories),
    announcementSeverities: uniqUpper(catalogs?.announcementSeverities ?? DEFAULT_CATALOGS.announcementSeverities),
  }
}

export function getOrgCatalogs(): OrgCatalogs {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return { ...DEFAULT_CATALOGS }
  try {
    return sanitize(JSON.parse(raw) as Partial<OrgCatalogs>)
  } catch {
    return { ...DEFAULT_CATALOGS }
  }
}

export function saveOrgCatalogs(next: OrgCatalogs) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(sanitize(next)))
}

export function resetOrgCatalogs() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(DEFAULT_CATALOGS))
}
