package com.example.guione

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the privacy configuration: the meal-history database must be excluded
 * from both OS cloud backup and device transfer so the "stays on this device"
 * claim stays true. Reads the shipped resource files directly.
 */
class BackupRulesTest {

    private fun res(name: String): String {
        // Unit tests run with the module (app/) as the working directory.
        val f = File("src/main/res/xml/$name")
        assertTrue("missing $name", f.exists())
        return f.readText()
    }

    @Test fun `data extraction rules exclude the meal database from cloud backup and transfer`() {
        val xml = res("data_extraction_rules.xml")
        assertTrue("no cloud-backup block", xml.contains("<cloud-backup>"))
        assertTrue("no device-transfer block", xml.contains("<device-transfer>"))
        // The db must be excluded, and appear for both blocks (>= 2 occurrences).
        val occurrences = Regex("bytebite-meals\\.db\"").findAll(xml).count()
        assertTrue("db not excluded in both blocks (found $occurrences)", occurrences >= 2)
        assertTrue("db exclude not marked", xml.contains("exclude") && xml.contains("bytebite-meals.db"))
    }

    @Test fun `full backup content excludes the meal database`() {
        val xml = res("backup_rules.xml")
        assertTrue(xml.contains("<exclude") && xml.contains("bytebite-meals.db"))
    }
}
