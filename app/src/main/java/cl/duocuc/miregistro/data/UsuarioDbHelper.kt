package cl.duocuc.miregistro.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cl.duocuc.miregistro.model.Rol
import cl.duocuc.miregistro.model.Usuario

// =====================================================================
//  NUEVO (SQLite) — UsuarioDbHelper.kt
//  ---------------------------------------------------------------------
//  UNA base de datos (miregistro.db) con DOS tablas relacionadas:
//
//   roles                         usuarios
//   ┌────────┬───────────────┐    ┌────────────┬────────┬────────┬────────┬────────┐
//   │ id_rol │ nombre_rol    │    │ id_usuario │ nombre │ correo │ ciudad │ id_rol │
//   ├────────┼───────────────┤    ├────────────┼────────┼────────┼────────┼────────┤
//   │ 1      │ Administrador │◀───│ 1          │ Camila │ ...    │ ...    │ 1      │
//   │ 2      │ Docente       │    │ 2          │ Diego  │ ...    │ ...    │ 2      │
//   │ 3      │ Estudiante    │    │ 3          │ Vale   │ ...    │ ...    │ 3      │
//   └────────┴───────────────┘    └────────────┴────────┴────────┴────────┴────────┘
//        clave PRIMARIA  ◀───────────────── clave FORÁNEA (usuarios.id_rol)
//
//  Regla: UN helper por base de datos. Todas las tablas se crean en el MISMO onCreate().
//  Las columnas tienen nombres distintos en cada tabla (id_rol / id_usuario,
//  nombre_rol / nombre) para que el JOIN no tenga columnas repetidas.
// =====================================================================

class UsuarioDbHelper(context: Context) :
    SQLiteOpenHelper(context, NOMBRE_BD, null, VERSION_BD) {

    companion object {
        // NUEVO (SQLite): estos dos valores viajan al constructor del padre (línea de arriba)
        private const val NOMBRE_BD = "miregistro.db"
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
        private const val USU_CIUDAD = "ciudad"
        private const val USU_ROL = "id_rol"

        // NUEVO (SQLite): las constantes se "pegan" dentro del texto SQL con $.
        // SQL_CREAR_ROLES termina siendo:
        // CREATE TABLE roles (id_rol INTEGER PRIMARY KEY AUTOINCREMENT, nombre_rol TEXT NOT NULL UNIQUE)
        private const val SQL_CREAR_ROLES =
            "CREATE TABLE $TABLA_ROLES (" +
                "$ROL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$ROL_NOMBRE TEXT NOT NULL UNIQUE)"

        // NUEVO (SQLite): REFERENCES crea la CLAVE FORÁNEA hacia roles(id_rol)
        private const val SQL_CREAR_USUARIOS =
            "CREATE TABLE $TABLA_USUARIOS (" +
                "$USU_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$USU_NOMBRE TEXT NOT NULL, " +
                "$USU_CORREO TEXT NOT NULL, " +
                "$USU_CIUDAD TEXT NOT NULL, " +
                "$USU_ROL INTEGER NOT NULL REFERENCES $TABLA_ROLES($ROL_ID))"

        // NUEVO (SQLite): datos precargados. Los roles llevan id explícito
        // para que Rol.ID_ESTUDIANTE (3) siempre apunte a "Estudiante".
        private const val SQL_PRECARGAR_ROLES =
            "INSERT INTO $TABLA_ROLES ($ROL_ID, $ROL_NOMBRE) VALUES " +
                "(1, 'Administrador'), (2, 'Docente'), (3, 'Estudiante')"

        private const val SQL_PRECARGAR_USUARIOS =
            "INSERT INTO $TABLA_USUARIOS ($USU_NOMBRE, $USU_CORREO, $USU_CIUDAD, $USU_ROL) VALUES " +
                "('Camila Rojas', 'camila@duocuc.cl', 'Santiago', 1), " +
                "('Diego Muñoz', 'diego@duocuc.cl', 'Valparaíso', 2), " +
                "('Valentina Soto', 'valentina@duocuc.cl', 'Concepción', 3), " +
                "('Matías Pérez', 'matias@duocuc.cl', 'Temuco', 3), " +
                "('Fernanda Díaz', 'fernanda@duocuc.cl', 'Santiago', 3)"

        // NUEVO (SQLite): SELECT base con INNER JOIN. Une cada usuario con SU rol:
        // "trae las columnas de u (usuarios) + el nombre_rol de r (roles)
        //  donde el id_rol del usuario sea igual al id_rol del rol".
        private const val SQL_SELECT_USUARIOS =
            "SELECT u.*, r.$ROL_NOMBRE AS $ROL_NOMBRE " +
                "FROM $TABLA_USUARIOS u " +
                "INNER JOIN $TABLA_ROLES r ON u.$USU_ROL = r.$ROL_ID"
    }

    // NUEVO (SQLite): se ejecuta CADA VEZ que se abre la conexión, antes de onCreate.
    // SQLite trae las claves foráneas APAGADAS por defecto: aquí se encienden.
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // NUEVO (SQLite): una sola vez, cuando miregistro.db no existe.
    // Orden: primero la tabla "padre" (roles), después la que la referencia (usuarios).
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREAR_ROLES)
        db.execSQL(SQL_CREAR_USUARIOS)
        db.execSQL(SQL_PRECARGAR_ROLES)
        db.execSQL(SQL_PRECARGAR_USUARIOS)
    }

    // NUEVO (SQLite): si sube VERSION_BD. Versión simple: borra y recrea (se pierden datos).
    // Orden inverso: primero la tabla "hija" (usuarios) y después la "padre" (roles).
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLA_USUARIOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLA_ROLES")
        onCreate(db)
    }

    // ----------------------------- ROLES -----------------------------

    // NUEVO (Rol): READ de la tabla roles (para la lista desplegable de Editar)
    fun listarRoles(): List<Rol> {
        val roles = mutableListOf<Rol>()
        readableDatabase.rawQuery(
            "SELECT $ROL_ID, $ROL_NOMBRE FROM $TABLA_ROLES ORDER BY $ROL_ID", null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                roles.add(
                    Rol(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(ROL_ID)),
                        nombre = cursor.getString(cursor.getColumnIndexOrThrow(ROL_NOMBRE))
                    )
                )
            }
        }
        return roles
    }

    // ---------------------------- USUARIOS ---------------------------

    // NUEVO (SQLite): CREATE -> INSERT. Devuelve el id generado (o -1 si falla).
    fun insertarUsuario(usuario: Usuario): Long =
        writableDatabase.insert(TABLA_USUARIOS, null, valoresDe(usuario))

    // NUEVO (SQLite): READ -> todos los usuarios con su rol, ordenados por nombre
    fun listarUsuarios(): List<Usuario> =
        consultarUsuarios("$SQL_SELECT_USUARIOS ORDER BY u.$USU_NOMBRE", null)

    // NUEVO (Buscar): READ con filtro. LIKE '%ana%' = "contiene ana" en cualquier posición.
    // El texto viaja en el "?" (nunca concatenado) para evitar inyección SQL.
    fun buscarUsuarios(texto: String): List<Usuario> {
        val patron = "%$texto%"
        return consultarUsuarios(
            "$SQL_SELECT_USUARIOS WHERE u.$USU_NOMBRE LIKE ? OR u.$USU_CORREO LIKE ? " +
                "ORDER BY u.$USU_NOMBRE",
            arrayOf(patron, patron)
        )
    }

    // NUEVO (SQLite): READ de UNA fila por su clave primaria (o null si no existe)
    fun buscarPorId(id: Long): Usuario? =
        consultarUsuarios("$SQL_SELECT_USUARIOS WHERE u.$USU_ID = ?", arrayOf(id.toString()))
            .firstOrNull()

    // NUEVO (SQLite): UPDATE. Cambia SOLO la fila cuyo id_usuario coincide (incluye el rol).
    fun actualizarUsuario(usuario: Usuario): Int =
        writableDatabase.update(
            TABLA_USUARIOS, valoresDe(usuario), "$USU_ID = ?", arrayOf(usuario.id.toString())
        )

    // NUEVO (Eliminar): DELETE. Devuelve cuántas filas borró (1 = OK, 0 = no existía).
    // ¡Sin el WHERE se borrarían TODOS los usuarios!
    fun eliminarUsuario(id: Long): Int =
        writableDatabase.delete(TABLA_USUARIOS, "$USU_ID = ?", arrayOf(id.toString()))

    // ---------------------------- AYUDANTES --------------------------

    // NUEVO (SQLite): ContentValues = mapa columna -> valor (para INSERT y UPDATE)
    private fun valoresDe(usuario: Usuario) = ContentValues().apply {
        put(USU_NOMBRE, usuario.nombre)
        put(USU_CORREO, usuario.correo)
        put(USU_CIUDAD, usuario.ciudad)
        put(USU_ROL, usuario.rolId)
    }

    // NUEVO (SQLite): ejecuta un SELECT y recorre el Cursor fila por fila.
    // La usan listar, buscar y buscarPorId: el SQL cambia, la lectura es la misma.
    private fun consultarUsuarios(sql: String, args: Array<String>?): List<Usuario> {
        val lista = mutableListOf<Usuario>()
        readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                lista.add(leerUsuario(cursor))
            }
        }
        return lista
    }

    // NUEVO (SQLite): convierte la fila actual del Cursor en un Usuario.
    // nombre_rol viene de la tabla roles gracias al JOIN.
    private fun leerUsuario(cursor: Cursor) = Usuario(
        nombre = cursor.getString(cursor.getColumnIndexOrThrow(USU_NOMBRE)),
        correo = cursor.getString(cursor.getColumnIndexOrThrow(USU_CORREO)),
        ciudad = cursor.getString(cursor.getColumnIndexOrThrow(USU_CIUDAD)),
        id = cursor.getLong(cursor.getColumnIndexOrThrow(USU_ID)),
        rolId = cursor.getLong(cursor.getColumnIndexOrThrow(USU_ROL)),
        rolNombre = cursor.getString(cursor.getColumnIndexOrThrow(ROL_NOMBRE))
    )
}
