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

/**
 * ESS control mode
 *
 * Register 40000
 */
@Serializable
enum class BatteryControlMode {

    MAXIMUM_SELF_CONSUMPTION,

    FULLY_FED_TO_GRID,

    TIME_OF_USE,

    THIRD_PARTY_DISPATCH;

    companion object {

        fun fromId(id: Int): BatteryControlMode = when (id) {
            2 -> MAXIMUM_SELF_CONSUMPTION
            4 -> FULLY_FED_TO_GRID
            5 -> TIME_OF_USE
            6 -> THIRD_PARTY_DISPATCH
            else -> throw IllegalArgumentException("Unknown BatteryControlMode: $id")
        }
    }
}
