package com.hubilon.modules.user;

import com.hubilon.modules.department.domain.model.Department;
import com.hubilon.modules.department.domain.port.out.DepartmentCommandPort;
import com.hubilon.modules.department.domain.port.out.DepartmentQueryPort;
import com.hubilon.modules.team.application.port.out.TeamCommandPort;
import com.hubilon.modules.team.application.port.out.TeamQueryPort;
import com.hubilon.modules.team.domain.model.Team;
import com.hubilon.modules.user.application.service.UserProvisioningService;
import com.hubilon.modules.user.domain.model.User;
import com.hubilon.modules.user.domain.port.out.UserCommandPort;
import com.hubilon.modules.user.domain.port.out.UserQueryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    @Mock
    UserQueryPort userQueryPort;

    @Mock
    UserCommandPort userCommandPort;

    @Mock
    TeamQueryPort teamQueryPort;

    @Mock
    TeamCommandPort teamCommandPort;

    @Mock
    DepartmentQueryPort departmentQueryPort;

    @Mock
    DepartmentCommandPort departmentCommandPort;

    @InjectMocks
    UserProvisioningService userProvisioningService;

    @Mock
    SecurityContext securityContext;

    @Mock
    Authentication authentication;

    @Mock
    Jwt jwt;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockJwtClaims(Map<String, Object> claims) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaims()).thenReturn(claims);
    }

    // ──────────────────────────────────────────────
    // 신규 유저 JIT 생성 (department/team 자동 생성 포함)
    // ──────────────────────────────────────────────

    @Test
    void 신규유저_신규department와_신규team이_자동생성된다() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "jdoe",
                "given_name", "John",
                "family_name", "Doe",
                "roles", List.of("USER"),
                "department", List.of("Engineering/Backend")
        );
        mockJwtClaims(claims);

        Department savedDept = Department.builder().id(10L).name("Engineering").build();
        Team savedTeam = Team.builder().id(20L).name("Backend").deptId(10L).build();
        User savedUser = User.builder().id(1L).name("John Doe").email("john@example.com")
                .keycloakUsername("jdoe").teamId(20L).role(User.Role.USER).build();

        when(userQueryPort.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(departmentQueryPort.findByName("Engineering")).thenReturn(Optional.empty());
        when(departmentCommandPort.save(any())).thenReturn(savedDept);
        when(teamQueryPort.findByNameAndDeptId("Backend", 10L)).thenReturn(Optional.empty());
        when(teamCommandPort.save(any())).thenReturn(savedTeam);
        when(userCommandPort.save(any())).thenReturn(savedUser);

        User result = userProvisioningService.provisionOrSync("john@example.com");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getTeamId()).isEqualTo(20L);
        assertThat(result.getRole()).isEqualTo(User.Role.USER);

        verify(departmentCommandPort).save(any());
        verify(teamCommandPort).save(any());
        verify(userCommandPort).save(any());
    }

    @Test
    void 신규유저_기존department_기존team_재사용() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "jdoe",
                "given_name", "John",
                "family_name", "Doe",
                "roles", List.of("USER"),
                "department", List.of("Engineering/Backend")
        );
        mockJwtClaims(claims);

        Department existingDept = Department.builder().id(10L).name("Engineering").build();
        Team existingTeam = Team.builder().id(20L).name("Backend").deptId(10L).build();
        User savedUser = User.builder().id(1L).name("John Doe").email("john@example.com")
                .keycloakUsername("jdoe").teamId(20L).role(User.Role.USER).build();

        when(userQueryPort.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(departmentQueryPort.findByName("Engineering")).thenReturn(Optional.of(existingDept));
        when(teamQueryPort.findByNameAndDeptId("Backend", 10L)).thenReturn(Optional.of(existingTeam));
        when(userCommandPort.save(any())).thenReturn(savedUser);

        User result = userProvisioningService.provisionOrSync("john@example.com");

        assertThat(result.getTeamId()).isEqualTo(20L);

        verify(departmentCommandPort, never()).save(any());
        verify(teamCommandPort, never()).save(any());
        verify(userCommandPort).save(any());
    }

    @Test
    void 신규유저_ADMIN_role_클레임_정상파싱() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "admin_user",
                "given_name", "Admin",
                "family_name", "User",
                "roles", List.of("ROLE_ADMIN"),
                "department", List.of("IT/Ops")
        );
        mockJwtClaims(claims);

        Department dept = Department.builder().id(5L).name("IT").build();
        Team team = Team.builder().id(15L).name("Ops").deptId(5L).build();
        User savedUser = User.builder().id(2L).name("Admin User").email("admin@example.com")
                .role(User.Role.ADMIN).teamId(15L).build();

        when(userQueryPort.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(departmentQueryPort.findByName("IT")).thenReturn(Optional.of(dept));
        when(teamQueryPort.findByNameAndDeptId("Ops", 5L)).thenReturn(Optional.of(team));
        when(userCommandPort.save(any())).thenReturn(savedUser);

        User result = userProvisioningService.provisionOrSync("admin@example.com");

        assertThat(result.getRole()).isEqualTo(User.Role.ADMIN);
    }

    // ──────────────────────────────────────────────
    // 기존 유저 role/team 재동기화
    // ──────────────────────────────────────────────

    @Test
    void 기존유저_role_변경시_업데이트된다() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "jdoe",
                "given_name", "John",
                "family_name", "Doe",
                "roles", List.of("ROLE_ADMIN"),
                "department", List.of("Engineering/Backend")
        );
        mockJwtClaims(claims);

        User existingUser = User.builder().id(1L).name("John Doe").email("john@example.com")
                .keycloakUsername("jdoe").teamId(20L).role(User.Role.USER).build();
        Department dept = Department.builder().id(10L).name("Engineering").build();
        Team team = Team.builder().id(20L).name("Backend").deptId(10L).build();
        User updatedUser = User.builder().id(1L).name("John Doe").email("john@example.com")
                .keycloakUsername("jdoe").teamId(20L).role(User.Role.ADMIN).build();

        when(userQueryPort.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));
        when(departmentQueryPort.findByName("Engineering")).thenReturn(Optional.of(dept));
        when(teamQueryPort.findByNameAndDeptId("Backend", 10L)).thenReturn(Optional.of(team));
        when(userCommandPort.save(any())).thenReturn(updatedUser);

        User result = userProvisioningService.provisionOrSync("john@example.com");

        assertThat(result.getRole()).isEqualTo(User.Role.ADMIN);
        verify(userCommandPort).save(any());
    }

    @Test
    void 기존유저_team_변경시_업데이트된다() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "jdoe",
                "given_name", "John",
                "family_name", "Doe",
                "roles", List.of("USER"),
                "department", List.of("Engineering/Frontend")
        );
        mockJwtClaims(claims);

        User existingUser = User.builder().id(1L).name("John Doe").email("john@example.com")
                .keycloakUsername("jdoe").teamId(20L).role(User.Role.USER).build();
        Department dept = Department.builder().id(10L).name("Engineering").build();
        Team newTeam = Team.builder().id(30L).name("Frontend").deptId(10L).build();
        User updatedUser = User.builder().id(1L).name("John Doe").email("john@example.com")
                .keycloakUsername("jdoe").teamId(30L).role(User.Role.USER).build();

        when(userQueryPort.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));
        when(departmentQueryPort.findByName("Engineering")).thenReturn(Optional.of(dept));
        when(teamQueryPort.findByNameAndDeptId("Frontend", 10L)).thenReturn(Optional.of(newTeam));
        when(userCommandPort.save(any())).thenReturn(updatedUser);

        User result = userProvisioningService.provisionOrSync("john@example.com");

        assertThat(result.getTeamId()).isEqualTo(30L);
        verify(userCommandPort).save(any());
    }

    @Test
    void 기존유저_변경없으면_저장_스킵된다() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "jdoe",
                "given_name", "John",
                "family_name", "Doe",
                "roles", List.of("USER"),
                "department", List.of("Engineering/Backend")
        );
        mockJwtClaims(claims);

        User existingUser = User.builder().id(1L).name("John Doe").email("john@example.com")
                .keycloakUsername("jdoe").teamId(20L).role(User.Role.USER).build();
        Department dept = Department.builder().id(10L).name("Engineering").build();
        Team team = Team.builder().id(20L).name("Backend").deptId(10L).build();

        when(userQueryPort.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));
        when(departmentQueryPort.findByName("Engineering")).thenReturn(Optional.of(dept));
        when(teamQueryPort.findByNameAndDeptId("Backend", 10L)).thenReturn(Optional.of(team));

        User result = userProvisioningService.provisionOrSync("john@example.com");

        assertThat(result.getId()).isEqualTo(1L);
        verify(userCommandPort, never()).save(any());
    }

    @Test
    void 기존유저_이름_변경시_업데이트된다() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "jdoe",
                "given_name", "Jonathan",
                "family_name", "Doe",
                "roles", List.of("USER"),
                "department", List.of("Engineering/Backend")
        );
        mockJwtClaims(claims);

        User existingUser = User.builder().id(1L).name("John Doe").email("john@example.com")
                .keycloakUsername("jdoe").teamId(20L).role(User.Role.USER).build();
        Department dept = Department.builder().id(10L).name("Engineering").build();
        Team team = Team.builder().id(20L).name("Backend").deptId(10L).build();
        User updatedUser = User.builder().id(1L).name("Jonathan Doe").email("john@example.com")
                .teamId(20L).role(User.Role.USER).build();

        when(userQueryPort.findByEmail("john@example.com")).thenReturn(Optional.of(existingUser));
        when(departmentQueryPort.findByName("Engineering")).thenReturn(Optional.of(dept));
        when(teamQueryPort.findByNameAndDeptId("Backend", 10L)).thenReturn(Optional.of(team));
        when(userCommandPort.save(any())).thenReturn(updatedUser);

        User result = userProvisioningService.provisionOrSync("john@example.com");

        assertThat(result.getName()).isEqualTo("Jonathan Doe");
        verify(userCommandPort).save(any());
    }

    // ──────────────────────────────────────────────
    // department claim null/빈값 → graceful 처리
    // ──────────────────────────────────────────────

    @Test
    void department_클레임_없으면_teamId_null로_유저_생성() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "nodept",
                "given_name", "No",
                "family_name", "Dept"
        );
        mockJwtClaims(claims);

        User savedUser = User.builder().id(3L).name("No Dept").email("nodept@example.com")
                .keycloakUsername("nodept").teamId(null).role(User.Role.USER).build();

        when(userQueryPort.findByEmail("nodept@example.com")).thenReturn(Optional.empty());
        when(userCommandPort.save(any())).thenReturn(savedUser);

        User result = userProvisioningService.provisionOrSync("nodept@example.com");

        assertThat(result.getTeamId()).isNull();
        verify(departmentQueryPort, never()).findByName(anyString());
        verify(teamQueryPort, never()).findByName(anyString());
        verify(teamQueryPort, never()).findByNameAndDeptId(anyString(), any());
    }

    @Test
    void department_클레임_빈_리스트면_teamId_null로_유저_생성() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "emptydept",
                "given_name", "Empty",
                "family_name", "Dept",
                "department", List.of()
        );
        mockJwtClaims(claims);

        User savedUser = User.builder().id(4L).name("Empty Dept").email("emptydept@example.com")
                .keycloakUsername("emptydept").teamId(null).role(User.Role.USER).build();

        when(userQueryPort.findByEmail("emptydept@example.com")).thenReturn(Optional.empty());
        when(userCommandPort.save(any())).thenReturn(savedUser);

        User result = userProvisioningService.provisionOrSync("emptydept@example.com");

        assertThat(result.getTeamId()).isNull();
        verify(departmentQueryPort, never()).findByName(anyString());
    }

    @Test
    void department_클레임_슬래시만_있으면_teamId_null로_유저_생성() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "slashdept",
                "given_name", "Slash",
                "family_name", "Dept",
                "department", List.of("/")
        );
        mockJwtClaims(claims);

        User savedUser = User.builder().id(5L).name("Slash Dept").email("slash@example.com")
                .keycloakUsername("slashdept").teamId(null).role(User.Role.USER).build();

        when(userQueryPort.findByEmail("slash@example.com")).thenReturn(Optional.empty());
        when(userCommandPort.save(any())).thenReturn(savedUser);

        User result = userProvisioningService.provisionOrSync("slash@example.com");

        assertThat(result.getTeamId()).isNull();
    }

    @Test
    void department_클레임_세그먼트_1개면_DefaultDept와_함께_팀_생성() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "onlydept",
                "given_name", "Only",
                "family_name", "Team",
                "department", List.of("Backend")
        );
        mockJwtClaims(claims);

        Department defaultDept = Department.builder().id(99L).name("Default").build();
        Team savedTeam = Team.builder().id(55L).name("Backend").deptId(99L).build();
        User savedUser = User.builder().id(6L).name("Only Team").email("onlyteam@example.com")
                .keycloakUsername("onlydept").teamId(55L).role(User.Role.USER).build();

        when(userQueryPort.findByEmail("onlyteam@example.com")).thenReturn(Optional.empty());
        when(teamQueryPort.findByName("Backend")).thenReturn(Optional.empty());
        when(departmentQueryPort.findByName("Default")).thenReturn(Optional.of(defaultDept));
        when(teamCommandPort.save(any())).thenReturn(savedTeam);
        when(userCommandPort.save(any())).thenReturn(savedUser);

        User result = userProvisioningService.provisionOrSync("onlyteam@example.com");

        assertThat(result.getTeamId()).isEqualTo(55L);
        verify(teamCommandPort).save(any());
    }

    // ──────────────────────────────────────────────
    // Race Condition 재시도 로직
    // ──────────────────────────────────────────────

    @Test
    void DataIntegrityViolation_발생시_재시도_후_성공() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "racedoe",
                "given_name", "Race",
                "family_name", "Doe",
                "department", List.of("Engineering/Backend")
        );
        mockJwtClaims(claims);

        Department dept = Department.builder().id(10L).name("Engineering").build();
        Team team = Team.builder().id(20L).name("Backend").deptId(10L).build();
        User savedUser = User.builder().id(7L).name("Race Doe").email("race@example.com")
                .keycloakUsername("racedoe").teamId(20L).role(User.Role.USER).build();

        // 첫 번째 호출에서 DataIntegrityViolationException, 두 번째에서 성공
        when(userQueryPort.findByEmail("race@example.com"))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenReturn(Optional.empty());
        when(departmentQueryPort.findByName("Engineering")).thenReturn(Optional.of(dept));
        when(teamQueryPort.findByNameAndDeptId("Backend", 10L)).thenReturn(Optional.of(team));
        when(userCommandPort.save(any())).thenReturn(savedUser);

        User result = userProvisioningService.provisionOrSync("race@example.com");

        assertThat(result.getId()).isEqualTo(7L);
        verify(userQueryPort, times(2)).findByEmail("race@example.com");
    }

    @Test
    void 팀저장시_DataIntegrityViolation_재시도_후_기존팀_반환() {
        Map<String, Object> claims = Map.of(
                "preferred_username", "racedoe",
                "given_name", "Race",
                "family_name", "Doe",
                "department", List.of("Engineering/Backend")
        );
        mockJwtClaims(claims);

        Department dept = Department.builder().id(10L).name("Engineering").build();
        Team existingTeam = Team.builder().id(20L).name("Backend").deptId(10L).build();
        User savedUser = User.builder().id(7L).name("Race Doe").email("race@example.com")
                .keycloakUsername("racedoe").teamId(20L).role(User.Role.USER).build();

        when(userQueryPort.findByEmail("race@example.com")).thenReturn(Optional.empty());
        when(departmentQueryPort.findByName("Engineering")).thenReturn(Optional.of(dept));
        // 처음엔 팀 없음, 저장 시 DataIntegrityViolation, 재조회에서 기존 팀 반환
        when(teamQueryPort.findByNameAndDeptId("Backend", 10L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingTeam));
        when(teamCommandPort.save(any())).thenThrow(new DataIntegrityViolationException("team duplicate"));
        when(userCommandPort.save(any())).thenReturn(savedUser);

        User result = userProvisioningService.provisionOrSync("race@example.com");

        assertThat(result.getTeamId()).isEqualTo(20L);
        verify(teamCommandPort).save(any());
        verify(teamQueryPort, times(2)).findByNameAndDeptId("Backend", 10L);
    }
}
