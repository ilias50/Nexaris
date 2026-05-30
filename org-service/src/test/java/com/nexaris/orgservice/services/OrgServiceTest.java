package com.nexaris.orgservice.services;

import com.nexaris.orgservice.dto.CreateNodeRequest;
import com.nexaris.orgservice.dto.NodeResponse;
import com.nexaris.orgservice.entities.OrgUserRole;
import com.nexaris.orgservice.entities.OrganizationNode;
import com.nexaris.orgservice.exceptions.BadRequestException;
import com.nexaris.orgservice.exceptions.ResourceNotFoundException;
import com.nexaris.orgservice.repositories.AnnouncementRepository;
import com.nexaris.orgservice.repositories.MembershipRolePermissionRepository;
import com.nexaris.orgservice.repositories.NodeAccessRuleRepository;
import com.nexaris.orgservice.repositories.NodeContentRepository;
import com.nexaris.orgservice.repositories.NodeLinkRepository;
import com.nexaris.orgservice.repositories.NodeMembershipRepository;
import com.nexaris.orgservice.repositories.OrgCatalogEntryRepository;
import com.nexaris.orgservice.repositories.OrgUserRoleRepository;
import com.nexaris.orgservice.repositories.OrganizationNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgServiceTest {

    private static final Integer ACTOR_USER_ID = 1;

    @Mock
    private OrganizationNodeRepository nodeRepository;

    @Mock
    private NodeContentRepository contentRepository;

    @Mock
    private NodeLinkRepository linkRepository;

    @Mock
    private NodeMembershipRepository membershipRepository;

    @Mock
    private OrgUserRoleRepository orgUserRoleRepository;

    @Mock
    private NodeAccessRuleRepository accessRuleRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private OrgCatalogEntryRepository catalogEntryRepository;

    @Mock
    private MembershipRolePermissionRepository membershipRolePermissionRepository;

    @InjectMocks
    private OrgService orgService;

    @Test
    void getNodeByPath_ShouldThrowNotFound_WhenNodeDoesNotExist() {
        when(nodeRepository.findByPath("/missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orgService.getNodeByPath("/missing", ACTOR_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Noeud introuvable pour path=/missing");
    }

    @Test
    void createNode_ShouldThrowBadRequest_WhenParentAlreadyHasSameSlug() {
        mockAdminActor();

        OrganizationNode parent = new OrganizationNode();
        parent.setId(10);
        parent.setPath("/root");
        parent.setDepth(0);

        when(nodeRepository.findById(10)).thenReturn(Optional.of(parent));
        when(nodeRepository.existsByParentIdAndSlug(10, "team-a")).thenReturn(true);

        CreateNodeRequest request = new CreateNodeRequest(10, "TEAM", "Team A", "team-a", 1, true);

        assertThatThrownBy(() -> orgService.createNode(request, ACTOR_USER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Un noeud avec ce slug existe deja sous le meme parent");
    }

    @Test
    void createNode_ShouldNormalizeSlugAndBuildPath_WhenRequestIsValid() {
        mockAdminActor();

        when(nodeRepository.existsByPath("/my-team")).thenReturn(false);
        when(nodeRepository.save(any(OrganizationNode.class))).thenAnswer(invocation -> {
            OrganizationNode saved = invocation.getArgument(0);
            saved.setId(99);
            return saved;
        });

        CreateNodeRequest request = new CreateNodeRequest(null, "TEAM", "My Team", "My Team", 2, true);

        NodeResponse response = orgService.createNode(request, ACTOR_USER_ID);

        assertThat(response.id()).isEqualTo(99);
        assertThat(response.slug()).isEqualTo("my-team");
        assertThat(response.path()).isEqualTo("/my-team");
        assertThat(response.depth()).isZero();
    }

    private void mockAdminActor() {
        OrgUserRole role = new OrgUserRole();
        role.setUserId(ACTOR_USER_ID);
        role.setRoleName("ROLE_ADMIN");
        when(orgUserRoleRepository.findByUserIdOrderByRoleNameAsc(ACTOR_USER_ID)).thenReturn(List.of(role));
    }
}
