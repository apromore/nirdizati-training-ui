package cs.ut.engine

import cs.ut.charts.Chart
import cs.ut.jobs.JobStatus
import cs.ut.jobs.SimulationJob
import cs.ut.logging.NirdizatiLogger
import cs.ut.util.Field
import cs.ut.util.readTrainingJson
import java.io.File
import java.util.Date

/**
 * Generic structure to represent cached items
 *
 * @param items list where to hold cached items
 */
data class CacheItem<T>(private val items: MutableList<T> = mutableListOf()) {
    private var lastAccessed: Date = Date()

    /**
     * Add item to cache
     *
     * @param item to add
     */
    fun addItem(item: T) {
        lastAccessed = Date()
        items.add(item)
    }

    /**
     * Add multiple items to cache
     *
     * @param items collection of items to add
     */
    fun addItems(items: Collection<T>) {
        lastAccessed = Date()
        this.items.addAll(items)
    }

    /**
     * Retrieve raw items from cache
     */
    fun rawData(): List<T> {
        lastAccessed = Date()
        return items
    }

    /**
     * Is item expired
     * @param timeToLive how long is item supposed to live
     * @return whether or not item is expired
     */
    fun isExpired(timeToLive: Long): Boolean {
        return Date().time - lastAccessed.time > timeToLive
    }
}

/**
 * Generic structure to hold cached items
 */
open class CacheHolder<T> {
    protected val cachedItems = mutableMapOf<String, CacheItem<T>>()

    /**
     * Add item to existing cache if it exists or create a new item
     *
     * @param key to look for
     * @param item to insert into cache
     *
     * @return cached item
     *
     * @see CacheItem
     */
    open fun addToCache(key: String, item: T) = (cachedItems[key] ?: createNewItem(key)).addItem(item)

    /**
     *  Add a collection of items to cache if it exists or create a new item
     *
     *  @param key to look for
     *  @param items to insert into cache
     *
     *  @return cached item
     *
     *  @see CacheItem
     */
    open fun addToCache(key: String, items: Collection<T>) = (cachedItems[key] ?: createNewItem(key)).addItems(items)

    open fun retrieveFromCache(key: String): CacheItem<T> = cachedItems[key] ?: CacheItem()

    private fun createNewItem(key: String): CacheItem<T> = CacheItem<T>().apply { cachedItems[key] = this }

    fun cachedItems() = cachedItems

    fun flush() = cachedItems.clear()
}

/**
 * Implementation of CacheHolder that holds Simulation jobs
 *
 * @see CacheHolder
 * @see SimulationJob
 */
class JobCacheHolder : CacheHolder<SimulationJob>() {

    override fun retrieveFromCache(key: String): CacheItem<SimulationJob> {
        val existing: CacheItem<SimulationJob>? = cachedItems[key]

        return when (existing) {
            is CacheItem<SimulationJob> -> {
                log.debug("Retrieved item from cache for key -> $key")
                existing
            }
            else -> {
                log.debug("Jobs for key $key not cached, fetching from disk")
                fetchFromDisk(key)
            }
        }
    }

    /**
     * Fetches jobs from disk based on given key
     *
     * @param key job to find
     *
     * @return cached item with simulation job as content
     *
     * @see CacheItem
     */
    private fun fetchFromDisk(key: String): CacheItem<SimulationJob> =
            CacheItem<SimulationJob>().apply {
                val items = loadFromDisk(key)
                this.addItems(items)
                cachedItems[key] = this
            }


    private fun loadFromDisk(key: String): List<SimulationJob> {
        return mutableListOf<SimulationJob>().also { c ->
            LogManager.loadJobIds(key)
                    .filter { it.id !in JobManager.queue.map { it.id } }
                    .forEach {
                        val params = readTrainingJson(it.id).flatMap { it.value }
                        c.add(SimulationJob(
                                params.first { it.type == Field.ENCODING.value },
                                params.first { it.type == Field.BUCKETING.value },
                                params.first { it.type == Field.LEARNER.value },
                                params.first { it.type == Field.PREDICTION.value },
                                File(it.path),
                                key,
                                it.id
                        ).apply {
                            this.status = JobStatus.COMPLETED
                            this.startTime = it.startTime
                        })
                    }
        }
    }

    companion object {
        val log = NirdizatiLogger.getLogger(JobCacheHolder::class.java)
    }
}

/**
 * Object that holds job and chart cache in Nirdizati Training System
 */
object Cache {
    var jobCache = JobCacheHolder()

    var chartCache: MutableMap<String, CacheHolder<Chart>> = mutableMapOf()
}

