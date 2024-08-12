package com.example.messagesenderapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class DisplayMessageActivity : AppCompatActivity() {

    private val CHANNEL_ID = "message_channel"
    private val REQUEST_CODE_NOTIFICATION_PERMISSION = 1

    private lateinit var replyInput: TextInputEditText
    private lateinit var replyButton: Button
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messagesAdapter: MessagesAdapter

    private val messages = mutableListOf<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_message)

        val message = intent.getStringExtra("MESSAGE")
        val messageTextView: TextView = findViewById(R.id.messageTextView)
        messageTextView.text = message ?: "No message received"

        replyInput = findViewById(R.id.replyInput)
        replyButton = findViewById(R.id.replyButton)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)

        // Initialize messages list
        messages.add(Message("Received", message ?: "No message received", isBot = true))
        messagesAdapter = MessagesAdapter(messages)
        messagesRecyclerView.adapter = messagesAdapter
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)

        replyButton.setOnClickListener {
            val reply = replyInput.text.toString()
            if (reply.isNotEmpty()) {
                messages.add(Message("You", reply, isBot = false))
                messagesAdapter.notifyItemInserted(messages.size - 1)
                replyInput.text?.clear()

                val resultIntent = Intent()
                resultIntent.putExtra("REPLY", reply)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }

        createNotificationChannel()
        sendNotification(message ?: "No message received")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Message Channel"
            val descriptionText = "Channel for Message Notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
                buildAndSendNotification(message)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION_PERMISSION)
            }
        } else {
            buildAndSendNotification(message)
        }
    }

    private fun buildAndSendNotification(message: String) {
        try {
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("New Message")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(this)) {
                notify(1, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val message = intent.getStringExtra("MESSAGE") ?: "No message received"
                buildAndSendNotification(message)
            }
        }
    }
}
