package com.example

import com.example.data.entity.ParcelEntity
import com.example.data.entity.RideEntity
import com.example.data.repository.RiderRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Repository-level regression tests for the Ride/Saman Bakaya & Dashboard balance
 * calculation fix: [RiderRepository] now always recomputes remainingBakaya /
 * paymentStatus itself from fare (or Saman+Duty charges) and amountPaid, instead of
 * trusting whatever value the caller (ViewModel/UI) happened to pass in. These tests
 * run against an in-memory [FakeRiderDao] so they exercise the real transaction logic
 * without needing Room or the Android runtime.
 */
class RiderRepositoryTest {

    private fun newRepository(): Pair<RiderRepository, FakeRiderDao> {
        val dao = FakeRiderDao()
        // database = null makes runInTransaction fall back to a plain suspend block,
        // which is fine for these single-threaded in-memory tests.
        return RiderRepository(dao, null) to dao
    }

    @Test
    fun addRide_newCustomer_dutyChargesFullyReflectInBakaya() = runBlocking {
        val (repo, dao) = newRepository()

        repo.addRide(
            RideEntity(
                customerName = "Ali",
                phone = "0300",
                pickupLocation = "A",
                dropoffLocation = "B",
                fare = 1000.0,
                amountPaid = 0.0
            )
        )

        val debtor = dao.debtors.values.first()
        assertEquals(1000.0, debtor.remainingDebt, 0.01)
        assertEquals(1000.0, debtor.totalDebt, 0.01)
        assertEquals(1000.0, dao.rides.values.first().remainingBakaya, 0.01)
    }

    @Test
    fun addRide_ignoresStaleRemainingBakayaPassedByCaller() = runBlocking {
        // Regression test for the exact bug being fixed: even if a caller mistakenly
        // constructs a RideEntity with a stale/incorrect remainingBakaya (e.g. 0 while
        // fare is unpaid), the repository must still derive the correct Bakaya from
        // fare - amountPaid so the customer's Dashboard balance is always correct.
        val (repo, dao) = newRepository()

        repo.addRide(
            RideEntity(
                customerName = "Kamran",
                phone = "0311",
                pickupLocation = "A",
                dropoffLocation = "B",
                fare = 1000.0,
                amountPaid = 0.0,
                remainingBakaya = 0.0 // intentionally wrong/stale value
            )
        )

        val debtor = dao.debtors.values.first()
        assertEquals(1000.0, debtor.remainingDebt, 0.01)
        assertEquals(1000.0, dao.rides.values.first().remainingBakaya, 0.01)
    }

    @Test
    fun updateRide_editingFare_reversesOldAndAppliesNewBakaya() = runBlocking {
        val (repo, dao) = newRepository()

        val rideId = repo.addRide(
            RideEntity(
                customerName = "Ali",
                phone = "0300",
                pickupLocation = "A",
                dropoffLocation = "B",
                fare = 1000.0,
                amountPaid = 0.0
            )
        )
        assertEquals(1000.0, dao.debtors.values.first().remainingDebt, 0.01)

        // Duty Charges corrected upward from 1000 -> 1200
        val savedRide = dao.rides.getValue(rideId)
        repo.updateRide(savedRide.copy(fare = 1200.0))

        val debtorAfterIncrease = dao.debtors.values.first()
        assertEquals(1200.0, debtorAfterIncrease.remainingDebt, 0.01)
        assertEquals(1200.0, dao.rides.getValue(rideId).remainingBakaya, 0.01)

        // Duty Charges corrected downward from 1200 -> 800
        repo.updateRide(dao.rides.getValue(rideId).copy(fare = 800.0))
        val debtorAfterDecrease = dao.debtors.values.first()
        assertEquals(800.0, debtorAfterDecrease.remainingDebt, 0.01)
        assertEquals(800.0, dao.rides.getValue(rideId).remainingBakaya, 0.01)
    }

    @Test
    fun updateRide_ignoresStaleRemainingBakayaPassedByCaller() = runBlocking {
        val (repo, dao) = newRepository()
        val rideId = repo.addRide(
            RideEntity(customerName = "Ali", phone = "0300", pickupLocation = "A", dropoffLocation = "B", fare = 1000.0, amountPaid = 0.0)
        )

        // Caller edits fare to 1500 but (bug scenario) still carries an old/incorrect
        // remainingBakaya of 1000 on the entity passed in - repository must ignore it.
        repo.updateRide(dao.rides.getValue(rideId).copy(fare = 1500.0, remainingBakaya = 1000.0))

        assertEquals(1500.0, dao.debtors.values.first().remainingDebt, 0.01)
        assertEquals(1500.0, dao.rides.getValue(rideId).remainingBakaya, 0.01)
    }

    @Test
    fun deleteRide_removesCorrespondingBakayaFromDashboard() = runBlocking {
        val (repo, dao) = newRepository()
        val rideId = repo.addRide(
            RideEntity(customerName = "Ali", phone = "0300", pickupLocation = "A", dropoffLocation = "B", fare = 1000.0, amountPaid = 0.0)
        )
        assertEquals(1000.0, dao.debtors.values.first().remainingDebt, 0.01)

        repo.deleteRide(dao.rides.getValue(rideId))

        assertEquals(0.0, dao.debtors.values.first().remainingDebt, 0.01)
        assertEquals(0, dao.rides.size)
    }

    @Test
    fun addParcel_newCustomer_samanAndDutyChargesFullyReflectInBakaya() = runBlocking {
        val (repo, dao) = newRepository()

        repo.addParcel(
            ParcelEntity(
                senderName = "Bilal",
                senderPhone = "0333",
                receiverName = "R",
                receiverPhone = "0344",
                pickupAddress = "A",
                deliveryAddress = "B",
                samanCharges = 500.0,
                deliveryCharges = 200.0,
                amountPaid = 0.0
            )
        )

        val debtor = dao.debtors.values.first()
        assertEquals(700.0, debtor.remainingDebt, 0.01)
        assertEquals(700.0, dao.parcels.values.first().remainingBakaya, 0.01)
    }

    @Test
    fun updateParcel_editingCharges_reversesOldAndAppliesNewBakaya() = runBlocking {
        val (repo, dao) = newRepository()
        val parcelId = repo.addParcel(
            ParcelEntity(
                senderName = "Bilal", senderPhone = "0333", receiverName = "R", receiverPhone = "0344",
                pickupAddress = "A", deliveryAddress = "B", samanCharges = 500.0, deliveryCharges = 200.0, amountPaid = 0.0
            )
        )
        assertEquals(700.0, dao.debtors.values.first().remainingDebt, 0.01)

        repo.updateParcel(dao.parcels.getValue(parcelId).copy(samanCharges = 900.0))

        assertEquals(1100.0, dao.debtors.values.first().remainingDebt, 0.01)
        assertEquals(1100.0, dao.parcels.getValue(parcelId).remainingBakaya, 0.01)
    }

    @Test
    fun deleteParcel_removesCorrespondingBakayaFromDashboard() = runBlocking {
        val (repo, dao) = newRepository()
        val parcelId = repo.addParcel(
            ParcelEntity(
                senderName = "Bilal", senderPhone = "0333", receiverName = "R", receiverPhone = "0344",
                pickupAddress = "A", deliveryAddress = "B", samanCharges = 500.0, deliveryCharges = 200.0, amountPaid = 0.0
            )
        )
        assertEquals(700.0, dao.debtors.values.first().remainingDebt, 0.01)

        repo.deleteParcel(dao.parcels.getValue(parcelId))

        assertEquals(0.0, dao.debtors.values.first().remainingDebt, 0.01)
        assertEquals(0, dao.parcels.size)
    }

    @Test
    fun rideThenParcel_sameCustomer_noDoubleCounting() = runBlocking {
        // Ensures Ride Duty Charges + Saman/Parcel charges both land on the same
        // customer's balance without double-counting or clobbering each other.
        val (repo, dao) = newRepository()

        repo.addRide(
            RideEntity(customerName = "Ali", phone = "0300", pickupLocation = "A", dropoffLocation = "B", fare = 1000.0, amountPaid = 0.0)
        )
        val debtorId = dao.debtors.values.first().id

        repo.addParcel(
            ParcelEntity(
                customerId = debtorId, senderName = "Ali", senderPhone = "0300", receiverName = "R", receiverPhone = "0344",
                pickupAddress = "A", deliveryAddress = "B", samanCharges = 300.0, deliveryCharges = 100.0, amountPaid = 0.0
            )
        )

        assertEquals(1, dao.debtors.size)
        assertEquals(1400.0, dao.debtors.getValue(debtorId).remainingDebt, 0.01)

        // Recording a payment (Wasooli) against the same customer must reduce the
        // combined balance and must not touch the ride/parcel Bakaya fields themselves.
        repo.addPayment(debtorId, 400.0, "Partial recovery")
        assertEquals(1000.0, dao.debtors.getValue(debtorId).remainingDebt, 0.01)
    }
}
