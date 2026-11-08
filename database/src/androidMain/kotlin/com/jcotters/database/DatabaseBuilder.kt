package com.jcotters.database

import android.content.Context
import androidx.room.Room

fun databaseBuilder(context: Context) = Room.databaseBuilder<MoversDatabase>(
    context.applicationContext,
    context.getDatabasePath("movers_database").absolutePath,
)
