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

import kotlinx.serialization.Serializable

@Serializable
data class PlantStatus(

    /** DC input power in kW */
    val pvOutputPower: Double,

    /** AC active power in kW */
    val loadPower: Double,

    /**
     * Power that's coming from the grid.
     * Negative if the plant is feeding to the grid.
     */
    val feedInPower: Double,

    val batteryPower: Double,
    val batteryStateOfCharge: Double

)
