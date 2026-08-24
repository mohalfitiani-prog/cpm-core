package com.example.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.model.*
import com.example.data.dao.*

@Database(
    entities = [
        User::class,
        Company::class,
        SubscriptionPlan::class,
        Subscription::class,
        Project::class,
        ProjectMember::class,
        InvitationCode::class,
        Stage::class,
        WorkItem::class,
        Approval::class,
        ProjectFile::class,
        Comment::class,
        Notification::class,
        SiteObservation::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CPDMSDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun companyDao(): CompanyDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun projectDao(): ProjectDao
    abstract fun invitationCodeDao(): InvitationCodeDao
    abstract fun stageDao(): StageDao
    abstract fun workItemDao(): WorkItemDao
    abstract fun approvalDao(): ApprovalDao
    abstract fun fileDao(): FileDao
    abstract fun commentDao(): CommentDao
    abstract fun notificationDao(): NotificationDao
    abstract fun siteObservationDao(): SiteObservationDao
}
