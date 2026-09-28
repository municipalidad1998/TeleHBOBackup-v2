package com.denilson.music.web

/**
 * Motor de filtrado con sintaxis ABP (Adblock Plus), compatible con
 * EasyList y EasyPrivacy.
 *
 * Soporta el subconjunto seguro de la especificacion:
 *  - `||dominio.com^`      ancla de dominio (incluye subdominios)
 *  - `|http://...`         ancla inicial
 *  - `texto|fin`           ancla final
 *  - `/expresion/`         expresion regular
 *  - `@@||dominio^`        excepcion (permitir)
 *  - `##selector`          regla cosmetica (ocultar elemento)
 *  - `dominio##selector`   regla cosmetica limitada a un dominio
 *
 * NO se ejecutan los *scriptlets* (`##+js`, `#?#`, `#$#`) porque
 * inyectan JavaScript arbitrario. Tampoco se tocan credenciales,
 * cookies, tokens ni DRM: esto filtra publicidad y rastreo, nada mas.
 *
 * La busqueda de dominios es O(etiquetas) mediante un HashSet, no un
 * escaneo lineal sobre las ~50.000 reglas de EasyList.
 */
class Blocklist private constructor(
    val domains: Set<String>,
    val patterns: List<String>,
    val regexes: Map<String, Regex>,
    val exceptions: Set<String>,
    val cosmetics: List<CosmeticRule>,
    val cosmeticDomainRules: Map<String, List<String>>,
    val ruleCount: Int
) {

    data class CosmeticRule(val domain: String?, val selector: String)

    val isEmpty: Boolean get() = ruleCount == 0

    /**
     * Decide si una URL debe cancelarse antes de descargarse.
     * Las excepciones tienen prioridad sobre el resto de reglas.
     */
    fun shouldBlock(url: String): Boolean {
        val lower = url.lowercase()
        val host = hostOf(lower)

        if (matchesDomain(host, exceptions)) return false
        if (host != null && matchesSubstring(lower, exceptions)) return false

        if (matchesDomain(host, domains)) return true
        return matchesSubstring(lower, emptySet())
    }

    /** Comprueba si el host o alguno de sus padres esta en el conjunto. */
    private fun matchesDomain(host: String?, set: Set<String>): Boolean {
        if (host == null || set.isEmpty()) return false
        var candidate: String = host
        while (candidate.isNotEmpty()) {
            if (set.contains(candidate)) return true
            val dot = candidate.indexOf('.')
            if (dot < 0) return false
            candidate = candidate.substring(dot + 1)
        }
        return false
    }
    /** Aplica anclas iniciales/finales, subcadenas y expresiones regulares. */
    private fun matchesSubstring(url: String, extra: Set<String>): Boolean {
        for (p in patterns) {
            val hit = when {
                p.startsWith("/") && p.endsWith("/") && p.length > 2 -> {
                    val re = regexes[p] ?: continue
                    re.containsMatchIn(url)
                }
                p.startsWith("|") -> url.contains(p.substring(1))
                p.endsWith("|") -> url.contains(p.dropLast(1))
                else -> url.contains(p)
            }
            if (hit) return true
        }
        if (extra.isNotEmpty()) {
            for (e in extra) {
                if (url.contains(e)) return true
            }
        }
        return false
    }

    /** Genera el CSS cosmetico aplicable a un dominio concreto. */
    fun cosmeticCssFor(host: String?): String {
        val selectors = ArrayList<String>(32)
        for (c in cosmetics) {
            if (c.domain == null) selectors.add(c.selector)
        }
        if (host != null) {
            var candidate: String = host
            while (candidate.isNotEmpty()) {
                cosmeticDomainRules[candidate]?.forEach { s -> selectors.add(s) }
                val dot = candidate.indexOf('.')
                if (dot < 0) break
                candidate = candidate.substring(dot + 1)
            }
        }
        if (selectors.isEmpty()) return ""
        val joined = selectors.distinct().joinToString(",")
        return "$joined{display:none!important}"
    }

    companion object {
        val EMPTY = Blocklist(
            emptySet(), emptyList(), emptyMap(), emptySet(),
            emptyList(), emptyMap(), 0
        )

        private fun hostOf(lowerUrl: String): String? {
            val schemeEnd = lowerUrl.indexOf("://")
            if (schemeEnd < 0) return null
            val start = schemeEnd + 3
            var end = lowerUrl.indexOf('/', start)
            if (end < 0) end = lowerUrl.length
            var host = lowerUrl.substring(start, end)
            val at = host.indexOf('@')
            if (at >= 0) host = host.substring(at + 1)
            val colon = host.indexOf(':')
            if (colon >= 0) host = host.substring(0, colon)
            return host.ifEmpty { null }
        }

        /**
         * Analiza una lista de filtros ABP. Descarta comentarios,
         * scriptlets y reglas con sintaxis no soportada.
         */
        fun parse(text: String): Blocklist {
            val domains = HashSet<String>(8192)
            val patterns = ArrayList<String>(4096)
            val regexes = HashMap<String, Regex>(256)
            val exceptions = HashSet<String>(1024)
            val cosmetics = ArrayList<CosmeticRule>(2048)
            val cosmeticDomains = HashMap<String, MutableList<String>>(1024)
            var count = 0

            for (rawLine in text.lineSequence()) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) continue
                // Scriptlets: inyectan JS arbitrario, no se ejecutan
                if (line.contains("#?#") || line.contains("##+js") || line.contains("#@#")) continue

                // ---- Regla cosmetica ----
                // El indice 0 es valido: las reglas globales empiezan por "##"
                val hashIndex = line.indexOf("##")
                if (hashIndex >= 0) {
                    val left = line.substring(0, hashIndex)
                    val selector = line.substring(hashIndex + 2)
                    // "#$#" (procedural) queda fuera; "###id" es un selector legitimo
                    if (selector.startsWith("$")) continue
                    if (selector.isNotBlank() && selector.none { it == '(' || it == '\\' }) {
                        val domain = left.trim().lowercase()
                            .removePrefix("@@").removePrefix("||").removeSuffix("^")
                            .takeIf { it.isNotEmpty() && !it.contains('/') && !it.contains('*') }
                        cosmetics.add(CosmeticRule(domain, selector))
                        if (domain != null) {
                            cosmeticDomains.getOrPut(domain) { ArrayList(2) }.add(selector)
                        }
                        count++
                    }
                    continue
                }

                // ---- Excepcion ----
                var body = line
                var isException = false
                if (body.startsWith("@@")) {
                    isException = true
                    body = body.substring(2)
                }
                if (body.isEmpty()) continue

                // ---- Ancla de dominio ||dominio.com^ ----
                // Solo si el nombre de dominio es limpio (sin comodines)
                if (body.startsWith("||")) {
                    val rest = body.substring(2)
                    val domain = rest.takeWhile { it != '/' && it != '^' }
                    val clean = domain.isNotEmpty() &&
                        domain.contains('.') &&
                        !domain.contains('|') &&
                        !domain.contains('*') &&
                        domain.last() != '.' &&
                        !domain.startsWith('.')
                    if (clean) {
                        val d = domain.lowercase()
                        if (isException) exceptions.add(d) else domains.add(d)
                        count++
                        continue
                    }
                }

                // ---- Expresion regular /.../ ----
                if (body.startsWith("/") && body.endsWith("/") && body.length > 2) {
                    try {
                        val re = Regex(body.substring(1, body.length - 1), RegexOption.IGNORE_CASE)
                        if (isException) exceptions.add(body)
                        else {
                            patterns.add(body)
                            regexes[body] = re
                        }
                        count++
                    } catch (_: Exception) {
                        // Expresion invalida: se ignora
                    }
                    continue
                }

                // ---- Filtro de subcadena / anclas ----
                val cleaned = body.replace("^", "").replace("*", "")
                if (cleaned.length in 3..180 && !cleaned.contains(' ')) {
                    patterns.add(cleaned)
                    count++
                }
            }

            return Blocklist(
                domains = domains,
                patterns = patterns,
                regexes = regexes,
                exceptions = exceptions,
                cosmetics = cosmetics,
                cosmeticDomainRules = cosmeticDomains,
                ruleCount = count
            )
        }
    }
}
