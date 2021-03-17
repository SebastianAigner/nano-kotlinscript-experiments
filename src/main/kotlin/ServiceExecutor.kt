import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.StringScriptSource
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

object ServiceExecutor {
    val services = mutableMapOf<String, (Input) -> JsonOutput?>()
    fun runServiceForId(id: String, input: Input): JsonOutput? {
        val serviceHandler = services.getOrPut(id) {
            val schema = File("$id/schema.json").readText()
            val sourceCode = File("$id/$id.kts").readText()
            val joined = prepareService(sourceCode, schema)
            val r = Registrar()
            val sc = StringScriptSource(joined)
            doScripting(sc, r)
            r.handler!!
        }
        return serviceHandler(input)
    }

    private fun prepareService(sourceCode: String, schema: String): String {
        val schemaObj = Json.parseToJsonElement(schema).jsonObject
        val props = schemaObj["properties"]!!.jsonObject
        val namesAndTypes = props.map {
            val name = it.key
            val type = it.value.jsonObject["type"]!!
            name to type.jsonPrimitive.content
        }
        namesAndTypes.forEach { println(it) }
        val inputDataClass = buildString {
            appendLine("data class Container(val map: Map<String, Any>) {")
            for ((name, type) in namesAndTypes) {
                appendLine("    val $name: ${type.toKotlinType()} by map")
            }
            appendLine("}")
            appendLine(
                """
            println("Registering handler!")
            
            register { rawInputParams: Input ->
                val inputParams = Container(rawInputParams.map)
        """.trimIndent()
            )
        }

        val joined = buildString {
            appendLine(inputDataClass)
            appendLine(sourceCode)
            appendLine("}")
        }

        println("Prepared Kotlin Script: ")
        println(joined)
        return joined
    }

    private fun doScripting(source: SourceCode, registrar: Registrar) {
        val compilationConfiguration = ScriptCompilationConfiguration {
            implicitReceivers(Registrar::class)
            jvm {
                dependenciesFromCurrentContext(wholeClasspath = true)
            }
            hostConfiguration(defaultJvmScriptingHostConfiguration)
            compilerOptions("-jvm-target", Runtime.version().feature().toString())
        }


        val evaluationConfiguration = ScriptEvaluationConfiguration {
            implicitReceivers(registrar)
        }
        BasicJvmScriptingHost().runInCoroutineContext {  }
        BasicJvmScriptingHost().eval(source, compilationConfiguration, evaluationConfiguration).onFailure {
            it.reports.forEach { scriptDiagnostic ->
                when (scriptDiagnostic.severity) {
                    ScriptDiagnostic.Severity.FATAL, ScriptDiagnostic.Severity.ERROR -> {
                        println(scriptDiagnostic.toString())
                        println(scriptDiagnostic.exception?.stackTraceToString())
                    }
                    ScriptDiagnostic.Severity.WARNING, ScriptDiagnostic.Severity.INFO, ScriptDiagnostic.Severity.DEBUG -> println(
                        scriptDiagnostic.severity.toString() + " " + scriptDiagnostic.toString()
                    )
                }
            }
        }
    }
}