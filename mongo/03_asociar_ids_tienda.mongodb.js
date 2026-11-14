// Compatibilidad de los documentos actuales con los IDs numéricos de la interfaz NODO.
// Ejecutar una vez en MongoDB Compass > MongoDB Shell. No reemplaza ni borra documentos.
use('nodo_inventario');

const productoIds = [
  'prod-ryzen7800', 'prod-i514600', 'prod-rtx4070', 'prod-rtx4060', 'prod-rx7800',
  'prod-kingston32', 'prod-corsair16', 'prod-samsung990', 'prod-wdsn850', 'prod-seagate2tb'
];
productoIds.forEach((id, i) => db.productos.updateOne(
  { _id: id }, { $set: { legacyId: i + 1 } }
));

const clienteIds = ['cli-juan', 'cli-maria', 'cli-diego', 'cli-camila', 'cli-sofia'];
clienteIds.forEach((id, i) => db.clientes.updateOne(
  { _id: id }, { $set: { legacyId: i + 1 } }
));

db.productos.createIndex({ legacyId: 1 }, { unique: true, sparse: true });
db.clientes.createIndex({ legacyId: 1 }, { unique: true, sparse: true });
print('Compatibilidad lista. Productos: ' + db.productos.countDocuments({ legacyId: { $exists: true } }) +
  '; clientes: ' + db.clientes.countDocuments({ legacyId: { $exists: true } }));
