package com.shinjaehun.oreumola.repositories

import com.shinjaehun.oreumola.db.Run
import com.shinjaehun.oreumola.db.RunDAO
import javax.inject.Inject

class MainRepository @Inject constructor(
    val runDao: RunDAO
){
    suspend fun insertRun(run: Run) = runDao.insertRun(run)

    suspend fun deleteRun(run: Run) = runDao.deleteRun(run)

    fun getAllRunSortedByDate() = runDao.getAllRunsSortedByDate()

    fun getAllRunSortedByDistance() = runDao.getAllRunsSortedByDistance()

    fun getAllRunSortedByTimeInMillis() = runDao.getAllRunsSortedByTimeInMillis()

    fun getAllRunSortedByAvgSpeed() = runDao.getAllRunsSortedByAvgSpeed()

    fun getAllRunSortedByCaloriesBurned() = runDao.getAllRunsSortedByCaloriesBurned()

    fun getTotalAvgSpeed() = runDao.getTotalAvgSpeed()

    fun getTotalDistance() = runDao.getTotalDistance()

    fun getTotalCaloriesBurned() = runDao.getTotalCaloriesBurned()

    fun getTotalTimeInMillis() = runDao.getTotalTimeInMillis()

}