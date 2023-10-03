package net.dodian.utilities.cache.services

import com.displee.cache.CacheLibrary
import com.displee.cache.index.archive.Archive
import net.dodian.utilities.cache.fonts.BitmapFont
import net.dodian.utilities.cache.fonts.FontLoader
import net.dodian.utilities.cache.types.*
import net.dodian.utilities.cache.types.iftype.IfType
import net.dodian.utilities.cache.types.iftype.IfTypeLoader

@Suppress("unused", "MemberVisibilityCanBePrivate")
class CacheService(
    val path: String = "./data/cache",
    val cache: CacheLibrary = CacheLibrary(path),

    val plain11: BitmapFont = FontLoader.load(cache, "p11_full"),
    val plain12: BitmapFont = FontLoader.load(cache, "p12_full"),
    val bold12: BitmapFont = FontLoader.load(cache, "b12_full"),
    val quill8: BitmapFont = FontLoader.load(cache, "q8_full", true),

    val titleArchive: Archive = cache.index(0).archive(1) ?: error("Title archive (1) could not be loaded"),
    val mediaArchive: Archive = cache.index(0).archive(4) ?: error("2D Graphics archive (4) could not be loaded"),

    val ifTypes: List<IfType> = IfTypeLoader.load(cache, listOf(plain11, plain12, bold12, quill8)),
    val floTypes: List<FloType> = FloTypeLoader.load(cache),
    val idkTypes: List<IdkType> = IdkTypeLoader.load(cache),
    val locTypes: List<LocType> = LocTypeLoader.load(cache),
    val objTypes: List<ObjType> = ObjTypeLoader.load(cache),
    val seqTypes: List<SeqType> = SeqTypeLoader.load(cache),
    val spotAnimTypes: List<SpotAnimType> = SpotAnimTypeLoader.load(cache),
    val varbits: List<VarbitType> = VarbitTypeLoader.load(cache),
    val varps: List<VarpType> = VarpTypeLoader.load(cache)
)