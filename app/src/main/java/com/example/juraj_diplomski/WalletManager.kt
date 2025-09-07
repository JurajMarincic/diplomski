package com.example.juraj_diplomski

import android.net.Uri
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.funkatronics.encoders.Base58
import com.funkatronics.kborsh.Borsh
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.programs.SystemProgram
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.solana.serialization.AnchorInstructionSerializer
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class WalletManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: WalletManager? = null

        fun getInstance(): WalletManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WalletManager().also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val solanaUri = Uri.parse("https://example.org")
    val iconUri = Uri.parse("res/drawable/transfer2.png")
    val identityName = "juraj_diplomski"

    private val connectionIdentity = ConnectionIdentity(
            identityUri = solanaUri,
            iconUri = iconUri,
            identityName = identityName
        )

    private var walletAdapter: MobileWalletAdapter? = null
    private var activityResultSender: ActivityResultSender? = null
    fun initialize(activity: FragmentActivity)
    {
        activityResultSender = ActivityResultSender(activity)
        walletAdapter = MobileWalletAdapter(connectionIdentity = connectionIdentity)
    }

    fun connectWallet(){
        scope.launch {
            val sender = activityResultSender

            if (walletAdapter == null) {
                walletAdapter = MobileWalletAdapter(connectionIdentity = connectionIdentity)
            }

            val adapter = walletAdapter
            val result = adapter?.connect(sender!!)

            when (result) {
                is TransactionResult.Success -> {
                    val authResult = result.authResult
                    val publicKey = authResult.accounts.firstOrNull()?.publicKey

                    if (publicKey != null) {
                        val publicKeyString = Base58.encodeToString(publicKey)
                        Log.d("TAG", "Wallet connected successfully. Public key: $publicKeyString")
                        Log.d("TAG", "Wallet connection completed")

                    }
                }

                is TransactionResult.NoWalletFound -> {
                    Log.w("TAG", "No MWA compatible wallet app found on device")
                }

                is TransactionResult.Failure -> {
                    Log.e("TAG", "Wallet connection failed")
                }

                else -> {
                    Log.e("TAG", "Connection succeeded but no public key received")
                }
            }
        }
    }

    fun sendTransaction(ticket: Ticket) {
        scope.launch {
            val sender = activityResultSender
            val adapter = walletAdapter
            val programId = SolanaPublicKey.from("F75bTjnaqScc9VZz6p5dKxFyxdBNQ48g7UURVZCwTSyH")

            adapter?.transact(sender!!) { authResult ->

                Log.d("TicketDApp", "Transact callback entered")
                val userPublicKey = SolanaPublicKey(authResult.accounts.first().publicKey)
                Log.d("TicketDApp", "User public key: $userPublicKey")
                Log.d("TicketDApp", "Transaction authorization result: $authResult")

                // Update seeds to include "ticket" and the user's public key
                val seeds = listOf("ticket".encodeToByteArray())

                val result = ProgramDerivedAddress.find(seeds, programId)
                val accountPDA = result.getOrNull()
                Log.d("TicketDApp", "Account PDA found: $accountPDA")

                val encodedInstructionData = Borsh.encodeToByteArray(
                    AnchorInstructionSerializer("create_ticket"),
                    Args_ticket(
                        ticket.id,
                        ticket.name,
                        ticket.departureTime,
                        ticket.arrivalTime,
                        ticket.price
                    )
                )

                Log.d("TicketDApp", "Encoded instruction data: ${encodedInstructionData.contentToString()}")

                // Create ticket instruction
                val ticketInstruction = TransactionInstruction(
                    programId,
                    listOf(
                        AccountMeta(accountPDA!!, false, true), // PDA is writable but not signer
                        AccountMeta(userPublicKey, true, true), // User is signer and writable
                        AccountMeta(
                            SolanaPublicKey.from("AG9e7iuDyZ5geTDLna6XAmNirtqdbHtrZmCUtma2bu7R"),
                            false,
                            true
                        ), // fee collector (writable)
                        AccountMeta(SystemProgram.PROGRAM_ID, false, false) // system program
                    ),
                    encodedInstructionData
                )
                Log.d("TicketDApp", "Ticket instruction created: $ticketInstruction")


                val rpcClient =
                    SolanaRpcClient("https://api.devnet.solana.com", KtorNetworkDriver())
                Log.d("TicketDApp", "RPC Client initialized")

                // Fetch latest blockhash
                val blockhashResponse = rpcClient.getLatestBlockhash()
                Log.d("TicketDApp", "Latest blockhash retrieved: ${blockhashResponse.result!!.blockhash}")

                // Build transaction message
                val transactionMessage = Message.Builder()
                    .addInstruction(ticketInstruction)
                    .setRecentBlockhash(blockhashResponse.result!!.blockhash)
                    .build()

                // Create and sign the transaction
                val unsignedTransaction = Transaction(transactionMessage)
                Log.d("TicketDApp", "Transaction created: $unsignedTransaction")

                // Sign and send the transaction
                signAndSendTransactions(arrayOf(unsignedTransaction.serialize()))
                Log.d("TicketDApp", "Transaction sent for signing and sending")
            }
        }
    }
    @Serializable
    class Args_ticket(
        val id: String,
        val name: String,
        val departureTime: String,
        val arrivalTime: String,
        val price: Float
    )
}

