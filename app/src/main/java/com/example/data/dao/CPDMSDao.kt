package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: String): Flow<User?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>
}

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies WHERE id = :id")
    fun getCompanyById(id: String): Flow<Company?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: Company)

    @Update
    suspend fun updateCompany(company: Company)

    @Query("SELECT * FROM companies")
    fun getAllCompanies(): Flow<List<Company>>
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscription_plans WHERE id = :id")
    fun getPlanById(id: String): Flow<SubscriptionPlan?>

    @Query("SELECT * FROM subscription_plans")
    fun getAllPlans(): Flow<List<SubscriptionPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SubscriptionPlan)

    @Delete
    suspend fun deletePlan(plan: SubscriptionPlan)

    @Query("SELECT * FROM subscriptions WHERE companyId = :companyId LIMIT 1")
    fun getSubscriptionByCompany(companyId: String): Flow<Subscription?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE companyId = :companyId")
    fun getProjectsByCompany(companyId: String): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: String): Flow<Project?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Update
    suspend fun updateProject(project: Project)

    @Query("SELECT * FROM project_members WHERE projectId = :projectId")
    fun getMembersByProject(projectId: String): Flow<List<ProjectMember>>

    @Query("SELECT pm.*, u.firstName, u.lastName, u.email, u.role FROM project_members pm INNER JOIN users u ON pm.userId = u.id WHERE pm.projectId = :projectId")
    fun getMembersWithUserByProject(projectId: String): Flow<List<ProjectMemberWithUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: ProjectMember)

    @Delete
    suspend fun deleteMember(member: ProjectMember)

    @Query("SELECT * FROM project_members WHERE projectId = :projectId AND userId = :userId LIMIT 1")
    suspend fun getMemberByProjectAndUser(projectId: String, userId: String): ProjectMember?

    @Query("SELECT p.* FROM projects p INNER JOIN project_members pm ON p.id = pm.projectId WHERE pm.userId = :userId")
    fun getProjectsByMember(userId: String): Flow<List<Project>>
}

// Data class to easily represent a member join with user info
data class ProjectMemberWithUser(
    val id: String,
    val projectId: String,
    val userId: String,
    val role: String,
    val contractorType: String?,
    val permissionSet: String,
    val joinDate: Long,
    val firstName: String,
    val lastName: String,
    val email: String
)

@Dao
interface InvitationCodeDao {
    @Query("SELECT * FROM invitation_codes WHERE code = :code LIMIT 1")
    suspend fun getCodeByString(code: String): InvitationCode?

    @Query("SELECT * FROM invitation_codes WHERE projectId = :projectId")
    fun getCodesByProject(projectId: String): Flow<List<InvitationCode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCode(code: InvitationCode)

    @Update
    suspend fun updateCode(code: InvitationCode)
}

@Dao
interface StageDao {
    @Query("SELECT * FROM stages WHERE projectId = :projectId")
    fun getStagesByProject(projectId: String): Flow<List<Stage>>

    @Query("SELECT * FROM stages WHERE id = :id")
    fun getStageById(id: String): Flow<Stage?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStage(stage: Stage)

    @Update
    suspend fun updateStage(stage: Stage)

    @Delete
    suspend fun deleteStage(stage: Stage)
}

@Dao
interface WorkItemDao {
    @Query("SELECT * FROM work_items WHERE stageId = :stageId")
    fun getItemsByStage(stageId: String): Flow<List<WorkItem>>

    @Query("SELECT * FROM work_items WHERE id = :id")
    fun getItemById(id: String): Flow<WorkItem?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WorkItem)

    @Update
    suspend fun updateItem(item: WorkItem)

    @Query("SELECT wi.* FROM work_items wi INNER JOIN stages s ON wi.stageId = s.id WHERE s.projectId = :projectId AND wi.contractorId = :contractorId")
    fun getItemsByProjectAndContractor(projectId: String, contractorId: String): Flow<List<WorkItem>>

    @Query("SELECT wi.* FROM work_items wi INNER JOIN stages s ON wi.stageId = s.id WHERE s.projectId = :projectId AND wi.engineerId = :engineerId")
    fun getItemsByProjectAndEngineer(projectId: String, engineerId: String): Flow<List<WorkItem>>
}

@Dao
interface ApprovalDao {
    @Query("SELECT * FROM approvals WHERE itemId = :itemId ORDER BY date DESC")
    fun getApprovalsByItem(itemId: String): Flow<List<Approval>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApproval(approval: Approval)
}

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE projectId = :projectId")
    fun getFilesByProject(projectId: String): Flow<List<ProjectFile>>

    @Query("SELECT * FROM files WHERE stageId = :stageId")
    fun getFilesByStage(stageId: String): Flow<List<ProjectFile>>

    @Query("SELECT * FROM files WHERE itemId = :itemId")
    fun getFilesByItem(itemId: String): Flow<List<ProjectFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFile)

    @Query("DELETE FROM files WHERE id = :fileId")
    suspend fun deleteFile(fileId: String)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE projectId = :projectId ORDER BY createdDate ASC")
    fun getCommentsByProject(projectId: String): Flow<List<Comment>>

    @Query("SELECT * FROM comments WHERE itemId = :itemId ORDER BY createdDate ASC")
    fun getCommentsByItem(itemId: String): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdDate DESC")
    fun getNotificationsByUser(userId: String): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Query("UPDATE notifications SET readStatus = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: String)
}

@Dao
interface SiteObservationDao {
    @Query("SELECT * FROM site_observations WHERE projectId = :projectId ORDER BY date DESC")
    fun getObservationsByProject(projectId: String): Flow<List<SiteObservation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(observation: SiteObservation)

    @Update
    suspend fun updateObservation(observation: SiteObservation)
}
