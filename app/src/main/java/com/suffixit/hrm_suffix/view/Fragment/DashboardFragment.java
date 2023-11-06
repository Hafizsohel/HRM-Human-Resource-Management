package com.suffixit.hrm_suffix.view.Fragment;


import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import com.squareup.picasso.Picasso;
import com.suffixit.hrm_suffix.R;
import com.suffixit.hrm_suffix.databinding.FragmentDashboadBinding;

import com.suffixit.hrm_suffix.preference.AppPreference;
import com.suffixit.hrm_suffix.view.Activities.LoginActivity;

import de.hdodenhof.circleimageview.CircleImageView;


public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboadFragment";
    private FragmentDashboadBinding binding;
    private AppPreference localStorage;
    private CircleImageView profileImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDashboadBinding.inflate(inflater, container, false);


        localStorage = new AppPreference(requireContext());
        String userId = localStorage.getUserName();
        Log.d(TAG, "userName: "+userId);

        CollectionReference usersCollection = FirebaseFirestore.getInstance().collection("Users");
        Query query = usersCollection.whereEqualTo("username", userId);

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    boolean userFound = false;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String name = document.getString("name");
                        String email = document.getString("Email");
                        String designation = document.getString("Designation");
                        String imageUrl = document.getString("profileImg");


                        binding. txtEmployeeId.setText("User ID: " + userId);
                        binding.txtEmployeeName.setText("Name: " + name);
                        binding.txtEmployeeMail.setText("Email: "+ email);
                        binding.txtEmployeeDesignation.setText("Designation: "+ designation);


                        userFound = true;

                        // Load and display the profile image using Picasso
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Picasso.get().load(imageUrl).into(profileImageView);
                        }

                        break;
                    }

                    if (!userFound) {
                      }
                } else {
                   task.getException().printStackTrace();
                }
            }
        });

        profileImageView = binding.imgEmployeeProfile;
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.cardViewEmployee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.FrameLayoutID, new EmployeeFragment()).commit();
            }
        });

        binding.cardViewLeave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLogoutConfirmationDialog();
            }
        });

        binding.cardViewAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.FrameLayoutID, new AttendanceFragment()).commit();
            }
        });
        binding.imgScrumMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.FrameLayoutID, new ReportFragment()).commit();
            }
        });
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setMessage("Are you sure you want to log out?");
        builder.setPositiveButton("Logout", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                navigateToLogoutPage();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void navigateToLogoutPage() {
        // Clear saved username and password
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }
}
