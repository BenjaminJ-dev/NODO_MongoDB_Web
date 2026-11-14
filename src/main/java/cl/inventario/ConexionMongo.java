package cl.inventario;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

/** Mantiene una conexión reutilizable con el servidor local MongoDB. */
final class ConexionMongo {
  private static final String URI=System.getenv().getOrDefault("MONGODB_URI","mongodb://127.0.0.1:27017");
  private static final String DATABASE=System.getenv().getOrDefault("MONGODB_DATABASE","nodo_inventario");
  private static MongoClient client;
  private ConexionMongo() {}

  static synchronized MongoDatabase connect() {
    boolean firstConnection=client==null;
    if(firstConnection) client=MongoClients.create(URI);
    MongoDatabase db=client.getDatabase(DATABASE);
    if(firstConnection) db.runCommand(new Document("ping",1));
    return db;
  }
  static synchronized MongoDatabase database() { return connect(); }
  static String databaseName() { return DATABASE; }
  static synchronized void close() { if(client!=null){client.close();client=null;} }
}
