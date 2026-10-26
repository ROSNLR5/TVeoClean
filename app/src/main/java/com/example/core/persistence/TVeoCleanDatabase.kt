package com.example.core.persistence

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.packs.DebloatRecommendationItem
import com.example.core.packs.PackDiffEntry
import com.example.core.packs.PackTarget
import com.example.core.packs.PackTrustTier
import com.example.core.packs.PackType
import com.example.core.packs.TvLauncherThemePreviewSpec
import com.example.core.packs.TveoPackSummary
import com.example.core.restore.AuditLogEntry
import com.example.core.restore.LocalSnapshotRecord
import com.example.core.restore.SnapshotScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey val id: String,
    val createdAtEpochMs: Long,
    val titleEs: String,
    val titleEn: String,
    val scopeName: String,
    val deviceLabel: String,
    val previousValueSummaryEs: String,
    val previousValueSummaryEn: String,
    val appliedPackId: String?,
    val tvPackageStatesJson: String?,
    val integritySha256: String,
    val canRevertOffline: Boolean
)

@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey val id: String,
    val timestampEpochMs: Long,
    val operationEs: String,
    val operationEn: String,
    val targetLabel: String,
    val outcomeEs: String,
    val outcomeEn: String,
    val snapshotRefId: String?
)

@Entity(tableName = "imported_packs")
data class ImportedPackEntity(
    @PrimaryKey val id: String,
    val importedAtEpochMs: Long,
    val packJson: String,
    val localStorageDir: String
)

@Entity(tableName = "saved_tv_devices")
data class SavedTvDeviceEntity(
    @PrimaryKey val endpoint: String,
    val friendlyName: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val lastConnectedEpochMs: Long
)

@Dao
interface TVeoCleanDao {
    @Query("SELECT * FROM snapshots ORDER BY createdAtEpochMs DESC")
    fun observeSnapshots(): Flow<List<SnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: SnapshotEntity)

    @Query("SELECT COUNT(*) FROM snapshots")
    suspend fun countSnapshots(): Int

    @Query("DELETE FROM snapshots WHERE id = :id")
    suspend fun deleteSnapshotById(id: String)

    @Query("SELECT * FROM audit_events ORDER BY timestampEpochMs DESC LIMIT 100")
    fun observeAuditEvents(): Flow<List<AuditEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditEvent(event: AuditEventEntity)

    @Query("SELECT * FROM imported_packs ORDER BY importedAtEpochMs DESC")
    fun observeImportedPacks(): Flow<List<ImportedPackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertImportedPack(pack: ImportedPackEntity)

    @Query("DELETE FROM imported_packs WHERE id = :id")
    suspend fun deleteImportedPackById(id: String)

    @Query("SELECT * FROM saved_tv_devices ORDER BY lastConnectedEpochMs DESC")
    fun observeSavedDevices(): Flow<List<SavedTvDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedDevice(device: SavedTvDeviceEntity)

    @Query("DELETE FROM saved_tv_devices WHERE endpoint = :endpoint")
    suspend fun deleteSavedDevice(endpoint: String)
}

@Database(
    entities = [
        SnapshotEntity::class,
        AuditEventEntity::class,
        ImportedPackEntity::class,
        SavedTvDeviceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TVeoCleanDatabase : RoomDatabase() {
    abstract fun dao(): TVeoCleanDao

    companion object {
        @Volatile
        private var INSTANCE: TVeoCleanDatabase? = null

        fun getInstance(context: Context): TVeoCleanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TVeoCleanDatabase::class.java,
                    "tveoclean_local.db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}

interface PersistenceRepository {
    val snapshotsFlow: Flow<List<LocalSnapshotRecord>>
    val auditEventsFlow: Flow<List<AuditLogEntry>>
    val importedPacksFlow: Flow<List<TveoPackSummary>>
    val savedDevicesFlow: Flow<List<SavedTvDeviceEntity>>

    suspend fun saveSnapshot(record: LocalSnapshotRecord)
    suspend fun deleteSnapshotSafely(id: String): Boolean
    suspend fun recordAudit(entry: AuditLogEntry)
    suspend fun saveImportedPack(pack: TveoPackSummary, localDir: String)
    suspend fun removeImportedPack(id: String)
    suspend fun rememberTvDevice(device: SavedTvDeviceEntity)
    suspend fun forgetTvDevice(endpoint: String)
}

class RoomPersistenceRepository(
    private val dao: TVeoCleanDao
) : PersistenceRepository {

    override val snapshotsFlow: Flow<List<LocalSnapshotRecord>> =
        dao.observeSnapshots().map { list -> list.map { it.toDomain() } }

    override val auditEventsFlow: Flow<List<AuditLogEntry>> =
        dao.observeAuditEvents().map { list -> list.map { it.toDomain() } }

    override val importedPacksFlow: Flow<List<TveoPackSummary>> =
        dao.observeImportedPacks().map { list -> list.mapNotNull { PackJsonCodec.decodePack(it.packJson) } }

    override val savedDevicesFlow: Flow<List<SavedTvDeviceEntity>> =
        dao.observeSavedDevices()

    override suspend fun saveSnapshot(record: LocalSnapshotRecord) {
        dao.upsertSnapshot(record.toEntity())
    }

    override suspend fun deleteSnapshotSafely(id: String): Boolean {
        // Section 13 rule: Never automatically delete the very last recovery snapshot
        if (dao.countSnapshots() <= 1) return false
        dao.deleteSnapshotById(id)
        return true
    }

    override suspend fun recordAudit(entry: AuditLogEntry) {
        dao.insertAuditEvent(entry.toEntity())
    }

    override suspend fun saveImportedPack(pack: TveoPackSummary, localDir: String) {
        dao.upsertImportedPack(
            ImportedPackEntity(
                id = pack.id,
                importedAtEpochMs = System.currentTimeMillis(),
                packJson = PackJsonCodec.encodePack(pack),
                localStorageDir = localDir
            )
        )
    }

    override suspend fun removeImportedPack(id: String) {
        dao.deleteImportedPackById(id)
    }

    override suspend fun rememberTvDevice(device: SavedTvDeviceEntity) {
        dao.upsertSavedDevice(device)
    }

    override suspend fun forgetTvDevice(endpoint: String) {
        dao.deleteSavedDevice(endpoint)
    }
}

class InMemoryPersistenceRepository : PersistenceRepository {
    private val _snapshots = MutableStateFlow<List<LocalSnapshotRecord>>(emptyList())
    private val _audits = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    private val _packs = MutableStateFlow<List<TveoPackSummary>>(emptyList())
    private val _devices = MutableStateFlow<List<SavedTvDeviceEntity>>(emptyList())

    override val snapshotsFlow: Flow<List<LocalSnapshotRecord>> = _snapshots
    override val auditEventsFlow: Flow<List<AuditLogEntry>> = _audits
    override val importedPacksFlow: Flow<List<TveoPackSummary>> = _packs
    override val savedDevicesFlow: Flow<List<SavedTvDeviceEntity>> = _devices

    override suspend fun saveSnapshot(record: LocalSnapshotRecord) {
        _snapshots.update { current -> listOf(record) + current.filterNot { it.id == record.id } }
    }

    override suspend fun deleteSnapshotSafely(id: String): Boolean {
        if (_snapshots.value.size <= 1) return false
        _snapshots.update { it.filterNot { s -> s.id == id } }
        return true
    }

    override suspend fun recordAudit(entry: AuditLogEntry) {
        _audits.update { (listOf(entry) + it).take(100) }
    }

    override suspend fun saveImportedPack(pack: TveoPackSummary, localDir: String) {
        _packs.update { current -> listOf(pack) + current.filterNot { it.id == pack.id } }
    }

    override suspend fun removeImportedPack(id: String) {
        _packs.update { it.filterNot { p -> p.id == id } }
    }

    override suspend fun rememberTvDevice(device: SavedTvDeviceEntity) {
        _devices.update { current -> listOf(device) + current.filterNot { it.endpoint == device.endpoint } }
    }

    override suspend fun forgetTvDevice(endpoint: String) {
        _devices.update { it.filterNot { d -> d.endpoint == endpoint } }
    }
}

private fun SnapshotEntity.toDomain(): LocalSnapshotRecord {
    val scope = runCatching { SnapshotScope.valueOf(scopeName) }.getOrDefault(SnapshotScope.MOBILE_UI)
    val pkgMap = mutableMapOf<String, Boolean>()
    if (!tvPackageStatesJson.isNullOrBlank()) {
        runCatching {
            val obj = JSONObject(tvPackageStatesJson)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                pkgMap[k] = obj.optBoolean(k, true)
            }
        }
    }
    return LocalSnapshotRecord(
        id = id,
        createdAtEpochMs = createdAtEpochMs,
        titleEs = titleEs,
        titleEn = titleEn,
        scope = scope,
        deviceLabel = deviceLabel,
        previousValueSummaryEs = previousValueSummaryEs,
        previousValueSummaryEn = previousValueSummaryEn,
        appliedPackId = appliedPackId,
        tvPackageEnabledStates = pkgMap,
        integritySha256 = integritySha256,
        canRevertOffline = canRevertOffline
    )
}

private fun LocalSnapshotRecord.toEntity(): SnapshotEntity {
    val pkgJson = if (tvPackageEnabledStates.isEmpty()) {
        null
    } else {
        JSONObject().apply {
            tvPackageEnabledStates.forEach { (k, v) -> put(k, v) }
        }.toString()
    }
    return SnapshotEntity(
        id = id,
        createdAtEpochMs = createdAtEpochMs,
        titleEs = titleEs,
        titleEn = titleEn,
        scopeName = scope.name,
        deviceLabel = deviceLabel,
        previousValueSummaryEs = previousValueSummaryEs,
        previousValueSummaryEn = previousValueSummaryEn,
        appliedPackId = appliedPackId,
        tvPackageStatesJson = pkgJson,
        integritySha256 = integritySha256,
        canRevertOffline = canRevertOffline
    )
}

private fun AuditEventEntity.toDomain() = AuditLogEntry(
    id = id,
    timestampEpochMs = timestampEpochMs,
    operationEs = operationEs,
    operationEn = operationEn,
    targetLabel = targetLabel,
    outcomeEs = outcomeEs,
    outcomeEn = outcomeEn,
    snapshotRefId = snapshotRefId
)

private fun AuditLogEntry.toEntity() = AuditEventEntity(
    id = id,
    timestampEpochMs = timestampEpochMs,
    operationEs = operationEs,
    operationEn = operationEn,
    targetLabel = targetLabel,
    outcomeEs = outcomeEs,
    outcomeEn = outcomeEn,
    snapshotRefId = snapshotRefId
)

object PackJsonCodec {
    fun encodePack(pack: TveoPackSummary): String {
        return JSONObject().apply {
            put("id", pack.id)
            put("schema", pack.schema)
            put("nameEs", pack.nameEs)
            put("nameEn", pack.nameEn)
            put("version", pack.version)
            put("descriptionEs", pack.descriptionEs)
            put("descriptionEn", pack.descriptionEn)
            put("author", pack.author)
            put("license", pack.license)
            put("creditsEs", pack.creditsEs)
            put("type", pack.type.name)
            put("target", pack.target.name)
            put("trustTier", pack.trustTier.name)
            put("sourceRepo", pack.sourceRepo)
            put("pinnedCommitSha", pack.pinnedCommitSha)
            put("sha256Manifest", pack.sha256Manifest)
            put("compatibilityLabelEs", pack.compatibilityLabelEs)
            put("isLegacyConverted", pack.isLegacyConverted)
            put("omittedLegacyKeys", JSONArray(pack.omittedLegacyKeys))
            pack.mobilePrimaryDarkHex?.let { put("mobilePrimaryDarkHex", it) }
            pack.mobilePrimaryLightHex?.let { put("mobilePrimaryLightHex", it) }
            pack.mobileSecondaryDarkHex?.let { put("mobileSecondaryDarkHex", it) }
            pack.mobileSecondaryLightHex?.let { put("mobileSecondaryLightHex", it) }
            pack.themePreview?.let { tp ->
                put("themePreview", JSONObject().apply {
                    put("backgroundHex", tp.backgroundHex)
                    put("surfaceHex", tp.surfaceHex)
                    put("accentHex", tp.accentHex)
                    put("glowHex", tp.glowHex)
                    put("columns", tp.columns)
                    put("dockApps", JSONArray(tp.dockApps))
                    put("fillDock", tp.fillDock)
                    put("ambientMotionEnabled", tp.ambientMotionEnabled)
                })
            }
            put("debloatItems", JSONArray().apply {
                pack.debloatItems.forEach { item ->
                    put(JSONObject().apply {
                        put("packageName", item.packageName)
                        put("reasonEs", item.reasonEs)
                        put("reasonEn", item.reasonEn)
                        put("riskLevelEs", item.riskLevelEs)
                    })
                }
            })
            put("diffsFromDefault", JSONArray().apply {
                pack.diffsFromDefault.forEach { d ->
                    put(JSONObject().apply {
                        put("propertyLabelEs", d.propertyLabelEs)
                        put("propertyLabelEn", d.propertyLabelEn)
                        put("currentValue", d.currentValue)
                        put("proposedValue", d.proposedValue)
                    })
                }
            })
        }.toString()
    }

    fun decodePack(raw: String): TveoPackSummary? = runCatching {
        val obj = JSONObject(raw)
        val omitted = mutableListOf<String>()
        obj.optJSONArray("omittedLegacyKeys")?.let { arr ->
            for (i in 0 until arr.length()) omitted.add(arr.optString(i))
        }
        val tpObj = obj.optJSONObject("themePreview")
        val themePreview = tpObj?.let { tp ->
            val dock = mutableListOf<String>()
            tp.optJSONArray("dockApps")?.let { arr ->
                for (i in 0 until arr.length()) dock.add(arr.optString(i))
            }
            TvLauncherThemePreviewSpec(
                backgroundHex = tp.optString("backgroundHex", "#0B1020"),
                surfaceHex = tp.optString("surfaceHex", "#121A2B"),
                accentHex = tp.optString("accentHex", "#55E6C1"),
                glowHex = tp.optString("glowHex", "#8585FF"),
                columns = tp.optInt("columns", 4),
                dockApps = dock,
                fillDock = tp.optBoolean("fillDock", true),
                ambientMotionEnabled = tp.optBoolean("ambientMotionEnabled", true)
            )
        }
        val debloats = mutableListOf<DebloatRecommendationItem>()
        obj.optJSONArray("debloatItems")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                debloats.add(
                    DebloatRecommendationItem(
                        packageName = item.optString("packageName"),
                        reasonEs = item.optString("reasonEs"),
                        reasonEn = item.optString("reasonEn"),
                        riskLevelEs = item.optString("riskLevelEs", "Bajo")
                    )
                )
            }
        }
        val diffs = mutableListOf<PackDiffEntry>()
        obj.optJSONArray("diffsFromDefault")?.let { arr ->
            for (i in 0 until arr.length()) {
                val d = arr.optJSONObject(i) ?: continue
                diffs.add(
                    PackDiffEntry(
                        propertyLabelEs = d.optString("propertyLabelEs"),
                        propertyLabelEn = d.optString("propertyLabelEn"),
                        currentValue = d.optString("currentValue"),
                        proposedValue = d.optString("proposedValue")
                    )
                )
            }
        }
        TveoPackSummary(
            id = obj.getString("id"),
            schema = obj.optString("schema", "1.0"),
            nameEs = obj.getString("nameEs"),
            nameEn = obj.optString("nameEn", obj.getString("nameEs")),
            version = obj.optString("version", "1.0.0"),
            descriptionEs = obj.optString("descriptionEs", ""),
            descriptionEn = obj.optString("descriptionEn", ""),
            author = obj.optString("author", "Comunidad"),
            license = obj.optString("license", "Sin especificar"),
            creditsEs = obj.optString("creditsEs", ""),
            type = PackType.valueOf(obj.optString("type", PackType.THEME.name)),
            target = PackTarget.valueOf(obj.optString("target", PackTarget.TV_LAUNCHER.name)),
            trustTier = PackTrustTier.valueOf(obj.optString("trustTier", PackTrustTier.UNVERIFIED_LINK.name)),
            sourceRepo = obj.optString("sourceRepo", ""),
            pinnedCommitSha = obj.optString("pinnedCommitSha", ""),
            sha256Manifest = obj.optString("sha256Manifest", ""),
            compatibilityLabelEs = obj.optString("compatibilityLabelEs", "Android TV / Google TV"),
            isLegacyConverted = obj.optBoolean("isLegacyConverted", false),
            omittedLegacyKeys = omitted,
            themePreview = themePreview,
            mobilePrimaryDarkHex = obj.optString("mobilePrimaryDarkHex").takeIf { it.isNotBlank() },
            mobilePrimaryLightHex = obj.optString("mobilePrimaryLightHex").takeIf { it.isNotBlank() },
            mobileSecondaryDarkHex = obj.optString("mobileSecondaryDarkHex").takeIf { it.isNotBlank() },
            mobileSecondaryLightHex = obj.optString("mobileSecondaryLightHex").takeIf { it.isNotBlank() },
            debloatItems = debloats,
            diffsFromDefault = diffs
        )
    }.getOrNull()
}
