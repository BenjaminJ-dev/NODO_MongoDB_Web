package cl.inventario;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {
  record Product(int id, String name, String category, String supplier, int stock, int minimum, long price) {
    boolean critical() { return stock < minimum; }
  }
  interface Inventory { List<Product> products() throws Exception; int order(int customerId,int productId,int quantity) throws Exception; }

  private static final Inventory inventory=new MongoInventory();
  private static final String mode="mongodb";

  public static void main(String[] args) throws Exception {
    int port=Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("8080"));
    ConexionMongo.connect();
    inventory.products();
    HttpServer server=HttpServer.create(new InetSocketAddress(port),0);
    server.createContext("/api/status",x->handle(x,App::status));
    server.createContext("/api/products",x->handle(x,App::products));
    server.createContext("/api/orders",x->handle(x,App::order));
    server.createContext("/",App::staticFile);
    server.setExecutor(Executors.newFixedThreadPool(8));
    Runtime.getRuntime().addShutdownHook(new Thread(()->{server.stop(1);ConexionMongo.close();}));
    server.start();
    System.out.println("Inventario listo en http://localhost:"+port);
    System.out.println("MongoDB conectado: "+ConexionMongo.databaseName());
  }

  interface Handler { void run(HttpExchange x) throws Exception; }
  static void handle(HttpExchange x,Handler h){try{h.run(x);}catch(IllegalArgumentException e){send(x,400,"{\"error\":"+json(e.getMessage())+"}");}catch(Exception e){e.printStackTrace();send(x,503,"{\"error\":"+json(e.getMessage()==null?"No se pudo completar la operación con MongoDB":e.getMessage())+"}");}}
  static void products(HttpExchange x)throws Exception{
    if(!x.getRequestMethod().equals("GET")){send(x,405,"{\"error\":\"Método no permitido\"}");return;}
    List<Product> ps=inventory.products();StringJoiner arr=new StringJoiner(",","[","]");
    for(Product p:ps) arr.add("{\"id\":"+p.id()+",\"name\":"+json(p.name())+",\"category\":"+json(p.category())+",\"supplier\":"+json(p.supplier())+",\"stock\":"+p.stock()+",\"minimum\":"+p.minimum()+",\"price\":"+p.price()+",\"critical\":"+p.critical()+"}");
    send(x,200,arr.toString());
  }
  static void status(HttpExchange x)throws IOException{
    if(!x.getRequestMethod().equals("GET")){send(x,405,"{\"error\":\"Método no permitido\"}");return;}
    send(x,200,"{\"mode\":"+json(mode)+",\"database\":"+json(ConexionMongo.databaseName())+"}");
  }
  static void order(HttpExchange x)throws Exception{
    if(!x.getRequestMethod().equals("POST")){send(x,405,"{\"error\":\"Método no permitido\"}");return;}
    String body=new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
    int customer=number(body,"customerId"),product=number(body,"productId"),qty=number(body,"quantity");
    int id=inventory.order(customer,product,qty);send(x,201,"{\"orderId\":"+id+",\"message\":\"Pedido registrado en MongoDB\"}");
  }
  static int number(String body,String key){Matcher m=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*(\\d+)").matcher(body);if(!m.find())throw new IllegalArgumentException("Falta el campo "+key);return Integer.parseInt(m.group(1));}
  static void staticFile(HttpExchange x)throws IOException{
    String path=x.getRequestURI().getPath();if(path.equals("/"))path="/index.html";
    if(path.contains("..")||!path.matches("/[a-zA-Z0-9._-]+")){send(x,404,"No encontrado");return;}
    try(InputStream in=App.class.getResourceAsStream("/static"+path)){
      if(in==null){send(x,404,"No encontrado");return;}byte[] bytes=in.readAllBytes();String type=path.endsWith(".css")?"text/css; charset=utf-8":path.endsWith(".js")?"application/javascript; charset=utf-8":"text/html; charset=utf-8";x.getResponseHeaders().set("Content-Type",type);x.sendResponseHeaders(200,bytes.length);try(OutputStream out=x.getResponseBody()){out.write(bytes);}
    }
  }
  static String json(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\"";}
  static void send(HttpExchange x,int status,String body){try{byte[] b=body.getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");x.getResponseHeaders().set("Cache-Control","no-store");x.sendResponseHeaders(status,b.length);x.getResponseBody().write(b);x.close();}catch(IOException ignored){}}
}
