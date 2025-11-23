package com.example.registry.service

import com.example.registry.domain.Status
import com.example.registry.domain.entity.AppUser
import com.example.registry.repo.AppUserRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any as anyArg
import org.mockito.ArgumentMatchers.anyString as anyStringArg
import org.mockito.ArgumentMatchers.isNull as isNullArg
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock
    private lateinit var appUserRepository: AppUserRepository

    @Mock
    private lateinit var membershipRepository: com.example.registry.repo.MembershipRepository

    @Mock
    private lateinit var auditService: AuditService

    @Mock
    private lateinit var authorizationService: com.example.registry.security.AuthorizationService

    @Mock
    private lateinit var entityManager: EntityManager

    @Mock
    private lateinit var query: Query

    @InjectMocks
    private lateinit var userService: UserService

    private lateinit var activeUser: AppUser
    private lateinit var inactiveUser: AppUser

    @BeforeEach
    fun setup() {
        activeUser = AppUser(
            id = 1L,
            email = "active@test.com",
            fullName = "Active User",
            mfaEnabled = false,
            status = Status.ACTIVE,
            createdAt = Instant.now()
        )

        inactiveUser = AppUser(
            id = 2L,
            email = "inactive@test.com",
            fullName = "Inactive User",
            mfaEnabled = false,
            status = Status.INACTIVE,
            createdAt = Instant.now()
        )
    }

    @Test
    fun `should update active user to inactive`() {
        // Given
        val userId = 1L
        val tenantId = 10L
        val updatedBy = 5L
        val reason = "User requested deactivation"

        @Suppress("UNCHECKED_CAST")
        doReturn(query).`when`(entityManager).createNativeQuery(anyString(), anyArg(Class::class.java))
        `when`(query.setParameter(anyString(), any())).thenReturn(query)
        `when`(query.resultList).thenReturn(listOf(activeUser))
        `when`(appUserRepository.save(any())).thenAnswer { invocation ->
            val user = invocation.getArgument<AppUser>(0)
            user.copy(status = Status.INACTIVE)
        }

        // When
        userService.updateUserStatus(userId, tenantId, Status.INACTIVE, reason, updatedBy)

        // Then
        val userCaptor = ArgumentCaptor.forClass(AppUser::class.java)
        verify(appUserRepository).save(userCaptor.capture())
        assertThat(userCaptor.value.status).isEqualTo(Status.INACTIVE)

        verify(auditService).log(
            anyArg(),
            anyArg(),
            anyStringArg(),
            anyStringArg(),
            anyStringArg(),
            anyArg(),
            anyArg()
        )
    }

    @Test
    fun `should update inactive user to active`() {
        // Given
        val userId = 2L
        val tenantId = 10L
        val updatedBy = 5L
        val reason = "User reactivated"

        @Suppress("UNCHECKED_CAST")
        doReturn(query).`when`(entityManager).createNativeQuery(anyString(), anyArg(Class::class.java))
        `when`(query.setParameter(anyString(), any())).thenReturn(query)
        `when`(query.resultList).thenReturn(listOf(inactiveUser))
        `when`(appUserRepository.save(any())).thenAnswer { invocation ->
            val user = invocation.getArgument<AppUser>(0)
            user.copy(status = Status.ACTIVE)
        }

        // When
        userService.updateUserStatus(userId, tenantId, Status.ACTIVE, reason, updatedBy)

        // Then
        val userCaptor = ArgumentCaptor.forClass(AppUser::class.java)
        verify(appUserRepository).save(userCaptor.capture())
        assertThat(userCaptor.value.status).isEqualTo(Status.ACTIVE)

        verify(auditService).log(
            anyArg(),
            anyArg(),
            anyStringArg(),
            anyStringArg(),
            anyStringArg(),
            anyArg(),
            anyArg()
        )
    }

    @Test
    fun `should throw exception when user not found`() {
        // Given
        val userId = 999L
        val tenantId = 10L
        val updatedBy = 5L

        @Suppress("UNCHECKED_CAST")
        doReturn(query).`when`(entityManager).createNativeQuery(anyString(), anyArg(Class::class.java))
        `when`(query.setParameter(anyString(), any())).thenReturn(query)
        `when`(query.resultList).thenReturn(emptyList<AppUser>())

        // When/Then
        assertThatThrownBy {
            userService.updateUserStatus(userId, tenantId, Status.INACTIVE, null, updatedBy)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("User not found")

        verify(appUserRepository, never()).save(any<AppUser>())
        verify(auditService, never()).log(
            anyArg(),
            anyArg(),
            anyStringArg(),
            anyStringArg(),
            anyStringArg(),
            anyArg(),
            anyArg()
        )
    }

    @Test
    fun `should use native query to bypass where clause`() {
        // Given
        val userId = 2L
        val tenantId = 10L
        val updatedBy = 5L

        @Suppress("UNCHECKED_CAST")
        doReturn(query).`when`(entityManager).createNativeQuery(anyString(), anyArg(Class::class.java))
        `when`(query.setParameter(anyString(), any())).thenReturn(query)
        `when`(query.resultList).thenReturn(listOf(inactiveUser))
        `when`(appUserRepository.save(any())).thenReturn(inactiveUser.copy(status = Status.ACTIVE))

        // When
        userService.updateUserStatus(userId, tenantId, Status.ACTIVE, null, updatedBy)

        // Then
        val queryCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(entityManager).createNativeQuery(queryCaptor.capture(), anyArg(Class::class.java))
        assertThat(queryCaptor.value).contains("SELECT * FROM app_users WHERE id = :id")

        val paramNameCaptor = ArgumentCaptor.forClass(String::class.java)
        val paramValueCaptor = ArgumentCaptor.forClass(Any::class.java)
        verify(query).setParameter(paramNameCaptor.capture(), paramValueCaptor.capture())
        assertThat(paramNameCaptor.value).isEqualTo("id")
        assertThat(paramValueCaptor.value).isEqualTo(userId)
    }

    @Test
    fun `should log audit with correct before and after states`() {
        // Given
        val userId = 1L
        val tenantId = 10L
        val updatedBy = 5L

        @Suppress("UNCHECKED_CAST")
        doReturn(query).`when`(entityManager).createNativeQuery(anyString(), anyArg(Class::class.java))
        `when`(query.setParameter(anyString(), any())).thenReturn(query)
        `when`(query.resultList).thenReturn(listOf(activeUser))
        `when`(appUserRepository.save(any())).thenReturn(activeUser.copy(status = Status.INACTIVE))

        // When
        userService.updateUserStatus(userId, tenantId, Status.INACTIVE, null, updatedBy)

        // Then
        val beforeCaptor = ArgumentCaptor.forClass(Any::class.java)
        val afterCaptor = ArgumentCaptor.forClass(Any::class.java)
        
        verify(auditService).log(
            anyArg(),
            anyArg(),
            anyStringArg(),
            anyStringArg(),
            anyStringArg(),
            beforeCaptor.capture(),
            afterCaptor.capture()
        )

        val before = beforeCaptor.value as AppUser
        val after = afterCaptor.value as AppUser

        assertThat(before.status).isEqualTo(Status.ACTIVE)
        assertThat(after.status).isEqualTo(Status.INACTIVE)
    }

    @Test
    fun `should handle null reason`() {
        // Given
        val userId = 1L
        val tenantId = 10L
        val updatedBy = 5L

        @Suppress("UNCHECKED_CAST")
        doReturn(query).`when`(entityManager).createNativeQuery(anyString(), anyArg(Class::class.java))
        `when`(query.setParameter(anyString(), any())).thenReturn(query)
        `when`(query.resultList).thenReturn(listOf(activeUser))
        `when`(appUserRepository.save(any())).thenReturn(activeUser.copy(status = Status.INACTIVE))

        // When
        userService.updateUserStatus(userId, tenantId, Status.INACTIVE, null, updatedBy)

        // Then
        verify(appUserRepository).save(any<AppUser>())
        verify(auditService).log(
            anyArg(),
            anyArg(),
            anyStringArg(),
            anyStringArg(),
            anyStringArg(),
            anyArg(),
            anyArg()
        )
    }
}

