package com.pedroeu.ficha

import android.app.Application
import com.pedroeu.ficha.data.CharacterRepository
import com.pedroeu.ficha.data.db.AppDatabase

class FichaApplication : Application() {

    val repository: CharacterRepository by lazy {
        CharacterRepository(AppDatabase.get(this).characterDao())
    }
}
