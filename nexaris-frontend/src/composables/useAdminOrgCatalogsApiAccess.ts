import { orgApi } from '@/api/org'
import type { CatalogEntry } from '@/api/org'

export function useAdminOrgCatalogsApiAccess() {
  async function getWorkspace(force = true) {
    return orgApi.getOrgCatalogWorkspace({ force })
  }

  async function updateCatalogColor(catalogType: string, value: string, color: string | null) {
    return orgApi.updateCatalogColor(catalogType, value, color)
  }

  async function addCatalogValue(catalogType: string, value: string) {
    return orgApi.addCatalogValue(catalogType, value)
  }

  async function removeCatalogValue(catalogType: string, value: string) {
    return orgApi.removeCatalogValue(catalogType, value)
  }

  function clearCatalogMetaCache(catalogType: string) {
    orgApi.clearCatalogMetaCache(catalogType)
  }

  async function getCatalogMetaCached(catalogType: string, force = true) {
    return orgApi.getCatalogMetaCached(catalogType, { force })
  }

  return {
    getWorkspace,
    updateCatalogColor,
    addCatalogValue,
    removeCatalogValue,
    clearCatalogMetaCache,
    getCatalogMetaCached,
  }
}

export type { CatalogEntry }
