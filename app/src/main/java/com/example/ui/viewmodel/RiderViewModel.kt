package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
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
import com.example.util.DistanceCalculator
import com.example.util.NotificationHelper
import com.example.util.PdfReportGenerator
import com.example.util.ShareUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

data class RiderUiState(
    val debtors: List<DebtorEntity> = emptyList(),
    val filteredDebtors: List<DebtorEntity> = emptyList(),
    val overdueDebtors: List<DebtorEntity> = emptyList(),
    val parcels: List<ParcelEntity> = emptyList(),
    val filteredParcels: List<ParcelEntity> = emptyList(),
    val rides: List<RideEntity> = emptyList(),
    val paymentHistory: List<PaymentHistoryEntity> = emptyList(),
    val customerHistory: List<CustomerHistoryEntity> = emptyList(),
    val searchQueryDebts: String = "",
    val searchQueryParcels: String = "",
    val totalQarza: Double = 0.0,
    val samanKePaise: Double = 0.0,
    val isMahineKiEarning: Double = 0.0,
    val todayRidesTotal: Double = 0.0,
    val thisMonthRidesTotal: Double = 0.0,
    val themeMode: AppThemeMode = AppThemeMode.BLUE,
    val isDarkMode: Boolean = false,
    val isBalanceHidden: Boolean = false,
    val selectedReportDateMillis: Long = System.currentTimeMillis()
)

private data class DatabaseData(
    val debtors: List<DebtorEntity>,
    val parcels: List<ParcelEntity>,
    val rides: List<RideEntity>,
    val history: List<PaymentHistoryEntity>,
    val customerHistory: List<CustomerHistoryEntity>
)

class RiderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RiderRepository
    private val prefs = application.getSharedPreferences("aqeel_rider_prefs", Context.MODE_PRIVATE)

    private val _searchQueryDebts = MutableStateFlow("")
    private val _searchQueryParcels = MutableStateFlow("")
    private val _themeMode = MutableStateFlow(
        AppThemeMode.fromNameOrDefault(prefs.getString("app_theme_mode", AppThemeMode.BLUE.name))
    )
    private val _isDarkMode = MutableStateFlow(false)
    private val _isBalanceHidden = MutableStateFlow(prefs.getBoolean("hide_balance", false))
    private val _selectedReportDateMillis = MutableStateFlow(System.currentTimeMillis())

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RiderRepository(database.riderDao())
        
        // Check overdue debts and send notification if required
        viewModelScope.launch {
            val debtors = repository.allDebtors.first()
            for (debtor in debtors) {
                if (NotificationHelper.isOverdue(debtor)) {
                    NotificationHelper.sendOverdueNotification(application, debtor)
                }
            }
        }
    }

    private val databaseDataFlow = combine(
        repository.allDebtors,
        repository.allParcels,
        repository.allRides,
        repository.allPaymentHistory,
        repository.allCustomerHistory
    ) { debtors, parcels, rides, history, custHistory ->
        DatabaseData(debtors, parcels, rides, history, custHistory)
    }

    private data class SearchPrefs(
        val queryDebts: String,
        val queryParcels: String,
        val reportDateMillis: Long
    )

    private val searchPrefsFlow = combine(
        _searchQueryDebts,
        _searchQueryParcels,
        _selectedReportDateMillis
    ) { queryDebts, queryParcels, reportDate ->
        SearchPrefs(queryDebts, queryParcels, reportDate)
    }

    private data class UiPrefs(
        val themeMode: AppThemeMode,
        val isDarkMode: Boolean,
        val isBalanceHidden: Boolean
    )

    private val uiPrefsFlow = combine(
        _themeMode,
        _isDarkMode,
        _isBalanceHidden
    ) { themeMode, dark, balanceHidden ->
        UiPrefs(themeMode, dark, balanceHidden)
    }

    val uiState: StateFlow<RiderUiState> = combine(
        databaseDataFlow,
        searchPrefsFlow,
        uiPrefsFlow
    ) { dbData, searchPrefs, prefs ->
        val debtors = dbData.debtors
        val parcels = dbData.parcels
        val rides = dbData.rides
        val history = dbData.history
        val customerHistory = dbData.customerHistory

        val queryDebts = searchPrefs.queryDebts
        val queryParcels = searchPrefs.queryParcels
        val reportDate = searchPrefs.reportDateMillis

        val themeMode = prefs.themeMode
        val dark = prefs.isDarkMode
        val balanceHidden = prefs.isBalanceHidden

        val filteredDebtsList = if (queryDebts.isBlank()) {
            debtors
        } else {
            val q = queryDebts.lowercase().trim()
            debtors.filter {
                it.name.lowercase().contains(q) || it.phoneNumber.contains(q)
            }
        }

        val filteredParcelsList = if (queryParcels.isBlank()) {
            parcels
        } else {
            val q = queryParcels.lowercase().trim()
            parcels.filter {
                it.recipientName.lowercase().contains(q) ||
                it.shopName.lowercase().contains(q) ||
                it.recipientAddress.lowercase().contains(q) ||
                it.samanName.lowercase().contains(q) ||
                it.itemDetails.lowercase().contains(q)
            }
        }

        val overdueList = debtors.filter { NotificationHelper.isOverdue(it) }

        // Dashboard Metrics
        val totalQarza = debtors.sumOf { it.totalDebt }
        val samanKePaise = parcels.filter { it.isPaid }.sumOf { it.itemPrice }

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        val currentDay = calendar.get(Calendar.DAY_OF_YEAR)

        val thisMonthRides = rides.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }

        val todayRides = rides.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            cal.get(Calendar.DAY_OF_YEAR) == currentDay && cal.get(Calendar.YEAR) == currentYear
        }

        val thisMonthParcelEarnings = parcels.filter {
            it.isPaid && it.deliveredTimestamp != null && run {
                val cal = Calendar.getInstance().apply { timeInMillis = it.deliveredTimestamp }
                cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
            }
        }.sumOf { it.deliveryCharges }

        val thisMonthRidesTotal = thisMonthRides.sumOf { it.fareAmount }
        val todayRidesTotal = todayRides.sumOf { it.fareAmount }
        val isMahineKiEarning = thisMonthRidesTotal

        RiderUiState(
            debtors = debtors,
            filteredDebtors = filteredDebtsList,
            overdueDebtors = overdueList,
            parcels = parcels,
            filteredParcels = filteredParcelsList,
            rides = rides,
            paymentHistory = history,
            customerHistory = customerHistory,
            searchQueryDebts = queryDebts,
            searchQueryParcels = queryParcels,
            totalQarza = totalQarza,
            samanKePaise = samanKePaise,
            isMahineKiEarning = isMahineKiEarning,
            todayRidesTotal = todayRidesTotal,
            thisMonthRidesTotal = thisMonthRidesTotal,
            themeMode = themeMode,
            isDarkMode = dark,
            isBalanceHidden = balanceHidden,
            selectedReportDateMillis = reportDate
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RiderUiState()
    )

    fun setSearchQueryDebts(query: String) {
        _searchQueryDebts.value = query
    }

    fun setSearchQueryParcels(query: String) {
        _searchQueryParcels.value = query
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("app_theme_mode", mode.name).apply()
    }

    fun toggleDarkMode() {
        val currentMode = _themeMode.value
        val entries = AppThemeMode.entries
        val nextIndex = (entries.indexOf(currentMode) + 1) % entries.size
        setThemeMode(entries[nextIndex])
    }

    fun toggleBalanceVisibility() {
        val nextVal = !_isBalanceHidden.value
        _isBalanceHidden.value = nextVal
        prefs.edit().putBoolean("hide_balance", nextVal).apply()
    }

    fun setSelectedReportDate(millis: Long) {
        _selectedReportDateMillis.value = millis
    }

    // Debts Actions
    fun addDebtor(name: String, phone: String, totalDebt: Double, note: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val existingDebtor = repository.allDebtors.first().find { it.name.equals(trimmedName, ignoreCase = true) }
            if (existingDebtor != null) {
                val updated = existingDebtor.copy(
                    totalDebt = existingDebtor.totalDebt + totalDebt,
                    phoneNumber = phone.ifBlank { existingDebtor.phoneNumber },
                    note = if (note.isBlank()) existingDebtor.note else "${existingDebtor.note} | $note",
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
                repository.updateDebtor(updated)
                repository.addCustomerHistory(
                    customerName = trimmedName,
                    phoneNumber = phone.ifBlank { existingDebtor.phoneNumber },
                    actionType = "Account",
                    title = "Updated Customer Account (+Rs. ${totalDebt.toInt()})",
                    details = "Total Due: Rs. ${updated.totalDebt.toInt()} | Note: ${note.ifBlank { "Account update" }}",
                    amount = totalDebt
                )
            } else {
                val debtor = DebtorEntity(
                    name = trimmedName,
                    phoneNumber = phone.trim(),
                    totalDebt = totalDebt,
                    note = note.trim()
                )
                repository.addDebtor(debtor)
                repository.addCustomerHistory(
                    customerName = trimmedName,
                    phoneNumber = phone.trim(),
                    actionType = "Account",
                    title = "New Customer Created (Due: Rs. ${totalDebt.toInt()})",
                    details = "Initial Due: Rs. ${totalDebt.toInt()}${if (note.isNotBlank()) " | $note" else ""}",
                    amount = totalDebt
                )
            }
        }
    }

    fun recordDebtPayment(debtor: DebtorEntity, amountPaid: Double, note: String) {
        viewModelScope.launch {
            repository.recordDebtPayment(debtor, amountPaid, note)
            repository.addCustomerHistory(
                customerName = debtor.name,
                phoneNumber = debtor.phoneNumber,
                actionType = "Payment",
                title = "Payment Received (Rs. ${amountPaid.toInt()})",
                details = "Remaining Balance: Rs. ${(debtor.totalDebt - amountPaid).coerceAtLeast(0.0).toInt()} | ${note.ifBlank { "Wasooli recorded" }}",
                amount = amountPaid
            )
        }
    }

    fun deleteDebtor(debtor: DebtorEntity) {
        viewModelScope.launch {
            repository.deleteDebtor(debtor)
            repository.addCustomerHistory(
                customerName = debtor.name,
                phoneNumber = debtor.phoneNumber,
                actionType = "Account",
                title = "Customer Ledger Deleted",
                details = "Customer ${debtor.name} ledger removed from active accounts",
                amount = debtor.totalDebt
            )
        }
    }

    // Parcel Actions - Auto bill + addition for customer
    fun addParcel(
        shopName: String,
        recipientName: String,
        recipientAddress: String,
        itemDetails: String,
        itemPrice: Double,
        deliveryCharges: Double,
        samanName: String = "",
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            val trimmedRecipient = recipientName.trim()
            val totalParcelCharges = itemPrice + deliveryCharges
            val effectiveSamanName = if (samanName.isNotBlank()) samanName.trim() else itemDetails.trim()

            val parcel = ParcelEntity(
                shopName = shopName.trim(),
                recipientName = trimmedRecipient,
                recipientAddress = recipientAddress.trim(),
                samanName = effectiveSamanName,
                itemDetails = itemDetails.trim(),
                itemPrice = itemPrice,
                deliveryCharges = deliveryCharges,
                imageUri = imageUri
            )
            repository.addParcel(parcel)

            // Auto addition to Customer's Bill / Debt Ledger & History
            if (trimmedRecipient.isNotBlank()) {
                val existingDebtor = repository.allDebtors.first().find { it.name.equals(trimmedRecipient, ignoreCase = true) }
                if (existingDebtor != null) {
                    val updated = existingDebtor.copy(
                        totalDebt = existingDebtor.totalDebt + totalParcelCharges,
                        lastUpdatedTimestamp = System.currentTimeMillis(),
                        note = if (existingDebtor.note.isBlank()) "Parcel: $effectiveSamanName" else "${existingDebtor.note} | Saman: $effectiveSamanName"
                    )
                    repository.updateDebtor(updated)
                } else if (totalParcelCharges > 0) {
                    val newDebtor = DebtorEntity(
                        name = trimmedRecipient,
                        phoneNumber = "",
                        totalDebt = totalParcelCharges,
                        note = "Saman: $effectiveSamanName ($shopName)",
                        createdTimestamp = System.currentTimeMillis(),
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                    repository.addDebtor(newDebtor)
                }

                repository.addCustomerHistory(
                    customerName = trimmedRecipient,
                    phoneNumber = "",
                    actionType = "Parcel",
                    title = "Parcel Booked: $effectiveSamanName",
                    details = "Shop: $shopName | Address: $recipientAddress | Price: Rs. ${itemPrice.toInt()} + Del: Rs. ${deliveryCharges.toInt()}",
                    amount = totalParcelCharges,
                    imageUri = imageUri
                )
            }
        }
    }

    fun toggleParcelDelivered(parcel: ParcelEntity) {
        viewModelScope.launch {
            val updated = parcel.copy(
                isDelivered = !parcel.isDelivered,
                deliveredTimestamp = if (!parcel.isDelivered) System.currentTimeMillis() else parcel.deliveredTimestamp
            )
            repository.updateParcel(updated)
            if (parcel.recipientName.isNotBlank()) {
                val sName = if (parcel.samanName.isNotBlank()) parcel.samanName else parcel.itemDetails
                repository.addCustomerHistory(
                    customerName = parcel.recipientName,
                    phoneNumber = "",
                    actionType = "Parcel",
                    title = if (updated.isDelivered) "Parcel Delivered: $sName" else "Parcel Delivery Pending",
                    details = "Shop: ${parcel.shopName} | Destination: ${parcel.recipientAddress}",
                    amount = parcel.itemPrice + parcel.deliveryCharges,
                    imageUri = parcel.imageUri
                )
            }
        }
    }

    fun toggleParcelPaid(parcel: ParcelEntity) {
        viewModelScope.launch {
            val willBePaid = !parcel.isPaid
            val updated = parcel.copy(
                isPaid = willBePaid,
                deliveredTimestamp = if (willBePaid && parcel.deliveredTimestamp == null) System.currentTimeMillis() else parcel.deliveredTimestamp
            )
            repository.updateParcel(updated)

            // Automatic ledger sync on payment state change
            val totalParcelCharges = parcel.itemPrice + parcel.deliveryCharges
            if (parcel.recipientName.isNotBlank() && totalParcelCharges > 0) {
                val existingDebtor = repository.allDebtors.first().find { it.name.equals(parcel.recipientName, ignoreCase = true) }
                if (existingDebtor != null) {
                    if (willBePaid) {
                        // Customer paid for parcel: deduct from total debt
                        repository.recordDebtPayment(
                            existingDebtor,
                            totalParcelCharges,
                            "Parcel Payment Cleared (${parcel.itemDetails})"
                        )
                    } else {
                        // Customer payment unmarked: add back to bill
                        val reAdded = existingDebtor.copy(
                            totalDebt = existingDebtor.totalDebt + totalParcelCharges,
                            lastUpdatedTimestamp = System.currentTimeMillis()
                        )
                        repository.updateDebtor(reAdded)
                    }
                }

                repository.addCustomerHistory(
                    customerName = parcel.recipientName,
                    phoneNumber = "",
                    actionType = "Parcel",
                    title = if (willBePaid) "Parcel Payment Cleared (Rs. ${totalParcelCharges.toInt()})" else "Parcel Payment Pending",
                    details = "Item: ${parcel.itemDetails} | Shop: ${parcel.shopName}",
                    amount = totalParcelCharges,
                    imageUri = parcel.imageUri
                )
            }
        }
    }

    fun deleteParcel(parcel: ParcelEntity) {
        viewModelScope.launch {
            repository.deleteParcel(parcel)
            if (parcel.recipientName.isNotBlank()) {
                repository.addCustomerHistory(
                    customerName = parcel.recipientName,
                    phoneNumber = "",
                    actionType = "Parcel",
                    title = "Parcel Order Deleted",
                    details = "Item: ${parcel.itemDetails} | Shop: ${parcel.shopName}",
                    amount = parcel.itemPrice + parcel.deliveryCharges
                )
            }
        }
    }

    // Ride Actions - Auto bill + addition if customer specified
    fun addRide(
        fromLocation: String,
        toLocation: String,
        distanceKm: Double,
        fareAmount: Double,
        timeString: String,
        note: String,
        customerName: String = ""
    ) {
        viewModelScope.launch {
            val rideNote = if (customerName.isNotBlank()) "Customer: ${customerName.trim()} | ${note.trim()}".trim() else note.trim()
            val ride = RideEntity(
                fromLocation = fromLocation.trim(),
                toLocation = toLocation.trim(),
                distanceKm = distanceKm,
                fareAmount = fareAmount,
                timeString = timeString.ifEmpty {
                    val cal = Calendar.getInstance()
                    String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                },
                note = rideNote
            )
            repository.addRide(ride)

            // Auto bill addition if passenger/customer name is specified
            val trimmedCustomer = customerName.trim()
            if (trimmedCustomer.isNotBlank()) {
                if (fareAmount > 0) {
                    val existingDebtor = repository.allDebtors.first().find { it.name.equals(trimmedCustomer, ignoreCase = true) }
                    if (existingDebtor != null) {
                        val updated = existingDebtor.copy(
                            totalDebt = existingDebtor.totalDebt + fareAmount,
                            lastUpdatedTimestamp = System.currentTimeMillis(),
                            note = if (existingDebtor.note.isBlank()) "Ride: $fromLocation to $toLocation" else "${existingDebtor.note} | Ride: $fromLocation -> $toLocation"
                        )
                        repository.updateDebtor(updated)
                    } else {
                        val newDebtor = DebtorEntity(
                            name = trimmedCustomer,
                            phoneNumber = "",
                            totalDebt = fareAmount,
                            note = "Ride: $fromLocation -> $toLocation",
                            createdTimestamp = System.currentTimeMillis(),
                            lastUpdatedTimestamp = System.currentTimeMillis()
                        )
                        repository.addDebtor(newDebtor)
                    }
                }

                repository.addCustomerHistory(
                    customerName = trimmedCustomer,
                    phoneNumber = "",
                    actionType = "Ride",
                    title = "Ride: $fromLocation ➔ $toLocation",
                    details = "Distance: $distanceKm KM | Fare: Rs. ${fareAmount.toInt()}${if (note.isNotBlank()) " | Note: $note" else ""}",
                    amount = fareAmount
                )
            }
        }
    }

    fun updateDebtor(debtor: DebtorEntity) {
        viewModelScope.launch {
            repository.updateDebtor(debtor)
            repository.addCustomerHistory(
                customerName = debtor.name,
                phoneNumber = debtor.phoneNumber,
                actionType = "Account",
                title = "Customer Info Updated",
                details = "Phone: ${debtor.phoneNumber.ifBlank { "N/A" }} | Debt: Rs. ${debtor.totalDebt.toInt()} | Note: ${debtor.note}",
                amount = debtor.totalDebt
            )
        }
    }

    fun updateRide(ride: RideEntity) {
        viewModelScope.launch {
            repository.updateRide(ride)
        }
    }

    fun updateParcel(parcel: ParcelEntity) {
        viewModelScope.launch {
            repository.updateParcel(parcel)
        }
    }

    fun updatePayment(payment: PaymentHistoryEntity, oldAmountPaid: Double? = null) {
        viewModelScope.launch {
            repository.updatePayment(payment)
            if (oldAmountPaid != null && oldAmountPaid != payment.amountPaid) {
                val debtor = repository.allDebtors.first().find { it.id == payment.debtorId || it.name.equals(payment.debtorName, ignoreCase = true) }
                if (debtor != null) {
                    val diff = oldAmountPaid - payment.amountPaid
                    val newDebt = (debtor.totalDebt + diff).coerceAtLeast(0.0)
                    val updated = debtor.copy(
                        totalDebt = newDebt,
                        lastUpdatedTimestamp = System.currentTimeMillis()
                    )
                    repository.updateDebtor(updated)
                }
            }
            repository.addCustomerHistory(
                customerName = payment.debtorName,
                phoneNumber = "",
                actionType = "Payment",
                title = "Payment Record Updated (Rs. ${payment.amountPaid.toInt()})",
                details = "Type: ${payment.paymentType} | Note: ${payment.note}",
                amount = payment.amountPaid
            )
        }
    }

    fun deletePayment(payment: PaymentHistoryEntity) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            val debtor = repository.allDebtors.first().find { it.id == payment.debtorId || it.name.equals(payment.debtorName, ignoreCase = true) }
            if (debtor != null) {
                val updated = debtor.copy(
                    totalDebt = debtor.totalDebt + payment.amountPaid,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
                repository.updateDebtor(updated)
            }
            repository.addCustomerHistory(
                customerName = payment.debtorName,
                phoneNumber = "",
                actionType = "Payment",
                title = "Payment Removed (+Rs. ${payment.amountPaid.toInt()} added back to due)",
                details = "Removed record of Rs. ${payment.amountPaid.toInt()}",
                amount = payment.amountPaid
            )
        }
    }

    fun addRideForDebtor(
        debtor: DebtorEntity,
        fromLocation: String,
        toLocation: String,
        distanceKm: Double,
        fareAmount: Double,
        timeString: String,
        note: String
    ) {
        viewModelScope.launch {
            val ride = RideEntity(
                debtorId = debtor.id,
                fromLocation = fromLocation.trim(),
                toLocation = toLocation.trim(),
                distanceKm = distanceKm,
                fareAmount = fareAmount,
                timeString = timeString.ifEmpty {
                    val cal = Calendar.getInstance()
                    String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                },
                note = "Customer: ${debtor.name} | ${note.trim()}".trim()
            )
            repository.addRide(ride)

            repository.addCustomerHistory(
                customerName = debtor.name,
                phoneNumber = debtor.phoneNumber,
                actionType = "Ride",
                title = "Ride: $fromLocation ➔ $toLocation",
                details = "Distance: $distanceKm KM | Fare: Rs. ${fareAmount.toInt()}",
                amount = fareAmount
            )
        }
    }

    fun addParcelForDebtor(
        debtor: DebtorEntity,
        shopName: String,
        recipientAddress: String,
        itemDetails: String,
        itemPrice: Double,
        deliveryCharges: Double,
        samanName: String = "",
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            val effectiveSamanName = if (samanName.isNotBlank()) samanName.trim() else itemDetails.trim()
            val parcel = ParcelEntity(
                debtorId = debtor.id,
                shopName = shopName.trim().ifBlank { "Dukan" },
                recipientName = debtor.name,
                recipientAddress = recipientAddress.trim(),
                samanName = effectiveSamanName,
                itemDetails = itemDetails.trim(),
                itemPrice = itemPrice,
                deliveryCharges = deliveryCharges,
                imageUri = imageUri
            )
            repository.addParcel(parcel)

            val total = itemPrice + deliveryCharges
            repository.addCustomerHistory(
                customerName = debtor.name,
                phoneNumber = debtor.phoneNumber,
                actionType = "Parcel",
                title = "Parcel: $effectiveSamanName",
                details = "Shop: $shopName | Price: Rs. ${itemPrice.toInt()} + Del: Rs. ${deliveryCharges.toInt()}",
                amount = total,
                imageUri = imageUri
            )
        }
    }

    fun deleteRide(ride: RideEntity) {
        viewModelScope.launch {
            repository.deleteRide(ride)
        }
    }

    // Customer History Management
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
            repository.deleteCustomerHistoryByIds(ids)
        }
    }

    fun deleteAllCustomerHistory() {
        viewModelScope.launch {
            repository.deleteAllCustomerHistory()
        }
    }

    // Auto Distance Calculation
    fun calculateKm(from: String, to: String): Double {
        return DistanceCalculator.estimateDistanceKm(from, to)
    }

    // PDF & Sharing
    fun generateAndSharePdfReport(
        context: Context,
        hideSamanTotal: Boolean = false,
        hideRideCharges: Boolean = false,
        customTitle: String = "AQEEL RIDER / عقیل رائڈر",
        customSubtitle: String = "",
        customNote: String = "",
        includeRides: Boolean = true,
        includeParcels: Boolean = true,
        includeDebtors: Boolean = true
    ) {
        val state = uiState.value
        val pdfFile: File = PdfReportGenerator.generatePdfReport(
            context = context,
            selectedDateMillis = state.selectedReportDateMillis,
            rides = state.rides,
            parcels = state.parcels,
            debtors = state.debtors,
            hideSamanTotal = hideSamanTotal,
            hideRideCharges = hideRideCharges,
            customTitle = customTitle,
            customSubtitle = customSubtitle,
            customNote = customNote,
            includeRides = includeRides,
            includeParcels = includeParcels,
            includeDebtors = includeDebtors
        )
        ShareUtil.sharePdfReport(context, pdfFile)
    }

    fun shareSummaryText(context: Context) {
        val state = uiState.value
        ShareUtil.shareSummaryText(
            context = context,
            selectedDateMillis = state.selectedReportDateMillis,
            rides = state.rides,
            parcels = state.parcels,
            debtors = state.debtors
        )
    }

    fun sendDebtorReminder(context: Context, debtor: DebtorEntity) {
        ShareUtil.sendWhatsAppReminderToDebtor(context, debtor)
    }
}

