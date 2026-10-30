💳 Servidor de Pagos ISO 8583 
Servidor de transacciones financieras de alto rendimiento basado en el estándar ISO 8583,
desarrollado en Java (Spring Boot) con arquitectura concurrente de hilos virtuales,
API REST web, persistencia robusta en PostgreSQL, patrones de resiliencia avanzados y monitoreo en tiempo real con React.

🏗️ 1. Arquitectura del Sistema
El sistema esta diseñado con una arquitectura modular y desacoplada para garantizar baja latencia, alta disponibilidad y cumplimiento estricto de seguridad 
*Capa TCP (Puerto 8583): Servidor TCP dedicado a la escucha de tramas ISO 8583 provenientes de terminales POS o simuladores. 
Utiliza Jva Virtual THREADS(Project Loom) para manejar miles de conexiones concurrentes de forma eficiente sin bloquear recursos del sistema

*Capa REST (Puerto 8080): API web construida con spring MVC para cunsultas y transacciones HTTP alternativas, con soporte CORS para integracion con el panel fronted 

*Capa de Resiliencia y Control:

*Rate Limiter: Control de tráfico multidimensional por terminal, comercio y nivel global para evitar saturación de la pasarela.

*Circuit Breaker: Protección contra caídas del sistema autorizador externo, abriendo el circuito ante fallos consecutivos.

*Watchdog: Monitor de salud encargado de detectar bloqueos de hilos (deadlocks) y medir latencias P99 en tiempo real.

*Capa de Datos: PostgreSQL con manejo de índices únicos para garantizar la idempotencia concurrente con TTL (Time To Live).

⚙️ 2. Decisiones de Diseño y Alternativas Descartadas

1. Uso de Java Virtual Threads (Virtual Threads): Se prefirio el modelos de los hilos virtuales de java +21 en lugar del clasico modelo de hilos por conexion (Threads pool tradicional).
Esto permite escalar masivamente el servidor TCP bajo alta concurrencia de datafonos sin agotar la memoria de la maquina virtual 

2. Idempotencia mediante Hash SHA-256 + Unique Constraint:para evitar cargos duplicados ante reintentos de red, se genera una clave unica basado en campos de clave(DE41 + DE11 + DE37 + DE4 + DE7).
aplicando una restriccion UNIQUE directamente en PostgreSQL, asegurando que condiciones de carrera bajo concurrencia extrema sean bloqueadas a nivel de base de datos 

3.Enmascaramiento Preventivo (PCI-DSS): el numero  de tarjeta (PAN- DE 2)
se enmascara de forma mandatoria antes de cualquier registro en consola, archviso de registro (logs) o base de datos temporal, evitando fugas de informacion sensible

3. Documento Breve de Operación (Guía de Alarmas y Monitoreo)
   Este manual describe el significado de las alarmas emitidas por el componente Watchdog del sistema y establece el procedimiento operativo recomendado ante cada eventualidad:

Alarma de Bloqueo (ALARM_DEADLOCK_DETECTED):

Significado operativo: Indica que se ha detectado un bloqueo de hilos o un proceso que superó el tiempo límite de espera sin liberar recursos del sistema.

Qué hacer: Revisar los registros de la aplicación filtrando por trazas de error, verificar el estado de las conexiones en la base de datos PostgreSQL y, si el bloqueo persiste, realizar un reinicio controlado del servicio de Spring Boot.

Alarma de Apertura de Circuito (CIRCUIT_BREAKER_OPEN - Código DE 39: 91):

Significado operativo: El mecanismo de resiliencia Circuit Breaker se ha abierto de manera automática debido a que la tasa de fallos con el emisor o autorizador superó el umbral configurado (mayor al 50%).

Qué hacer: Comprobar inmediatamente la conectividad de red con el autorizador externo y monitorear sus tiempos de respuesta. El sistema intentará recuperarse de forma automática pasando a un estado de prueba (Half-Open) tras el tiempo de espera preestablecido (30 segundos).

Alarma de Exceso de Tráfico (RATE_LIMIT_EXCEEDED - Código DE 39: 92):

Significado operativo: Un terminal específico (identificado por el campo DE 41) o un comercio ha superado el número máximo de peticiones por segundo permitidas por las políticas de la pasarela.

Qué hacer: Investigar si se trata de un incremento legítimo de operaciones por un pico comercial o si corresponde a un comportamiento anómalo. En caso de ser un terminal autorizado de alto volumen, ajustar los umbrales correspondientes en el archivo de configuración.

Alarma de Incumplimiento de SLA de Latencia (LATENCY_SLA_BREACH):

Significado operativo: El percentil 99 (P99) de la latencia de respuesta del servidor ha superado el límite aceptable establecido en las métricas de rendimiento (SLA mayor a 300 milisegundos).

Qué hacer: Inspeccionar el rendimiento del motor de base de datos en busca de consultas lentas o bloqueos de concurrencia, y analizar el uso general de CPU y memoria en el servidor host.

4. Guía de Ejecución Rápida
   Base de Datos: Asegúrate de tener PostgreSQL corriendo y la base de datos creada.

Ejecutar el Servidor (Spring Boot):

Bash
./gradlew bootRun

Ejecutar el Simulador de POS (TCP):
Ejecuta la clase IsoClientSimulator.java desde tu IDE para enviar una transacción de prueba.

Panel Frontend (React):
Entra a la carpeta del frontend, arranca el servidor local y visualiza las transacciones en tiempo real:

Bash
npm run dev