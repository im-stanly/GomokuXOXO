package com.example.mobilegomoku.userdata

import android.content.Context
import androidx.room.*

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val password: String
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
}

@Database(entities = [UserEntity::class], version = 1, exportSchema = false)
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
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

