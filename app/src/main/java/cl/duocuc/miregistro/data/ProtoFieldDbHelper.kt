package cl.duocuc.miregistro.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cl.duocuc.miregistro.model.Equipo
import cl.duocuc.miregistro.model.EstadoProtocolo
import cl.duocuc.miregistro.model.Instalacion
import cl.duocuc.miregistro.model.Medicion
import cl.duocuc.miregistro.model.Protocolo
import cl.duocuc.miregistro.model.ResumenProtocolos
import cl.duocuc.miregistro.model.TipoEquipo
import cl.duocuc.miregistro.model.Usuario

// =====================================================================
//  NUEVO (SQLite) — ProtoFieldDbHelper.kt
//  ---------------------------------------------------------------------
//  UNA base de datos (protofield.db) con SEIS tablas relacionadas:
//
//   roles ◀── usuarios ◀── protocolos ──▶ equipos ──▶ instalaciones
//                              ▲
//                              └── mediciones
//
//   roles          (id_rol, nombre_rol)
//   usuarios       (id_usuario, nombre, correo, contrasena, id_rol, avatar)
//   instalaciones  (id_instalacion, cliente, planta, ciudad)
//   equipos        (id_equipo, codigo, tipo, marca, id_instalacion)
//   protocolos     (id_protocolo, id_equipo, id_usuario, fecha_hora, estado,
//                   resultado, observaciones)
//   mediciones     (id_medicion, id_protocolo, parametro, valor, unidad, minimo, maximo)
//
//  Cada flecha es una CLAVE FORÁNEA: la tabla "hija" guarda el id de la "padre".
//  Regla: UN helper por base de datos. Todas las tablas se crean en el MISMO onCreate().
//  Las columnas tienen nombres distintos en cada tabla (id_rol / id_usuario,
//  nombre_rol / nombre...) para que el JOIN no tenga columnas repetidas.
//
//  Todo se guarda en el teléfono: la app funciona SIN internet.
// =====================================================================

class ProtoFieldDbHelper(context: Context) :
    SQLiteOpenHelper(context, NOMBRE_BD, null, VERSION_BD) {

    companion object {
        // NUEVO (SQLite): estos dos valores viajan al constructor del padre (línea de arriba)
        private const val NOMBRE_BD = "protofield.db"
        private const val VERSION_BD = 1

        // NUEVO (SQLite): tabla roles
        private const val TABLA_ROLES = "roles"
        private const val ROL_ID = "id_rol"
        private const val ROL_NOMBRE = "nombre_rol"

        // NUEVO (SQLite): tabla usuarios
        private const val TABLA_USUARIOS = "usuarios"
        private const val USU_ID = "id_usuario"
        private const val USU_NOMBRE = "nombre"
        private const val USU_CORREO = "correo"
        private const val USU_CONTRASENA = "contrasena"
        private const val USU_ROL = "id_rol"
        private const val USU_AVATAR = "avatar"

        // NUEVO (SQLite): tabla instalaciones
        private const val TABLA_INSTALACIONES = "instalaciones"
        private const val INS_ID = "id_instalacion"
        private const val INS_CLIENTE = "cliente"
        private const val INS_PLANTA = "planta"
        private const val INS_CIUDAD = "ciudad"

        // NUEVO (SQLite): tabla equipos
        private const val TABLA_EQUIPOS = "equipos"
        private const val EQU_ID = "id_equipo"
        private const val EQU_CODIGO = "codigo"
        private const val EQU_TIPO = "tipo"
        private const val EQU_MARCA = "marca"
        private const val EQU_INSTALACION = "id_instalacion"

        // NUEVO (SQLite): tabla protocolos
        private const val TABLA_PROTOCOLOS = "protocolos"
        private const val PRO_ID = "id_protocolo"
        private const val PRO_EQUIPO = "id_equipo"
        private const val PRO_USUARIO = "id_usuario"
        private const val PRO_FECHA = "fecha_hora"
        private const val PRO_ESTADO = "estado"
        private const val PRO_RESULTADO = "resultado"
        private const val PRO_OBSERVACIONES = "observaciones"

        // NUEVO (SQLite): tabla mediciones
        private const val TABLA_MEDICIONES = "mediciones"
        private const val MED_ID = "id_medicion"
        private const val MED_PROTOCOLO = "id_protocolo"
        private const val MED_PARAMETRO = "parametro"
        private const val MED_VALOR = "valor"
        private const val MED_UNIDAD = "unidad"
        private const val MED_MINIMO = "minimo"
        private const val MED_MAXIMO = "maximo"

        // NUEVO (SQLite): nombres de columnas calculadas (alias) que no existen en ninguna tabla
        private const val ALIAS_TECNICO = "nombre_tecnico"
        private const val ALIAS_FUERA_RANGO = "fuera_rango"

        // NUEVO (SQLite): un día en milisegundos (para fechar los protocolos precargados)
        private const val DIA_MS = 24L * 60 * 60 * 1000

        // NUEVO (SQLite): las constantes se "pegan" dentro del texto SQL con $.
        private const val SQL_CREAR_ROLES =
            "CREATE TABLE $TABLA_ROLES (" +
                "$ROL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$ROL_NOMBRE TEXT NOT NULL UNIQUE)"

        // NUEVO (SQLite): UNIQUE en correo -> no pueden existir dos cuentas con el mismo correo.
        // avatar es BLOB sin NOT NULL: un usuario nuevo todavía no tiene foto.
        private const val SQL_CREAR_USUARIOS =
            "CREATE TABLE $TABLA_USUARIOS (" +
                "$USU_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$USU_NOMBRE TEXT NOT NULL, " +
                "$USU_CORREO TEXT NOT NULL UNIQUE, " +
                "$USU_CONTRASENA TEXT NOT NULL, " +
                "$USU_ROL INTEGER NOT NULL REFERENCES $TABLA_ROLES($ROL_ID), " +
                "$USU_AVATAR BLOB)"

        private const val SQL_CREAR_INSTALACIONES =
            "CREATE TABLE $TABLA_INSTALACIONES (" +
                "$INS_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$INS_CLIENTE TEXT NOT NULL, " +
                "$INS_PLANTA TEXT NOT NULL, " +
                "$INS_CIUDAD TEXT NOT NULL)"

        private const val SQL_CREAR_EQUIPOS =
            "CREATE TABLE $TABLA_EQUIPOS (" +
                "$EQU_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$EQU_CODIGO TEXT NOT NULL UNIQUE, " +
                "$EQU_TIPO TEXT NOT NULL, " +
                "$EQU_MARCA TEXT NOT NULL, " +
                "$EQU_INSTALACION INTEGER NOT NULL REFERENCES $TABLA_INSTALACIONES($INS_ID))"

        // NUEVO (SQLite): fecha_hora es INTEGER (milisegundos). estado guarda el .name del enum.
        private const val SQL_CREAR_PROTOCOLOS =
            "CREATE TABLE $TABLA_PROTOCOLOS (" +
                "$PRO_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$PRO_EQUIPO INTEGER NOT NULL REFERENCES $TABLA_EQUIPOS($EQU_ID), " +
                "$PRO_USUARIO INTEGER NOT NULL REFERENCES $TABLA_USUARIOS($USU_ID), " +
                "$PRO_FECHA INTEGER NOT NULL, " +
                "$PRO_ESTADO TEXT NOT NULL, " +
                "$PRO_RESULTADO TEXT NOT NULL, " +
                "$PRO_OBSERVACIONES TEXT NOT NULL)"

        private const val SQL_CREAR_MEDICIONES =
            "CREATE TABLE $TABLA_MEDICIONES (" +
                "$MED_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$MED_PROTOCOLO INTEGER NOT NULL REFERENCES $TABLA_PROTOCOLOS($PRO_ID), " +
                "$MED_PARAMETRO TEXT NOT NULL, " +
                "$MED_VALOR REAL NOT NULL, " +
                "$MED_UNIDAD TEXT NOT NULL, " +
                "$MED_MINIMO REAL NOT NULL, " +
                "$MED_MAXIMO REAL NOT NULL)"

        // NUEVO (SQLite): datos precargados (TODOS ficticios). Los roles llevan id explícito
        // para que Rol.ID_TECNICO (1) y Rol.ID_SUPERVISOR (2) siempre apunten al rol correcto.
        private const val SQL_PRECARGAR_ROLES =
            "INSERT INTO $TABLA_ROLES ($ROL_ID, $ROL_NOMBRE) VALUES " +
                "(1, 'Técnico'), (2, 'Supervisor')"

        // NUEVO (Login): contraseñas de prueba en texto plano porque son ficticias.
        // En una app real NUNCA se guardan así: se guarda un hash.
        private const val SQL_PRECARGAR_USUARIOS =
            "INSERT INTO $TABLA_USUARIOS ($USU_NOMBRE, $USU_CORREO, $USU_CONTRASENA, $USU_ROL) VALUES " +
                "('Mateo Rivas', 'mateo.rivas@induztek.cl', 'tecnico123', 1), " +
                "('Sofía Araya', 'sofia.araya@induztek.cl', 'tecnico123', 1), " +
                "('Carlos Fuentes', 'carlos.fuentes@induztek.cl', 'super123', 2)"

        private const val SQL_PRECARGAR_INSTALACIONES =
            "INSERT INTO $TABLA_INSTALACIONES ($INS_CLIENTE, $INS_PLANTA, $INS_CIUDAD) VALUES " +
                "('Minera Los Andes', 'Planta Concentradora Norte', 'Antofagasta'), " +
                "('Papelera del Sur', 'Planta de Celulosa', 'Concepción'), " +
                "('Agroindustrial Maule', 'Planta Frigorífico', 'Talca')"

        // NUEVO (SQLite): el id de cada equipo es su posición (1 a 6); los protocolos
        // precargados lo usan. El tipo es el .name de TipoEquipo.
        private const val SQL_PRECARGAR_EQUIPOS =
            "INSERT INTO $TABLA_EQUIPOS ($EQU_CODIGO, $EQU_TIPO, $EQU_MARCA, $EQU_INSTALACION) VALUES " +
                "('TR-001', 'TRANSFORMADOR', 'Siemens', 1), " +
                "('RL-101', 'RELE', 'SEL', 1), " +
                "('CE-201', 'CELDA', 'ABB', 2), " +
                "('CB-301', 'CABLE', 'Prysmian', 2), " +
                "('TR-002', 'TRANSFORMADOR', 'ABB', 3), " +
                "('RL-102', 'RELE', 'Schneider', 3)"

        // NUEVO (SQLite): SELECT base de usuarios con INNER JOIN a su rol.
        private const val SQL_SELECT_USUARIOS =
            "SELECT u.*, r.$ROL_NOMBRE AS $ROL_NOMBRE " +
                "FROM $TABLA_USUARIOS u " +
                "INNER JOIN $TABLA_ROLES r ON u.$USU_ROL = r.$ROL_ID"

        // NUEVO (SQLite): SELECT base de equipos. El JOIN trae los datos de su instalación.
        private const val SQL_SELECT_EQUIPOS =
            "SELECT e.*, i.$INS_CLIENTE AS $INS_CLIENTE, i.$INS_PLANTA AS $INS_PLANTA, " +
                "i.$INS_CIUDAD AS $INS_CIUDAD " +
                "FROM $TABLA_EQUIPOS e " +
                "INNER JOIN $TABLA_INSTALACIONES i ON e.$EQU_INSTALACION = i.$INS_ID"

        // NUEVO (SQLite): SELECT base de protocolos con TRES JOIN:
        // protocolo -> equipo (código y tipo) -> instalación (planta) y protocolo -> usuario (técnico).
        // La subconsulta cuenta las mediciones fuera de rango de CADA protocolo:
        // "valor menor al mínimo o mayor al máximo".
        private const val SQL_SELECT_PROTOCOLOS =
            "SELECT p.*, e.$EQU_CODIGO AS $EQU_CODIGO, e.$EQU_TIPO AS $EQU_TIPO, " +
                "i.$INS_PLANTA AS $INS_PLANTA, i.$INS_CLIENTE AS $INS_CLIENTE, " +
                "u.$USU_NOMBRE AS $ALIAS_TECNICO, " +
                "(SELECT COUNT(*) FROM $TABLA_MEDICIONES m WHERE m.$MED_PROTOCOLO = p.$PRO_ID " +
                "AND (m.$MED_VALOR < m.$MED_MINIMO OR m.$MED_VALOR > m.$MED_MAXIMO)) " +
                "AS $ALIAS_FUERA_RANGO " +
                "FROM $TABLA_PROTOCOLOS p " +
                "INNER JOIN $TABLA_EQUIPOS e ON p.$PRO_EQUIPO = e.$EQU_ID " +
                "INNER JOIN $TABLA_INSTALACIONES i ON e.$EQU_INSTALACION = i.$INS_ID " +
                "INNER JOIN $TABLA_USUARIOS u ON p.$PRO_USUARIO = u.$USU_ID"
    }

    // NUEVO (SQLite): se ejecuta CADA VEZ que se abre la conexión, antes de onCreate.
    // SQLite trae las claves foráneas APAGADAS por defecto: aquí se encienden.
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // NUEVO (SQLite): una sola vez, cuando protofield.db no existe.
    // Orden: primero las tablas "padre" y después las que las referencian.
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREAR_ROLES)
        db.execSQL(SQL_CREAR_USUARIOS)
        db.execSQL(SQL_CREAR_INSTALACIONES)
        db.execSQL(SQL_CREAR_EQUIPOS)
        db.execSQL(SQL_CREAR_PROTOCOLOS)
        db.execSQL(SQL_CREAR_MEDICIONES)

        db.execSQL(SQL_PRECARGAR_ROLES)
        db.execSQL(SQL_PRECARGAR_USUARIOS)
        db.execSQL(SQL_PRECARGAR_INSTALACIONES)
        db.execSQL(SQL_PRECARGAR_EQUIPOS)
        precargarProtocolos(db)
    }

    // NUEVO (SQLite): si sube VERSION_BD. Versión simple: borra y recrea (se pierden datos).
    // Orden inverso: primero las tablas "hija" y al final las "padre".
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLA_MEDICIONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLA_PROTOCOLOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLA_EQUIPOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLA_INSTALACIONES")
        db.execSQL("DROP TABLE IF EXISTS $TABLA_USUARIOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLA_ROLES")
        onCreate(db)
    }

    // NUEVO (SQLite): protocolos ANTIGUOS (2 o 3 por equipo) para que el historial tenga con qué
    // comparar. "dias" = hace cuántos días se hizo. "valores" va en el MISMO orden que
    // TipoEquipo.parametros. Se arman con las mismas funciones que usa la app al guardar.
    private fun precargarProtocolos(db: SQLiteDatabase) {
        val ahora = System.currentTimeMillis()
        val semillas = listOf(
            // TR-001: el aislamiento baja prueba a prueba (aún dentro del rango).
            // La última, de Mateo, es reciente y sigue "Registrado en terreno".
            SemillaProtocolo(1, TipoEquipo.TRANSFORMADOR, 540, 1, listOf(4200.0, 20.01)),
            SemillaProtocolo(1, TipoEquipo.TRANSFORMADOR, 360, 2, listOf(3650.0, 20.02)),
            SemillaProtocolo(
                1, TipoEquipo.TRANSFORMADOR, 5, 1, listOf(3100.0, 19.98),
                estado = EstadoProtocolo.EN_TERRENO
            ),
            // RL-101: el relé se vuelve más lento, pero sigue dentro del rango
            SemillaProtocolo(2, TipoEquipo.RELE, 500, 2, listOf(32.0, 5.01)),
            SemillaProtocolo(2, TipoEquipo.RELE, 250, 1, listOf(36.0, 4.98)),
            SemillaProtocolo(2, TipoEquipo.RELE, 90, 2, listOf(41.0, 4.95)),
            // CE-201: la última prueba sale fuera de rango (contacto sobre 80 µΩ)
            SemillaProtocolo(3, TipoEquipo.CELDA, 420, 1, listOf(45.0, 2400.0)),
            SemillaProtocolo(3, TipoEquipo.CELDA, 200, 1, listOf(62.0, 1800.0)),
            SemillaProtocolo(
                3, TipoEquipo.CELDA, 60, 2, listOf(88.0, 1500.0),
                Protocolo.RESULTADO_RECHAZADO, "Contactos con desgaste: se recomienda mantención"
            ),
            // CB-301: el aislamiento cae bajo el mínimo (1000 MΩ) en la última prueba
            SemillaProtocolo(4, TipoEquipo.CABLE, 480, 2, listOf(8200.0)),
            SemillaProtocolo(4, TipoEquipo.CABLE, 240, 1, listOf(4300.0)),
            SemillaProtocolo(
                4, TipoEquipo.CABLE, 120, 1, listOf(950.0),
                Protocolo.RESULTADO_RECHAZADO, "Aislamiento bajo el mínimo: revisar humedad en el tramo"
            ),
            // TR-002: la última, de Mateo, es de hace 2 días y está "Registrado en terreno"
            SemillaProtocolo(5, TipoEquipo.TRANSFORMADOR, 400, 2, listOf(3800.0, 20.0)),
            SemillaProtocolo(
                5, TipoEquipo.TRANSFORMADOR, 2, 1, listOf(3500.0, 20.05),
                estado = EstadoProtocolo.EN_TERRENO
            ),
            // RL-102: la última, de Sofía, aún está "Registrado en terreno" (falta formalizar)
            SemillaProtocolo(6, TipoEquipo.RELE, 300, 1, listOf(28.0, 5.0)),
            SemillaProtocolo(
                6, TipoEquipo.RELE, 7, 2, listOf(44.0, 5.03),
                estado = EstadoProtocolo.EN_TERRENO
            )
        )
        semillas.forEach { semilla ->
            val mediciones = semilla.tipo.parametros.mapIndexed { indice, parametro ->
                Medicion(
                    parametro = parametro.nombre,
                    valor = semilla.valores[indice],
                    unidad = parametro.unidad,
                    minimo = parametro.minimo,
                    maximo = parametro.maximo
                )
            }
            guardarProtocolo(
                db,
                Protocolo(
                    equipoId = semilla.equipoId,
                    usuarioId = semilla.usuarioId,
                    fechaHora = ahora - semilla.dias * DIA_MS,
                    resultado = semilla.resultado,
                    estado = semilla.estado,
                    observaciones = semilla.observaciones,
                    mediciones = mediciones
                )
            )
        }
    }

    // ----------------------------- USUARIOS ----------------------------

    // NUEVO (Login): READ con DOS condiciones. Devuelve el usuario si el correo y la contraseña
    // coinciden, o null si no existe. COLLATE NOCASE: el correo no distingue mayúsculas.
    fun iniciarSesion(correo: String, contrasena: String): Usuario? =
        consultarUsuarios(
            "$SQL_SELECT_USUARIOS WHERE u.$USU_CORREO = ? COLLATE NOCASE AND u.$USU_CONTRASENA = ?",
            arrayOf(correo, contrasena)
        ).firstOrNull()

    // NUEVO (SQLite): READ de UNA fila por su clave primaria (o null si no existe).
    // La usa la sesión guardada: SharedPreferences solo recuerda el id.
    fun buscarUsuarioPorId(id: Long): Usuario? =
        consultarUsuarios("$SQL_SELECT_USUARIOS WHERE u.$USU_ID = ?", arrayOf(id.toString()))
            .firstOrNull()

    // NUEVO (Avatar): UPDATE de UNA columna. El ByteArray (JPEG) va a la columna BLOB.
    fun actualizarAvatar(idUsuario: Long, avatar: ByteArray): Int {
        val valores = ContentValues().apply { put(USU_AVATAR, avatar) }
        return writableDatabase.update(TABLA_USUARIOS, valores, "$USU_ID = ?", arrayOf(idUsuario.toString()))
    }

    // ----------------------------- EQUIPOS -----------------------------

    // NUEVO (Equipo): READ de todos los equipos con su instalación (para el desplegable)
    fun listarEquipos(): List<Equipo> {
        val equipos = mutableListOf<Equipo>()
        readableDatabase.rawQuery("$SQL_SELECT_EQUIPOS ORDER BY e.$EQU_CODIGO", null).use { cursor ->
            while (cursor.moveToNext()) {
                equipos.add(leerEquipo(cursor))
            }
        }
        return equipos
    }

    // NUEVO (Equipo): READ de UN equipo por id (para el encabezado del historial)
    fun buscarEquipoPorId(id: Long): Equipo? {
        readableDatabase.rawQuery("$SQL_SELECT_EQUIPOS WHERE e.$EQU_ID = ?", arrayOf(id.toString()))
            .use { cursor ->
                return if (cursor.moveToFirst()) leerEquipo(cursor) else null
            }
    }

    // --------------------------- PROTOCOLOS ----------------------------

    // NUEVO (Buscar): READ con filtros OPCIONALES. Parte de "WHERE 1 = 1" (siempre verdadero)
    // y agrega cada condición con AND solo si se pidió. El texto del SQL es fijo:
    // los valores viajan en los "?" (nunca concatenados) para evitar inyección SQL.
    //   idUsuario -> solo los protocolos de ese técnico (null = todos, para el Supervisor)
    //   texto     -> código de equipo que CONTIENE el texto (LIKE '%tr-0%')
    //   estado    -> solo ese estado (null = Todos)
    fun listarProtocolos(idUsuario: Long?, texto: String, estado: EstadoProtocolo?): List<Protocolo> {
        val sql = StringBuilder("$SQL_SELECT_PROTOCOLOS WHERE 1 = 1")
        val argumentos = mutableListOf<String>()
        if (idUsuario != null) {
            sql.append(" AND p.$PRO_USUARIO = ?")
            argumentos.add(idUsuario.toString())
        }
        if (texto.isNotEmpty()) {
            sql.append(" AND e.$EQU_CODIGO LIKE ?")
            argumentos.add("%$texto%")
        }
        if (estado != null) {
            sql.append(" AND p.$PRO_ESTADO = ?")
            argumentos.add(estado.name)
        }
        sql.append(" ORDER BY p.$PRO_FECHA DESC")
        return consultarProtocolos(sql.toString(), argumentos.toTypedArray())
    }

    // NUEVO (Detalle): READ de UN protocolo con sus mediciones (o null si no existe)
    fun obtenerProtocolo(id: Long): Protocolo? =
        consultarProtocolos("$SQL_SELECT_PROTOCOLOS WHERE p.$PRO_ID = ?", arrayOf(id.toString()))
            .firstOrNull()
            ?.let { it.copy(mediciones = listarMediciones(it.id)) }

    // NUEVO (Historial): todos los protocolos de un equipo, del más reciente al más antiguo,
    // cada uno con sus mediciones (para comparar un parámetro contra la prueba anterior)
    fun historialEquipo(idEquipo: Long): List<Protocolo> =
        consultarProtocolos(
            "$SQL_SELECT_PROTOCOLOS WHERE p.$PRO_EQUIPO = ? ORDER BY p.$PRO_FECHA DESC",
            arrayOf(idEquipo.toString())
        ).map { it.copy(mediciones = listarMediciones(it.id)) }

    // NUEVO (SQLite): CREATE -> INSERT del protocolo Y de todas sus mediciones.
    // Es una TRANSACCIÓN: o se guarda todo, o no se guarda nada (si algo falla a la mitad
    // no queda un protocolo sin mediciones). Devuelve el id generado (o -1 si falla).
    fun insertarProtocolo(protocolo: Protocolo): Long {
        val db = writableDatabase
        var id = -1L
        db.beginTransaction()
        try {
            id = guardarProtocolo(db, protocolo)
            if (id != -1L) db.setTransactionSuccessful()
        } finally {
            db.endTransaction()   // sin setTransactionSuccessful() esto deshace todo (rollback)
        }
        return id
    }

    // NUEVO (Formalizar): UPDATE de UNA columna. El Supervisor pasa el protocolo a FORMALIZADO.
    // Devuelve cuántas filas cambió (1 = OK, 0 = no existía).
    fun formalizarProtocolo(id: Long): Int {
        val valores = ContentValues().apply { put(PRO_ESTADO, EstadoProtocolo.FORMALIZADO.name) }
        return writableDatabase.update(TABLA_PROTOCOLOS, valores, "$PRO_ID = ?", arrayOf(id.toString()))
    }

    // NUEVO (Inicio): los 4 contadores en UNA consulta. SUM(CASE WHEN ...) cuenta solo
    // las filas que cumplen la condición. EXISTS pregunta "¿este protocolo tiene al menos una
    // medición fuera de rango?". idUsuario = null cuenta los de todos.
    fun resumen(idUsuario: Long?): ResumenProtocolos {
        val sql = StringBuilder(
            "SELECT COUNT(*), " +
                "COALESCE(SUM(CASE WHEN p.$PRO_ESTADO = ? THEN 1 ELSE 0 END), 0), " +
                "COALESCE(SUM(CASE WHEN p.$PRO_ESTADO = ? THEN 1 ELSE 0 END), 0), " +
                "COALESCE(SUM(CASE WHEN EXISTS (SELECT 1 FROM $TABLA_MEDICIONES m " +
                "WHERE m.$MED_PROTOCOLO = p.$PRO_ID " +
                "AND (m.$MED_VALOR < m.$MED_MINIMO OR m.$MED_VALOR > m.$MED_MAXIMO)) " +
                "THEN 1 ELSE 0 END), 0) " +
                "FROM $TABLA_PROTOCOLOS p WHERE 1 = 1"
        )
        val argumentos = mutableListOf(EstadoProtocolo.EN_TERRENO.name, EstadoProtocolo.FORMALIZADO.name)
        if (idUsuario != null) {
            sql.append(" AND p.$PRO_USUARIO = ?")
            argumentos.add(idUsuario.toString())
        }
        readableDatabase.rawQuery(sql.toString(), argumentos.toTypedArray()).use { cursor ->
            cursor.moveToFirst()
            return ResumenProtocolos(
                total = cursor.getInt(0),
                enTerreno = cursor.getInt(1),
                formalizados = cursor.getInt(2),
                fueraDeRango = cursor.getInt(3)
            )
        }
    }

    // ---------------------------- AYUDANTES ----------------------------

    // NUEVO (SQLite): inserta el protocolo y luego cada medición con el id que SQLite acaba de
    // generar. Recibe la conexión (db) para poder usarse dentro de onCreate y de la transacción.
    private fun guardarProtocolo(db: SQLiteDatabase, protocolo: Protocolo): Long {
        val valores = ContentValues().apply {
            put(PRO_EQUIPO, protocolo.equipoId)
            put(PRO_USUARIO, protocolo.usuarioId)
            put(PRO_FECHA, protocolo.fechaHora)
            put(PRO_ESTADO, protocolo.estado.name)
            put(PRO_RESULTADO, protocolo.resultado)
            put(PRO_OBSERVACIONES, protocolo.observaciones)
        }
        val idProtocolo = db.insert(TABLA_PROTOCOLOS, null, valores)
        if (idProtocolo == -1L) return -1L
        protocolo.mediciones.forEach { medicion ->
            val valoresMedicion = ContentValues().apply {
                put(MED_PROTOCOLO, idProtocolo)
                put(MED_PARAMETRO, medicion.parametro)
                put(MED_VALOR, medicion.valor)
                put(MED_UNIDAD, medicion.unidad)
                put(MED_MINIMO, medicion.minimo)
                put(MED_MAXIMO, medicion.maximo)
            }
            db.insert(TABLA_MEDICIONES, null, valoresMedicion)
        }
        return idProtocolo
    }

    // NUEVO (SQLite): READ de las mediciones de un protocolo, en el orden en que se guardaron
    private fun listarMediciones(idProtocolo: Long): List<Medicion> {
        val mediciones = mutableListOf<Medicion>()
        readableDatabase.rawQuery(
            "SELECT * FROM $TABLA_MEDICIONES WHERE $MED_PROTOCOLO = ? ORDER BY $MED_ID",
            arrayOf(idProtocolo.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                mediciones.add(
                    Medicion(
                        parametro = cursor.getString(cursor.getColumnIndexOrThrow(MED_PARAMETRO)),
                        valor = cursor.getDouble(cursor.getColumnIndexOrThrow(MED_VALOR)),
                        unidad = cursor.getString(cursor.getColumnIndexOrThrow(MED_UNIDAD)),
                        minimo = cursor.getDouble(cursor.getColumnIndexOrThrow(MED_MINIMO)),
                        maximo = cursor.getDouble(cursor.getColumnIndexOrThrow(MED_MAXIMO)),
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(MED_ID)),
                        protocoloId = cursor.getLong(cursor.getColumnIndexOrThrow(MED_PROTOCOLO))
                    )
                )
            }
        }
        return mediciones
    }

    // NUEVO (SQLite): ejecuta un SELECT de usuarios y recorre el Cursor fila por fila.
    private fun consultarUsuarios(sql: String, args: Array<String>?): List<Usuario> {
        val lista = mutableListOf<Usuario>()
        readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                lista.add(leerUsuario(cursor))
            }
        }
        return lista
    }

    // NUEVO (SQLite): ejecuta un SELECT de protocolos (listar, buscar, historial...).
    // El SQL cambia, la lectura es la misma.
    private fun consultarProtocolos(sql: String, args: Array<String>?): List<Protocolo> {
        val lista = mutableListOf<Protocolo>()
        readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                lista.add(leerProtocolo(cursor))
            }
        }
        return lista
    }

    // NUEVO (SQLite): convierte la fila actual del Cursor en un Usuario.
    // nombre_rol viene de la tabla roles gracias al JOIN. El BLOB puede ser NULL (sin foto).
    private fun leerUsuario(cursor: Cursor): Usuario {
        val columnaAvatar = cursor.getColumnIndexOrThrow(USU_AVATAR)
        return Usuario(
            nombre = cursor.getString(cursor.getColumnIndexOrThrow(USU_NOMBRE)),
            correo = cursor.getString(cursor.getColumnIndexOrThrow(USU_CORREO)),
            id = cursor.getLong(cursor.getColumnIndexOrThrow(USU_ID)),
            rolId = cursor.getLong(cursor.getColumnIndexOrThrow(USU_ROL)),
            rolNombre = cursor.getString(cursor.getColumnIndexOrThrow(ROL_NOMBRE)),
            contrasena = cursor.getString(cursor.getColumnIndexOrThrow(USU_CONTRASENA)),
            avatar = if (cursor.isNull(columnaAvatar)) null else cursor.getBlob(columnaAvatar)
        )
    }

    // NUEVO (SQLite): convierte la fila actual en un Equipo con su Instalacion (por el JOIN).
    // valueOf convierte el texto guardado ("RELE") otra vez en el enum TipoEquipo.RELE.
    private fun leerEquipo(cursor: Cursor) = Equipo(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(EQU_ID)),
        codigo = cursor.getString(cursor.getColumnIndexOrThrow(EQU_CODIGO)),
        tipo = TipoEquipo.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(EQU_TIPO))),
        marca = cursor.getString(cursor.getColumnIndexOrThrow(EQU_MARCA)),
        instalacion = Instalacion(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(EQU_INSTALACION)),
            cliente = cursor.getString(cursor.getColumnIndexOrThrow(INS_CLIENTE)),
            planta = cursor.getString(cursor.getColumnIndexOrThrow(INS_PLANTA)),
            ciudad = cursor.getString(cursor.getColumnIndexOrThrow(INS_CIUDAD))
        )
    )

    // NUEVO (SQLite): convierte la fila actual en un Protocolo (sin mediciones: se piden aparte).
    // Los datos del equipo, la planta y el técnico vienen de los JOIN.
    private fun leerProtocolo(cursor: Cursor) = Protocolo(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(PRO_ID)),
        equipoId = cursor.getLong(cursor.getColumnIndexOrThrow(PRO_EQUIPO)),
        usuarioId = cursor.getLong(cursor.getColumnIndexOrThrow(PRO_USUARIO)),
        fechaHora = cursor.getLong(cursor.getColumnIndexOrThrow(PRO_FECHA)),
        estado = EstadoProtocolo.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(PRO_ESTADO))),
        resultado = cursor.getString(cursor.getColumnIndexOrThrow(PRO_RESULTADO)),
        observaciones = cursor.getString(cursor.getColumnIndexOrThrow(PRO_OBSERVACIONES)),
        equipoCodigo = cursor.getString(cursor.getColumnIndexOrThrow(EQU_CODIGO)),
        equipoTipo = TipoEquipo.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(EQU_TIPO))),
        planta = cursor.getString(cursor.getColumnIndexOrThrow(INS_PLANTA)),
        cliente = cursor.getString(cursor.getColumnIndexOrThrow(INS_CLIENTE)),
        tecnicoNombre = cursor.getString(cursor.getColumnIndexOrThrow(ALIAS_TECNICO)),
        medicionesFueraDeRango = cursor.getInt(cursor.getColumnIndexOrThrow(ALIAS_FUERA_RANGO))
    )
}

// NUEVO (SQLite): datos de una fila precargada. Solo existe para armar la semilla de arriba,
// por eso es private y vive en este mismo archivo.
private data class SemillaProtocolo(
    val equipoId: Long,
    val tipo: TipoEquipo,
    val dias: Int,
    val usuarioId: Long,
    val valores: List<Double>,
    val resultado: String = Protocolo.RESULTADO_APROBADO,
    val observaciones: String = "Prueba sin novedades",
    val estado: EstadoProtocolo = EstadoProtocolo.FORMALIZADO
)
