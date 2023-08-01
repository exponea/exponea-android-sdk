package com.exponea.sdk.testutil

import com.exponea.sdk.database.ExportedEventRealmDao
import com.exponea.sdk.repository.EventRepositoryImpl

internal fun EventRepositoryImpl.close() {
    if (exportedEventDao is ExportedEventRealmDao) {
        exportedEventDao.database.close()
    }
}
