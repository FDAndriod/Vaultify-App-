package com.dyiz.vaultify.Database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
abstract class PinDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPin(pin: PinEntity)

    @Query("SELECT * FROM pin_table WHERE id = 1 LIMIT 1")
    abstract suspend fun getPin(): PinEntity?

    @Query("SELECT COUNT(*) FROM pin_table WHERE id = 1")
    abstract suspend fun hasPin(): Int

    @Query("DELETE FROM pin_table")
    abstract suspend fun deleteAll()
}
