package cl.inventario;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import java.util.*;
import org.bson.Document;

/** Adapta los documentos MongoDB al formato que consume la tienda web. */
final class MongoInventory implements App.Inventory {
  private static final Map<Integer,String> PRODUCT_IDS=Map.ofEntries(
    Map.entry(1,"prod-ryzen7800"),Map.entry(2,"prod-i514600"),Map.entry(3,"prod-rtx4070"),Map.entry(4,"prod-rtx4060"),
    Map.entry(5,"prod-rx7800"),Map.entry(6,"prod-kingston32"),Map.entry(7,"prod-corsair16"),Map.entry(8,"prod-samsung990"),
    Map.entry(9,"prod-wdsn850"),Map.entry(10,"prod-seagate2tb"));
  private static final Map<Integer,String> CUSTOMER_IDS=Map.of(1,"cli-juan",2,"cli-maria",3,"cli-diego",4,"cli-camila",5,"cli-sofia");

  private MongoCollection<Document> collection(String name){return ConexionMongo.database().getCollection(name);}
  private static int integer(Object value){return value instanceof Number n?n.intValue():0;}
  private static long longValue(Object value){return value instanceof Number n?n.longValue():0L;}
  private static String text(Object value){return value==null?"":value.toString();}

  @Override public List<App.Product> products(){
    List<App.Product> result=new ArrayList<>();
    for(Document d:collection("productos").find().sort(new Document("sku",1))){
      String id=text(d.get("_id"));
      int legacy=integer(d.get("legacyId"));
      if(legacy==0) legacy=PRODUCT_IDS.entrySet().stream().filter(e->e.getValue().equals(id)).map(Map.Entry::getKey).findFirst().orElse(0);
      Document category=d.get("categoria",Document.class),supplier=d.get("proveedor",Document.class);
      String categoryName=category==null?"Sin categoría":text(category.get("nombre"));
      String supplierName=supplier==null?"Sin proveedor":text(supplier.get("nombre"));
      result.add(new App.Product(legacy,text(d.get("nombre")),categoryName,supplierName,integer(d.get("stockActual")),integer(d.get("stockMinimo")),longValue(d.get("precio"))));
    }
    result.removeIf(p->p.id()==0);
    return result;
  }

  @Override public int order(int customerId,int productId,int quantity){
    if(quantity<1) throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
    String customerKey=CUSTOMER_IDS.get(customerId),productKey=PRODUCT_IDS.get(productId);
    if(customerKey==null) throw new IllegalArgumentException("Selecciona un cliente válido.");
    if(productKey==null) throw new IllegalArgumentException("El producto seleccionado no existe.");
    MongoCollection<Document> products=collection("productos");
    Document product=products.find(eq("_id",productKey)).first();
    if(product==null) throw new IllegalArgumentException("No se encontró el producto en MongoDB.");
    Document customer=collection("clientes").find(eq("_id",customerKey)).first();
    if(customer==null) throw new IllegalArgumentException("No se encontró el cliente en MongoDB.");

    // El filtro con stock suficiente evita que dos solicitudes descuenten stock negativo.
    Document updated=products.findOneAndUpdate(and(eq("_id",productKey),gte("stockActual",quantity)),inc("stockActual",-quantity),new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
    if(updated==null) throw new IllegalArgumentException("Stock insuficiente. Actualiza el catálogo e intenta otra vez.");

    String orderKey="ped-"+UUID.randomUUID();
    long unitPrice=longValue(product.get("precio"));
    Date now=new Date();
    Document item=new Document("productoId",productKey).append("sku",text(product.get("sku"))).append("nombre",text(product.get("nombre")))
      .append("cantidad",quantity).append("precioUnitario",unitPrice).append("subtotal",unitPrice*quantity);
    Document order=new Document("_id",orderKey).append("fechaPedido",now)
      .append("cliente",new Document("id",customerKey).append("nombre",text(customer.get("nombre"))).append("email",text(customer.get("email"))))
      .append("items",List.of(item)).append("total",unitPrice*quantity).append("estado","registrado");
    try {
      collection("pedidos").insertOne(order);
    } catch(RuntimeException e) {
      products.updateOne(eq("_id",productKey),inc("stockActual",quantity));
      throw e;
    }
    collection("auditoriaStock").insertOne(new Document("_id","aud-"+UUID.randomUUID()).append("productoId",productKey)
      .append("sku",text(product.get("sku"))).append("tipo","salida_por_pedido").append("cantidad",quantity)
      .append("stockResultante",integer(updated.get("stockActual"))).append("pedidoId",orderKey).append("fechaEvento",now));
    return (int)(System.currentTimeMillis()%2_000_000_000L);
  }
}
