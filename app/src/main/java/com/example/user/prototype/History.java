package com.example.user.prototype;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class History extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference();
        // Read from the database
        final ArrayList<Total> totalList = new ArrayList<>();
        final ListView listview = (ListView) findViewById(R.id.listTotal);
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, totalList);

        myRef.addValueEventListener(new ValueEventListener() {
            TextView total;
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                totalList.clear();

                for(DataSnapshot ds : dataSnapshot.getChildren()) {
                    Map<String, Total> map = (Map<String, Total>) ds.getValue();
                    totalList.addAll(map.values());

                }
                listview.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                total = findViewById(R.id.Total);
                total.setText(error.toString());
            }
        });
    }
}
