package com.daunpos.app

import android.app.Application
import com.daunpos.app.data.local.DaunPOSDatabase
import com.daunpos.app.data.DaunPOSRepository
import com.daunpos.app.data.SettingsStore

class DaunPOSApplication : Application() {

    val database: DaunPOSDatabase by lazy { DaunPOSDatabase.build(this) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val repository: DaunPOSRepository by lazy {
        DaunPOSRepository(database, settingsStore)
    }
}
