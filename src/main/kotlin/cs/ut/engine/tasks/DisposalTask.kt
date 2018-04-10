package cs.ut.engine.tasks

import cs.ut.configuration.ConfigurationReader
import cs.ut.engine.Cache
import cs.ut.engine.JobCacheHolder
import cs.ut.engine.LogManager
import cs.ut.jobs.SimulationJob
import cs.ut.logging.NirdizatiLogger
import cs.ut.providers.Dir
import cs.ut.providers.DirectoryConfiguration
import java.io.File
import java.time.Instant
import java.util.Date
import java.util.TimerTask
import kotlin.system.measureTimeMillis

class DisposalTask : TimerTask() {
    override fun run() {
        val time = measureTimeMillis {
            log.debug("Running disposal task")

            JobCacheHolder.parse(LogManager.loadAllJobs()).forEach { it.dispose() }

            log.debug("Disposed of $disposed jobs")
            disposed = 0

            log.debug("Flushing caches")
            Cache.jobCache.flush()
            Cache.chartCache.clear()
            log.debug("Finished flushing caches")
        }
        log.debug("Finished running disposal task in $time ms")
    }

    private fun SimulationJob.isExpired() = Date().time - Date.from(Instant.parse(this.startTime)).time >= age

    private fun SimulationJob.dispose() {
        if (this.isExpired()) {
            log.debug("${this.id} is expired, disposing of the job")

            File(DirectoryConfiguration.dirPath(Dir.TRAIN_DIR) + "${this.id}.json").apply {
                this.safeDelete()
                log.debug("Deleted training file for job ${this.absoluteFile}")
            }

            LogManager.getDetailedFile(this, safe = true).apply {
                this.safeDelete()
                log.debug("Deleted detailed file for job -> ${this.absoluteFile}")
            }

            LogManager.getFeatureImportanceFiles(this, safe = true).apply {
                log.debug("Deleting ${this.size} feature importance file")
                this.forEach { it.safeDelete() }
                log.debug("Finished feature importance file deletion")
            }

            LogManager.getDetailedFile(this, safe = true).apply {
                this.safeDelete()
                log.debug("Deleted detailed file -> ${this.absoluteFile}")
            }

            log.debug("Finished disposal of job ${this.id}")
            disposed++
        }
    }

    private fun File.safeDelete() {
        try {
            this.delete()
        } catch (e: Exception) {
            log.error("Could not delete ${this.absoluteFile}", e)
        }
    }

    companion object {
        private val log = NirdizatiLogger.getLogger(DisposalTask::class.java)
        private val age = ConfigurationReader.findNode("tasks/DisposalTask").valueWithIdentifier("age").long()

        var disposed: Int = 0
    }

}