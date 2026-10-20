# Configuración de Modos de Operación

Este repositorio opera con dos modalidades de trabajo según las indicaciones del usuario. El asistente DEBE respetar estrictamente el modo activo en todo momento.

---

## 1. Detección y Cambio de Modo

El usuario puede alternar entre modos en cualquier momento o al inicio de cada conversación:

- **Modo Absoluto**:
  - **Comandos / Frases de activación**: `modo absoluto`, `permiso absoluto`, `absoluto`, `modo autónomo`, `modo sin preguntas`.
  - **Comportamiento por defecto**: Cuando el usuario pida editar, refactorizar, corregir o implementar código, se opera en **Modo Absoluto** salvo que el usuario indique explícitamente lo contrario.

- **Modo Normal**:
  - **Comandos / Frases de activación**: `modo normal`, `normal`, `modo interactivo`, `modo consultivo`, `pregúntame`.

---

## 2. Reglas del Modo Absoluto (Permiso Absoluto)

1. **Máxima Autonomía y Proactividad**:
   - NO hacer preguntas redundantes ni pedir confirmación para tareas estándar (ej. "¿puedo editar este archivo?", "¿quieres que compile?", "¿procedo con el siguiente cambio?").
   - Realizar de forma integral todo el trabajo requerido: análisis de código, edición, creación de archivos, refactorizaciones, ejecución de comandos/pruebas si corresponde y verificación final.
   - Tomar decisiones técnicas informadas basándote en las convenciones existentes del proyecto (Spring Boot, arquitectura en capas/módulos, estándares Java, etc.).

2. **Filtro Crítico (Únicas Excepciones para Preguntar)**:
   - Solo debes detenerte a consultar al usuario si se presenta una situación de riesgo crítico real:
     - **Pérdida irreversible de datos**: Comandos o scripts destructivos (`DROP TABLE`, `TRUNCATE`, `DELETE` masivos sin condición en bases de datos persistidas).
     - **Destrucción de código/archivos no recuperable**: Eliminación de módulos enteros, git hard resets destructivos o borrado masivo de fuentes sin respaldo.
     - **Decisiones arquitectónicas o de negocio divergentes**: Casos donde existan requerimientos radicalmente ambiguos y contradictorios que no se puedan deducir del código y cuya elección errónea rompa el sistema.
     - **Seguridad y credenciales**: Exposición o manipulación de contraseñas, secretos o llaves de producción.

---

## 3. Reglas del Modo Normal (Interactivo / Consultivo)

1. **Colaboración y Validación Paso a Paso**:
   - Preguntar antes de realizar cambios estructurales o modificaciones de gran escala.
   - Presentar opciones o alternativas técnicas para que el usuario elija.
   - Solicitar clarificación y confirmación de diseño cuando haya varias formas viables de implementar una funcionalidad.
   - Proceder paso a paso con retroalimentación continua del usuario.

---

## 4. Indicador de Estado

- Al recibir un cambio de modo o al iniciar una tarea, indicar brevemente el estado activo mediante una etiqueta concisa (ejemplo: `[Modo Absoluto]` o `[Modo Normal]`) para que el usuario siempre tenga visibilidad del estado actual.
