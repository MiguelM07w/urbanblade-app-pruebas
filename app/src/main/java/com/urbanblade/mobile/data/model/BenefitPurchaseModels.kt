package com.urbanblade.mobile.data.model

import com.google.gson.annotations.SerializedName

data class FavoriteBarberRequest(@SerializedName("barber_id") val barberId: String?)
data class FavoriteBarberResponse(@SerializedName("barbero_favorito_id") val barberId: String?)
data class PackagePurchaseRequest(@SerializedName("service_package_id") val packageId: String)
