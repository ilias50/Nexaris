package com.nexaris.orgservice.services;

import com.nexaris.orgservice.dto.*;
import com.nexaris.orgservice.entities.*;
import com.nexaris.orgservice.exceptions.BadRequestException;
import com.nexaris.orgservice.exceptions.ForbiddenException;
import com.nexaris.orgservice.exceptions.ResourceNotFoundException;
import com.nexaris.orgservice.repositories.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrgService {

    private static final Set<String> ALL_PERMISSIONS = Set.of(
            "READ",
            "EDIT_CONTENT",
            "EDIT_LINKS",
            "MANAGE_MEMBERS",
            "MANAGE_ACCESS",
            "MANAGE_ANNOUNCEMENTS",
            "CREATE_CHILD",
            "DELETE_NODE"
    );

    private static final Set<String> EDIT_PERMISSION_SET = Set.of(
            "EDIT_CONTENT", "EDIT_LINKS", "MANAGE_MEMBERS", "MANAGE_ACCESS",
            "MANAGE_ANNOUNCEMENTS", "CREATE_CHILD", "DELETE_NODE"
    );

        private static final String ROLE_ADMIN = "ROLE_ADMIN";

        private static final Set<String> ALLOWED_RULE_EFFECTS = Set.of("ALLOW", "DENY");
        private static final Set<String> ALLOWED_RULE_SUBJECT_TYPES = Set.of("USER", "ROLE", "MEMBERSHIP");

    private final OrganizationNodeRepository nodeRepository;
    private final NodeContentRepository contentRepository;
    private final NodeLinkRepository linkRepository;
    private final NodeMembershipRepository membershipRepository;
    private final OrgUserRoleRepository orgUserRoleRepository;
    private final NodeAccessRuleRepository accessRuleRepository;
    private final AnnouncementRepository announcementRepository;
    private final OrgCatalogEntryRepository catalogEntryRepository;
    private final MembershipRolePermissionRepository membershipRolePermissionRepository;
    private final NotificationClient notificationClient;

    public OrgService(
            OrganizationNodeRepository nodeRepository,
            NodeContentRepository contentRepository,
            NodeLinkRepository linkRepository,
            NodeMembershipRepository membershipRepository,
            OrgUserRoleRepository orgUserRoleRepository,
            NodeAccessRuleRepository accessRuleRepository,
            AnnouncementRepository announcementRepository,
                OrgCatalogEntryRepository catalogEntryRepository,
                MembershipRolePermissionRepository membershipRolePermissionRepository,
                NotificationClient notificationClient
    ) {
        this.nodeRepository = nodeRepository;
        this.contentRepository = contentRepository;
        this.linkRepository = linkRepository;
        this.membershipRepository = membershipRepository;
        this.orgUserRoleRepository = orgUserRoleRepository;
        this.accessRuleRepository = accessRuleRepository;
        this.announcementRepository = announcementRepository;
        this.catalogEntryRepository = catalogEntryRepository;
        this.membershipRolePermissionRepository = membershipRolePermissionRepository;
        this.notificationClient = notificationClient;
    }

    public List<NodeTreeResponse> getTree() {
        return nodeRepository.findByParentIsNullOrderBySortOrderAsc()
                .stream()
                .map(this::toTreeResponse)
                .toList();
    }

    public List<MyNodeTreeResponse> getMyTree(Integer userId) {
        if (userId == null) throw new BadRequestException("userId est obligatoire");

        Set<String> normalizedRoles = resolveGlobalRoles(userId);

        if (normalizedRoles.contains("ROLE_ADMIN")) {
            // Admins see the full tree with full edit rights
            return nodeRepository.findByParentIsNullOrderBySortOrderAsc()
                    .stream()
                    .map(root -> toMyTreeResponse(root, true))
                    .toList();
        }

        // Pre-load all memberships for this user (avoids N+1)
        Map<Integer, Set<String>> membershipByNode = new HashMap<>();
        for (NodeMembership m : membershipRepository.findByUserId(userId)) {
            membershipByNode.computeIfAbsent(m.getNode().getId(), k -> new HashSet<>())
                    .add(m.getMembershipRole().toUpperCase());
        }

        // Pre-load all access rules (avoids N+1)
        Map<Integer, List<NodeAccessRule>> rulesByNode = new HashMap<>();
        for (NodeAccessRule rule : accessRuleRepository.findAll()) {
            rulesByNode.computeIfAbsent(rule.getNode().getId(), k -> new ArrayList<>()).add(rule);
        }

        return nodeRepository.findByParentIsNullOrderBySortOrderAsc()
                .stream()
            .flatMap(root -> buildVisibleNodes(root, userId, normalizedRoles, membershipByNode, rulesByNode, List.of()).stream())
                .toList();
    }

        private List<MyNodeTreeResponse> buildVisibleNodes(
            OrganizationNode node,
            Integer userId,
            Set<String> globalRoles,
            Map<Integer, Set<String>> membershipByNode,
            Map<Integer, List<NodeAccessRule>> rulesByNode,
            List<OrganizationNode> ancestors
    ) {
        Set<String> perms = computeEffectivePermissions(node, userId, globalRoles, membershipByNode, rulesByNode, ancestors);
        boolean canRead = perms.contains("READ");
        boolean canEdit = perms.stream().anyMatch(EDIT_PERMISSION_SET::contains);

        List<OrganizationNode> nextAncestors = new ArrayList<>(ancestors);
        nextAncestors.add(node);

        List<MyNodeTreeResponse> visibleChildren = nodeRepository.findByParentIdOrderBySortOrderAsc(node.getId())
                .stream()
            .flatMap(child -> buildVisibleNodes(child, userId, globalRoles, membershipByNode, rulesByNode, nextAncestors).stream())
                .toList();

        // Hide unreadable nodes themselves but keep readable descendants accessible in the tree.
        if (!canRead) {
            return visibleChildren;
        }

        return List.of(new MyNodeTreeResponse(
            node.getId(),
            node.getParent() == null ? null : node.getParent().getId(),
            node.getNodeType(),
            node.getName(),
            node.getSlug(),
            node.getPath(),
            node.getDepth(),
            node.getSortOrder(),
            node.isActive(),
            canEdit,
            visibleChildren
        ));
    }

    private Set<String> computeEffectivePermissions(
            OrganizationNode node,
            Integer userId,
            Set<String> globalRoles,
            Map<Integer, Set<String>> membershipByNode,
            Map<Integer, List<NodeAccessRule>> rulesByNode,
            List<OrganizationNode> ancestors
    ) {
        Set<String> permissions = new HashSet<>();

        // Direct memberships on the target node grant base permissions
        Set<String> membershipRoles = membershipByNode.getOrDefault(node.getId(), Set.of());
        List<String> membershipRolesList = new ArrayList<>(membershipRoles);
        permissions.addAll(resolveMembershipPermissions(membershipRoles));

        // Apply access rules from ancestors (inherited) then from the node itself
        List<OrganizationNode> hierarchy = new ArrayList<>(ancestors);
        hierarchy.add(node);

        for (int i = 0; i < hierarchy.size(); i++) {
            OrganizationNode scopedNode = hierarchy.get(i);
            boolean isTargetNode = (i == hierarchy.size() - 1);
            for (NodeAccessRule rule : rulesByNode.getOrDefault(scopedNode.getId(), List.of())) {
                if (!isTargetNode && !rule.isAppliesToChildren()) continue;
                if (!matchesRule(rule, userId, globalRoles, membershipRolesList)) continue;
                String perm = rule.getPermission().toUpperCase();
                if ("DENY".equalsIgnoreCase(rule.getEffect())) {
                    permissions.remove(perm);
                } else {
                    permissions.add(perm);
                }
            }
        }

        return permissions;
    }

    private Set<String> resolveGlobalRoles(Integer userId) {
        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");

        // Roles coming from auth-service via gateway headers (trusted by internal secret).
        roles.addAll(resolveGatewayRolesFromCurrentRequest());

        orgUserRoleRepository.findByUserIdOrderByRoleNameAsc(userId).forEach(r -> {
            if (r.getRoleName() != null && !r.getRoleName().isBlank()) {
                roles.add(r.getRoleName().trim().toUpperCase());
            }
        });
        return roles;
    }

    private Set<String> resolveGatewayRolesFromCurrentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return Set.of();
        }

        HttpServletRequest request = servletAttrs.getRequest();
        if (request == null) {
            return Set.of();
        }

        String rolesHeader = request.getHeader("X-User-Roles");
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    private MyNodeTreeResponse toMyTreeResponse(OrganizationNode node, boolean canEdit) {
        List<MyNodeTreeResponse> children = nodeRepository.findByParentIdOrderBySortOrderAsc(node.getId())
                .stream()
                .map(child -> toMyTreeResponse(child, canEdit))
                .toList();
        return new MyNodeTreeResponse(
                node.getId(),
                node.getParent() == null ? null : node.getParent().getId(),
                node.getNodeType(),
                node.getName(),
                node.getSlug(),
                node.getPath(),
                node.getDepth(),
                node.getSortOrder(),
                node.isActive(),
                canEdit,
                children
        );
    }

    public NodeResponse getNodeById(Integer nodeId, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "READ");
        return toNodeResponse(getNodeOrThrow(nodeId));
    }

    public NodeResponse getNodeByPath(String path, Integer actorUserId) {
        OrganizationNode node = nodeRepository.findByPath(path)
                .orElseThrow(() -> new ResourceNotFoundException("Noeud introuvable pour path=" + path));
        requirePermissionOnNode(node.getId(), actorUserId, "READ");
        return toNodeResponse(node);
    }

    public List<NodeResponse> getChildren(Integer nodeId, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "READ");
        getNodeOrThrow(nodeId);
        return nodeRepository.findByParentIdOrderBySortOrderAsc(nodeId)
                .stream()
                .map(this::toNodeResponse)
                .toList();
    }

    @Transactional
    public NodeResponse createNode(CreateNodeRequest request, Integer actorUserId) {
        validateRequired(request.nodeType(), "nodeType");
        validateRequired(request.name(), "name");
        validateRequired(request.slug(), "slug");

        OrganizationNode parent = null;
        if (request.parentId() != null) {
            parent = getNodeOrThrow(request.parentId());
            requirePermissionOnNode(parent.getId(), actorUserId, "CREATE_CHILD");
            if (nodeRepository.existsByParentIdAndSlug(parent.getId(), request.slug())) {
                throw new BadRequestException("Un noeud avec ce slug existe deja sous le meme parent");
            }
        } else {
            requireAdmin(actorUserId);
        }

        String path = buildPath(parent, request.slug());
        if (nodeRepository.existsByPath(path)) {
            throw new BadRequestException("Un noeud avec ce path existe deja: " + path);
        }

        OrganizationNode node = new OrganizationNode();
        node.setParent(parent);
        node.setNodeType(request.nodeType().trim().toUpperCase());
        node.setName(request.name().trim());
        node.setSlug(normalizeSlug(request.slug()));
        node.setPath(path);
        node.setDepth(parent == null ? 0 : parent.getDepth() + 1);
        node.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        node.setActive(request.isActive() == null || request.isActive());

        return toNodeResponse(nodeRepository.save(node));
    }

    @Transactional
    public NodeResponse updateNode(Integer nodeId, UpdateNodeRequest request, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "EDIT_CONTENT");
        OrganizationNode node = getNodeOrThrow(nodeId);

        if (request.nodeType() != null && !request.nodeType().isBlank()) {
            node.setNodeType(request.nodeType().trim().toUpperCase());
        }
        if (request.name() != null && !request.name().isBlank()) {
            node.setName(request.name().trim());
        }
        if (request.sortOrder() != null) {
            node.setSortOrder(request.sortOrder());
        }
        if (request.isActive() != null) {
            node.setActive(request.isActive());
        }

        boolean slugChanged = false;
        if (request.slug() != null && !request.slug().isBlank()) {
            String newSlug = normalizeSlug(request.slug());
            if (!newSlug.equals(node.getSlug())) {
                Integer parentId = node.getParent() == null ? null : node.getParent().getId();
                if (parentId != null && nodeRepository.existsByParentIdAndSlug(parentId, newSlug)) {
                    throw new BadRequestException("Un noeud avec ce slug existe deja sous le meme parent");
                }
                node.setSlug(newSlug);
                slugChanged = true;
            }
        }

        nodeRepository.save(node);
        if (slugChanged) {
            refreshSubtreePath(node);
        }

        return toNodeResponse(node);
    }

    @Transactional
    public void deleteNode(Integer nodeId, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "DELETE_NODE");
        OrganizationNode node = getNodeOrThrow(nodeId);
        if (node.getParent() == null) {
            throw new BadRequestException("Le noeud racine ne peut pas etre supprime");
        }
        deleteNodeSubtree(node);
    }

    private void deleteNodeSubtree(OrganizationNode node) {
        List<OrganizationNode> children = nodeRepository.findByParentIdOrderBySortOrderAsc(node.getId());
        for (OrganizationNode child : children) {
            deleteNodeSubtree(child);
        }

        List<Announcement> announcements = announcementRepository.findByNodeId(node.getId());
        if (!announcements.isEmpty()) {
            announcementRepository.deleteAll(announcements);
        }

        nodeRepository.delete(node);
    }

    @Transactional
    public NodeResponse moveNode(Integer nodeId, MoveNodeRequest request, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "DELETE_NODE");
        OrganizationNode node = getNodeOrThrow(nodeId);
        OrganizationNode newParent = null;

        if (request.newParentId() != null) {
            requirePermissionOnNode(request.newParentId(), actorUserId, "CREATE_CHILD");
            newParent = getNodeOrThrow(request.newParentId());
            if (isDescendant(node, newParent)) {
                throw new BadRequestException("Impossible de deplacer un noeud dans son propre sous-arbre");
            }
            if (nodeRepository.existsByParentIdAndSlug(newParent.getId(), node.getSlug())) {
                throw new BadRequestException("Un noeud avec ce slug existe deja sous le parent cible");
            }
        } else {
            requireAdmin(actorUserId);
        }

        node.setParent(newParent);
        if (request.newSortOrder() != null) {
            node.setSortOrder(request.newSortOrder());
        }
        node.setDepth(newParent == null ? 0 : newParent.getDepth() + 1);
        node.setPath(buildPath(newParent, node.getSlug()));
        nodeRepository.save(node);

        refreshChildrenDepthAndPath(node);
        return toNodeResponse(node);
    }

    @Transactional
    public List<NodeResponse> reorderChildren(Integer nodeId, ReorderChildrenRequest request, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "CREATE_CHILD");
        getNodeOrThrow(nodeId);
        List<OrganizationNode> children = nodeRepository.findByParentIdOrderBySortOrderAsc(nodeId);
        Map<Integer, OrganizationNode> childById = new HashMap<>();
        for (OrganizationNode child : children) {
            childById.put(child.getId(), child);
        }

        for (NodeOrderItemRequest item : safeItems(request.items())) {
            OrganizationNode child = childById.get(item.nodeId());
            if (child == null) {
                throw new BadRequestException("Enfant introuvable dans ce parent: " + item.nodeId());
            }
            child.setSortOrder(item.sortOrder() == null ? 0 : item.sortOrder());
        }

        nodeRepository.saveAll(children);
        return nodeRepository.findByParentIdOrderBySortOrderAsc(nodeId)
                .stream()
                .map(this::toNodeResponse)
                .toList();
    }

    public NodeContentResponse getNodeContent(Integer nodeId, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "READ");
        getNodeOrThrow(nodeId);
        return contentRepository.findByNodeId(nodeId)
                .map(this::toNodeContentResponse)
                .orElse(null);
    }

    @Transactional
    public NodeContentResponse upsertNodeContent(Integer nodeId, NodeContentRequest request, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "EDIT_CONTENT");
        OrganizationNode node = getNodeOrThrow(nodeId);
        NodeContent content = contentRepository.findByNodeId(nodeId).orElseGet(NodeContent::new);
        content.setNode(node);
        content.setSummary(request.summary());
        content.setDescription(request.description());
        content.setContactEmail(request.contactEmail());
        content.setLocation(request.location());
        content.setMetadataJson(request.metadataJson());
        return toNodeContentResponse(contentRepository.save(content));
    }

    public List<NodeLinkResponse> getNodeLinks(Integer nodeId, boolean activeOnly, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "READ");
        getNodeOrThrow(nodeId);
        List<NodeLink> links = activeOnly
                ? linkRepository.findByNodeIdAndIsActiveTrueOrderBySortOrderAsc(nodeId)
                : linkRepository.findByNodeIdOrderBySortOrderAsc(nodeId);
        return links.stream().map(this::toNodeLinkResponse).toList();
    }

    @Transactional
    public NodeLinkResponse createNodeLink(Integer nodeId, NodeLinkRequest request, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "EDIT_LINKS");
        OrganizationNode node = getNodeOrThrow(nodeId);
        validateRequired(request.label(), "label");
        validateRequired(request.url(), "url");

        NodeLink link = new NodeLink();
        link.setNode(node);
        link.setLabel(request.label().trim());
        link.setUrl(request.url().trim());
        link.setCategory(request.category() == null || request.category().isBlank() ? "GENERAL" : request.category().trim().toUpperCase());
        link.setIcon(request.icon());
        link.setVisibility(request.visibility() == null || request.visibility().isBlank() ? "INHERIT" : request.visibility().trim().toUpperCase());
        link.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        link.setActive(request.isActive() == null || request.isActive());
        return toNodeLinkResponse(linkRepository.save(link));
    }

    @Transactional
    public NodeLinkResponse updateNodeLink(Integer linkId, NodeLinkRequest request, Integer actorUserId) {
        NodeLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Lien introuvable: " + linkId));
        requirePermissionOnNode(link.getNode().getId(), actorUserId, "EDIT_LINKS");

        if (request.label() != null && !request.label().isBlank()) {
            link.setLabel(request.label().trim());
        }
        if (request.url() != null && !request.url().isBlank()) {
            link.setUrl(request.url().trim());
        }
        if (request.category() != null && !request.category().isBlank()) {
            link.setCategory(request.category().trim().toUpperCase());
        }
        if (request.icon() != null) {
            link.setIcon(request.icon());
        }
        if (request.visibility() != null && !request.visibility().isBlank()) {
            link.setVisibility(request.visibility().trim().toUpperCase());
        }
        if (request.sortOrder() != null) {
            link.setSortOrder(request.sortOrder());
        }
        if (request.isActive() != null) {
            link.setActive(request.isActive());
        }

        return toNodeLinkResponse(linkRepository.save(link));
    }

    @Transactional
    public void deleteNodeLink(Integer linkId, Integer actorUserId) {
        NodeLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Lien introuvable: " + linkId));
        requirePermissionOnNode(link.getNode().getId(), actorUserId, "EDIT_LINKS");
        linkRepository.delete(link);
    }

    @Transactional
    public List<NodeLinkResponse> reorderLinks(Integer nodeId, ReorderLinksRequest request, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "EDIT_LINKS");
        getNodeOrThrow(nodeId);
        List<NodeLink> links = linkRepository.findByNodeIdOrderBySortOrderAsc(nodeId);
        Map<Integer, NodeLink> byId = new HashMap<>();
        for (NodeLink link : links) {
            byId.put(link.getId(), link);
        }

        for (NodeOrderItemRequest item : safeItems(request.items())) {
            NodeLink link = byId.get(item.nodeId());
            if (link == null) {
                throw new BadRequestException("Lien introuvable pour ce noeud: " + item.nodeId());
            }
            link.setSortOrder(item.sortOrder() == null ? 0 : item.sortOrder());
        }

        linkRepository.saveAll(links);
        return linkRepository.findByNodeIdOrderBySortOrderAsc(nodeId)
                .stream()
                .map(this::toNodeLinkResponse)
                .toList();
    }

    public List<NodeMembershipResponse> getNodeMemberships(Integer nodeId) {
        getNodeOrThrow(nodeId);
        return membershipRepository.findByNodeId(nodeId).stream().map(this::toNodeMembershipResponse).toList();
    }

    public List<NodeMembershipResponse> getUserMemberships(Integer userId) {
        return membershipRepository.findByUserId(userId).stream().map(this::toNodeMembershipResponse).toList();
    }

    @Transactional
    public NodeMembershipResponse createMembership(Integer nodeId, NodeMembershipRequest request, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "MANAGE_MEMBERS");
        OrganizationNode node = getNodeOrThrow(nodeId);
        if (request.userId() == null) {
            throw new BadRequestException("userId est obligatoire");
        }

        String role = normalizeMembershipRole(request.membershipRole());
        validateMembershipRoleAllowed(role);
        membershipRepository.findByNodeIdAndUserIdAndMembershipRole(nodeId, request.userId(), role)
                .ifPresent(existing -> {
                    throw new BadRequestException("Ce membership existe deja");
                });

        NodeMembership membership = new NodeMembership();
        membership.setNode(node);
        membership.setUserId(request.userId());
        membership.setMembershipRole(role);
        membership.setPrimary(request.isPrimary() != null && request.isPrimary());
        membership.setActiveFrom(request.activeFrom());
        membership.setActiveTo(request.activeTo());
        return toNodeMembershipResponse(membershipRepository.save(membership));
    }

    @Transactional
    public NodeMembershipResponse updateMembership(Integer membershipId, NodeMembershipRequest request, Integer actorUserId) {
        NodeMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership introuvable: " + membershipId));
        requirePermissionOnNode(membership.getNode().getId(), actorUserId, "MANAGE_MEMBERS");

        if (request.membershipRole() != null && !request.membershipRole().isBlank()) {
            String role = normalizeMembershipRole(request.membershipRole());
            validateMembershipRoleAllowed(role);
            membership.setMembershipRole(role);
        }
        if (request.isPrimary() != null) {
            membership.setPrimary(request.isPrimary());
        }
        if (request.activeFrom() != null || request.activeTo() != null) {
            membership.setActiveFrom(request.activeFrom());
            membership.setActiveTo(request.activeTo());
        }

        return toNodeMembershipResponse(membershipRepository.save(membership));
    }

    @Transactional
    public void deleteMembership(Integer membershipId, Integer actorUserId) {
        NodeMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership introuvable: " + membershipId));
        requirePermissionOnNode(membership.getNode().getId(), actorUserId, "MANAGE_MEMBERS");
        membershipRepository.delete(membership);
    }

    public List<NodeAccessRuleResponse> getAccessRules(Integer nodeId) {
        getNodeOrThrow(nodeId);
        return accessRuleRepository.findByNodeId(nodeId).stream().map(this::toNodeAccessRuleResponse).toList();
    }

    @Transactional
    public NodeAccessRuleResponse createAccessRule(Integer nodeId, NodeAccessRuleRequest request, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "MANAGE_ACCESS");
        OrganizationNode node = getNodeOrThrow(nodeId);
        validateRequired(request.subjectType(), "subjectType");
        validateRequired(request.subjectValue(), "subjectValue");
        validateRequired(request.permission(), "permission");

        String effect = request.effect() == null || request.effect().isBlank()
                ? "ALLOW"
                : request.effect().trim().toUpperCase();
        String subjectType = request.subjectType().trim().toUpperCase();
        String subjectValue = request.subjectValue().trim();
        String permission = request.permission().trim().toUpperCase();

        if (!ALLOWED_RULE_EFFECTS.contains(effect)) {
            throw new BadRequestException("effect invalide: " + effect);
        }
        if (!ALLOWED_RULE_SUBJECT_TYPES.contains(subjectType)) {
            throw new BadRequestException("subjectType invalide: " + subjectType);
        }
        if (!ALL_PERMISSIONS.contains(permission)) {
            throw new BadRequestException("permission invalide: " + permission);
        }
        if (subjectValue.isBlank()) {
            throw new BadRequestException("subjectValue est obligatoire");
        }
        if ("ROLE".equals(subjectType)) {
            validateCatalogValueExists("ACCESS_RULE_ROLE", subjectValue.toUpperCase(), "Role de regle d'acces non autorise");
        }
        if ("MEMBERSHIP".equals(subjectType)) {
            validateCatalogValueExists("MEMBERSHIP_ROLE", subjectValue.toUpperCase(), "Role membership non autorise");
        }

        NodeAccessRule rule = new NodeAccessRule();
        rule.setNode(node);
        rule.setEffect(effect);
        rule.setSubjectType(subjectType);
        rule.setSubjectValue(subjectValue);
        rule.setPermission(permission);
        rule.setAppliesToChildren(request.appliesToChildren() != null && request.appliesToChildren());
        return toNodeAccessRuleResponse(accessRuleRepository.save(rule));
    }

    @Transactional
    public void deleteAccessRule(Integer ruleId, Integer actorUserId) {
        NodeAccessRule rule = accessRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Regle introuvable: " + ruleId));
        requirePermissionOnNode(rule.getNode().getId(), actorUserId, "MANAGE_ACCESS");
        accessRuleRepository.delete(rule);
    }

    public PermissionsResponse getMyPermissions(Integer nodeId, Integer userId) {
        if (userId == null) {
            throw new BadRequestException("userId est obligatoire");
        }

        OrganizationNode node = getNodeOrThrow(nodeId);
        Set<String> normalizedGlobalRoles = resolveGlobalRoles(userId);

        if (normalizedGlobalRoles.contains(ROLE_ADMIN)) {
            return new PermissionsResponse(nodeId, userId, new HashSet<>(ALL_PERMISSIONS));
        }

        List<String> membershipRoles = membershipRepository.findByNodeIdAndUserId(nodeId, userId)
                .stream()
                .map(NodeMembership::getMembershipRole)
                .map(String::toUpperCase)
                .toList();

        Set<String> permissions = new HashSet<>();
        permissions.addAll(resolveMembershipPermissions(membershipRoles));

        List<OrganizationNode> hierarchy = new ArrayList<>();
        OrganizationNode current = node;
        while (current != null) {
            hierarchy.add(current);
            current = current.getParent();
        }
        Collections.reverse(hierarchy);

        for (OrganizationNode scopedNode : hierarchy) {
            boolean isCurrentNode = Objects.equals(scopedNode.getId(), nodeId);
            List<NodeAccessRule> rules = accessRuleRepository.findByNodeId(scopedNode.getId());
            for (NodeAccessRule rule : rules) {
                if (!isCurrentNode && !rule.isAppliesToChildren()) {
                    continue;
                }
                if (!matchesRule(rule, userId, normalizedGlobalRoles, membershipRoles)) {
                    continue;
                }

                String permission = rule.getPermission().toUpperCase();
                if ("DENY".equalsIgnoreCase(rule.getEffect())) {
                    permissions.remove(permission);
                } else {
                    permissions.add(permission);
                }
            }
        }

        return new PermissionsResponse(nodeId, userId, permissions);
    }

    public List<OrgUserRoleResponse> getUserGlobalRoles(Integer userId) {
        List<OrgUserRoleResponse> roles = orgUserRoleRepository.findByUserIdOrderByRoleNameAsc(userId)
            .stream()
            .map(r -> new OrgUserRoleResponse(r.getId(), r.getUserId(), r.getRoleName()))
            .collect(Collectors.toCollection(ArrayList::new));

        boolean hasRoleUser = roles.stream()
            .map(OrgUserRoleResponse::roleName)
            .filter(Objects::nonNull)
            .map(String::trim)
            .map(String::toUpperCase)
            .anyMatch("ROLE_USER"::equals);

        if (!hasRoleUser) {
            roles.add(new OrgUserRoleResponse(null, userId, "ROLE_USER"));
        }

        roles.sort(Comparator.comparing(OrgUserRoleResponse::roleName, String.CASE_INSENSITIVE_ORDER));
        return roles;
    }

    @Transactional
    public List<OrgUserRoleResponse> assignUserGlobalRole(Integer userId, String roleName, Integer actorUserId) {
        requireAdmin(actorUserId);
        String normalizedRole = normalizeGlobalRole(roleName);
        boolean allowed = catalogEntryRepository
                .findByCatalogTypeAndValue("ACCESS_RULE_ROLE", normalizedRole)
                .isPresent();
        if (!allowed) {
            throw new BadRequestException("Role global non autorise: " + normalizedRole);
        }

        orgUserRoleRepository.findByUserIdAndRoleName(userId, normalizedRole).orElseGet(() -> {
            OrgUserRole role = new OrgUserRole();
            role.setUserId(userId);
            role.setRoleName(normalizedRole);
            return orgUserRoleRepository.save(role);
        });

        return getUserGlobalRoles(userId);
    }

    @Transactional
    public List<OrgUserRoleResponse> removeUserGlobalRole(Integer userId, String roleName, Integer actorUserId) {
        requireAdmin(actorUserId);
        String normalizedRole = normalizeGlobalRole(roleName);
        if ("ROLE_USER".equals(normalizedRole)) {
            throw new BadRequestException("Le role ROLE_USER est obligatoire et ne peut pas etre supprime");
        }
        orgUserRoleRepository.deleteByUserIdAndRoleName(userId, normalizedRole);
        return getUserGlobalRoles(userId);
    }

    @Transactional
    public UserRightsPurgeResponse purgeUserRights(Integer userId) {
        if (userId == null) {
            throw new BadRequestException("userId est obligatoire");
        }

        String subjectValue = String.valueOf(userId);

        long removedUserAccessRules = accessRuleRepository.deleteBySubjectTypeAndSubjectValue("USER", subjectValue);
        long removedMemberships = membershipRepository.deleteByUserId(userId);
        long removedGlobalRoles = orgUserRoleRepository.deleteByUserId(userId);

        return new UserRightsPurgeResponse(
                userId,
                removedGlobalRoles,
                removedMemberships,
                removedUserAccessRules
        );
    }

    public List<AnnouncementResponse> getActiveAnnouncements() {
        LocalDateTime now = LocalDateTime.now();
        return announcementRepository.findByIsActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqual(now, now)
                .stream()
                .map(this::toAnnouncementResponse)
                .toList();
    }

    public List<AnnouncementResponse> getNodeAnnouncements(Integer nodeId, Integer actorUserId) {
        requirePermissionOnNode(nodeId, actorUserId, "READ");
        getNodeOrThrow(nodeId);
        return announcementRepository.findByNodeIdAndIsActiveTrue(nodeId)
                .stream()
                .map(this::toAnnouncementResponse)
                .toList();
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request, Integer actorUserId) {
        validateRequired(request.title(), "title");
        validateRequired(request.body(), "body");

        if (request.nodeId() != null) {
            requirePermissionOnNode(request.nodeId(), actorUserId, "MANAGE_ANNOUNCEMENTS");
        } else {
            requireAdmin(actorUserId);
        }

        Announcement announcement = new Announcement();
        if (request.nodeId() != null) {
            announcement.setNode(getNodeOrThrow(request.nodeId()));
        }
        announcement.setScopeType(request.scopeType() == null || request.scopeType().isBlank() ? "GLOBAL" : request.scopeType().trim().toUpperCase());
        announcement.setTitle(request.title().trim());
        announcement.setBody(request.body().trim());
        announcement.setSeverity(request.severity() == null || request.severity().isBlank() ? "INFO" : request.severity().trim().toUpperCase());
        announcement.setStartAt(request.startAt());
        announcement.setEndAt(request.endAt());
        announcement.setActive(request.isActive() == null || request.isActive());
        announcement.setCreatedByUserId(request.createdByUserId() != null ? request.createdByUserId() : actorUserId);
        Announcement saved = announcementRepository.save(announcement);
        notifyAnnouncementChange(saved, actorUserId, "ORG_ANNOUNCEMENT_CREATED");
        return toAnnouncementResponse(saved);
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(Integer announcementId, AnnouncementRequest request, Integer actorUserId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Annonce introuvable: " + announcementId));

        if (announcement.getNode() != null) {
            requirePermissionOnNode(announcement.getNode().getId(), actorUserId, "MANAGE_ANNOUNCEMENTS");
        } else {
            requireAdmin(actorUserId);
        }

        if (request.nodeId() != null) {
            announcement.setNode(getNodeOrThrow(request.nodeId()));
        }
        if (request.scopeType() != null && !request.scopeType().isBlank()) {
            announcement.setScopeType(request.scopeType().trim().toUpperCase());
        }
        if (request.title() != null && !request.title().isBlank()) {
            announcement.setTitle(request.title().trim());
        }
        if (request.body() != null && !request.body().isBlank()) {
            announcement.setBody(request.body().trim());
        }
        if (request.severity() != null && !request.severity().isBlank()) {
            announcement.setSeverity(request.severity().trim().toUpperCase());
        }
        if (request.startAt() != null || request.endAt() != null) {
            announcement.setStartAt(request.startAt());
            announcement.setEndAt(request.endAt());
        }
        if (request.isActive() != null) {
            announcement.setActive(request.isActive());
        }
        if (request.createdByUserId() != null) {
            announcement.setCreatedByUserId(request.createdByUserId());
        }

        Announcement saved = announcementRepository.save(announcement);
        notifyAnnouncementChange(saved, actorUserId, "ORG_ANNOUNCEMENT_UPDATED");
        return toAnnouncementResponse(saved);
    }

    @Transactional
    public void deleteAnnouncement(Integer announcementId, Integer actorUserId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Annonce introuvable: " + announcementId));
        if (announcement.getNode() != null) {
            requirePermissionOnNode(announcement.getNode().getId(), actorUserId, "MANAGE_ANNOUNCEMENTS");
        } else {
            requireAdmin(actorUserId);
        }
        String title = announcement.getTitle();
        String severity = announcement.getSeverity();
        Integer nodeId = announcement.getNode() != null ? announcement.getNode().getId() : null;
        String scopeType = announcement.getScopeType();

        announcementRepository.delete(announcement);
        notifyAnnouncementChange(title, severity, nodeId, scopeType, actorUserId, "ORG_ANNOUNCEMENT_DELETED");
    }

    private void notifyAnnouncementChange(Announcement announcement, Integer actorUserId, String notificationType) {
        Integer nodeId = announcement.getNode() != null ? announcement.getNode().getId() : null;
        notifyAnnouncementChange(
                announcement.getTitle(),
                announcement.getSeverity(),
                nodeId,
                announcement.getScopeType(),
                actorUserId,
                notificationType
        );
    }

    private void notifyAnnouncementChange(String title,
                                          String severity,
                                          Integer nodeId,
                                          String scopeType,
                                          Integer actorUserId,
                                          String notificationType) {
        Set<Integer> recipients = resolveAnnouncementRecipients(nodeId, scopeType);
        if (actorUserId != null) {
            recipients.remove(actorUserId);
        }
        if (recipients.isEmpty()) {
            return;
        }

        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("announcementTitle", title != null && !title.isBlank() ? title : "-");
        templateParams.put("severity", severity != null && !severity.isBlank() ? severity : "INFO");
        templateParams.put("scope", nodeId != null ? "node-" + nodeId : "global");

        notificationClient.sendNotification(
                new ArrayList<>(recipients),
                "/organization",
            notificationType,
            templateParams
        );
    }

    private Set<Integer> resolveAnnouncementRecipients(Integer nodeId, String scopeType) {
        Set<Integer> recipients = new HashSet<>();

        boolean nodeScoped = nodeId != null && (scopeType == null || !"GLOBAL".equalsIgnoreCase(scopeType.trim()));
        if (nodeScoped) {
            Set<Integer> subtreeNodeIds = collectSubtreeNodeIds(nodeId);
            recipients.addAll(membershipRepository.findDistinctUserIdsByNodeIds(subtreeNodeIds));
            return recipients;
        }

        recipients.addAll(membershipRepository.findDistinctUserIds());
        recipients.addAll(orgUserRoleRepository.findDistinctUserIds());
        return recipients;
    }

    private Set<Integer> collectSubtreeNodeIds(Integer rootNodeId) {
        Set<Integer> ids = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(rootNodeId);

        while (!stack.isEmpty()) {
            Integer current = stack.pop();
            if (!ids.add(current)) {
                continue;
            }
            List<OrganizationNode> children = nodeRepository.findByParentIdOrderBySortOrderAsc(current);
            for (OrganizationNode child : children) {
                stack.push(child.getId());
            }
        }
        return ids;
    }

    private OrganizationNode getNodeOrThrow(Integer nodeId) {
        return nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("Noeud introuvable: " + nodeId));
    }

    private NodeTreeResponse toTreeResponse(OrganizationNode node) {
        List<NodeTreeResponse> children = nodeRepository.findByParentIdOrderBySortOrderAsc(node.getId())
                .stream()
                .map(this::toTreeResponse)
                .toList();
        return new NodeTreeResponse(
                node.getId(),
                node.getParent() == null ? null : node.getParent().getId(),
                node.getNodeType(),
                node.getName(),
                node.getSlug(),
                node.getPath(),
                node.getDepth(),
                node.getSortOrder(),
                node.isActive(),
                children
        );
    }

    private NodeResponse toNodeResponse(OrganizationNode node) {
        return new NodeResponse(
                node.getId(),
                node.getParent() == null ? null : node.getParent().getId(),
                node.getNodeType(),
                node.getName(),
                node.getSlug(),
                node.getPath(),
                node.getDepth(),
                node.getSortOrder(),
                node.isActive()
        );
    }

    private NodeContentResponse toNodeContentResponse(NodeContent content) {
        return new NodeContentResponse(
                content.getId(),
                content.getNode().getId(),
                content.getSummary(),
                content.getDescription(),
                content.getContactEmail(),
                content.getLocation(),
                content.getMetadataJson()
        );
    }

    private NodeLinkResponse toNodeLinkResponse(NodeLink link) {
        return new NodeLinkResponse(
                link.getId(),
                link.getNode().getId(),
                link.getLabel(),
                link.getUrl(),
                link.getCategory(),
                link.getIcon(),
                link.getVisibility(),
                link.getSortOrder(),
                link.isActive()
        );
    }

    private NodeMembershipResponse toNodeMembershipResponse(NodeMembership membership) {
        return new NodeMembershipResponse(
                membership.getId(),
                membership.getNode().getId(),
                membership.getUserId(),
                membership.getMembershipRole(),
                membership.isPrimary(),
                membership.getActiveFrom(),
                membership.getActiveTo()
        );
    }

    private NodeAccessRuleResponse toNodeAccessRuleResponse(NodeAccessRule rule) {
        return new NodeAccessRuleResponse(
                rule.getId(),
                rule.getNode().getId(),
                rule.getEffect(),
                rule.getSubjectType(),
                rule.getSubjectValue(),
                rule.getPermission(),
                rule.isAppliesToChildren()
        );
    }

    private AnnouncementResponse toAnnouncementResponse(Announcement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getNode() == null ? null : announcement.getNode().getId(),
                announcement.getScopeType(),
                announcement.getTitle(),
                announcement.getBody(),
                announcement.getSeverity(),
                announcement.getStartAt(),
                announcement.getEndAt(),
                announcement.isActive(),
                announcement.getCreatedByUserId()
        );
    }

    // ── Catalog entries ──────────────────────────────────────────────────────

    public List<String> getCatalogValues(String catalogType) {
        return catalogEntryRepository.findByCatalogTypeOrderByValueAsc(catalogType)
                .stream().map(OrgCatalogEntry::getValue).toList();
    }

    public List<CatalogEntryDto> getCatalogMeta(String catalogType) {
        return catalogEntryRepository.findByCatalogTypeOrderByValueAsc(catalogType)
                .stream().map(e -> new CatalogEntryDto(e.getValue(), e.getColor())).toList();
    }

    @Transactional
    public CatalogEntryDto updateCatalogColor(String catalogType, String value, String color, Integer actorUserId) {
        requireAdmin(actorUserId);
        String normalized = value.trim().toUpperCase();
        OrgCatalogEntry entry = catalogEntryRepository
                .findByCatalogTypeAndValue(catalogType, normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog entry not found"));
        entry.setColor(color != null && !color.isBlank() ? color.trim() : null);
        catalogEntryRepository.save(entry);
        return new CatalogEntryDto(entry.getValue(), entry.getColor());
    }

    @Transactional
    public String addCatalogValue(String catalogType, String value, Integer actorUserId) {
        requireAdmin(actorUserId);
        String normalized = value.trim().toUpperCase();
        if (normalized.isBlank()) throw new BadRequestException("La valeur ne peut pas être vide.");
        if (catalogEntryRepository.findByCatalogTypeAndValue(catalogType, normalized).isPresent()) {
            return normalized;
        }
        OrgCatalogEntry entry = new OrgCatalogEntry();
        entry.setCatalogType(catalogType);
        entry.setValue(normalized);
        catalogEntryRepository.save(entry);
        return normalized;
    }

    @Transactional
    public void removeCatalogValue(String catalogType, String value, Integer actorUserId) {
        requireAdmin(actorUserId);
        catalogEntryRepository.deleteByCatalogTypeAndValue(catalogType, value.trim().toUpperCase());
    }

    public List<String> getAvailablePermissionValues() {
        return ALL_PERMISSIONS.stream().sorted().toList();
    }

    public List<String> getMembershipRolePermissions(String membershipRole) {
        String normalizedRole = normalizeRequiredUpperValue(membershipRole, "membershipRole");
        validateMembershipRoleAllowed(normalizedRole);

        return membershipRolePermissionRepository.findByMembershipRoleOrderByPermissionAsc(normalizedRole)
                .stream()
                .map(MembershipRolePermission::getPermission)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();
    }

    @Transactional
    public List<String> replaceMembershipRolePermissions(String membershipRole, List<String> permissions, Integer actorUserId) {
        requireAdmin(actorUserId);
        String normalizedRole = normalizeRequiredUpperValue(membershipRole, "membershipRole");
        validateMembershipRoleAllowed(normalizedRole);

        Set<String> normalizedPermissions = new HashSet<>();
        if (permissions != null) {
            for (String permission : permissions) {
                if (permission == null || permission.isBlank()) {
                    continue;
                }
                String normalizedPermission = permission.trim().toUpperCase();
                if (!ALL_PERMISSIONS.contains(normalizedPermission)) {
                    throw new BadRequestException("permission invalide: " + normalizedPermission);
                }
                normalizedPermissions.add(normalizedPermission);
            }
        }

        membershipRolePermissionRepository.deleteByMembershipRole(normalizedRole);

        if (!normalizedPermissions.isEmpty()) {
            List<MembershipRolePermission> rows = new ArrayList<>();
            for (String permission : normalizedPermissions) {
                MembershipRolePermission row = new MembershipRolePermission();
                row.setMembershipRole(normalizedRole);
                row.setPermission(permission);
                rows.add(row);
            }
            membershipRolePermissionRepository.saveAll(rows);
        }

        return getMembershipRolePermissions(normalizedRole);
    }

    private String normalizeSlug(String slug) {
        String normalized = slug.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '-')
            .replaceAll("[^a-z0-9-]", "");
        if (normalized.isBlank()) {
            throw new BadRequestException("slug invalide");
        }
        return normalized;
    }

    private String buildPath(OrganizationNode parent, String slug) {
        String normalizedSlug = normalizeSlug(slug);
        return parent == null ? "/" + normalizedSlug : parent.getPath() + "/" + normalizedSlug;
    }

    private void refreshSubtreePath(OrganizationNode node) {
        node.setPath(buildPath(node.getParent(), node.getSlug()));
        nodeRepository.save(node);
        refreshChildrenDepthAndPath(node);
    }

    private void refreshChildrenDepthAndPath(OrganizationNode parent) {
        List<OrganizationNode> children = nodeRepository.findByParentIdOrderBySortOrderAsc(parent.getId());
        for (OrganizationNode child : children) {
            child.setDepth(parent.getDepth() + 1);
            child.setPath(parent.getPath() + "/" + child.getSlug());
            nodeRepository.save(child);
            refreshChildrenDepthAndPath(child);
        }
    }

    private boolean isDescendant(OrganizationNode node, OrganizationNode candidateParent) {
        OrganizationNode current = candidateParent;
        while (current != null) {
            if (Objects.equals(current.getId(), node.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private boolean matchesRule(NodeAccessRule rule, Integer userId, Set<String> globalRoles, List<String> membershipRoles) {
        String subjectType = rule.getSubjectType().toUpperCase();
        String subjectValue = rule.getSubjectValue();

        if ("USER".equals(subjectType)) {
            return subjectValue.equals(String.valueOf(userId));
        }
        if ("ROLE".equals(subjectType)) {
            return globalRoles.contains(subjectValue.toUpperCase());
        }
        if ("MEMBERSHIP".equals(subjectType)) {
            return membershipRoles.contains(subjectValue.toUpperCase());
        }
        return false;
    }

    private Set<String> resolveMembershipPermissions(Collection<String> membershipRoles) {
        if (membershipRoles == null || membershipRoles.isEmpty()) {
            return Set.of();
        }

        Set<String> normalizedRoles = membershipRoles.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        if (normalizedRoles.isEmpty()) {
            return Set.of();
        }

        return membershipRolePermissionRepository.findByMembershipRoleIn(normalizedRoles)
                .stream()
                .map(MembershipRolePermission::getPermission)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(ALL_PERMISSIONS::contains)
                .collect(Collectors.toSet());
    }

    private void validateMembershipRoleAllowed(String role) {
        validateCatalogValueExists("MEMBERSHIP_ROLE", role, "Role membership non autorise");
    }

    private void validateCatalogValueExists(String catalogType, String value, String errorPrefix) {
        boolean allowed = catalogEntryRepository.findByCatalogTypeAndValue(catalogType, value).isPresent();
        if (!allowed) {
            throw new BadRequestException(errorPrefix + ": " + value);
        }
    }

    private String normalizeMembershipRole(String role) {
        if (role == null || role.isBlank()) {
            return resolveDefaultMembershipRole();
        }
        return role.trim().toUpperCase();
    }

    private String normalizeRequiredUpperValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " est obligatoire");
        }
        return value.trim().toUpperCase();
    }

    private String resolveDefaultMembershipRole() {
        return catalogEntryRepository.findByCatalogTypeOrderByValueAsc("MEMBERSHIP_ROLE")
                .stream()
                .map(OrgCatalogEntry::getValue)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(value -> !value.isBlank())
                .min((a, b) -> {
                    if ("MEMBRE".equals(a) && !"MEMBRE".equals(b)) return -1;
                    if ("MEMBRE".equals(b) && !"MEMBRE".equals(a)) return 1;
                    return a.compareTo(b);
                })
                .orElseThrow(() -> new BadRequestException("Aucun role membership configure en base"));
    }

    private String normalizeGlobalRole(String role) {
        if (role == null || role.isBlank()) {
            throw new BadRequestException("roleName est obligatoire");
        }
        String trimmed = role.trim().toUpperCase();
        return trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed;
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " est obligatoire");
        }
    }

    private void requirePermissionOnNode(Integer nodeId, Integer actorUserId, String permission) {
        if (actorUserId == null) {
            throw new BadRequestException("X-User-Id est obligatoire");
        }

        String normalizedPermission = normalizeRequiredUpperValue(permission, "permission");
        if (!ALL_PERMISSIONS.contains(normalizedPermission)) {
            throw new BadRequestException("permission invalide: " + normalizedPermission);
        }

        PermissionsResponse permissions = getMyPermissions(nodeId, actorUserId);
        if (!permissions.permissions().contains(normalizedPermission)) {
            throw new ForbiddenException("Permission manquante: " + normalizedPermission + " sur le noeud " + nodeId);
        }
    }

    private void requireAdmin(Integer actorUserId) {
        if (actorUserId == null) {
            throw new BadRequestException("X-User-Id est obligatoire");
        }

        Set<String> roles = resolveGlobalRoles(actorUserId);
        if (!roles.contains(ROLE_ADMIN)) {
            throw new ForbiddenException("Role admin requis");
        }
    }

    private List<NodeOrderItemRequest> safeItems(List<NodeOrderItemRequest> items) {
        return items == null ? List.of() : items;
    }
}
