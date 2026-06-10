# AppTareas — TaskFlow: Gestion de tareas con ubicacion

## Integrantes
- Valentin Ruiz
- Brenda Cruz

## Descripcion
Aplicacion Android nativa para gestionar tareas personales. 
Permite crear, editar y organizar tareas con prioridad, categoria, 
ubicacion en el mapa, foto adjunta y recordatorios locales.

## Tecnologias
- Kotlin + XML (modelo tradicional)
- Firebase Authentication
- Firebase Firestore
- Firebase Storage
- OpenStreetMap (OSMDroid)
- AlarmManager (notificaciones locales)

## Requisitos funcionales implementados
- RF1: Autenticacion con email/contrasena (Firebase Auth)
- RF2: Listado de tareas por usuario desde Firestore
- RF3: Detalle de tarea con foto y ubicacion
- RF4: Crear y editar tareas con persistencia en Firestore
- RF5: Mapa con ubicacion de tareas (OSMDroid + Nominatim)
- RF6: Camara para adjuntar foto a la tarea (Firebase Storage)
- RF7: Notificaciones locales 30 minutos antes del vencimiento

## Como configurar y ejecutar el proyecto

### 1. Clonar el repositorio
git clone https://github.com/Brendac87/AppTareas

### 2. Configurar Firebase
Este proyecto requiere un archivo google-services.json que 
no esta incluido en el repositorio por seguridad.

Para obtenerlo:
1. Ir a https://console.firebase.google.com
2. Crear un proyecto o usar uno existente
3. Agregar una app Android con el package: com.example.apptareas
4. Descargar el archivo google-services.json
5. Colocarlo en la carpeta /app del proyecto

### 3. Habilitar servicios en Firebase
En la consola de Firebase habilitar:
- Authentication luego Email/contraseña
- Firestore Database
- Storage

### 4. Reglas de Firestore


### 5. Ejecutar
Abrir el proyecto en Android Studio y ejecutar en un 
emulador con API 26 o superior, o en un dispositivo fisico.

## Notas
- Las notificaciones locales requieren otorgar permisos 
  manualmente en Android 13+
- Las imagenes se almacenan en Firebase Storage bajo 
  la carpeta fotos_tareas/