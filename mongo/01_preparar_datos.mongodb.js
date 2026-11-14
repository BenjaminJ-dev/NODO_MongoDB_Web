// NODO - Inventario MongoDB
// Ejecutar en MongoDB Compass > Open MongoDB Shell, conectado a localhost.
// Script idempotente: inserta o actualiza datos de ejemplo usando identificadores estables.
// No elimina colecciones ni datos existentes.

use('nodo_inventario');

const categorias = [
  { _id: 'cat-procesadores', nombre: 'Procesadores', descripcion: 'CPUs para equipos gaming' },
  { _id: 'cat-video', nombre: 'Tarjetas de Video', descripcion: 'GPUs dedicadas' },
  { _id: 'cat-ram', nombre: 'Memorias RAM', descripcion: 'Memorias DDR4 y DDR5' },
  { _id: 'cat-almacenamiento', nombre: 'Almacenamiento', descripcion: 'SSD y HDD' },
  { _id: 'cat-accesorios', nombre: 'Accesorios RGB', descripcion: 'Categoría sin productos al inicio' }
];

const proveedores = [
  { _id: 'prov-techdist', nombre: 'TechDist SpA', contacto: 'Pablo Iturra', telefono: '+56912345601' },
  { _id: 'prov-gamerparts', nombre: 'GamerParts Ltda', contacto: 'Fernanda Munoz', telefono: '+56912345602' },
  { _id: 'prov-importpc', nombre: 'ImportPC S.A.', contacto: 'Rodrigo Vega', telefono: '+56912345603' }
];

const clientes = [
  { _id: 'cli-juan', nombre: 'Juan Perez', email: 'juan.perez@mail.com', telefono: '+56911111111', fechaRegistro: ISODate('2026-11-01T12:00:00Z') },
  { _id: 'cli-maria', nombre: 'Maria Soto', email: 'maria.soto@mail.com', telefono: '+56922222222', fechaRegistro: ISODate('2026-11-02T12:00:00Z') },
  { _id: 'cli-diego', nombre: 'Diego Rojas', email: 'diego.rojas@mail.com', telefono: '+56933333333', fechaRegistro: ISODate('2026-11-03T12:00:00Z') },
  { _id: 'cli-camila', nombre: 'Camila Vidal', email: 'camila.vidal@mail.com', telefono: '+56944444444', fechaRegistro: ISODate('2026-11-04T12:00:00Z') },
  { _id: 'cli-sofia', nombre: 'Sofia Torres', email: 'sofia.torres@mail.com', telefono: '+56955555555', fechaRegistro: ISODate('2026-11-05T12:00:00Z') }
];

const productos = [
  { _id: 'prod-ryzen7800', sku: 'CPU-001', nombre: 'AMD Ryzen 7 7800X3D', categoria: { id: 'cat-procesadores', nombre: 'Procesadores' }, proveedor: { id: 'prov-techdist', nombre: 'TechDist SpA' }, precio: 349990, stockActual: 12, stockMinimo: 5 },
  { _id: 'prod-i514600', sku: 'CPU-002', nombre: 'Intel Core i5-14600K', categoria: { id: 'cat-procesadores', nombre: 'Procesadores' }, proveedor: { id: 'prov-importpc', nombre: 'ImportPC S.A.' }, precio: 289990, stockActual: 3, stockMinimo: 5 },
  { _id: 'prod-rtx4070', sku: 'GPU-001', nombre: 'NVIDIA RTX 4070 Super', categoria: { id: 'cat-video', nombre: 'Tarjetas de Video' }, proveedor: { id: 'prov-gamerparts', nombre: 'GamerParts Ltda' }, precio: 699990, stockActual: 2, stockMinimo: 4 },
  { _id: 'prod-rtx4060', sku: 'GPU-002', nombre: 'NVIDIA RTX 4060', categoria: { id: 'cat-video', nombre: 'Tarjetas de Video' }, proveedor: { id: 'prov-gamerparts', nombre: 'GamerParts Ltda' }, precio: 429990, stockActual: 15, stockMinimo: 5 },
  { _id: 'prod-rx7800', sku: 'GPU-003', nombre: 'AMD Radeon RX 7800 XT', categoria: { id: 'cat-video', nombre: 'Tarjetas de Video' }, proveedor: { id: 'prov-techdist', nombre: 'TechDist SpA' }, precio: 549990, stockActual: 4, stockMinimo: 3 },
  { _id: 'prod-kingston32', sku: 'RAM-001', nombre: 'Kingston Fury Beast 32GB', categoria: { id: 'cat-ram', nombre: 'Memorias RAM' }, proveedor: { id: 'prov-techdist', nombre: 'TechDist SpA' }, precio: 59990, stockActual: 25, stockMinimo: 10 },
  { _id: 'prod-corsair16', sku: 'RAM-002', nombre: 'Corsair Vengeance 16GB', categoria: { id: 'cat-ram', nombre: 'Memorias RAM' }, proveedor: { id: 'prov-importpc', nombre: 'ImportPC S.A.' }, precio: 34990, stockActual: 4, stockMinimo: 10 },
  { _id: 'prod-samsung990', sku: 'SSD-001', nombre: 'Samsung 990 Pro 2TB SSD', categoria: { id: 'cat-almacenamiento', nombre: 'Almacenamiento' }, proveedor: { id: 'prov-techdist', nombre: 'TechDist SpA' }, precio: 159990, stockActual: 10, stockMinimo: 5 },
  { _id: 'prod-wdsn850', sku: 'SSD-002', nombre: 'WD Black SN850X 1TB', categoria: { id: 'cat-almacenamiento', nombre: 'Almacenamiento' }, proveedor: { id: 'prov-importpc', nombre: 'ImportPC S.A.' }, precio: 99990, stockActual: 2, stockMinimo: 6 },
  { _id: 'prod-seagate2tb', sku: 'HDD-001', nombre: 'Seagate Barracuda 2TB HDD', categoria: { id: 'cat-almacenamiento', nombre: 'Almacenamiento' }, proveedor: { id: 'prov-gamerparts', nombre: 'GamerParts Ltda' }, precio: 54990, stockActual: 20, stockMinimo: 8 }
];

function upsertById(collection, docs) {
  docs.forEach(doc => db[collection].replaceOne({ _id: doc._id }, doc, { upsert: true }));
}

upsertById('categorias', categorias);
upsertById('proveedores', proveedores);
upsertById('clientes', clientes);
upsertById('productos', productos);

// Índices útiles para búsquedas y unicidad.
db.productos.createIndex({ sku: 1 }, { unique: true });
db.clientes.createIndex({ email: 1 }, { unique: true });
db.pedidos.createIndex({ fechaPedido: -1 });
db.pedidos.createIndex({ 'cliente.id': 1, fechaPedido: -1 });
db.auditoriaStock.createIndex({ productoId: 1, fechaEvento: -1 });

print('Base nodo_inventario preparada. Productos: ' + db.productos.countDocuments());
print('Clientes: ' + db.clientes.countDocuments() + '. Pedidos existentes: ' + db.pedidos.countDocuments());
