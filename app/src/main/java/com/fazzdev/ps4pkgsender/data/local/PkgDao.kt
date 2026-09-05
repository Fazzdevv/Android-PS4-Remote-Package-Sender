package com.fazzdev.ps4pkgsender.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PkgDao {
    @Query("SELECT * FROM pkg_files ORDER BY name ASC")
    fun getAllPkgFilesFlow(): Flow<List<PkgEntity>>

    @Query("SELECT * FROM pkg_files ORDER BY name ASC")
    suspend fun getAllPkgFiles(): List<PkgEntity>

    @Query("SELECT * FROM pkg_files WHERE id = :id LIMIT 1")
    suspend fun getPkgById(id: String): PkgEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<PkgEntity>)

    @Query("DELETE FROM pkg_files WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pkg_files")
    suspend fun clearAll()
}
