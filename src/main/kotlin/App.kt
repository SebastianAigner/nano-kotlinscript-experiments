import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.*
import java.io.File


fun main() {
    println("Hello, Ingress!")

    embeddedServer(Netty, port = 8080) {

        install(CORS) {
            anyHost()
        }
        routing {
            route("/service") {
                route("{serviceId}") {
                    post {
                        val serviceId = call.parameters["serviceId"]!!
                        val input = call.receive<String>()
                        val elem = Json.parseToJsonElement(input).jsonObject.toMap().mapValues { (k, v) ->
                            v.jsonPrimitive.content
                        }
                        val output = ServiceExecutor.runServiceForId(serviceId, Input(elem))
                        if (output != null) {
                            val json = Json.encodeToJsonElement(output.map)
                            call.respondText(json.toString(), contentType = ContentType.Application.Json)
                        } else {
                            call.respondText("output was null.")
                        }
                    }
                    route("/schema") {
                        get {
                            val serviceId = call.parameters["serviceId"]!!
                            call.respondText(ContentType.Application.Json) {
                                File("$serviceId/schema.json").readText()
                            }
                        }
                        post {
                            val serviceId = call.parameters["serviceId"]!!
                            val newSchema = call.receive<String>()
                            File("$serviceId/schema.json").writeText(newSchema)
                        }
                    }
                    route("/source") {
                        get {
                            val serviceId = call.parameters["serviceId"]!!
                            call.respondText(ContentType.Application.Json) {
                                File("$serviceId/$serviceId.kts").readText()
                            }
                        }
                        post {
                            val serviceId = call.parameters["serviceId"]!!
                            val newSchema = call.receive<String>()
                            File("$serviceId/$serviceId.kts").writeText(newSchema)
                        }
                    }
                }
            }
        }
    }.start(wait = true)
}

fun String.toKotlinType(): String {
    return when (this.toLowerCase()) {
        "string" -> "String"
        "int" -> "Int"
        else -> error("No idea how to handle $this as a type.")
    }
}


data class Input(val map: Map<String, Any>)

data class JsonOutput(val map: Map<String, String>)

class Registrar {
    var handler: ((Input) -> JsonOutput?)? = null

    fun getInputMap(): Map<String, Any> {
        return mapOf("text" to "quentin")
    }

    fun register(handler: (Input) -> JsonOutput?) {
        this.handler = handler
    }
}

