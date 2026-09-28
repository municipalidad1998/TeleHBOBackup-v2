package com.denilson.music.web

import android.content.Context
import android.content.SharedPreferences

/**
 * Bloqueador de anuncios para los navegadores integrados (YouTube y
 * YouTube Music). Siempre activo: no expone interruptor en la interfaz.
 */
object AdBlocker {

    private const val PREFS = "denilson_adblock_stats"
    private const val KEY_BLOCKED = "blocked_total"
    private const val KEY_SESSION = "blocked_session"

    @Volatile
    private var prefs: SharedPreferences? = null

    /** Contador global persistente. */
    var totalBlocked: Int = 0
        private set

    /** Contador de la sesion actual de navegacion. */
    var sessionBlocked: Int = 0
        private set

    fun init(context: Context) {
        if (prefs != null) return
        synchronized(this) {
            if (prefs != null) return
            prefs = context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .also { totalBlocked = it.getInt(KEY_BLOCKED, 0) }
        }
    }

    private fun bump() {
        sessionBlocked++
        val p = prefs ?: return
        totalBlocked++
        p.edit().putInt(KEY_BLOCKED, totalBlocked).apply()
    }

    fun resetStats() {
        sessionBlocked = 0
        totalBlocked = 0
        prefs?.edit()?.putInt(KEY_BLOCKED, 0)?.apply()
    }

    private fun jsEscape(s: String): String =
        "\"" + s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", " ") + "\""

    private val adHosts = listOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "app-measurement.com",
        "admob.com",
        "adservice.google.com",
        "amazon-adsystem.com",
        "adnxs.com",
        "adsystem.amazon.com",
        "scorecardresearch.com",
        "quantserve.com",
        "moatads.com",
        "doubleverify.com",
        "adsafeprotected.com",
        "pubmatic.com",
        "rubiconproject.com",
        "criteo.com",
        "taboola.com",
        "outbrain.com",
        "openx.net",
        "smartadserver.com",
        "adform.net",
        "yieldmo.com",
        "sharethrough.com",
        "3lift.com",
        "connect.facebook.net",
        "ads-twitter.com",
        "analytics.tiktok.com",
        "hotjar.io",
        "hotjar.com",
        "teads.tv",
        "vidazoo.com",
        "disqusads.com"
    )

    private val adPaths = listOf(
        "pagead/",
        "/pagead/",
        "/api/stats/ads",
        "/get_midroll_",
        "/ad_status.js",
        "/vprtb/",
        "/adsid/",
        "/pubsv/"
    )

    /**
     * Decide si una peticion debe bloquearse. NUNCA bloquea googlevideo.com ni stream chunks
     * para que los videos y pistas de audio no se queden cargando indefinidamente.
     */
    fun shouldBlock(url: String): Boolean {
        val lower = url.lowercase()

        // PROTECCIÓN CRÍTICA DE REPRODUCCIÓN:
        // No bloquear streams multimedia legítimos de YouTube ni APIs de reproducción
        if (lower.contains("googlevideo.com") ||
            lower.contains("/videoplayback") ||
            lower.contains("/player") ||
            lower.contains("player_ias.vflset") ||
            lower.contains("/base.js") ||
            lower.contains("/core.js")
        ) {
            return false
        }

        val community = BlocklistLoader.current
        if (!community.isEmpty && community.shouldBlock(url)) return true
        if (!community.isEmpty) return false

        return adHosts.any { lower.contains(it) } || adPaths.any { lower.contains(it) }
    }

    /** CSS cosmetico de las listas comunitarias mas el respaldo embebido. */
    fun cosmeticCssFor(host: String?): String {
        val community = BlocklistLoader.current.cosmeticCssFor(host)
        val fallback = EMBEDDED_COSMETIC
        return when {
            community.isEmpty() -> fallback
            fallback.isEmpty() -> community
            else -> "$community,$fallback"
        }
    }

    /** Registra un bloqueo y actualiza los contadores. */
    fun registerBlock() {
        bump()
        BlocklistLoader.registerBlocked()
    }

    /**
     * Oculta anuncios en la interfaz (banners, promos, overlays y banners de app externa).
     */
    val EMBEDDED_COSMETIC = (
        ".ytp-ad-overlay-slot,#player-ads,#masthead-ad,.ytd-display-ad-renderer," +
        ".ytd-promoted-sparkles-web-renderer,.ytd-promoted-video-renderer," +
        ".ytd-ad-slot-renderer,.ytd-in-feed-ad-layout-renderer," +
        "ytd-banner-promo-renderer,.ytp-ad-module,.ad-frame," +
        ".ytp-ad-persistent-progress-bar-container,.ytp-ad-overlay," +
        "ytmusic-ad-slot-renderer,.ytmusic-player-bar .badge," +
        "ytm-mealbar-promo-renderer,.ytm-upsell-dialog-renderer," +
        ".upsell-dialog-renderer,.mobile-topbar-header-sign-in-button," +
        ".pivot-bar-item-tab"
    ) + "{display:none!important}"

    /**
     * Script inyectado que emula la app nativa y acelera/salta anuncios de video de inmediato.
     */
    fun adKillerJs(css: String): String = """
        (function () {
          if (window.__denilsonAdBlock) { window.__denilsonReapply(); return; }
          window.__denilsonAdBlock = true;

          var st = document.createElement('style');
          st.id = 'denilson-adblock-style';
          st.textContent = ${jsEscape(css)} + 
            " .ytm-pivot-bar-renderer { background: #0f0f0f !important; border-top: 1px solid #272727 !important; } " +
            " body { -webkit-tap-highlight-color: transparent !important; } ";
          (document.head || document.documentElement).appendChild(st);

          window.__denilsonReapply = function () {
            var el = document.getElementById('denilson-adblock-style');
            if (el) el.textContent = ${jsEscape(css)};
          };

          setInterval(function () {
            try {
              // Botón de saltar anuncio
              var skip = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytp-ad-skip-button-slot button, .ytp-ad-text.ytp-ad-preview-text');
              if (skip) skip.click();

              var closeBtn = document.querySelector('.ytp-ad-overlay-close-button, .ytp-ad-interstitial-close-button, button[aria-label="Cerrar anuncio"], button[aria-label="Close ad"]');
              if (closeBtn) closeBtn.click();

              // Si hay anuncio en reproducción en el reproductor de YouTube:
              var adShowing = document.querySelector('.ad-showing, .ad-interrupting');
              var v = document.querySelector('video');
              if (v && adShowing) {
                v.muted = true;
                v.playbackRate = 16.0;
                if (v.duration > 0 && isFinite(v.duration)) {
                  v.currentTime = v.duration;
                }
              }

              // Quitar carteles de "abrir en la app de youtube"
              var openAppDialog = document.querySelector('ytm-upsell-dialog-renderer, ytm-mealbar-promo-renderer');
              if (openAppDialog) openAppDialog.remove();
            } catch (e) {}
          }, 300);
        })();
    """
}
