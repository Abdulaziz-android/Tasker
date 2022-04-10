package com.abdulaziz.tasker

import android.app.Application
import com.abdulaziz.tasker.database.AppDatabase
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class App : Application() {

  private val dataModule = module {
    factory { AppDatabase.getDatabase(androidApplication()).taskDao() }
  }

  override fun onCreate() {
    super.onCreate()
    startKoin {
      androidContext(this@App)
      modules(dataModule)
    }
  }
}