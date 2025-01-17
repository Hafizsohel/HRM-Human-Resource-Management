package com.suffixit.hrm_suffix.view.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.suffixit.hrm_suffix.Adapter.AttendanceAdapter;
import com.suffixit.hrm_suffix.R;
import com.suffixit.hrm_suffix.databinding.FragmentAttendanceBinding;
import com.suffixit.hrm_suffix.models.AttendanceModel;
import com.suffixit.hrm_suffix.models.ReportModel;
import com.suffixit.hrm_suffix.preference.AppPreference;
import com.suffixit.hrm_suffix.view.Activities.MainActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceFragment extends Fragment {
    private static final String TAG = "AttendanceFragment";
    private FragmentAttendanceBinding binding;
    private DatabaseReference databaseReference;
    private AttendanceAdapter adapter;
    private List<AttendanceModel> attendanceModelList= new ArrayList();;
    private SharedPreferences sharedPreferences;
    private AppPreference localStorage;
    private boolean hasCheckedInForDay = false;
    private static final String PREFS_KEY_CHECKIN_TIME = "checkin_time";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAttendanceBinding.inflate(getLayoutInflater());

        sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        localStorage = new AppPreference(requireContext());
        String userName = localStorage.getUserName();
        Log.d(TAG, "userName: "+userName);


        CollectionReference usersCollection = FirebaseFirestore.getInstance().collection("Users");
        com.google.firebase.firestore.Query query = usersCollection.whereEqualTo("username", userName);

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    boolean userFound = false;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String name = document.getString("name");

                        Log.d(TAG, "onComplete: "+name);
                        userFound = true;
                        break;
                    }

                    if (!userFound) {
                    }
                } else {
                    task.getException().printStackTrace();
                }
            }
        });

        autoCheckoutAtMidnight();
        setUpOnBackPressed();

        binding.recyclerViewId.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AttendanceAdapter();
        binding.recyclerViewId.setAdapter(adapter);

        String savedCheckInTime = sharedPreferences.getString(PREFS_KEY_CHECKIN_TIME, "");

        if (!savedCheckInTime.isEmpty()) {
            binding.cardViewCheckIn.setVisibility(View.GONE);
            binding.cardViewCheckout.setVisibility(View.VISIBLE);
            binding.savedCheckInTimeTextView.setText("Check-in time: " + savedCheckInTime);
        }

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("username");
        fetchDataFromFirebase(userName);

        binding.cardViewCheckIn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!hasCheckedInForDay) {
                    String checkInTime = getCurrentTime();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(PREFS_KEY_CHECKIN_TIME, checkInTime);
                    editor.apply();


                    String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    String currentDay = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());
                    String checkoutTime = "00.00";
                    String totalHrs = "00.00";
                    String username = userName;

                    // Check if the user has already checked in for the current date in Firebase
                    checkIfUserAlreadyCheckedIn(username, currentDate, new OnCheckedInCallback() {
                        @Override
                        public void onCheckedIn(boolean checkedIn) {
                            if (checkedIn) {
                                binding.cardViewCheckIn.setVisibility(View.VISIBLE);
                                Toast.makeText(getActivity(), "You have already checked in for the day", Toast.LENGTH_SHORT).show();
                            } else {
                                binding.cardViewCheckIn.setVisibility(View.VISIBLE);
                                usersCollection.whereEqualTo("username", userName).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            boolean userFound = false;
                                            for (QueryDocumentSnapshot document : task.getResult()) {
                                                String name = document.getString("name");
                                                userFound = true;

                                                String fullName = name;

                                                AttendanceModel userDetails = new AttendanceModel(username, fullName, currentDate, currentDay, checkInTime, checkoutTime, totalHrs);
                                                attendanceModelList.add(userDetails);

                                                if (attendanceModelList != null) {
                                                    adapter.setData(attendanceModelList);
                                                }
                                                DatabaseReference checkInRef = FirebaseDatabase.getInstance().getReference().child("Users").child("username").push();
                                                checkInRef.setValue(userDetails)
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                binding.cardViewCheckIn.setVisibility(View.GONE);
                                                                binding.cardViewCheckout.setVisibility(View.VISIBLE);
                                                                    Toast.makeText(getActivity(), "Check-in successful", Toast.LENGTH_SHORT).show();
                                                                }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Toast.makeText(getActivity(), "Failed to check in", Toast.LENGTH_SHORT).show();
                                                            }
                                                        });

                                                break;
                                            }

                                            if (!userFound) {
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });

        binding.cardViewCheckout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String checkInTime = sharedPreferences.getString(PREFS_KEY_CHECKIN_TIME, "");

                if (!checkInTime.isEmpty()) {
                    String checkoutTime = getCurrentTime();
                    double totalHrs = calculateTotalHours(checkInTime, checkoutTime);

                    String finalCheckoutTime = checkoutTime;
                    databaseReference.orderByChild("checkInTime").equalTo(checkInTime)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                        snapshot.getRef().child("checkoutTime").setValue(finalCheckoutTime);
                                        snapshot.getRef().child("totalHrs").setValue(String.format("%.2f", totalHrs));
                                    }
                                    if (attendanceModelList != null) {
                                        adapter.setData(attendanceModelList);
                                    }

                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.remove(PREFS_KEY_CHECKIN_TIME);
                                    editor.apply();

                                    binding.cardViewCheckIn.setVisibility(View.VISIBLE);
                                    binding.cardViewCheckout.setVisibility(View.GONE);

                                    updateTotalHrsTextView(String.format("%.2f", totalHrs));
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Toast.makeText(getActivity(), "Failed to update checkout time", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Toast.makeText(getActivity(), "No check-in time found", Toast.LENGTH_SHORT).show();
                }
            }
        });

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                updateClock();
                handler.postDelayed(this, 1000);
                updateDateAndDay();
            }
        });
        return binding.getRoot();
    }

    private double calculateTotalHours(String checkInTime, String checkoutTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        try {
            Date checkInDate = sdf.parse(checkInTime);
            Date checkoutDate = sdf.parse(checkoutTime);

            // Check if checkout time is earlier than check-in time
            if (checkoutDate.before(checkInDate)) {
                // Add 24 hours to checkout date if it's earlier
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(checkoutDate);
                calendar.add(Calendar.HOUR_OF_DAY, 24);
                checkoutDate = calendar.getTime();
            }
            long timeDifference = checkoutDate.getTime() - checkInDate.getTime();
            return (double) timeDifference / (60 * 60 * 1000);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    //If the time is 11:59 PM performAutoCheckout method, which performs the checkout process.
    private void performAutoCheckout() {
        String checkInTime = sharedPreferences.getString(PREFS_KEY_CHECKIN_TIME, "");

        if (!checkInTime.isEmpty()) {
            String checkoutTime = "11:59 PM";
            double totalHrs = calculateTotalHours(checkInTime, checkoutTime);

            databaseReference.orderByChild("checkInTime").equalTo(checkInTime)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                snapshot.getRef().child("checkoutTime").setValue(checkoutTime);
                                snapshot.getRef().child("totalHrs").setValue(String.format("%.2f", totalHrs));
                            }

                            if (attendanceModelList != null) {
                                adapter.setData(attendanceModelList);
                            }

                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.remove(PREFS_KEY_CHECKIN_TIME);
                            editor.apply();
                            binding.cardViewCheckIn.setVisibility(View.VISIBLE);
                            binding.cardViewCheckout.setVisibility(View.GONE);

                            updateTotalHrsTextView(String.format("%.2f", totalHrs));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Toast.makeText(getActivity(), "Failed to update checkout time", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getActivity(), "No check-in time found", Toast.LENGTH_SHORT).show();
        }
    }

    //automatically check the time every minute
    private void autoCheckoutAtMidnight() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String currentTime = getCurrentTime();
                // Check if it's 11:59 PM to trigger auto-checkout
                if (currentTime.equals("11:59 PM")) {
                    performAutoCheckout();
                }

                handler.postDelayed(this, 60000);
            }
        }, 60000);
    }

    private void updateTotalHrsTextView(String totalHrs) {
        View view = binding.getRoot();

        if (view != null) {
            TextView txtTotalHrs = view.findViewById(R.id.txtTotalHrs);

            if (txtTotalHrs != null) {
                txtTotalHrs.setText("Total hrs: " + totalHrs);
            } else {
                Toast.makeText(getActivity(), "txtTotalHrs is null", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), "Root view is null", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchDataFromFirebase(String userId) {
        binding.pleaseWaitText.setVisibility(View.VISIBLE);
        Query query = databaseReference.orderByChild("userId").equalTo(userId);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                binding.pleaseWaitText.setVisibility(View.GONE);
                attendanceModelList.clear();

                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Log.d("FirebaseData", snapshot.getValue().toString());
                        AttendanceModel attendance = snapshot.getValue(AttendanceModel.class);
                        attendanceModelList.add(attendance);
                    }
                    if (attendanceModelList != null) {
                        adapter.setData(attendanceModelList);
                    } else {
                        showNoDataMessage();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                binding.pleaseWaitText.setVisibility(View.GONE);
                Toast.makeText(getActivity(), "Failed to fetch data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNoDataMessage() {
        binding.noDataTextView.setVisibility(View.VISIBLE);
    }

    private String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(calendar.getTime());
    }

    private void updateDateAndDay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());

        String currentDate = dateFormat.format(new Date());
        String currentDay = dayFormat.format(new Date());

        binding.dateTextView.setText(currentDate);
        binding.dayTextView.setText(currentDay);
    }

    private void updateClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        binding.clockTextView.setText(currentTime);
    }

    private void setUpOnBackPressed() {
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isAdded()) {
                    setEnabled(false);
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);
        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void onDestroy() {
                onBackPressedCallback.remove();
            }
        });
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.Toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.FrameLayoutID, new DashboadFragment()).commit();
            }
        });
    }
    interface OnCheckedInCallback {
        void onCheckedIn(boolean checkedIn);
    }
    private void checkIfUserAlreadyCheckedIn(String username, String currentDate, OnCheckedInCallback callback) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("username");
        databaseReference.orderByChild("userId").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean checkedIn = false;
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    if (data.child("date").getValue(String.class).equals(currentDate)) {
                        checkedIn = true;
                        break;
                    }
                }
                callback.onCheckedIn(checkedIn);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
}
