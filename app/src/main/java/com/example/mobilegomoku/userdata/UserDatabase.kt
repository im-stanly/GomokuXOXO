package com.example.mobilegomoku.userdata

import android.content.Context
import androidx.room.*

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val password: String,
    val winCount: Int = 0,
    val winStreak: Int = 0,
    val lossCount: Int = 0
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    fun getUser(username: String): UserEntity?
    
    @Query("SELECT * FROM users ORDER BY rowid DESC LIMIT 1")
    fun getLatestUser(): UserEntity?

    @Query("DELETE FROM users WHERE username = :username")
    fun deleteUser(username: String)

    @Query("""
      UPDATE users 
      SET 
        winCount = winCount + CASE WHEN :isWin = 1 THEN 1 ELSE 0 END,
        lossCount = lossCount + CASE WHEN :isWin = 0 THEN 1 ELSE 0 END,
        winStreak = CASE WHEN :isWin = 1 THEN winStreak + 1 ELSE 0 END
      WHERE username = :username
    """)
    fun updateResult(username: String, isWin: Int)
}

@Database(entities = [UserEntity::class], version = 2, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getInstance(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_database"
                )
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
