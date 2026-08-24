package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val passwordHash: String,
    val role: String, // "Super Admin", "Engineering Office", "Owner", "Contractor", "Resident Engineer"
    val accountStatus: String, // "Active", "Inactive", "Suspended"
    val emailVerified: Boolean,
    val createdDate: Long,
    val companyId: String? = null
)

@Entity(tableName = "companies")
data class Company(
    @PrimaryKey val id: String,
    val name: String,
    val logo: String? = null,
    val country: String,
    val city: String,
    val address: String,
    val phone: String,
    val email: String,
    val registrationNumber: String? = null,
    val taxNumber: String? = null,
    val status: String, // "Active", "Suspended"
    val subscriptionPlanId: String? = null
)

@Entity(tableName = "subscription_plans")
data class SubscriptionPlan(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double,
    val durationDays: Int,
    val maxProjects: Int,
    val maxOwners: Int,
    val maxContractors: Int,
    val maxEngineers: Int,
    val storageLimitGb: Int,
    val status: String // "Active", "Inactive"
)

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey val id: String,
    val companyId: String,
    val planId: String,
    val startDate: Long,
    val endDate: Long,
    val status: String, // "Active", "Inactive"
    val paymentStatus: String // "Pending", "Paid", "Failed"
)

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String,
    val companyId: String,
    val name: String,
    val description: String,
    val location: String,
    val projectType: String, // "Residential Building", "Commercial Building", "Infrastructure Project", "Industrial Project", "Other"
    val startDate: Long,
    val expectedEndDate: Long,
    val status: String, // "Planning", "Active", "On Hold", "Completed", "Archived"
    val createdBy: String
)

@Entity(tableName = "project_members")
data class ProjectMember(
    @PrimaryKey val id: String,
    val projectId: String,
    val userId: String,
    val role: String, // "Owner", "Contractor", "Resident Engineer"
    val contractorType: String? = null, // Used if role is Contractor
    val permissionSet: String, // JSON or simple comma-separated string
    val joinDate: Long
)

@Entity(tableName = "invitation_codes")
data class InvitationCode(
    @PrimaryKey val id: String,
    val code: String, // e.g., OWN-84X29K
    val projectId: String,
    val userType: String, // "Owner", "Contractor", "Resident Engineer"
    val contractorType: String? = null, // e.g. "Structural", "Finishing", etc.
    val createdBy: String,
    val expiryDate: Long,
    val status: String, // "Active", "Used", "Expired", "Cancelled"
    val usedBy: String? = null
)

@Entity(tableName = "stages")
data class Stage(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val description: String,
    val startDate: Long,
    val endDate: Long,
    val status: String, // "Not Started", "In Progress", "Submitted For Review", "Approved", "Rejected", "Completed"
    val assignedContractorId: String? = null,
    val assignedEngineerId: String? = null
)

@Entity(tableName = "work_items")
data class WorkItem(
    @PrimaryKey val id: String,
    val stageId: String,
    val contractorId: String?,
    val engineerId: String?,
    val name: String,
    val description: String,
    val dueDate: Long,
    val status: String, // "In Progress", "Submitted For Review", "Approved", "Rejected", "Needs Correction"
    val completionPercentage: Int
)

@Entity(tableName = "approvals")
data class Approval(
    @PrimaryKey val id: String,
    val itemId: String,
    val submittedBy: String,
    val reviewedBy: String?,
    val status: String, // "Submitted", "Approved", "Rejected", "Needs Correction"
    val comment: String?,
    val date: Long
)

@Entity(tableName = "files")
data class ProjectFile(
    @PrimaryKey val id: String,
    val projectId: String,
    val stageId: String?,
    val itemId: String?,
    val uploadedBy: String,
    val fileName: String,
    val filePath: String,
    val fileType: String, // "pdf", "image", "doc", etc.
    val uploadDate: Long
)

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userRole: String,
    val projectId: String,
    val itemId: String?,
    val parentId: String?,
    val commentText: String,
    val createdDate: Long
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val message: String,
    val type: String, // "invitation", "assignment", "approval", "communication"
    val readStatus: Boolean,
    val createdDate: Long
)

@Entity(tableName = "site_observations")
data class SiteObservation(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val description: String,
    val location: String,
    val relatedStageId: String?,
    val relatedContractorId: String?,
    val photoPath: String?,
    val date: Long,
    val status: String // "Open", "In Progress", "Closed"
)
