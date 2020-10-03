package com.editiorsstack.chatapp;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String messageReceiverID;
    private String messageSenderID;
    private TextView userName;
    private CircleImageView userImage;
    private DatabaseReference RootRef;
    private ImageButton SendMessageButton;
    private EditText MessageInputText;
    private final List<Messages> messagesList = new ArrayList<>();
    private MessageAdapter mMessageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        messageSenderID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();

        messageReceiverID = Objects.requireNonNull(Objects.requireNonNull(getIntent().getExtras()).get("visit_user_id")).toString();
        String messageReceiverName = Objects.requireNonNull(getIntent().getExtras().get("visit_user_name")).toString();
        String messageReceiverImage = Objects.requireNonNull(getIntent().getExtras().get("visit_image")).toString();

        intializeControllers();

        userName.setText(messageReceiverName);
        Picasso.get().load(messageReceiverImage).placeholder(R.drawable.profile_image).into(userImage);

        SendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendMessage();

            }
        });
    }

    private void intializeControllers() {
        Toolbar chatToolBar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(chatToolBar);

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater =  (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert layoutInflater != null;
        View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null);
        actionBar.setCustomView(actionBarView);

        userImage = findViewById(R.id.custom_profile_image);
        userName = findViewById(R.id.custom_profile_name);
        TextView userLastSeen = findViewById(R.id.custom_user_last_seen);

        SendMessageButton = findViewById(R.id.send_message_btn);
        MessageInputText = findViewById(R.id.input_message);

        mMessageAdapter = new MessageAdapter(messagesList);
        RecyclerView userMessagesList = findViewById(R.id.private_messages_list_of_users);

        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(mLinearLayoutManager);
        userMessagesList.setAdapter(mMessageAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        RootRef.child("Messages").child(messageSenderID).child(messageReceiverID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                        Messages messages = dataSnapshot.getValue(Messages.class);
                        messagesList.add(messages);
                        mMessageAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {}
                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}
                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {}
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
    }

    private void SendMessage() {
        String messageText = MessageInputText.getText().toString();

        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "Please type a message.", Toast.LENGTH_SHORT).show();
        } else {
            byte [] key =AES.getKey();
            byte [] arr = messageText.getBytes();
            Aes_128_bit_algorithm obj =new Aes_128_bit_algorithm();
            byte []tmp=obj.encrypt(arr,key);
            StringBuilder message = new StringBuilder();
            for (byte b : tmp) {
                message.append(Integer.toString(b)).append("#");
            }

            String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
            String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

            DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                    .child(messageSenderID).child(messageReceiverID).push();

            String messagePushID = userMessageKeyRef.getKey();

            Map<String, String> messageTextBody = new HashMap<>();
            messageTextBody.put("message", message.toString());
            messageTextBody.put("type", "text");
            messageTextBody.put("from", messageSenderID);

            Map<String, Object> messageBodyDetails = new HashMap<>();
            messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
            messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        //Toast.makeText(ChatActivity.this, "Message Sent.", Toast.LENGTH_SHORT).show();
                    } else {
                        String message = Objects.requireNonNull(task.getException()).getMessage();
                        //Toast.makeText(ChatActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                    MessageInputText.setText("");

                }
            });
        }

    }
}
