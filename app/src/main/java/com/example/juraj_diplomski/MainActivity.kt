package com.example.juraj_diplomski

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.funkatronics.kborsh.Borsh
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.solana.mobilewalletadapter.clientlib.*
import com.solana.programs.SystemProgram
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.solana.serialization.AnchorInstructionSerializer
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        val sender = ActivityResultSender(this)
        setContent {
            val ticketManager = remember { TicketManager() }
            var navController = rememberNavController()
            NavHost(navController = navController, startDestination = "mainScreen") {
                composable("mainScreen") { MainLazyColumn(navController) }
                composable("ScreenLogin") {
                    LoginScreen(navController)
                }
                composable("ScreenRegister") {
                    RegistrationScreen(navController)
                }
                composable("LoggedUserScreen") {
                    LoggedUserScreen(navController)
                }
                composable("AdminScreen") {
                    AdminScreen(navController)
                }
                composable("ScreenAddTicket") {
                    AddTicketScreen(navController = navController, ticketManager = ticketManager)
                }
                composable("ScreenTicket") {
                    TicketScreen(navController = navController, ticketManager = ticketManager, sender = sender)
                }
                composable("ScreenMyTickets") {
                    MyTicketsScreen(navController = navController, ticketManager = ticketManager)
                }
                composable("ScreenRemoveTicket") {
                    RemoveTicketScreen(navController = navController, ticketManager = ticketManager)
                }
            }
        }
    }
}

@Composable
fun MainButton(name: String, navController: NavController, destination: String) {

    Button(
        onClick = {
            navController.navigate("Screen$destination")
        },
        modifier = Modifier.padding(24.dp)
    ) {
        Text(text = name)
    }
}

@Composable
fun LogOutButton(navController: NavController) {
    val auth = Firebase.auth
    Button(
        onClick = {
            auth.signOut()
            navController.navigate("mainScreen")
        },
        modifier = Modifier.padding(24.dp)
    ) {
        Text(text = "Log Out")
    }
}

@Composable
fun MainLazyColumn(navController: NavController) {

    val buttonNames =
        listOf(
            "Login",
            "Register"
        )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF89CFF0)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = "Travel and pay safe",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 10.dp, top = 80.dp)
            )
        }
        item {
            Text(
                text = "with Solana",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 40.dp)
            )
        }

        items(buttonNames.size) { index ->
            MainButton(
                buttonNames[index],
                navController,
                buttonNames[index],
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF89CFF0)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Email Text
        Text(
            text = "Email:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        // Email TextField
        TextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Enter your email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(color = Color.White)
        )

        // Password Text
        Text(
            text = "Password:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        // Password TextField
        TextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Enter your password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(color = Color.White)
        )

        Button(
            onClick = {
                val activity = context as? Activity

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(activity!!) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            user?.let {
                                val docRef = firestore.collection("users").document(user.uid)
                                docRef.get()
                                    .addOnSuccessListener { document ->
                                        if (document != null && document.exists()) {
                                            val role = document.getString("role")
                                            if (role == "admin") {
                                                navController.navigate("AdminScreen")
                                            } else {
                                                navController.navigate("LoggedUserScreen")
                                            }
                                        }
                                    }
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Login failed: " + (task.exception?.message ?: "Unknown error"),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
            },
            modifier = Modifier
                .padding(top = 16.dp)
        ) {
            Text(
                text = "Login",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val auth = Firebase.auth
    val firestore = FirebaseFirestore.getInstance()

    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Color(0xFF89CFF0)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Email Text
        Text(
            text = "Email:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        // Email TextField
        TextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Enter your email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(color = Color.White)
        )

        // Password Text
        Text(
            text = "Password:",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        // Password TextField
        TextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Enter your password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(color = Color.White)
        )

        Button(
            onClick = {
                val activity = context as? Activity

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(activity!!) { task ->
                        if (task.isSuccessful) {
                            val role = if (email == "admin@admin.com") "admin" else "user"
                            val user = auth.currentUser
                            user?.let {
                                val userInfo = hashMapOf(
                                    "email" to email,
                                    "role" to role
                                )
                                firestore.collection("users")
                                    .document(user.uid)
                                    .set(userInfo)
                                    .addOnSuccessListener {
                                        navController.navigate("LoggedUserScreen")
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            context,
                                            "Failed to save user data.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Authentication failed.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
            },
            modifier = Modifier.padding(24.dp)
        ) {
            Text("Register")
        }
    }
}

@Composable
fun LoggedUserScreen(navController: NavController) {

    // Updated button names
    val buttonNames = listOf(
        "Buy a Ticket",
        "My Tickets"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF89CFF0)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = "Welcome to the Ticket App!",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 10.dp, top = 80.dp)
            )
        }
        item {
            Text(
                text = "Choose an option below",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 40.dp)
            )
        }

        // Iterate through the updated buttonNames
        items(buttonNames.size) { index ->
            MainButton(
                buttonNames[index],
                navController,
                if(index == 0) "Ticket" else "MyTickets"
            )
        }
        item()
        {
            LogOutButton(navController)
        }
    }
}

@Composable
fun AdminScreen(navController: NavController) {

    val buttonNames = listOf(
        "Add a ticket",
        "Remove a ticket"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF89CFF0)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        item {
            Text(
                text = "Welcome admin!",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 10.dp, top = 80.dp)
            )
        }

        items(buttonNames.size) { index ->
            MainButton(
                buttonNames[index],
                navController,
                if(index == 0) "AddTicket" else "RemoveTicket"
            )
        }

        item(){
            Button(
                onClick = {
                    navController.navigate("ScreenTicket")
                },
                modifier = Modifier.padding(24.dp)
            ) {
                Text("Tickets")
            }
        }

        item()
        {
            LogOutButton(navController)
        }
    }
}

@Composable
fun TicketScreen(navController: NavController, ticketManager: TicketManager,sender: ActivityResultSender) {

    var tickets by remember { mutableStateOf<List<Ticket>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ticketManager.fetchTickets { fetchedTickets ->
            tickets = fetchedTickets
        }
    }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0xFF89CFF0))
        ) {
            items(tickets.size) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "ID: ${tickets[index].id}", color = Color.White, fontSize = 18.sp)
                        Text(text = "Name: ${tickets[index].name}", color = Color.White, fontSize = 18.sp)
                        Text(text = "Departure: ${tickets[index].departureTime}", color = Color.White, fontSize = 18.sp)
                        Text(text = "Arrival: ${tickets[index].arrivalTime}", color = Color.White, fontSize = 18.sp)
                        Text(text = "Price: ${tickets[index].price} SOL", color = Color.White, fontSize = 18.sp)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                    onBuyTicket(tickets[index],sender)
                                    //ticketManager.buyTicket(tickets[index])
                            }
                        },
                        modifier = Modifier
                            .padding(start = 16.dp)
                    ) {
                        Text("Buy the ticket")
                    }
                }
                Divider(color = Color.Black, thickness = 2.dp)
            }
        }
}

@Composable
fun MyTicketsScreen(navController: NavController, ticketManager: TicketManager) {
    var purchasedTickets by remember { mutableStateOf<List<Ticket>>(emptyList()) }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid

    LaunchedEffect(Unit) {
        if (userId != null) {
            ticketManager.fetchPurchasedTickets(userId) { fetchedTickets ->
                purchasedTickets = fetchedTickets
            }
        }
    }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0xFF89CFF0))
        ) {
            items(purchasedTickets.size) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "ID: ${purchasedTickets[index].id}", color = Color.White, fontSize = 18.sp)
                        Text(text = "Name: ${purchasedTickets[index].name}", color = Color.White, fontSize = 18.sp)
                        Text(text = "Departure: ${purchasedTickets[index].departureTime}", color = Color.White, fontSize = 18.sp)
                        Text(text = "Arrival: ${purchasedTickets[index].arrivalTime}", color = Color.White, fontSize = 18.sp)
                        Text(text = "Price: ${purchasedTickets[index].price} SOL", color = Color.White, fontSize = 18.sp)
                    }
                }
                Divider(color = Color.Black, thickness = 2.dp)
            }
        }
}

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTicketScreen(navController: NavController, ticketManager: TicketManager) {
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var departureTime by remember { mutableStateOf(TextFieldValue("")) }
    var arrivalTime by remember { mutableStateOf(TextFieldValue("")) }
    var price by remember { mutableStateOf(TextFieldValue("")) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .background(Color(0xFF89CFF0))
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Ticket Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        TextField(
            value = departureTime,
            onValueChange = { departureTime = it },
            label = { Text("Departure Time") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        TextField(
            value = arrivalTime,
            onValueChange = { arrivalTime = it },
            label = { Text("Arrival Time") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        TextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Ticket Price") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val ticketPrice = price.text.toFloat()

                    coroutineScope.launch {
                            val currentCounter = ticketManager.getCounter()
                            val newTicket = Ticket(
                                id = currentCounter.toString(),
                                name = name.text,
                                departureTime = departureTime.text,
                                arrivalTime = arrivalTime.text,
                                price = ticketPrice
                            )
                            ticketManager.addTicket(newTicket)
                            navController.navigate("ScreenTicket")
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Ticket")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveTicketScreen(navController: NavController, ticketManager: TicketManager) {

    var id by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .background(Color(0xFF89CFF0))
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = id,
            onValueChange = { id = it },
            label = { Text("ID of the ticket to be removed") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                val ticketId = id.text.trim()
                if (ticketId.isNotEmpty()) {
                    ticketManager.removeTicket(ticketId)
                    navController.navigate("AdminScreen")
                } else {
                    Log.w("RemoveTicketScreen", "Ticket ID is empty")
                }
            },
            modifier = Modifier.padding(24.dp)
        ) {
            Text("Remove the ticket")
        }
    }
}

suspend fun onBuyTicket(ticket: Ticket, sender: ActivityResultSender) {

        Log.d("TicketDApp", "Starting to buy ticket: ${ticket.id}")

        val programId = SolanaPublicKey.from("F75bTjnaqScc9VZz6p5dKxFyxdBNQ48g7UURVZCwTSyH")

        val solanaUri = Uri.parse("https://juraj_diplomski.com")
        val iconUri = Uri.parse("favicon.ico")
        val identityName = "juraj_diplomski"

        val walletAdapter = MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = solanaUri,
                iconUri = iconUri,
                identityName = identityName
            ),
            scenarioProvider = AssociationScenarioProvider()
        )

        Log.d("TicketDApp", "Wallet adapter initialized")

        walletAdapter.transact(sender) { authResult ->

        Log.d("TicketDApp", "Transaction authorization result: $authResult")

        // Get user public key from auth result
        val userPublicKey = SolanaPublicKey(authResult.accounts.first().publicKey)
        Log.d("TicketDApp", "User public key: $userPublicKey")

        // Update seeds to include "ticket" and the user's public key
        val seeds = listOf("ticket".encodeToByteArray())

        val result = ProgramDerivedAddress.find(seeds, programId)
        val accountPDA = result.getOrNull() ?: run {
            Log.e("TicketDApp", "Failed to find Program Derived Address")
            return@transact
        }
        Log.d("TicketDApp", "Account PDA found: $accountPDA")

        val rpcClient = SolanaRpcClient("https://api.devnet.solana.com", KtorNetworkDriver())
        Log.d("TicketDApp", "RPC Client initialized")

        // Fetch latest blockhash
        val blockhashResponse = rpcClient.getLatestBlockhash()
        val latestBlockhash = blockhashResponse.result?.blockhash ?: run {
            Log.e("TicketDApp", "Failed to get latest blockhash")
            return@transact
        }
        Log.d("TicketDApp", "Latest blockhash retrieved: $latestBlockhash")

        // Recipient address
        val recipientAddress = SolanaPublicKey.from("HJ6BiGSGJM6dwCypk2uMPkgnHm82JC32YPuy4yhv93qN")
        Log.d("TicketDApp", "Recipient address: $recipientAddress")

        // Encode the instruction data
        val encodedInstructionData = Borsh.encodeToByteArray(
            AnchorInstructionSerializer("create_ticket"),
            Args_ticket(ticket.id, ticket.name, ticket.departureTime, ticket.arrivalTime, ticket.price)
        )
        Log.d("TicketDApp", "Encoded instruction data: ${encodedInstructionData.contentToString()}")

        // Create ticket instruction
        val ticketInstruction = TransactionInstruction(
            programId,
            listOf(
                AccountMeta(accountPDA, false, true), // PDA is writable but not signer
                AccountMeta(userPublicKey, true, true) // User is signer and writable
            ),
            encodedInstructionData
        )
        Log.d("TicketDApp", "Ticket instruction created: $ticketInstruction")

        // Create transfer instruction
        val transferInstruction = SystemProgram.transfer(
            userPublicKey,
            recipientAddress,
            (ticket.price * 1_000_000_000.0).toLong()
        )
        Log.d("TicketDApp", "Transfer instruction created: $transferInstruction")

        // Build transaction message
        val transactionMessage = Message.Builder()
            .addInstruction(transferInstruction)
            .addInstruction(ticketInstruction)
            .setRecentBlockhash(latestBlockhash)
            .build()

        // Create and sign the transaction
        val transaction = Transaction(transactionMessage)
        Log.d("TicketDApp", "Transaction created: $transaction")

        // Sign and send the transaction
        signAndSendTransactions(arrayOf(transaction.serialize()))
        Log.d("TicketDApp", "Transaction sent for signing and sending")
    }
}




@Serializable
class Args_ticket(val id:String,val name:String,val departureTime:String, val arrivalTime:String, val price:Float)