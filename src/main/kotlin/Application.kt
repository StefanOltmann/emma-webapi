/*
 * EMMA Web API
 * Copyright (C) 2025 Stefan Oltmann
 * https://github.com/StefanOltmann/emma-webapi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

/**
 * Global ModbusClient instance to maintain a persistent connection
 */
private lateinit var modbusClient: ModbusClient

private const val APP_NAME = "Huawei EMMA Web API"
private const val UNKNOWN_ERROR = "Unknown error"
private const val API_KEY_HEADER = "API_KEY"

private val EMMA_ADDRESS = System.getenv("EMMA_ADDRESS")
private val API_KEY = System.getenv("API_KEY") ?: ""

fun main() {

    if (EMMA_ADDRESS == null) {

        logger.error("EMMA_ADDRESS environment variable not set")

        return
    }

    modbusClient = ModbusClient(address = EMMA_ADDRESS)

    modbusClient.connect()

    embeddedServer(
        factory = CIO,
        port = 8080
    ) {

        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                }
            )
        }

        routing {

            get("/") {

                if (modbusClient.isConnected)
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = "$APP_NAME: EMMA is connected."
                    )
                else
                    call.respond(
                        status = HttpStatusCode.ServiceUnavailable,
                        message = "$APP_NAME: Failed to connect to EMMA."
                    )
            }

            get("/info") {

                try {

                    if (!call.isAuthorized()) {
                        call.respond(HttpStatusCode.Unauthorized, "Wrong API key.")
                        return@get
                    }

                    val info = modbusClient.getPlantInfo()

                    call.respond(info)

                } catch (ex: Exception) {

                    logger.error("Failed to get plant info.", ex)

                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = ex.message ?: UNKNOWN_ERROR
                    )
                }
            }

            get("/status") {

                try {

                    if (!call.isAuthorized()) {
                        call.respond(HttpStatusCode.Unauthorized, "Wrong API key.")
                        return@get
                    }

                    val status = modbusClient.getPlantStatus()

                    call.respond(status)

                } catch (ex: Exception) {

                    logger.error("Failed to get plant status.", ex)

                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = ex.message ?: UNKNOWN_ERROR
                    )
                }
            }

            get("/battery") {

                try {

                    if (!call.isAuthorized()) {
                        call.respond(HttpStatusCode.Unauthorized, "Wrong API key.")
                        return@get
                    }

                    val batteryControl = modbusClient.getBatteryControl()

                    call.respond(batteryControl)

                } catch (ex: Exception) {

                    logger.error("Failed to get battery control settings.", ex)

                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = ex.message ?: UNKNOWN_ERROR
                    )
                }
            }

            /**
             * Endpoint for Docker health.
             *
             * Does a simple request to see if everything is working fine.
             */
            get("/health") {
                try {

                    modbusClient.getPlantStatus()

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = "Service is healthy."
                    )

                    logger.info("Health check successful.")

                } catch (ex: Exception) {

                    logger.error("Health check failed: ${ex.message}", ex)

                    call.respond(
                        status = HttpStatusCode.InternalServerError,
                        message = "Service is unhealthy."
                    )
                }
            }

            /**
             * The current version of the service
             */
            get("/version") {
                call.respondText(VERSION)
            }
        }
    }.start(wait = true)
}

private fun RoutingCall.isAuthorized(): Boolean {

    val requestApiKey: String? = request.headers[API_KEY_HEADER]

    return !(API_KEY.isNotBlank() && requestApiKey != API_KEY)
}
