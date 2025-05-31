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

import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import com.ghgande.j2mod.modbus.procimg.InputRegister
import com.ghgande.j2mod.modbus.procimg.Register
import org.slf4j.LoggerFactory

const val DEFAULT_DEVICE_ID: Int = 0
const val DEFAULT_MODBUS_PORT: Int = 502

private val logger = LoggerFactory.getLogger("ModbusClient")

/**
 * ModbusClient maintains a connection to the Modbus server
 * and provides methods to get the plant status on demand.
 */
class ModbusClient(
    private val address: String,
    private val port: Int = DEFAULT_MODBUS_PORT
) {

    private val master = ModbusTCPMaster(address, port)

    val isConnected: Boolean
        get() = master.isConnected

    /**
     * Connect to the Modbus server
     */
    fun connect() {

        if (master.isConnected)
            return

        try {

            master.connect()

            logger.info("Connected to Modbus server at $address:$port")

        } catch (ex: Exception) {
            logger.error("Failed to connect to Modbus server.", ex)
        }
    }

    fun getPlantInfo(): PlantInfo {

        val infoRegisters: Array<Register> =
            master.readMultipleRegisters(DEFAULT_DEVICE_ID, 30000, 60)

        val offeringName = convertToString(
            registers = infoRegisters.sliceArray(0..14)
        )

        val serialNumber = convertToString(
            registers = infoRegisters.sliceArray(15..24)
        )

        val softwareVersion = convertToString(
            registers = infoRegisters.sliceArray(35..59)
        )

        return PlantInfo(
            offeringName = offeringName,
            serialNumber = serialNumber,
            softwareVersion = softwareVersion
        )
    }

    fun getPlantStatus(): PlantStatus {

        val inputPowerRegisters: Array<Register> =
            master.readMultipleRegisters(DEFAULT_DEVICE_ID, 30354, 8)

        val batteryRegisters: Array<Register> =
            master.readMultipleRegisters(DEFAULT_DEVICE_ID, 30368, 1)

        /*
         * Register 30354: PV output power
         */
        val pvOutputPower = convertToInt32(
            highWord = inputPowerRegisters[0].value,
            lowWord = inputPowerRegisters[1].value
        ) / 1000.0

        /*
         * Register 30356: Load power
         */
        val loadPower = convertToInt32(
            highWord = inputPowerRegisters[2].value,
            lowWord = inputPowerRegisters[3].value
        ) / 1000.0

        /*
         * Register 30358: Feed-in power
         */
        val feedInPower = convertToInt32(
            highWord = inputPowerRegisters[4].value,
            lowWord = inputPowerRegisters[5].value
        ) / 1000.0

        /*
         * Register 30360: Battery charge/discharge power
         */
        val batteryPower = convertToInt32(
            highWord = inputPowerRegisters[6].value,
            lowWord = inputPowerRegisters[7].value
        ) / 1000.0

        /*
         * Register 30368: SOC (State of Charge)
         */
        val batteryStateOfCharge = batteryRegisters[0].value / 100.0

        return PlantStatus(
            pvOutputPower = pvOutputPower,
            loadPower = loadPower,
            feedInPower = feedInPower,
            batteryPower = batteryPower,
            batteryStateOfCharge = batteryStateOfCharge
        )
    }

    fun getBatteryControl(): BatterySettings {

        val essRegisters: Array<Register> =
            master.readMultipleRegisters(DEFAULT_DEVICE_ID, 40000, 4)

        /*
         * Register 40000: ESS control mode
         */
        val controlMode = BatteryControlMode.fromId(
            id = essRegisters[0].value
        )

        /*
         * Register 40001: [Time of Use mode] Preferred use of surplus PV power
         */
        val useOfSurplusPower = UseOfSurplusPower.fromId(
            id = essRegisters[1].value
        )

        /*
         * Register 40002: [Time of Use mode] Maximum power for charging batteries from grid
         */
        val maximumPowerForCharging = convertToInt32(
            highWord = essRegisters[2].value,
            lowWord = essRegisters[3].value
        ) / 1000.0

        return BatterySettings(
            controlMode = controlMode,
            useOfSurplusPower = useOfSurplusPower,
            maximumPowerForCharging = maximumPowerForCharging
        )
    }

}

private fun convertToInt32(
    highWord: Int,
    lowWord: Int
): Int = (highWord shl 16) or (lowWord and 0xFFFF)

/* Converts Modbus registers to a String. Assumes each register is 2 ASCII characters (1 byte per char). */
fun convertToString(registers: Array<out InputRegister>): String {

    val byteBuffer = ByteArray(registers.size * 2)

    for ((i, reg) in registers.withIndex()) {

        val value = reg.value

        /* The high byte */
        byteBuffer[i * 2] = (value shr 8).toByte()

        /* The low byte */
        byteBuffer[i * 2 + 1] = (value and 0xFF).toByte()
    }

    return byteBuffer.toString(Charsets.US_ASCII).trimEnd('\u0000')
}
