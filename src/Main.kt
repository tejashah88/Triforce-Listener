
import io.vertx.core.Vertx

fun main(args : Array<String>) {
    val vertx = Vertx.vertx()
    vertx.deployVerticle(Server()) { ar ->
        if (ar.succeeded()) {
            println("Application started")
        } else {
            println("Could not start application")
            ar.cause().printStackTrace()
        }
    }
}
