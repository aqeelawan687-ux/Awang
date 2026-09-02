package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.CustomerHistoryEntity
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.data.entity.RideEntity
import com.example.data.repository.RiderRepository
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RiderUiState(
    val rides: List<RideEntity> = emptyList(),
    val parcels: List<ParcelEntity> = emptyList(),
    val debtors: List<DebtorEntity> = emptyList(),
    val payments: List<PaymentHistoryEntity> = emptyList(),
    val customerHistory: List<CustomerHistoryEntity> = emptyList(),
    val searchQuery: String = "",
    val rideFilter: String = "ALL", // ALL, TODAY, UNPAID
    val parcelFilter: String = "ALL", // ALL, PENDING, DELIVERED
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val selectedCustomerName: String? = null,
    val selectedCustomerPhone: String? = null
) {
    val totalRideFare: Double get() = rides.sumOf { it.fare }
    val totalRidePaid: Double get() = rides.sumOf { it.amountPaid }
    val totalRideBakaya: Double get() = rides.sumOf { it.remainingBakaya }

    val totalParcelCharges: Double get() = parcels.sumOf { it.deliveryCharges }
    val totalParcelPaid: Double get() = parcels.sumOf { it.amountPaid }
    val totalParcelBakaya: Double get() = parcels.sumOf { it.remainingBakaya }
    val totalDeliveredParcels: Int get() = parcels.count { it.isDelivered }

    val totalDebtorsCount: Int get() = debtors.count { it.remainingDebt > 0 }
    val totalRemainingDebt: Double get() = debtors.sumOf { it.remainingDebt }
    val totalRecoveredCash: Double get() = payments.sumOf { it.amountPaid }

    val netCashInHand: Double get() = totalRidePaid + totalParcelPaid + totalRecoveredCash
}

class RiderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RiderRepository
    private val searchQueryFlow = MutableStateFlow("")
    private val rideFilterFlow = MutableStateFlow("ALL")
    private val parcelFilterFlow = MutableStateFlow("ALL")
    private val themeModeFlow = MutableStateFlow(AppThemeMode.SYSTEM)
    private val selectedCustomerFlow = MutableStateFlow<Pair<String?, String?>>(null to null)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RiderRepository(database.riderDao())
    }

    private data class DatabaseData(
        val rides: List<RideEntity>,
        val parcels: List<ParcelEntity>,
        val debtors: List<DebtorEntity>,
        val payments: List<PaymentHistoryEntity>,
        val customerHistory: List<CustomerHistoryEntity>
    )

    private val databaseDataFlow = combine(
        repository.allRides,
        repository.allParcels,
        repository.allDebtors,
        repository.allPayments,
        repository.allCustomerHistory
    ) { rides, parcels, debtors, payments, history ->
        DatabaseData(rides, parcels, debtors, payments, history)
    }

    private data class SearchPrefs(val query: String, val rideFilter: String, val parcelFilter: String)
    private val searchPrefsFlow = combine(
        searchQueryFlow,
        rideFilterFlow,
        parcelFilterFlow
    ) { query, rideF, parcelF ->
        SearchPrefs(query, rideF, parcelF)
    }

    private data class UiPrefs(val theme: AppThemeMode, val customer: Pair<String?, String?>)
    private val uiPrefsFlow = combine(
        themeModeFlow,
        selectedCustomerFlow
    ) { theme, customer ->
        UiPrefs(theme, customer)
    }

    val uiState: StateFlow<RiderUiState> = combine(
        databaseDataFlow,
        searchPrefsFlow,
        uiPrefsFlow
    ) { db, search, ui ->
        val q = search.query.trim().lowercase()

        val filteredRides = db.rides.filter { ride ->
            val matchesQuery = q.isEmpty() ||
                    ride.customerName.lowercase().contains(q) ||
                    ride.phone.contains(q) ||
                    ride.pickupLocation.lowercase().contains(q) ||
                    ride.dropoffLocation.lowercase().contains(q)

            val matchesFilter = when (search.rideFilter) {
                "UNPAID" -> ride.remainingBakaya > 0
                else -> true
            }
            matchesQuery && matchesFilter
        }

        val filteredParcels = db.parcels.filter { parcel ->
            val matchesQuery = q.isEmpty() ||
                    parcel.senderName.lowercase().contains(q) ||
                    parcel.senderPhone.contains(q) ||
                    parcel.receiverName.lowercase().contains(q) ||
                    parcel.deliveryAddress.lowercase().contains(q)

            val matchesFilter = when (search.parcelFilter) {
                "PENDING" -> !parcel.isDelivered
                "DELIVERED" -> parcel.isDelivered
                "UNPAID" -> parcel.remainingBakaya > 0
                else -> true
            }
            matchesQuery && matchesFilter
        }

        val filteredDebtors = db.debtors.filter { debtor ->
            q.isEmpty() || debtor.name.lowercase().contains(q) || debtor.phone.contains(q)
        }

        RiderUiState(
            rides = filteredRides,
            parcels = filteredParcels,
            debtors = filteredDebtors,
            payments = db.payments,
            customerHistory = db.customerHistory,
            searchQuery = search.query,
            rideFilter = search.rideFilter,
            parcelFilter = search.parcelFilter,
            themeMode = ui.theme,
            selectedCustomerName = ui.customer.first,
            selectedCustomerPhone = ui.customer.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RiderUiState()
    )

    fun setSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun setRideFilter(filter: String) {
        rideFilterFlow.value = filter
    }

    fun setParcelFilter(filter: String) {
        parcelFilterFlow.value = filter
    }

    fun setThemeMode(mode: AppThemeMode) {
        themeModeFlow.value = mode
    }

    fun selectCustomer(name: String?, phone: String?) {
        selectedCustomerFlow.value = name to phone
    }

    // Rides CRUD
    fun addRide(
        customerName: String,
        phone: String,
        pickup: String,
        dropoff: String,
        fare: Double,
        amountPaid: Double,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val remaining = (fare - amountPaid).coerceAtLeast(0.0)
            val paymentStatus = when {
                remaining <= 0 -> "PAID"
                amountPaid > 0 -> "PARTIAL"
                else -> "UNPAID"
            }
            val ride = RideEntity(
                customerName = customerName.trim(),
                phone = phone.trim(),
                pickupLocation = pickup.trim(),
                dropoffLocation = dropoff.trim(),
                fare = fare,
                paymentStatus = paymentStatus,
                amountPaid = amountPaid,
                remainingBakaya = remaining,
                rideDate = System.currentTimeMillis(),
                notes = notes.trim()
            )
            repository.addRide(ride)
        }
    }

    fun updateRide(ride: RideEntity) {
        viewModelScope.launch {
            repository.updateRide(ride)
        }
    }

    fun deleteRide(ride: RideEntity) {
        viewModelScope.launch {
            repository.deleteRide(ride)
        }
    }

    // Parcels CRUD
    fun addParcel(
        senderName: String,
        senderPhone: String,
        receiverName: String,
        receiverPhone: String,
        pickupAddress: String,
        deliveryAddress: String,
        deliveryCharges: Double,
        amountPaid: Double,
        isDelivered: Boolean = false,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val remaining = (deliveryCharges - amountPaid).coerceAtLeast(0.0)
            val parcel = ParcelEntity(
                senderName = senderName.trim(),
                senderPhone = senderPhone.trim(),
                receiverName = receiverName.trim(),
                receiverPhone = receiverPhone.trim(),
                pickupAddress = pickupAddress.trim(),
                deliveryAddress = deliveryAddress.trim(),
                deliveryCharges = deliveryCharges,
                isDelivered = isDelivered,
                isPaid = remaining <= 0,
                amountPaid = amountPaid,
                remainingBakaya = remaining,
                date = System.currentTimeMillis(),
                notes = notes.trim()
            )
            repository.addParcel(parcel)
        }
    }

    fun updateParcel(parcel: ParcelEntity) {
        viewModelScope.launch {
            repository.updateParcel(parcel)
        }
    }

    fun toggleParcelDelivered(parcel: ParcelEntity) {
        viewModelScope.launch {
            val updated = parcel.copy(isDelivered = !parcel.isDelivered)
            repository.updateParcel(updated)
        }
    }

    fun toggleParcelPaid(parcel: ParcelEntity) {
        viewModelScope.launch {
            val newIsPaid = !parcel.isPaid
            val newPaid = if (newIsPaid) parcel.deliveryCharges else 0.0
            val newBakaya = if (newIsPaid) 0.0 else parcel.deliveryCharges
            val updated = parcel.copy(isPaid = newIsPaid, amountPaid = newPaid, remainingBakaya = newBakaya)
            repository.updateParcel(updated)
        }
    }

    fun deleteParcel(parcel: ParcelEntity) {
        viewModelScope.launch {
            repository.deleteParcel(parcel)
        }
    }

    // Debtors & Payments
    fun addDebtor(name: String, phone: String, totalDebt: Double, notes: String = "") {
        viewModelScope.launch {
            val debtor = DebtorEntity(
                name = name.trim(),
                phone = phone.trim(),
                totalDebt = totalDebt,
                remainingDebt = totalDebt,
                lastUpdated = System.currentTimeMillis(),
                notes = notes.trim()
            )
            repository.addDebtor(debtor)
        }
    }

    fun updateDebtor(debtor: DebtorEntity) {
        viewModelScope.launch {
            repository.updateDebtor(debtor)
        }
    }

    fun deleteDebtor(debtor: DebtorEntity) {
        viewModelScope.launch {
            repository.deleteDebtor(debtor)
        }
    }

    fun recordPayment(debtorId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            repository.addPayment(debtorId, amount, note)
        }
    }

    fun updatePayment(payment: PaymentHistoryEntity, oldAmount: Double) {
        viewModelScope.launch {
            repository.updatePayment(payment, oldAmount)
        }
    }

    fun deletePayment(payment: PaymentHistoryEntity) {
        viewModelScope.launch {
            repository.deletePayment(payment)
        }
    }

    fun updateCustomerBakaya(name: String, phone: String, newBakaya: Double) {
        viewModelScope.launch {
            repository.updateCustomerBakaya(name, phone, newBakaya)
        }
    }

    fun deleteCustomerBakaya(name: String, phone: String) {
        viewModelScope.launch {
            repository.deleteCustomerBakaya(name, phone)
        }
    }

    fun deleteCustomerHistory(history: CustomerHistoryEntity) {
        viewModelScope.launch {
            repository.deleteCustomerHistory(history)
        }
    }

    fun deleteCustomerHistoryById(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomerHistoryById(id)
        }
    }

    fun deleteCustomerHistoryBatch(ids: List<Long>) {
        viewModelScope.launch {
            repository.deleteCustomerHistoryBatch(ids)
        }
    }

    fun deleteAllCustomerHistory() {
        viewModelScope.launch {
            repository.deleteAllCustomerHistory()
        }
    }

    fun resetAppData() {
        viewModelScope.launch {
            repository.resetAllData()
        }
    }
}
