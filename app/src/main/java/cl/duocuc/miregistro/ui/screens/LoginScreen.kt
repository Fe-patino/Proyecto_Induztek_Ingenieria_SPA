package cl.duocuc.miregistro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duocuc.miregistro.R
import cl.duocuc.miregistro.model.LoginUiState
import cl.duocuc.miregistro.ui.components.BotonPrincipal
import cl.duocuc.miregistro.ui.components.CampoContrasena
import cl.duocuc.miregistro.ui.components.CampoTexto
import cl.duocuc.miregistro.ui.components.TarjetaSeccion
import cl.duocuc.miregistro.ui.theme.Dimens
import cl.duocuc.miregistro.ui.theme.ProtoFieldTheme
import cl.duocuc.miregistro.viewmodel.LoginViewModel

// =====================================================================
//  NUEVO (Login) — LoginScreen.kt
//  ---------------------------------------------------------------------
//  Primera pantalla si no hay sesión guardada. Se divide en dos:
//   - LoginScreen  : conecta con el LoginViewModel (formato del formulario)
//   - LoginContent : solo dibuja; recibe datos y lambdas, por eso tiene @Preview
//  Si el formato es válido, LoginScreen pregunta a la BD con onIniciarSesion
//  (viene de AppNavegacion). Si responde false, se muestra el error.
// =====================================================================

@Composable
fun LoginScreen(
    onIniciarSesion: (correo: String, contrasena: String) -> Boolean,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginContent(
        uiState = uiState,
        onCorreoCambia = viewModel::onCorreoCambia,
        onContrasenaCambia = viewModel::onContrasenaCambia,
        onAlternarVisibilidad = viewModel::onAlternarVisibilidad,
        onIngresar = {
            // NUEVO (Login): primero el formato; solo si es válido se consulta la BD
            viewModel.onIngresar()?.let { credenciales ->
                if (!onIniciarSesion(credenciales.correo, credenciales.contrasena)) {
                    viewModel.onCredencialesInvalidas()
                }
            }
        }
    )
}

@Composable
fun LoginContent(
    uiState: LoginUiState,
    onCorreoCambia: (String) -> Unit,
    onContrasenaCambia: (String) -> Unit,
    onAlternarVisibilidad: () -> Unit,
    onIngresar: () -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            EncabezadoLogin()

            Column(
                modifier = Modifier.padding(Dimens.espacioPantalla),
                verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
            ) {
                TarjetaSeccion(titulo = stringResource(R.string.login_seccion)) {
                    CampoTexto(
                        valor = uiState.correo,
                        onValorCambia = onCorreoCambia,
                        etiqueta = stringResource(R.string.campo_correo),
                        icono = R.drawable.ic_correo,
                        error = uiState.errorCorreo?.let { stringResource(it) },
                        tipoTeclado = KeyboardType.Email
                    )
                    CampoContrasena(
                        valor = uiState.contrasena,
                        onValorCambia = onContrasenaCambia,
                        etiqueta = stringResource(R.string.campo_contrasena),
                        visible = uiState.contrasenaVisible,
                        onAlternarVisible = onAlternarVisibilidad,
                        error = uiState.errorContrasena?.let { stringResource(it) }
                    )

                    // NUEVO (Login): error que no pertenece a un solo campo
                    uiState.errorCredenciales?.let { mensaje ->
                        Text(
                            text = stringResource(mensaje),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                BotonPrincipal(
                    texto = stringResource(R.string.btn_ingresar),
                    onClick = onIngresar,
                    icono = R.drawable.ic_check,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(Dimens.espacioGrande))
            }
        }
    }
}

// NUEVO (Login): encabezado verde con el nombre de la app (colores del tema, sin imagen)
@Composable
private fun EncabezadoLogin() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.altoBanner)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = stringResource(R.string.login_subtitulo),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = Dimens.espacioMini)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginVacioPreview() {
    ProtoFieldTheme {
        LoginContent(
            uiState = LoginUiState(),
            onCorreoCambia = {}, onContrasenaCambia = {},
            onAlternarVisibilidad = {}, onIngresar = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Con errores")
@Composable
private fun LoginConErroresPreview() {
    ProtoFieldTheme {
        LoginContent(
            uiState = LoginUiState(
                correo = "mateo.rivas@induztek.cl",
                contrasena = "clave-mala",
                errorCredenciales = R.string.error_credenciales
            ),
            onCorreoCambia = {}, onContrasenaCambia = {},
            onAlternarVisibilidad = {}, onIngresar = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Modo oscuro")
@Composable
private fun LoginOscuroPreview() {
    ProtoFieldTheme(darkTheme = true) {
        LoginContent(
            uiState = LoginUiState(),
            onCorreoCambia = {}, onContrasenaCambia = {},
            onAlternarVisibilidad = {}, onIngresar = {}
        )
    }
}
