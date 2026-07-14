package com.obd.scanner.domain.model

data class VehicleInfo(
    val vin: String,
    val make: String?,
    val year: Int?
) {
    val label: String
        get() = listOfNotNull(make, year?.toString()).joinToString(" · ").ifBlank { "Vehículo" }

    companion object {
        // World Manufacturer Identifier (primeros 3 chars del VIN) → marca.
        private val WMI = mapOf(
            "VR3" to "Peugeot", "VF3" to "Peugeot", "VF7" to "Citroën", "VR7" to "Citroën",
            "VF1" to "Renault", "VF6" to "Renault", "VXK" to "Nissan Europa",
            "1HG" to "Honda", "JHM" to "Honda", "JH" to "Honda",
            "1N4" to "Nissan", "JN1" to "Nissan", "3N1" to "Nissan",
            "WVW" to "Volkswagen", "3VW" to "Volkswagen", "WV1" to "VW Comercial",
            "WBA" to "BMW", "WDB" to "Mercedes-Benz", "WDD" to "Mercedes-Benz",
            "JTD" to "Toyota", "JTM" to "Toyota", "MR0" to "Toyota", "2T1" to "Toyota",
            "1G1" to "Chevrolet", "KL1" to "Chevrolet", "3GN" to "Chevrolet",
            "9BD" to "Fiat", "ZFA" to "Fiat", "1FA" to "Ford", "3FA" to "Ford",
            "MAJ" to "Ford", "KMH" to "Hyundai", "KNA" to "Kia", "5NP" to "Hyundai",
            "3MZ" to "Mazda", "JM1" to "Mazda", "LSV" to "SEAT/VW China",
            "VSS" to "SEAT", "TMB" to "Škoda", "SJN" to "Nissan UK"
        )

        // Código de año del VIN (posición 10). Ciclo de 30 años.
        private val YEAR_CODE = buildMap {
            val codes = "ABCDEFGHJKLMNPRSTVWXY123456789"
            var y = 2010
            for (c in codes) { put(c, y); y++ }  // A=2010 ... 9=2039
        }

        fun fromVin(vin: String): VehicleInfo {
            val make = WMI[vin.take(3)] ?: WMI[vin.take(2)]
            val year = vin.getOrNull(9)?.let { YEAR_CODE[it] }
            return VehicleInfo(vin = vin, make = make, year = year)
        }
    }
}
