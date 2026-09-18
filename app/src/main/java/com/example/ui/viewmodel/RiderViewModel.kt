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
    val allRides: List<RideEntity> = emptyList(),
    val allParcels: List<ParcelEntity> = emptyList(),
    val allDebtors: List<DebtorEntity> = emptyList(),
    val searchQuery: String = "",
    val rideFilter: String = "ALL", // ALL, TODAY, UNPAID
    val parcelFilter: String = "ALL", // ALL, PENDING, DELIVERED
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val selectedCustomerId: Long? = null,
    val selectedCustomerName: String? = null,
    val selectedCustomerPhone: String? = null
) {
    val totalRideFare: Double get() = allRides.sumOf { it.fare }
    val totalRidePaid: Double get() = allRides.sumOf { it.amountPaid }
    val totalRideBakaya: Double get() = allRides.sumOf { it.remainingBakaya }

    val totalParcelCharges: Double get() = allParcels.sumOf { it.deliveryCharges + it.samanCharges }
    val totalParcelPaid: Double get() = allParcels.sumOf { it.amountPaid }
    val totalParcelBakaya: Double get() = allParcels.sumOf { it.remainingBakaya }
    val totalDeliveredParcels: Int get() = allParcels.count { it.isDelivered }

    val totalDebtorsCount: Int get() = allDebtors.count { it.remainingDebt > 0 }
    val totalRemainingDebt: Double get() = allDebtors.sumOf { it.remainingDebt }
    val totalRecoveredCash: Double get() = payments.sumOf { it.amountPaid }

    val netCashInHand: Double get() = totalRidePaid + totalParcelPaid + totalRecoveredCash
}

data class CustomerSelection(
    val id: Long? = null,
    val name: String? = null,
    val phone: String? = null
)

class RiderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RiderRepository
    private val searchQueryFlow = MutableStateFlow("")
    private val rideFilterFlow = MutableStateFlow("ALL")
    private val parcelFilterFlow = MutableStateFlow("ALL")
    private val themeModeFlow = MutableStateFlow(AppThemeMode.SYSTEM)
    private val selectedCustomerFlow = MutableStateFlow(CustomerSelection())

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RiderRepository(database.riderDao(), database)
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

    private data class UiPrefs(val theme: AppThemeMode, val customer: CustomerSelection)
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
                    parcel.deliveryAddress.lowercase().contains(q) ||
                    parcel.shopName.lowercase().contains(q)

            val matchesFilter = when (search.parcelFilter) {
                "PENDING" -> !parcel.isDelivered
                "DELIVERED" -> parcel.isDelivered
                "UNPAID" -> parcel.remainingBakaya > 0
                else -> true
            }
            matchesQuery && matchesFilter
        }

        val filteredDebtors = db.debtors.filter { debtor ->
            q.isEmpty() || debtor.name.lowercase().contains(q) || debtor.phone.contains(q) || (debtor.address?.lowercase()?.contains(q) == true)
        }

        RiderUiState(
            rides = filteredRides,
            parcels = filteredParcels,
            debtors = filteredDebtors,
            payments = db.payments,
            customerHistory = db.customerHistory,
            allRides = db.rides,
            allParcels = db.parcels,
            allDebtors = db.debtors,
            searchQuery = search.query,
            rideFilter = search.rideFilter,
            parcelFilter = search.parcelFilter,
            themeMode = ui.theme,
            selectedCustomerId = ui.customer.id,
            selectedCustomerName = ui.customer.name,
            selectedCustomerPhone = ui.customer.phone
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
        selectedCustomerFlow.value = CustomerSelection(null, name, phone)
    }

    fun selectCustomer(id: Long?, name: String?, phone: String?) {
        selectedCustomerFlow.value = CustomerSelection(id, name, phone)
    }

    fun selectCustomer(debtor: DebtorEntity?) {
        selectedCustomerFlow.value = CustomerSelection(debtor?.id, debtor?.name, debtor?.phone)
    }

    // Rides CRUD
    fun addRide(
        customerName: String,
        phone: String,
        pickup: String,
        dropoff: String,
        fare: Double,
        amountPaid: Double,
        notes: String = "",
        customerId: Long? = null,
        rideDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val remaining = (fare - amountPaid).coerceAtLeast(0.0)
            val paymentStatus = when {
                remaining <= 0 -> "PAID"
                amountPaid > 0 -> "PARTIAL"
                else -> "UNPAID"
            }
            val ride = RideEntity(
                customerId = customerId,
                customerName = customerName.trim(),
                phone = phone.trim(),
                pickupLocation = pickup.trim(),
                dropoffLocation = dropoff.trim(),
                fare = fare,
                paymentStatus = paymentStatus,
                amountPaid = amountPaid,
                remainingBakaya = remaining,
                rideDate = rideDate,
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
        notes: String = "",
        customerId: Long? = null,
        shopName: String = "",
        samanCharges: Double = 0.0,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val total = samanCharges + deliveryCharges
            val remaining = (total - amountPaid).coerceAtLeast(0.0)
            val parcel = ParcelEntity(
                customerId = customerId,
                senderName = senderName.trim(),
                senderPhone = senderPhone.trim(),
                receiverName = receiverName.trim(),
                receiverPhone = receiverPhone.trim(),
                pickupAddress = pickupAddress.trim(),
                deliveryAddress = deliveryAddress.trim(),
                shopName = shopName.trim(),
                samanCharges = samanCharges,
                deliveryCharges = deliveryCharges,
                isDelivered = isDelivered,
                isPaid = remaining <= 0,
                amountPaid = amountPaid,
                remainingBakaya = remaining,
                date = date,
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
            val total = parcel.samanCharges + parcel.deliveryCharges
            val newPaid = if (newIsPaid) total else 0.0
            val newBakaya = if (newIsPaid) 0.0 else total
            val updated = parcel.copy(isPaid = newIsPaid, amountPaid = newPaid, remainingBakaya = newBakaya)
            repository.updateParcel(updated)
        }
    }

    fun deleteParcel(parcel: ParcelEntity) {
        viewModelScope.launch {
            repository.deleteParcel(parcel)
        }
    }

    // Debtors & Customers
    fun addCustomer(
        name: String,
        phone: String,
        address: String = "",
        location: String = "",
        photoUri: String? = null,
        initialBalance: Double = 0.0,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val debtor = DebtorEntity(
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                location = location.trim(),
                photoUri = photoUri,
                totalDebt = initialBalance,
                remainingDebt = initialBalance,
                lastUpdated = System.currentTimeMillis(),
                notes = notes.trim()
            )
            repository.addDebtor(debtor)
        }
    }

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

    fun recordCustomerPayment(name: String, phone: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.recordCustomerPayment(name, phone, amount, note)
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

    fun updateCustomerBakayaById(customerId: Long, newBakaya: Double) {
        viewModelScope.launch {
            repository.updateCustomerBakayaById(customerId, newBakaya)
        }
    }

    fun deleteCustomerBakayaById(customerId: Long) {
        viewModelScope.launch {
            repository.deleteCustomerBakayaById(customerId)
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
