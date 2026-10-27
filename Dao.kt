package com.gym.hisab.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface HisabDao {
    // ---- live lists for the UI ----
    @Query("SELECT * FROM members WHERE gymId = :g ORDER BY name COLLATE NOCASE")
    fun members(g: String): Flow<List<MemberE>>

    @Query("SELECT * FROM memberships WHERE gymId = :g")
    fun memberships(g: String): Flow<List<MembershipE>>

    @Query("SELECT * FROM payments WHERE gymId = :g ORDER BY paidOn DESC, receiptNo DESC")
    fun payments(g: String): Flow<List<PaymentE>>

    @Query("SELECT * FROM expenses WHERE gymId = :g AND deleted = 0 ORDER BY spentOn DESC")
    fun expenses(g: String): Flow<List<ExpenseE>>

    @Query("SELECT * FROM plans WHERE gymId = :g AND active = 1 ORDER BY durationMonths, name")
    fun plans(g: String): Flow<List<PlanE>>

    @Query("SELECT * FROM attendance WHERE gymId = :g AND attendedOn = :day AND present = 1")
    fun attendanceOn(g: String, day: String): Flow<List<AttendanceE>>

    @Query("SELECT * FROM attendance WHERE gymId = :g AND memberId = :m AND present = 1 ORDER BY attendedOn DESC")
    fun attendanceOfMember(g: String, m: String): Flow<List<AttendanceE>>

    // ---- single rows (used when pushing) ----
    @Query("SELECT * FROM plans WHERE id = :id") suspend fun plan(id: String): PlanE?
    @Query("SELECT * FROM members WHERE id = :id") suspend fun member(id: String): MemberE?
    @Query("SELECT * FROM memberships WHERE id = :id") suspend fun membership(id: String): MembershipE?
    @Query("SELECT * FROM payments WHERE id = :id") suspend fun payment(id: String): PaymentE?
    @Query("SELECT * FROM expenses WHERE id = :id") suspend fun expense(id: String): ExpenseE?
    @Query("SELECT * FROM attendance WHERE id = :id") suspend fun attendance(id: String): AttendanceE?

    // ---- writes ----
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPlans(v: List<PlanE>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMembers(v: List<MemberE>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertMemberships(v: List<MembershipE>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPayments(v: List<PaymentE>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertExpenses(v: List<ExpenseE>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAttendance(v: List<AttendanceE>)

    // ---- outbox ----
    @Insert suspend fun enqueue(o: OutboxE): Long
    @Query("SELECT * FROM outbox WHERE failed = 0 ORDER BY seq") suspend fun pending(): List<OutboxE>
    @Query("SELECT COUNT(*) FROM outbox WHERE failed = 0") fun pendingCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM outbox WHERE failed = 1") fun failedCount(): Flow<Int>
    @Query("SELECT rowId FROM outbox WHERE tableName = :t") suspend fun outboxRowIds(t: String): List<String>
    @Query("DELETE FROM outbox WHERE seq = :seq") suspend fun dropOutbox(seq: Long)
    @Query("UPDATE outbox SET attempts = attempts + 1, lastError = :err, failed = :failed WHERE seq = :seq")
    suspend fun markOutbox(seq: Long, err: String?, failed: Boolean)
    @Query("UPDATE outbox SET failed = 0, attempts = 0, lastError = NULL WHERE failed = 1") suspend fun retryFailed()
    @Query("SELECT lastError FROM outbox WHERE failed = 1 ORDER BY seq LIMIT 1") suspend fun firstFailureMessage(): String?
}

@Database(
    entities = [PlanE::class, MemberE::class, MembershipE::class, PaymentE::class,
        ExpenseE::class, AttendanceE::class, OutboxE::class],
    version = 1,
    exportSchema = false,
)
abstract class HisabDb : RoomDatabase() {
    abstract fun dao(): HisabDao
}
