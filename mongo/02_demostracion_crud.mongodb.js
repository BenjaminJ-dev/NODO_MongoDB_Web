// NODO - Demostración CRUD en MongoDB (mongosh)
// Requiere ejecutar antes 01_preparar_datos.mongodb.js.
// Ejecuta cada bloque por separado para capturar los resultados en Compass.
use('nodo_inventario');

// CREATE: crear un pedido con productos embebidos como snapshot histórico.
// El filtro stockActual >= cantidad evita dejar existencias negativas.
const cliente = db.clientes.findOne({ _id: 'cli-juan' });
const producto = db.productos.findOne({ _id: 'prod-ryzen7800' });
const cantidad = 1;
if (!cliente || !producto) throw new Error('No existe el cliente o producto indicado.');
const rebaja = db.productos.updateOne(
  { _id: producto._id, stockActual: { $gte: cantidad } },
  { $inc: { stockActual: -cantidad } }
);
if (rebaja.modifiedCount !== 1) throw new Error('Stock insuficiente; pedido no creado.');
const nuevoPedido = {
  _id: 'pedido-demo-001',
  fechaPedido: new Date(),
  estado: 'PROCESADO',
  cliente: { id: cliente._id, nombre: cliente.nombre, email: cliente.email },
  items: [{
    productoId: producto._id,
    sku: producto.sku,
    nombre: producto.nombre,
    cantidad,
    precioUnitario: producto.precio,
    subtotal: producto.precio
  }],
  total: producto.precio
};
try {
  db.pedidos.insertOne(nuevoPedido);
  printjson(db.pedidos.findOne({ _id: nuevoPedido._id }));
} catch (e) {
  // Compensa el descuento si el insert falla en esta demostración local.
  db.productos.updateOne({ _id: producto._id }, { $inc: { stockActual: cantidad } });
  throw e;
}

// READ: productos bajo mínimo (equivale al reporte de stock crítico Oracle).
db.productos.find(
  { $expr: { $lt: ['$stockActual', '$stockMinimo'] } },
  { _id: 0, sku: 1, nombre: 1, 'categoria.nombre': 1, 'proveedor.nombre': 1, stockActual: 1, stockMinimo: 1 }
).sort({ stockActual: 1 }).toArray();

// UPDATE: modificar mínimo de reposición de un producto.
db.productos.updateOne(
  { _id: 'prod-ryzen7800' },
  { $set: { stockMinimo: 6, actualizadoEn: new Date() } }
);
db.productos.findOne({ _id: 'prod-ryzen7800' });

// DELETE: borrar exclusivamente el pedido de demostración creado arriba.
// El reembolso de inventario se hace explícito antes de borrar el documento.
const pedidoDemo = db.pedidos.findOne({ _id: 'pedido-demo-001' });
if (pedidoDemo) {
  pedidoDemo.items.forEach(item => db.productos.updateOne(
    { _id: item.productoId }, { $inc: { stockActual: item.cantidad } }
  ));
  db.pedidos.deleteOne({ _id: 'pedido-demo-001' });
}
print('Pedido demo eliminado; stock restaurado si el pedido existía.');
