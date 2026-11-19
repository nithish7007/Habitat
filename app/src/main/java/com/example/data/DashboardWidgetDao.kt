package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for persisting and querying per-user customizable dashboard widget instances.
 */
@Dao
interface DashboardWidgetDao {

    @Query("SELECT * FROM dashboard_widgets WHERE userId = :uid ORDER BY position ASC")
    fun observeWidgetStates(uid: String): Flow<List<WidgetStateEntity>>

    @Query("SELECT * FROM dashboard_widgets WHERE userId = :uid AND isVisible = 1 ORDER BY position ASC")
    fun observeVisibleWidgetStates(uid: String): Flow<List<WidgetStateEntity>>

    @Query("SELECT * FROM dashboard_widgets WHERE userId = :uid ORDER BY position ASC")
    suspend fun getWidgetStatesOnce(uid: String): List<WidgetStateEntity>

    @Query("SELECT * FROM dashboard_widgets WHERE userId = :uid AND id = :id LIMIT 1")
    suspend fun getWidgetById(uid: String, id: String): WidgetStateEntity?

    @Query("SELECT * FROM dashboard_widgets WHERE userId = :uid AND widgetType = :type LIMIT 1")
    suspend fun getWidgetByType(uid: String, type: DashboardWidgetType): WidgetStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWidgetState(entity: WidgetStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWidgetStates(entities: List<WidgetStateEntity>)

    @Query(
        "UPDATE dashboard_widgets SET isVisible = :isVisible, updatedAtMs = :updatedAtMs " +
            "WHERE userId = :uid AND id = :id"
    )
    suspend fun updateVisibility(
        uid: String,
        id: String,
        isVisible: Boolean,
        updatedAtMs: Long = System.currentTimeMillis()
    )

    @Query(
        "UPDATE dashboard_widgets SET position = :position, updatedAtMs = :updatedAtMs " +
            "WHERE userId = :uid AND id = :id"
    )
    suspend fun updatePosition(
        uid: String,
        id: String,
        position: Int,
        updatedAtMs: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM dashboard_widgets WHERE userId = :uid AND id = :id")
    suspend fun deleteWidgetState(uid: String, id: String)

    @Query("DELETE FROM dashboard_widgets WHERE userId = :uid")
    suspend fun clearWidgetStates(uid: String)

    /**
     * Atomically replaces all stored dashboard widget preferences for a user
     * (e.g., after reordering, resetting to defaults, or reconciling cloud sync).
     */
    @Transaction
    suspend fun replaceUserWidgetStates(uid: String, entities: List<WidgetStateEntity>) {
        clearWidgetStates(uid)
        if (entities.isNotEmpty()) {
            upsertWidgetStates(entities)
        }
    }
}
