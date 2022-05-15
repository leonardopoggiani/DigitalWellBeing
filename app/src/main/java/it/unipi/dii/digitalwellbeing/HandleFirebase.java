package it.unipi.dii.digitalwellbeing;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.kontakt.sdk.android.common.profile.RemoteBluetoothDevice;

import it.unipi.dii.digitalwellbeing.MainActivity;

public class HandleFirebase {

    public void insert (DatabaseReference db, RemoteBluetoothDevice beacon, Context context){
        //create an unique id
        String id = db.push().getKey();
        //Saving the beacon object
        db.child(id).setValue(beacon);
        //displaying a success toast
        Toast.makeText(context, "Insert firebase", Toast.LENGTH_LONG).show();

    }
}