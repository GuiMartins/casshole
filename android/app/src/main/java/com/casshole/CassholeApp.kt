package com.casshole

import android.app.Application
import com.casshole.data.email.EmailFetchCoordinator
import com.casshole.data.local.CategoryRefresher
import com.casshole.data.reminder.InvoiceReminderScheduler
import com.casshole.data.update.AppUpdateCoordinator
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class CassholeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        EmailFetchCoordinator.fetchOnAppStart(this)
        CategoryRefresher.refreshOnAppStart(this)
        AppUpdateCoordinator.checkOnAppStart(this)
        InvoiceReminderScheduler.startOnAppStart(this)
    }
}
