package com.daybreak.animelauncher.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Primera etapa reversible de 07C-7B-1.
 *
 * Las preferencias conservan las URI histÃ³ricas. Esta polÃ­tica evita que las 30
 * rutas de marcas conocidas lleguen a Coil tras retirarse del paquete: un acceso
 * con packageName usa PackageManager y uno sin packageName usa el pictograma
 * genÃ©rico existente del acceso. No escribe ni elimina preferencias.
 */
internal object CustomIconMigrationPolicy {
    private const val ASSET_PREFIX = "file:///android_asset/iconos/"

    /**
     * These historical asset URIs are persisted in user preferences. Keep the
     * mapping explicit so their PNGs can be removed without losing selections.
     */
    internal val originalPictogramAssetUris: Set<String> = setOf(
        "abrir-documento.png", "agregar-usuario.png", "ajuste.png", "big-gear.png",
        "camara-fotografica.png", "carpeta (1).png", "chat.png", "configuraciones.png",
        "descarga-directa.png", "descargas.png", "enviar.png", "galeria-de-fotos (1).png",
        "galeria-de-fotos.png", "galeria-de-imagenes.png", "llamada.png",
        "llamada-telefonica (1).png", "llamada-telefonica.png", "mapas-y-banderas.png",
        "marcador-de-posicion.png", "perfil.png", "rueda-de-engranaje.png", "subir.png",
        "usuario (1).png", "usuario.png"
    ).mapTo(mutableSetOf()) { ASSET_PREFIX + it }

    /** Authorized Material Icons replacements; no android_asset image is read. */
    fun materialIcon(uri: String?): ImageVector? = when (uri) {
        ASSET_PREFIX + "abrir-documento.png" -> Icons.Outlined.Description
        ASSET_PREFIX + "agregar-usuario.png" -> Icons.Outlined.PersonAdd
        ASSET_PREFIX + "ajuste.png", ASSET_PREFIX + "big-gear.png",
        ASSET_PREFIX + "configuraciones.png", ASSET_PREFIX + "rueda-de-engranaje.png" -> Icons.Outlined.Settings
        ASSET_PREFIX + "camara-fotografica.png" -> Icons.Outlined.CameraAlt
        ASSET_PREFIX + "carpeta (1).png" -> Icons.Outlined.Folder
        ASSET_PREFIX + "chat.png" -> Icons.Outlined.ChatBubbleOutline
        ASSET_PREFIX + "descarga-directa.png", ASSET_PREFIX + "descargas.png" -> Icons.Outlined.Download
        ASSET_PREFIX + "enviar.png" -> Icons.AutoMirrored.Outlined.Send
        ASSET_PREFIX + "galeria-de-fotos (1).png", ASSET_PREFIX + "galeria-de-fotos.png",
        ASSET_PREFIX + "galeria-de-imagenes.png" -> Icons.Outlined.Collections
        ASSET_PREFIX + "llamada.png", ASSET_PREFIX + "llamada-telefonica (1).png",
        ASSET_PREFIX + "llamada-telefonica.png" -> Icons.Outlined.Phone
        ASSET_PREFIX + "mapas-y-banderas.png" -> Icons.Outlined.Map
        ASSET_PREFIX + "marcador-de-posicion.png" -> Icons.Outlined.LocationOn
        ASSET_PREFIX + "perfil.png", ASSET_PREFIX + "usuario (1).png",
        ASSET_PREFIX + "usuario.png" -> Icons.Outlined.Person
        ASSET_PREFIX + "subir.png" -> Icons.Outlined.FileUpload
        else -> null
    }

    internal val knownThirdPartyAssetUris: Set<String> = setOf(
        "cromo.png", "cromo (1).png",
        "discordia.png", "discordia (1).png",
        "facebook.png", "facebook (1).png",
        "gmail.png", "gmail (1).png",
        "google.png", "google (1).png", "google (2).png",
        "google-play.png", "google-play (1).png", "google-play (2).png",
        "google-drive.png", "google-drive (1).png", "google-drive (2).png",
        "logotipo-de-instagram.png",
        "pinterest.png", "pinterest (1).png",
        "telegrama.png", "telegrama (1).png", "telegrama (2).png", "telegrama (3).png",
        "twitter.png", "twitter-forma-negra.png",
        "whatsapp.png", "whatsapp (1).png",
        "youtube.png", "youtube (1).png"
    ).mapTo(mutableSetOf()) { ASSET_PREFIX + it }

    fun isKnownThirdPartyAssetUri(uri: String?): Boolean = uri in knownThirdPartyAssetUris

    /** Only the approved generic pictograms must bypass custom icon tinting. */
    fun isOriginalPictogramAssetUri(uri: String?): Boolean = uri in originalPictogramAssetUris

    fun shouldUsePackageIcon(uri: String?, packageName: String?): Boolean =
        !packageName.isNullOrBlank() && isKnownThirdPartyAssetUri(uri)

    /**
     * Una URI histÃ³rica de marca sin paquete no tiene una asociaciÃ³n segura que
     * permita inventar otro icono de aplicaciÃ³n. El consumidor debe usar su
     * fallback funcional local, sin intentar abrir el PNG ya retirado.
     */
    fun shouldUseGenericFallback(uri: String?, packageName: String?): Boolean =
        packageName.isNullOrBlank() && isKnownThirdPartyAssetUri(uri)
}

