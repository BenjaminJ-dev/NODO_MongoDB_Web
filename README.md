# NODO — Inventario web conectado a MongoDB

Esta versión mantiene la interfaz web del proyecto y reemplaza la conexión Oracle/PLSQL por el driver oficial de MongoDB para Java. La aplicación consulta y modifica la base `nodo_inventario` en las colecciones `productos`, `clientes`, `pedidos` y `auditoriaStock`.

## Requisitos

- MongoDB Community Server instalado y ejecutándose en el equipo. MongoDB Compass sirve para revisar la base, pero la aplicación necesita que el servidor esté iniciado.
- JDK 17 y Maven.
- Tener los documentos de ejemplo cargados en `nodo_inventario` (se pueden usar los scripts incluidos en la carpeta `mongo`).

## Preparar la base existente

1. Abre MongoDB Compass y conecta con `mongodb://127.0.0.1:27017` (la conexión suele aparecer como `Local`).
2. Confirma que exista la base `nodo_inventario` y que tenga productos y clientes.
3. En Compass abre MongoDB Shell, selecciona/usa la base `nodo_inventario` y ejecuta `mongo/03_asociar_ids_tienda.mongodb.js`. Este paso agrega identificadores numéricos compatibles con la tienda y no reemplaza productos ni restablece el stock.

Si aún no habías cargado documentos, ejecuta una vez `mongo/01_preparar_datos.mongodb.js` y después `mongo/03_asociar_ids_tienda.mongodb.js`. El script 01 vuelve a cargar los valores iniciales de los productos con los mismos `_id`; no lo ejecutes si necesitas preservar cambios de stock o de mínimo ya realizados.

## Iniciar la aplicación

Desde esta carpeta, en PowerShell o una terminal:

```powershell
mvn compile exec:java
```

Cuando aparezca `MongoDB conectado: nodo_inventario`, abre [http://localhost:8080](http://localhost:8080). Es importante entrar por esa dirección; abrir `index.html` directamente usa el modo de demostración local del navegador.

Por defecto, Java conecta a `mongodb://127.0.0.1:27017` y a `nodo_inventario`. Se pueden cambiar sin editar el código:

```powershell
$env:MONGODB_URI="mongodb://127.0.0.1:27017"
$env:MONGODB_DATABASE="nodo_inventario"
mvn compile exec:java
```

## Qué ocurre al registrar un pedido

Java busca al cliente y al producto en MongoDB, descuenta el stock solo si hay unidades suficientes, guarda una copia del cliente y del producto comprado dentro del documento del pedido, y registra el movimiento en `auditoriaStock`. La interfaz actualiza el catálogo al terminar.

El descuento utiliza una actualización condicionada por el stock disponible para impedir que una solicitud normal deje stock negativo. La actualización del producto y la inserción del pedido son operaciones separadas en esta configuración local; si el proyecto se despliega en producción, conviene usar transacciones con un replica set para hacer ambas operaciones atómicas.

## Colecciones relacionadas

- `productos`: catálogo, precio, stock y referencias descriptivas de categoría/proveedor.
- `clientes`: datos base de clientes.
- `pedidos`: cliente y detalle de compra guardados como snapshot embebido.
- `auditoriaStock`: historial de movimientos de inventario.

La carpeta `sql` y el archivo de instalación Oracle corresponden a la versión relacional original; no se ejecutan para iniciar esta versión MongoDB.
