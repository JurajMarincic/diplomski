package com.example.juraj_diplomski

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

class TicketManager {

    private val firestore = FirebaseFirestore.getInstance()
    private val ticketCollection = firestore.collection("tickets")
    private val counterDocument = firestore.collection("utilities").document("counter")
    private var counter = 0


    init {
        initializeCounter()
    }

    private fun initializeCounter() {
        counterDocument.get().addOnSuccessListener { document ->
            if (document.exists()) {
                counter = document.getLong("value")?.toInt() ?: 0
            } else {
                counter = 0
                counterDocument.set(mapOf("value" to counter))
            }
        }
    }

    private fun updateCounter(newValue: Int) {
        counterDocument.set(mapOf("value" to newValue))
    }

    suspend fun getCounter(): Int {
            val counterSnapshot = counterDocument.get().await()
            return counterSnapshot.getLong("value")?.toInt() ?: 0
    }

    suspend fun addTicket(ticket: Ticket) {

            val currentCounter = counterDocument.get().await().getLong("value")?.toInt() ?: 0

            val ticketMap = mapOf(
                "id" to ticket.id,
                "name" to ticket.name,
                "departureTime" to ticket.departureTime,
                "arrivalTime" to ticket.arrivalTime,
                "price" to ticket.price
            )

            ticketCollection.document(currentCounter.toString()).set(ticketMap).await()
            updateCounter(currentCounter + 1)
    }

    fun removeTicket(ticketId: String) {

        val docRef = firestore.collection("tickets").document(ticketId)

        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                docRef.delete()
            }
        }
    }

    suspend fun fetchTickets(callback: (List<Ticket>) -> Unit) {
            val snapshot = ticketCollection.get().await()
            val tickets = snapshot.documents.mapNotNull { document ->
                val data = document.data
                if (data != null) {
                    Ticket(
                        id =  data["id"] as? String ?: "",
                        name = data["name"] as? String ?: "",
                        departureTime = data["departureTime"] as? String ?: "",
                        arrivalTime = data["arrivalTime"] as? String ?: "",
                        price = (((data["price"] as? Number)?.toDouble()
                            ?: 0.0) * 10).roundToInt() / 10.0
                    )
                } else {
                    null
                }
            }
            callback(tickets)
    }

    suspend fun fetchPurchasedTickets(userId: String, callback: (List<Ticket>) -> Unit) {
            val userTicketsRef = firestore.collection("users").document(userId).collection("purchased_tickets")
            val snapshot = userTicketsRef.get().await()
            val tickets = snapshot.documents.mapNotNull { document ->
                val data = document.data
                if (data != null) {
                    Ticket(
                        id = data["id"] as? String ?: "",
                        name = data["name"] as? String ?: "",
                        departureTime = data["departureTime"] as? String ?: "",
                        arrivalTime = data["arrivalTime"] as? String ?: "",
                        price = (data["price"] as? Number)?.toDouble() ?: 0.0
                    )
                } else {
                    null
                }
            }
            callback(tickets)
    }

    fun buyTicket(ticket: Ticket) {

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {

                val userDocRef = firestore.collection("users").document(currentUser.uid)
                val purchasedTicketsCollection = userDocRef.collection("purchased_tickets")

                val ticketMap = mapOf(
                    "id" to ticket.id,
                    "name" to ticket.name,
                    "departureTime" to ticket.departureTime,
                    "arrivalTime" to ticket.arrivalTime,
                    "price" to ticket.price
                )
                purchasedTicketsCollection.document(ticket.id).set(ticketMap)
            }
    }
}

data class Ticket(
    val id: String,
    val name: String,
    val departureTime: String,
    val arrivalTime: String,
    val price: Double
)