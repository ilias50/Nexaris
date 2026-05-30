package com.nexaris.orgservice.controllers;

import com.nexaris.orgservice.dto.*;
import com.nexaris.orgservice.services.OrgService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/org")
public class OrgController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    @GetMapping("/tree")
    public List<NodeTreeResponse> getTree() {
        return orgService.getTree();
    }

    @GetMapping("/tree/my-tree")
    public List<MyNodeTreeResponse> getMyTree(@RequestHeader("X-User-Id") Integer userId) {
        return orgService.getMyTree(userId);
    }

    @GetMapping("/nodes/{nodeId}")
    public NodeResponse getNode(@PathVariable Integer nodeId,
                                @RequestHeader("X-User-Id") Integer userId) {
        return orgService.getNodeById(nodeId, userId);
    }

    @GetMapping("/nodes/by-path")
    public NodeResponse getNodeByPath(@RequestParam String path,
                                      @RequestHeader("X-User-Id") Integer userId) {
        return orgService.getNodeByPath(path, userId);
    }

    @GetMapping("/nodes/{nodeId}/children")
    public List<NodeResponse> getChildren(@PathVariable Integer nodeId,
                                          @RequestHeader("X-User-Id") Integer userId) {
        return orgService.getChildren(nodeId, userId);
    }

    @PostMapping("/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public NodeResponse createNode(@RequestBody CreateNodeRequest request,
                                   @RequestHeader("X-User-Id") Integer userId) {
        return orgService.createNode(request, userId);
    }

    @PatchMapping("/nodes/{nodeId}")
    public NodeResponse updateNode(@PathVariable Integer nodeId,
                                   @RequestBody UpdateNodeRequest request,
                                   @RequestHeader("X-User-Id") Integer userId) {
        return orgService.updateNode(nodeId, request, userId);
    }

    @DeleteMapping("/nodes/{nodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNode(@PathVariable Integer nodeId,
                           @RequestHeader("X-User-Id") Integer userId) {
        orgService.deleteNode(nodeId, userId);
    }

    @PatchMapping("/nodes/{nodeId}/move")
    public NodeResponse moveNode(@PathVariable Integer nodeId,
                                 @RequestBody MoveNodeRequest request,
                                 @RequestHeader("X-User-Id") Integer userId) {
        return orgService.moveNode(nodeId, request, userId);
    }

    @PatchMapping("/nodes/{nodeId}/children/order")
    public List<NodeResponse> reorderChildren(@PathVariable Integer nodeId,
                                              @RequestBody ReorderChildrenRequest request,
                                              @RequestHeader("X-User-Id") Integer userId) {
        return orgService.reorderChildren(nodeId, request, userId);
    }

    @GetMapping("/nodes/{nodeId}/content")
    public NodeContentResponse getNodeContent(@PathVariable Integer nodeId,
                                              @RequestHeader("X-User-Id") Integer userId) {
        return orgService.getNodeContent(nodeId, userId);
    }

    @PutMapping("/nodes/{nodeId}/content")
    public NodeContentResponse upsertNodeContent(@PathVariable Integer nodeId,
                                                 @RequestBody NodeContentRequest request,
                                                 @RequestHeader("X-User-Id") Integer userId) {
        return orgService.upsertNodeContent(nodeId, request, userId);
    }

    @GetMapping("/nodes/{nodeId}/links")
    public List<NodeLinkResponse> getNodeLinks(@PathVariable Integer nodeId,
                                               @RequestHeader("X-User-Id") Integer userId,
                                               @RequestParam(defaultValue = "true") boolean activeOnly) {
        return orgService.getNodeLinks(nodeId, activeOnly, userId);
    }

    @PostMapping("/nodes/{nodeId}/links")
    @ResponseStatus(HttpStatus.CREATED)
    public NodeLinkResponse createNodeLink(@PathVariable Integer nodeId,
                                           @RequestBody NodeLinkRequest request,
                                           @RequestHeader("X-User-Id") Integer userId) {
        return orgService.createNodeLink(nodeId, request, userId);
    }

    @PatchMapping("/links/{linkId}")
    public NodeLinkResponse updateNodeLink(@PathVariable Integer linkId,
                                           @RequestBody NodeLinkRequest request,
                                           @RequestHeader("X-User-Id") Integer userId) {
        return orgService.updateNodeLink(linkId, request, userId);
    }

    @DeleteMapping("/links/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNodeLink(@PathVariable Integer linkId,
                               @RequestHeader("X-User-Id") Integer userId) {
        orgService.deleteNodeLink(linkId, userId);
    }

    @PatchMapping("/nodes/{nodeId}/links/order")
    public List<NodeLinkResponse> reorderLinks(@PathVariable Integer nodeId,
                                               @RequestBody ReorderLinksRequest request,
                                               @RequestHeader("X-User-Id") Integer userId) {
        return orgService.reorderLinks(nodeId, request, userId);
    }

    @GetMapping("/nodes/{nodeId}/memberships")
    public List<NodeMembershipResponse> getNodeMemberships(@PathVariable Integer nodeId) {
        return orgService.getNodeMemberships(nodeId);
    }

    @GetMapping("/users/{userId}/memberships")
    public List<NodeMembershipResponse> getUserMemberships(@PathVariable Integer userId) {
        return orgService.getUserMemberships(userId);
    }

    @GetMapping("/users/{userId}/roles")
    public List<OrgUserRoleResponse> getUserGlobalRoles(@PathVariable Integer userId) {
        return orgService.getUserGlobalRoles(userId);
    }

    @PostMapping("/users/{userId}/roles")
    public List<OrgUserRoleResponse> assignUserGlobalRole(@PathVariable Integer userId,
                                                          @RequestHeader("X-User-Id") Integer actorUserId,
                                                          @RequestBody String roleName) {
        return orgService.assignUserGlobalRole(userId, roleName, actorUserId);
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    public List<OrgUserRoleResponse> removeUserGlobalRole(@PathVariable Integer userId,
                                                          @RequestHeader("X-User-Id") Integer actorUserId,
                                                          @PathVariable String roleName) {
        return orgService.removeUserGlobalRole(userId, roleName, actorUserId);
    }

    @PostMapping("/internal/users/{userId}/purge-rights")
    public UserRightsPurgeResponse purgeUserRights(@PathVariable Integer userId) {
        return orgService.purgeUserRights(userId);
    }

    @PostMapping("/nodes/{nodeId}/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    public NodeMembershipResponse createMembership(@PathVariable Integer nodeId,
                                                   @RequestBody NodeMembershipRequest request,
                                                   @RequestHeader("X-User-Id") Integer userId) {
        return orgService.createMembership(nodeId, request, userId);
    }

    @PatchMapping("/memberships/{membershipId}")
    public NodeMembershipResponse updateMembership(@PathVariable Integer membershipId,
                                                   @RequestBody NodeMembershipRequest request,
                                                   @RequestHeader("X-User-Id") Integer userId) {
        return orgService.updateMembership(membershipId, request, userId);
    }

    @DeleteMapping("/memberships/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMembership(@PathVariable Integer membershipId,
                                 @RequestHeader("X-User-Id") Integer userId) {
        orgService.deleteMembership(membershipId, userId);
    }

    @GetMapping("/nodes/{nodeId}/access-rules")
    public List<NodeAccessRuleResponse> getAccessRules(@PathVariable Integer nodeId) {
        return orgService.getAccessRules(nodeId);
    }

    @PostMapping("/nodes/{nodeId}/access-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public NodeAccessRuleResponse createAccessRule(@PathVariable Integer nodeId,
                                                   @RequestHeader("X-User-Id") Integer userId,
                                                   @RequestBody NodeAccessRuleRequest request) {
        return orgService.createAccessRule(nodeId, request, userId);
    }

    @DeleteMapping("/access-rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccessRule(@PathVariable Integer ruleId,
                                 @RequestHeader("X-User-Id") Integer userId) {
        orgService.deleteAccessRule(ruleId, userId);
    }

    @GetMapping("/nodes/{nodeId}/my-permissions")
    public PermissionsResponse getMyPermissions(@PathVariable Integer nodeId,
                                                @RequestHeader("X-User-Id") Integer userId) {
        return orgService.getMyPermissions(nodeId, userId);
    }

    @GetMapping("/announcements/active")
    public List<AnnouncementResponse> getActiveAnnouncements() {
        return orgService.getActiveAnnouncements();
    }

    @GetMapping("/nodes/{nodeId}/announcements")
    public List<AnnouncementResponse> getNodeAnnouncements(@PathVariable Integer nodeId,
                                                           @RequestHeader("X-User-Id") Integer userId) {
        return orgService.getNodeAnnouncements(nodeId, userId);
    }

    @PostMapping("/announcements")
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse createAnnouncement(@RequestBody AnnouncementRequest request,
                                                   @RequestHeader("X-User-Id") Integer userId) {
        return orgService.createAnnouncement(request, userId);
    }

    @PatchMapping("/announcements/{announcementId}")
    public AnnouncementResponse updateAnnouncement(@PathVariable Integer announcementId,
                                                   @RequestBody AnnouncementRequest request,
                                                   @RequestHeader("X-User-Id") Integer userId) {
        return orgService.updateAnnouncement(announcementId, request, userId);
    }

    @DeleteMapping("/announcements/{announcementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnnouncement(@PathVariable Integer announcementId,
                                   @RequestHeader("X-User-Id") Integer userId) {
        orgService.deleteAnnouncement(announcementId, userId);
    }

    // ── Catalog endpoints ───────────────────────────────────────────────

    @GetMapping("/catalogs/{catalogType}")
    public List<String> getCatalogValues(@PathVariable String catalogType) {
        return orgService.getCatalogValues(catalogType);
    }

    @PostMapping("/catalogs/{catalogType}")
    @ResponseStatus(HttpStatus.CREATED)
    public List<String> addCatalogValue(@PathVariable String catalogType,
                                        @RequestBody String value,
                                        @RequestHeader("X-User-Id") Integer userId) {
        orgService.addCatalogValue(catalogType, value, userId);
        return orgService.getCatalogValues(catalogType);
    }

    @DeleteMapping("/catalogs/{catalogType}/{value}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCatalogValue(@PathVariable String catalogType,
                                   @PathVariable String value,
                                   @RequestHeader("X-User-Id") Integer userId) {
        orgService.removeCatalogValue(catalogType, value, userId);
    }

    @GetMapping("/catalogs/{catalogType}/meta")
    public List<CatalogEntryDto> getCatalogMeta(@PathVariable String catalogType) {
        return orgService.getCatalogMeta(catalogType);
    }

    @PatchMapping("/catalogs/{catalogType}/{value}/color")
    public CatalogEntryDto updateCatalogColor(
            @PathVariable String catalogType,
            @PathVariable String value,
            @RequestBody(required = false) String color,
            @RequestHeader("X-User-Id") Integer userId) {
        return orgService.updateCatalogColor(catalogType, value, color, userId);
    }

    @GetMapping("/permissions/available")
    public List<String> getAvailablePermissions() {
        return orgService.getAvailablePermissionValues();
    }

    @GetMapping("/membership-roles/{membershipRole}/permissions")
    public List<String> getMembershipRolePermissions(@PathVariable String membershipRole) {
        return orgService.getMembershipRolePermissions(membershipRole);
    }

    @PutMapping("/membership-roles/{membershipRole}/permissions")
    public List<String> replaceMembershipRolePermissions(@PathVariable String membershipRole,
                                                         @RequestHeader("X-User-Id") Integer userId,
                                                         @RequestBody List<String> permissions) {
        return orgService.replaceMembershipRolePermissions(membershipRole, permissions, userId);
    }
}
